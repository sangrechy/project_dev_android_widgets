package com.sangrechy.devwidgets.data.network

import com.sangrechy.devwidgets.data.model.AccountStatus
import com.sangrechy.devwidgets.data.model.AppUpdateInfo
import com.sangrechy.devwidgets.data.model.GitHubEvent
import com.sangrechy.devwidgets.data.model.GitHubLanguageStat
import com.sangrechy.devwidgets.data.model.GitHubStats
import com.sangrechy.devwidgets.data.model.LeetCodeStats
import com.sangrechy.devwidgets.data.model.LeetCodeSubmission
import com.sangrechy.devwidgets.data.model.ProblemTopicTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class DevApiService(private val okHttpClient: OkHttpClient = defaultClient()) {

    companion object {
        private fun defaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()
        }
    }

    suspend fun fetchLeetCodeStats(username: String): LeetCodeStats = withContext(Dispatchers.IO) {
        val trimmed = username.trim()
        if (trimmed.isBlank()) return@withContext LeetCodeStats(status = AccountStatus.NOT_CONFIGURED)

        val query = """
            query getUserProfile(${'$'}username: String!) {
              matchedUser(username: ${'$'}username) {
                username
                submitStatsGlobal {
                  acSubmissionNum {
                    difficulty
                    count
                  }
                }
                submissionCalendar
                tagProblemCounts {
                  advanced {
                    tagName
                    tagSlug
                    problemsSolved
                  }
                  intermediate {
                    tagName
                    tagSlug
                    problemsSolved
                  }
                  fundamental {
                    tagName
                    tagSlug
                    problemsSolved
                  }
                }
              }
              recentAcSubmissionList(username: ${'$'}username, limit: 100) {
                title
                titleSlug
                timestamp
              }
            }
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("query", query)
            put("variables", JSONObject().apply { put("username", trimmed) })
        }

        val request = Request.Builder()
            .url("https://leetcode.com/graphql")
            .header("Content-Type", "application/json")
            .header("Referer", "https://leetcode.com")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val (statusFailed, failMsg, body) = okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Triple(true, "HTTP ${response.code}: Could not connect to LeetCode", null)
                } else {
                    val b = response.body?.string()
                    if (b == null) {
                        Triple(true, "Empty response from LeetCode", null)
                    } else {
                        Triple(false, "", b)
                    }
                }
            }

            if (statusFailed || body == null) {
                return@withContext LeetCodeStats(
                    username = trimmed,
                    status = AccountStatus.FAILED,
                    statusMessage = failMsg
                )
            }

            val root = JSONObject(body)
            val data = root.optJSONObject("data")
            val matchedUser = data?.optJSONObject("matchedUser")

            if (matchedUser == null) {
                return@withContext LeetCodeStats(
                    username = trimmed,
                    status = AccountStatus.FAILED,
                    statusMessage = "LeetCode user '$trimmed' not found"
                )
            }

            var total = 0
            var easy = 0
            var med = 0
            var hard = 0

            matchedUser.optJSONObject("submitStatsGlobal")?.optJSONArray("acSubmissionNum")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val diff = item.optString("difficulty")
                    val count = item.optInt("count", 0)
                    when (diff) {
                        "All" -> total = count
                        "Easy" -> easy = count
                        "Medium" -> med = count
                        "Hard" -> hard = count
                    }
                }
            }
            if (total == 0 && (easy + med + hard > 0)) {
                total = easy + med + hard
            } else {
                total = maxOf(total, easy + med + hard)
            }

            val calendarMap = mutableMapOf<Long, Int>()
            val rawCalendar = matchedUser.optString("submissionCalendar", "{}")
            try {
                val calJson = JSONObject(rawCalendar)
                calJson.keys().forEach { tsStr ->
                    tsStr.toLongOrNull()?.let { ts ->
                        calendarMap[ts] = calJson.optInt(tsStr, 0)
                    }
                }
            } catch (_: Exception) {}

            val subs = mutableListOf<LeetCodeSubmission>()
            data.optJSONArray("recentAcSubmissionList")?.let { sArr ->
                for (i in 0 until sArr.length()) {
                    val sObj = sArr.getJSONObject(i)
                    subs.add(
                        LeetCodeSubmission(
                            title = sObj.optString("title", "Submission"),
                            titleSlug = sObj.optString("titleSlug", ""),
                            statusDisplay = "Accepted",
                            lang = "Code",
                            timestamp = sObj.optLong("timestamp", 0L)
                        )
                    )
                }
            }

            val topicTags = mutableListOf<ProblemTopicTag>()
            matchedUser.optJSONObject("tagProblemCounts")?.let { tpc ->
                val categories = listOf(
                    "Advanced" to tpc.optJSONArray("advanced"),
                    "Intermediate" to tpc.optJSONArray("intermediate"),
                    "Fundamental" to tpc.optJSONArray("fundamental")
                )
                for ((cat, arr) in categories) {
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            val tagName = item.optString("tagName")
                            val solved = item.optInt("problemsSolved", 0)
                            if (tagName.isNotBlank() && solved > 0) {
                                topicTags.add(ProblemTopicTag(name = tagName, solvedCount = solved, category = cat))
                            }
                        }
                    }
                }
            }

            val actualUsername = matchedUser.optString("username", trimmed)
            LeetCodeStats(
                username = actualUsername,
                totalSolved = total,
                easySolved = easy,
                mediumSolved = med,
                hardSolved = hard,
                submissionCalendar = calendarMap,
                recentSubmissions = subs,
                topicTags = topicTags,
                status = AccountStatus.CONNECTED,
                statusMessage = "Linked successfully ($total solved)"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            LeetCodeStats(
                username = trimmed,
                status = AccountStatus.FAILED,
                statusMessage = "Error: ${e.localizedMessage ?: "Connection failed"}"
            )
        }
    }

    suspend fun fetchGitHubStats(username: String, token: String = ""): GitHubStats = withContext(Dispatchers.IO) {
        val trimmed = username.trim()
        if (trimmed.isBlank()) return@withContext GitHubStats(status = AccountStatus.NOT_CONFIGURED)

        try {
            val userCheckReq = Request.Builder()
                .url("https://api.github.com/users/$trimmed")
                .header("User-Agent", "DevWidgets-Android")
                .apply {
                    if (token.isNotBlank()) header("Authorization", "Bearer $token")
                }
                .build()

            val (statusFailed, failMsg) = okHttpClient.newCall(userCheckReq).execute().use { checkResp ->
                if (checkResp.code == 404) {
                    Pair(true, "GitHub user '$trimmed' not found")
                } else if (!checkResp.isSuccessful && checkResp.code != 403) {
                    Pair(true, "GitHub returned HTTP ${checkResp.code}")
                } else {
                    Pair(false, "")
                }
            }
            if (statusFailed) {
                return@withContext GitHubStats(
                    username = trimmed,
                    status = AccountStatus.FAILED,
                    statusMessage = failMsg
                )
            }

            val recentEvents = fetchGitHubRecentEvents(trimmed, token)
            val topLanguages = fetchGitHubLanguages(trimmed, token)
            val (calendarMap, totalContribs, commits, prs, issues, reviews) = fetchGitHubContributions(trimmed, token)

            GitHubStats(
                username = trimmed,
                totalContributions = totalContribs,
                commitsCount = commits,
                pullRequestsCount = prs,
                issuesCount = issues,
                reviewsCount = reviews,
                contributionDays = calendarMap,
                recentEvents = recentEvents,
                topLanguages = topLanguages,
                status = AccountStatus.CONNECTED,
                statusMessage = "Linked successfully ($totalContribs contributions)"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            GitHubStats(
                username = trimmed,
                status = AccountStatus.FAILED,
                statusMessage = "Error: ${e.localizedMessage ?: "Connection failed"}"
            )
        }
    }

    private fun fetchGitHubRecentEvents(username: String, token: String): List<GitHubEvent> {
        val reqBuilder = Request.Builder()
            .url("https://api.github.com/users/$username/events?per_page=100")
            .header("Accept", "application/vnd.github.v3+json")
            .header("User-Agent", "DevWidgets-Android")

        if (token.isNotBlank()) {
            reqBuilder.header("Authorization", "Bearer $token")
        }

        val events = mutableListOf<GitHubEvent>()
        try {
            okHttpClient.newCall(reqBuilder.build()).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    val arr = JSONArray(body)
                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        val type = item.optString("type", "Activity")
                        val repo = item.optJSONObject("repo")?.optString("name", "repo") ?: "repo"
                        val createdAt = item.optString("created_at", "")

                        val summary = when (type) {
                            "PushEvent" -> {
                                val count = item.optJSONObject("payload")?.optJSONArray("commits")?.length() ?: 1
                                "Pushed $count commit(s)"
                            }
                            "PullRequestEvent" -> {
                                val action = item.optJSONObject("payload")?.optString("action", "action")
                                "PR: $action"
                            }
                            "IssuesEvent" -> {
                                val action = item.optJSONObject("payload")?.optString("action", "action")
                                "Issue: $action"
                            }
                            "WatchEvent" -> "Starred repo"
                            "ForkEvent" -> "Forked repo"
                            "CreateEvent" -> "Created branch/tag"
                            else -> type.replace("Event", "")
                        }

                        events.add(
                            GitHubEvent(
                                type = type,
                                repoName = repo.substringAfter("/"),
                                actionSummary = summary,
                                createdAt = createdAt
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return events
    }

    private data class GitHubContribResult(
        val days: Map<String, Int>,
        val total: Int,
        val commits: Int,
        val prs: Int,
        val issues: Int,
        val reviews: Int
    )

    private fun fetchGitHubContributions(username: String, token: String): GitHubContribResult {
        // First try the public contributions API (no token required, returns full grid)
        try {
            val req = Request.Builder()
                .url("https://github-contributions-api.jogruber.de/v4/$username?y=last")
                .header("User-Agent", "DevWidgets-Android")
                .build()

            okHttpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: "{}"
                    val json = JSONObject(body)
                    val total = json.optJSONObject("total")?.optInt("lastYear", 0) ?: 0
                    val contribDays = mutableMapOf<String, Int>()

                    val contribArr = json.optJSONArray("contributions")
                    if (contribArr != null) {
                        for (i in 0 until contribArr.length()) {
                            val cItem = contribArr.getJSONObject(i)
                            val date = cItem.optString("date")
                            val count = cItem.optInt("count", 0)
                            if (date.isNotEmpty()) {
                                contribDays[date] = count
                            }
                        }
                    }

                    // Breakdown approximation from total
                    val commits = if (total > 0) (total * 0.70).toInt().coerceAtLeast(1) else 0
                    val prs = (total * 0.15).toInt()
                    val issues = (total * 0.08).toInt()
                    val reviews = (total * 0.07).toInt()

                    return GitHubContribResult(contribDays, total, commits, prs, issues, reviews)
                }
            }
        } catch (_: Exception) {}

        // Fallback: If user provided a GitHub token, query GitHub GraphQL API
        if (token.isNotBlank()) {
            try {
                val gql = """
                    query {
                      user(login: "$username") {
                        contributionsCollection {
                          contributionCalendar {
                            totalContributions
                            weeks {
                              contributionDays {
                                date
                                contributionCount
                              }
                            }
                          }
                          totalCommitContributions
                          totalPullRequestContributions
                          totalIssueContributions
                          totalPullRequestReviewContributions
                        }
                      }
                    }
                """.trimIndent()

                val jsonBody = JSONObject().apply { put("query", gql) }
                val req = Request.Builder()
                    .url("https://api.github.com/graphql")
                    .header("Authorization", "Bearer $token")
                    .header("User-Agent", "DevWidgets-Android")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                okHttpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val root = JSONObject(resp.body?.string() ?: "{}")
                        val collection = root.optJSONObject("data")?.optJSONObject("user")?.optJSONObject("contributionsCollection")
                        if (collection != null) {
                            val cal = collection.optJSONObject("contributionCalendar")
                            val total = cal?.optInt("totalContributions", 0) ?: 0
                            val days = mutableMapOf<String, Int>()
                            val weeks = cal?.optJSONArray("weeks")
                            if (weeks != null) {
                                for (w in 0 until weeks.length()) {
                                    val cDays = weeks.getJSONObject(w).optJSONArray("contributionDays") ?: continue
                                    for (d in 0 until cDays.length()) {
                                        val item = cDays.getJSONObject(d)
                                        days[item.getString("date")] = item.getInt("contributionCount")
                                    }
                                }
                            }
                            return GitHubContribResult(
                                days = days,
                                total = total,
                                commits = collection.optInt("totalCommitContributions", 0),
                                prs = collection.optInt("totalPullRequestContributions", 0),
                                issues = collection.optInt("totalIssueContributions", 0),
                                reviews = collection.optInt("totalPullRequestReviewContributions", 0)
                            )
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        return GitHubContribResult(emptyMap(), 0, 0, 0, 0, 0)
    }

    private fun fetchGitHubLanguages(username: String, token: String): List<GitHubLanguageStat> {
        val reqBuilder = Request.Builder()
            .url("https://api.github.com/users/$username/repos?per_page=50&sort=updated")
            .header("Accept", "application/vnd.github.v3+json")
            .header("User-Agent", "DevWidgets-Android")
        if (token.isNotBlank()) reqBuilder.header("Authorization", "Bearer $token")

        val langMap = mutableMapOf<String, Int>()
        try {
            okHttpClient.newCall(reqBuilder.build()).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: "[]"
                    val arr = JSONArray(body)
                    for (i in 0 until arr.length()) {
                        val r = arr.getJSONObject(i)
                        val l = r.optString("language")
                        if (l.isNotBlank() && l != "null") {
                            langMap[l] = (langMap[l] ?: 0) + 1
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val colors = mapOf(
            "Kotlin" to "#A97BFF", "Python" to "#3572A5", "Java" to "#B07219",
            "JavaScript" to "#F1E05A", "TypeScript" to "#3178C6", "C++" to "#F34B7D",
            "C" to "#555555", "HTML" to "#E34C26", "CSS" to "#563D7C",
            "Dart" to "#00B4AB", "Swift" to "#F05138", "Go" to "#00ADD8",
            "Rust" to "#DEA584", "Shell" to "#89E051", "Ruby" to "#701516"
        )
        return langMap.entries
            .sortedByDescending { it.value }
            .take(8)
            .map { (lang, count) ->
                GitHubLanguageStat(
                    name = lang,
                    repoCount = count,
                    colorHex = colors[lang] ?: "#3B82F6"
                )
            }
    }

    suspend fun checkForAppUpdate(currentVersion: String, token: String = ""): AppUpdateInfo = withContext(Dispatchers.IO) {
        val reqBuilder = Request.Builder()
            .url("https://api.github.com/repos/sangrechy/project_dev_android_widgets/releases/latest")
            .header("Accept", "application/vnd.github.v3+json")
            .header("User-Agent", "DevWidgets-Android")
        if (token.isNotBlank()) {
            reqBuilder.header("Authorization", "Bearer ${token.trim()}")
        }
        val req = reqBuilder.build()
        try {
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val errMsg = when (resp.code) {
                        403 -> "GitHub API rate limit exceeded. Add a GitHub token in Settings."
                        404 -> "Release repository not found."
                        else -> "HTTP ${resp.code}: Failed to reach GitHub release API"
                    }
                    return@withContext AppUpdateInfo(hasUpdate = false, error = errMsg)
                }
                val body = resp.body?.string() ?: "{}"
                val json = JSONObject(body)
                val tagName = json.optString("tag_name", "").trim()
                val name = json.optString("name", tagName)
                val notes = json.optString("body", "")
                val htmlUrl = json.optString("html_url", "https://github.com/sangrechy/project_dev_android_widgets/releases")
                var apkUrl = ""
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val a = assets.getJSONObject(i)
                        val aName = a.optString("name", "")
                        if (aName.endsWith(".apk")) {
                            apkUrl = a.optString("browser_download_url", "")
                            break
                        }
                    }
                }
                if (apkUrl.isBlank()) {
                    apkUrl = htmlUrl
                }

                val cleanRemote = tagName.removePrefix("v").trim()
                val cleanCurrent = currentVersion.removePrefix("v").trim()
                val hasUpdate = isVersionNewer(cleanRemote, cleanCurrent)

                AppUpdateInfo(
                    hasUpdate = hasUpdate,
                    latestVersion = tagName,
                    releaseName = name,
                    releaseNotes = notes,
                    downloadUrl = apkUrl,
                    releaseHtmlUrl = htmlUrl,
                    error = null
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            AppUpdateInfo(
                hasUpdate = false,
                error = e.localizedMessage ?: "Unable to connect to GitHub releases"
            )
        }
    }

    private fun isVersionNewer(remote: String, current: String): Boolean {
        if (remote.isBlank() || current.isBlank()) return false
        if (remote == current) return false
        val rParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val cParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(rParts.size, cParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
