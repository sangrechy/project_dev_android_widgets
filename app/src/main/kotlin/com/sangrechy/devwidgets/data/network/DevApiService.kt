package com.sangrechy.devwidgets.data.network

import com.sangrechy.devwidgets.data.model.AccountStatus
import com.sangrechy.devwidgets.data.model.GitHubEvent
import com.sangrechy.devwidgets.data.model.GitHubStats
import com.sangrechy.devwidgets.data.model.LeetCodeStats
import com.sangrechy.devwidgets.data.model.LeetCodeSubmission
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
              }
              recentAcSubmissionList(username: ${'$'}username, limit: 10) {
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

            val actualUsername = matchedUser.optString("username", trimmed)
            LeetCodeStats(
                username = actualUsername,
                totalSolved = total,
                easySolved = easy,
                mediumSolved = med,
                hardSolved = hard,
                submissionCalendar = calendarMap,
                recentSubmissions = subs,
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
            .url("https://api.github.com/users/$username/events?per_page=10")
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
}
