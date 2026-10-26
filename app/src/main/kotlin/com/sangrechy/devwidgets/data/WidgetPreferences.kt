package com.sangrechy.devwidgets.data

import android.content.Context
import android.content.SharedPreferences
import com.sangrechy.devwidgets.data.model.AccountStatus
import com.sangrechy.devwidgets.data.model.GitHubEvent
import com.sangrechy.devwidgets.data.model.GitHubLanguageStat
import com.sangrechy.devwidgets.data.model.GitHubStats
import com.sangrechy.devwidgets.data.model.LeetCodeSubmission
import com.sangrechy.devwidgets.data.model.LeetCodeStats
import com.sangrechy.devwidgets.data.model.ProblemTopicTag
import com.sangrechy.devwidgets.data.model.WidgetDashboardData
import org.json.JSONArray
import org.json.JSONObject

class WidgetPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var leetCodeUsername: String
        get() = prefs.getString(KEY_LEETCODE_USERNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LEETCODE_USERNAME, value.trim()).apply()

    var gitHubUsername: String
        get() = prefs.getString(KEY_GITHUB_USERNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GITHUB_USERNAME, value.trim()).apply()

    var gitHubToken: String
        get() = prefs.getString(KEY_GITHUB_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GITHUB_TOKEN, value.trim()).apply()

    var autoRefreshInterval: String
        get() = prefs.getString(KEY_REFRESH_INTERVAL, "1h") ?: "1h"
        set(value) = prefs.edit().putString(KEY_REFRESH_INTERVAL, value).apply()

    var ignoredUpdateVersion: String
        get() = prefs.getString(KEY_IGNORED_UPDATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_IGNORED_UPDATE, value).apply()

    fun clearAll() {
        prefs.edit()
            .remove(KEY_LEETCODE_USERNAME)
            .remove(KEY_GITHUB_USERNAME)
            .remove(KEY_GITHUB_TOKEN)
            .remove(KEY_CACHED_DASHBOARD)
            .apply()
    }

    fun saveDashboardData(data: WidgetDashboardData) {
        val root = JSONObject()
        root.put("lastUpdatedMillis", data.lastUpdatedMillis)

        // LeetCode
        val lcObj = JSONObject().apply {
            put("username", data.leetCode.username)
            put("totalSolved", data.leetCode.totalSolved)
            put("easySolved", data.leetCode.easySolved)
            put("mediumSolved", data.leetCode.mediumSolved)
            put("hardSolved", data.leetCode.hardSolved)
            put("status", data.leetCode.status.name)
            put("statusMessage", data.leetCode.statusMessage)

            val calObj = JSONObject()
            data.leetCode.submissionCalendar.forEach { (ts, count) ->
                calObj.put(ts.toString(), count)
            }
            put("calendar", calObj)

            val subsArr = JSONArray()
            data.leetCode.recentSubmissions.forEach { sub ->
                val sObj = JSONObject().apply {
                    put("title", sub.title)
                    put("titleSlug", sub.titleSlug)
                    put("statusDisplay", sub.statusDisplay)
                    put("lang", sub.lang)
                    put("timestamp", sub.timestamp)
                }
                subsArr.put(sObj)
            }
            put("recentSubmissions", subsArr)

            val tagsArr = JSONArray()
            data.leetCode.topicTags.forEach { tag ->
                val tObj = JSONObject().apply {
                    put("name", tag.name)
                    put("solvedCount", tag.solvedCount)
                    put("category", tag.category)
                }
                tagsArr.put(tObj)
            }
            put("topicTags", tagsArr)
        }
        root.put("leetCode", lcObj)

        // GitHub
        val ghObj = JSONObject().apply {
            put("username", data.gitHub.username)
            put("totalContributions", data.gitHub.totalContributions)
            put("commitsCount", data.gitHub.commitsCount)
            put("pullRequestsCount", data.gitHub.pullRequestsCount)
            put("issuesCount", data.gitHub.issuesCount)
            put("reviewsCount", data.gitHub.reviewsCount)
            put("status", data.gitHub.status.name)
            put("statusMessage", data.gitHub.statusMessage)

            val calObj = JSONObject()
            data.gitHub.contributionDays.forEach { (date, count) ->
                calObj.put(date, count)
            }
            put("calendar", calObj)

            val evArr = JSONArray()
            data.gitHub.recentEvents.forEach { ev ->
                val eObj = JSONObject().apply {
                    put("type", ev.type)
                    put("repoName", ev.repoName)
                    put("actionSummary", ev.actionSummary)
                    put("createdAt", ev.createdAt)
                }
                evArr.put(eObj)
            }
            put("recentEvents", evArr)

            val langArr = JSONArray()
            data.gitHub.topLanguages.forEach { l ->
                val lObj = JSONObject().apply {
                    put("name", l.name)
                    put("repoCount", l.repoCount)
                    put("colorHex", l.colorHex)
                }
                langArr.put(lObj)
            }
            put("topLanguages", langArr)
        }
        root.put("gitHub", ghObj)

        prefs.edit().putString(KEY_CACHED_DASHBOARD, root.toString()).apply()
    }

    fun getDashboardData(): WidgetDashboardData {
        val raw = prefs.getString(KEY_CACHED_DASHBOARD, null)
        if (raw == null) {
            return if (leetCodeUsername.isNotEmpty() || gitHubUsername.isNotEmpty()) {
                WidgetDashboardData(
                    leetCode = LeetCodeStats(username = leetCodeUsername, status = AccountStatus.NOT_CONFIGURED),
                    gitHub = GitHubStats(username = gitHubUsername, status = AccountStatus.NOT_CONFIGURED)
                )
            } else {
                getSampleDashboardData()
            }
        }
        return try {
            val root = JSONObject(raw)
            val updated = root.optLong("lastUpdatedMillis", System.currentTimeMillis())

            val lcObj = root.optJSONObject("leetCode") ?: JSONObject()
            val lcCal = mutableMapOf<Long, Int>()
            lcObj.optJSONObject("calendar")?.let { cObj ->
                cObj.keys().forEach { k ->
                    lcCal[k.toLong()] = cObj.optInt(k, 0)
                }
            }
            val lcSubs = mutableListOf<LeetCodeSubmission>()
            lcObj.optJSONArray("recentSubmissions")?.let { sArr ->
                for (i in 0 until sArr.length()) {
                    val item = sArr.getJSONObject(i)
                    lcSubs.add(
                        LeetCodeSubmission(
                            title = item.optString("title", ""),
                            titleSlug = item.optString("titleSlug", ""),
                            statusDisplay = item.optString("statusDisplay", "Accepted"),
                            lang = item.optString("lang", "Kotlin"),
                            timestamp = item.optLong("timestamp", 0L)
                        )
                    )
                }
            }
            val lcTags = mutableListOf<ProblemTopicTag>()
            lcObj.optJSONArray("topicTags")?.let { tArr ->
                for (i in 0 until tArr.length()) {
                    val item = tArr.getJSONObject(i)
                    lcTags.add(
                        ProblemTopicTag(
                            name = item.optString("name", ""),
                            solvedCount = item.optInt("solvedCount", 0),
                            category = item.optString("category", "General")
                        )
                    )
                }
            }
            val lcStatusStr = lcObj.optString("status", AccountStatus.NOT_CONFIGURED.name)
            val lcStatus = try { AccountStatus.valueOf(lcStatusStr) } catch (_: Exception) { AccountStatus.NOT_CONFIGURED }
            val lcStats = LeetCodeStats(
                username = lcObj.optString("username", leetCodeUsername),
                totalSolved = lcObj.optInt("totalSolved", 0),
                easySolved = lcObj.optInt("easySolved", 0),
                mediumSolved = lcObj.optInt("mediumSolved", 0),
                hardSolved = lcObj.optInt("hardSolved", 0),
                submissionCalendar = lcCal,
                recentSubmissions = lcSubs,
                topicTags = lcTags,
                status = lcStatus,
                statusMessage = lcObj.optString("statusMessage", "")
            )

            val ghObj = root.optJSONObject("gitHub") ?: JSONObject()
            val ghCal = mutableMapOf<String, Int>()
            ghObj.optJSONObject("calendar")?.let { cObj ->
                cObj.keys().forEach { k ->
                    ghCal[k] = cObj.optInt(k, 0)
                }
            }
            val ghEvents = mutableListOf<GitHubEvent>()
            ghObj.optJSONArray("recentEvents")?.let { eArr ->
                for (i in 0 until eArr.length()) {
                    val item = eArr.getJSONObject(i)
                    ghEvents.add(
                        GitHubEvent(
                            type = item.optString("type", ""),
                            repoName = item.optString("repoName", ""),
                            actionSummary = item.optString("actionSummary", ""),
                            createdAt = item.optString("createdAt", "")
                        )
                    )
                }
            }
            val ghLangs = mutableListOf<GitHubLanguageStat>()
            ghObj.optJSONArray("topLanguages")?.let { lArr ->
                for (i in 0 until lArr.length()) {
                    val item = lArr.getJSONObject(i)
                    ghLangs.add(
                        GitHubLanguageStat(
                            name = item.optString("name", ""),
                            repoCount = item.optInt("repoCount", 0),
                            colorHex = item.optString("colorHex", "#3B82F6")
                        )
                    )
                }
            }
            val ghStatusStr = ghObj.optString("status", AccountStatus.NOT_CONFIGURED.name)
            val ghStatus = try { AccountStatus.valueOf(ghStatusStr) } catch (_: Exception) { AccountStatus.NOT_CONFIGURED }
            val ghStats = GitHubStats(
                username = ghObj.optString("username", gitHubUsername),
                totalContributions = ghObj.optInt("totalContributions", 0),
                commitsCount = ghObj.optInt("commitsCount", 0),
                pullRequestsCount = ghObj.optInt("pullRequestsCount", 0),
                issuesCount = ghObj.optInt("issuesCount", 0),
                reviewsCount = ghObj.optInt("reviewsCount", 0),
                contributionDays = ghCal,
                recentEvents = ghEvents,
                topLanguages = ghLangs,
                status = ghStatus,
                statusMessage = ghObj.optString("statusMessage", "")
            )

            WidgetDashboardData(
                leetCode = lcStats,
                gitHub = ghStats,
                lastUpdatedMillis = updated
            )
        } catch (e: Exception) {
            WidgetDashboardData(
                leetCode = LeetCodeStats(username = leetCodeUsername, status = AccountStatus.FAILED, statusMessage = "Cache read failed"),
                gitHub = GitHubStats(username = gitHubUsername, status = AccountStatus.FAILED, statusMessage = "Cache read failed")
            )
        }
    }

    fun getSampleDashboardData(): WidgetDashboardData {
        val now = System.currentTimeMillis() / 1000
        val sampleLcCal = mutableMapOf<Long, Int>()
        // Generate realistic 14 weeks of activity
        val secondsInDay = 86400L
        for (i in 0..365) {
            val ts = now - (i * secondsInDay)
            val count = when {
                i % 7 in 1..5 && (i % 3 != 0) -> (1..6).random()
                i % 4 == 0 -> (1..3).random()
                else -> 0
            }
            if (count > 0) sampleLcCal[ts] = count
        }

        val sampleLcSubs = listOf(
            LeetCodeSubmission("Two Sum", "two-sum", "Accepted", "Kotlin", now - 3600),
            LeetCodeSubmission("LRU Cache", "lru-cache", "Accepted", "Kotlin", now - 7200),
            LeetCodeSubmission("Median of Two Sorted Arrays", "median-of-two-sorted-arrays", "Wrong Answer", "Java", now - 18000),
            LeetCodeSubmission("Trapping Rain Water", "trapping-rain-water", "Accepted", "Kotlin", now - 86400),
            LeetCodeSubmission("Merge k Sorted Lists", "merge-k-sorted-lists", "Accepted", "C++", now - 120000),
            LeetCodeSubmission("Valid Parentheses", "valid-parentheses", "Accepted", "Python", now - 180000),
            LeetCodeSubmission("Number of Islands", "number-of-islands", "Accepted", "Kotlin", now - 250000),
            LeetCodeSubmission("Coin Change", "coin-change", "Time Limit Exceeded", "Java", now - 300000),
            LeetCodeSubmission("Word Break", "word-break", "Accepted", "Kotlin", now - 400000),
            LeetCodeSubmission("Longest Substring Without Repeating Characters", "longest-substring", "Accepted", "Kotlin", now - 500000)
        )

        val sampleGhCal = mutableMapOf<String, Int>()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        for (i in 0..365) {
            val d = java.util.Date(System.currentTimeMillis() - (i * 86400000L))
            val dateStr = sdf.format(d)
            val count = when {
                i % 7 in 1..5 && (i % 2 != 0) -> (2..9).random()
                i % 5 == 0 -> (1..4).random()
                else -> 0
            }
            if (count > 0) sampleGhCal[dateStr] = count
        }

        val sampleGhEvents = listOf(
            GitHubEvent("PushEvent", "project_dev_android_widgets", "Pushed 3 commits to main", "1h ago"),
            GitHubEvent("PullRequestEvent", "awesome-android", "Opened PR #42: Jetpack Glance widget", "4h ago"),
            GitHubEvent("PushEvent", "project_dev_android_widgets", "Pushed 1 commit to main", "1d ago"),
            GitHubEvent("IssuesEvent", "compose-multiplatform", "Created issue: Canvas bitmap rendering", "2d ago"),
            GitHubEvent("PullRequestReviewEvent", "developer-tools", "Approved PR #108", "3d ago"),
            GitHubEvent("PushEvent", "portfolio", "Pushed 5 commits to master", "4d ago"),
            GitHubEvent("ForkEvent", "kotlin-coroutines", "Forked repository", "5d ago"),
            GitHubEvent("WatchEvent", "android/architecture-samples", "Starred repository", "6d ago"),
            GitHubEvent("PushEvent", "algorithms-repo", "Pushed 2 commits to dev", "1w ago"),
            GitHubEvent("CreateEvent", "sandbox-compose", "Created repository sandbox-compose", "1w ago")
        )

        val sampleLcTags = listOf(
            ProblemTopicTag("Array", 145, "Fundamental"),
            ProblemTopicTag("String", 98, "Fundamental"),
            ProblemTopicTag("Hash Table", 85, "Fundamental"),
            ProblemTopicTag("Dynamic Programming", 64, "Advanced"),
            ProblemTopicTag("Tree", 52, "Intermediate"),
            ProblemTopicTag("Math", 48, "Fundamental"),
            ProblemTopicTag("Depth-First Search", 45, "Intermediate"),
            ProblemTopicTag("Binary Search", 36, "Intermediate"),
            ProblemTopicTag("Greedy", 30, "Intermediate"),
            ProblemTopicTag("Two Pointers", 28, "Intermediate"),
            ProblemTopicTag("Graph", 24, "Advanced"),
            ProblemTopicTag("Stack", 22, "Fundamental")
        )

        val sampleGhLangs = listOf(
            GitHubLanguageStat("Kotlin", 14, "#A97BFF"),
            GitHubLanguageStat("Python", 11, "#3572A5"),
            GitHubLanguageStat("TypeScript", 7, "#3178C6"),
            GitHubLanguageStat("Java", 5, "#B07219"),
            GitHubLanguageStat("Rust", 4, "#DEA584"),
            GitHubLanguageStat("C++", 3, "#F34B7D")
        )

        return WidgetDashboardData(
            leetCode = LeetCodeStats(
                username = if (leetCodeUsername.isNotEmpty()) leetCodeUsername else "sangrechy",
                totalSolved = 485,
                easySolved = 192,
                mediumSolved = 238,
                hardSolved = 55,
                submissionCalendar = sampleLcCal,
                recentSubmissions = sampleLcSubs,
                topicTags = sampleLcTags,
                status = AccountStatus.CONNECTED,
                statusMessage = "Linked (485 solved)"
            ),
            gitHub = GitHubStats(
                username = if (gitHubUsername.isNotEmpty()) gitHubUsername else "sangrechy",
                totalContributions = 842,
                commitsCount = 590,
                pullRequestsCount = 84,
                issuesCount = 38,
                reviewsCount = 130,
                contributionDays = sampleGhCal,
                recentEvents = sampleGhEvents,
                topLanguages = sampleGhLangs,
                status = AccountStatus.CONNECTED,
                statusMessage = "Linked (842 contribs)"
            ),
            lastUpdatedMillis = System.currentTimeMillis()
        )
    }

    companion object {
        private const val PREFS_NAME = "dev_widgets_prefs"
        private const val KEY_LEETCODE_USERNAME = "leetcode_username"
        private const val KEY_GITHUB_USERNAME = "github_username"
        private const val KEY_GITHUB_TOKEN = "github_token"
        private const val KEY_CACHED_DASHBOARD = "cached_dashboard_json"
        private const val KEY_REFRESH_INTERVAL = "auto_refresh_interval"
        private const val KEY_IGNORED_UPDATE = "ignored_update_version"
    }
}
