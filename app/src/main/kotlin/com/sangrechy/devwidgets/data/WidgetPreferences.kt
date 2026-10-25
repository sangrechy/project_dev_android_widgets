package com.sangrechy.devwidgets.data

import android.content.Context
import android.content.SharedPreferences
import com.sangrechy.devwidgets.data.model.GitHubEvent
import com.sangrechy.devwidgets.data.model.GitHubStats
import com.sangrechy.devwidgets.data.model.LeetCodeSubmission
import com.sangrechy.devwidgets.data.model.LeetCodeStats
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
        }
        root.put("gitHub", ghObj)

        prefs.edit().putString(KEY_CACHED_DASHBOARD, root.toString()).apply()
    }

    fun getDashboardData(): WidgetDashboardData {
        val raw = prefs.getString(KEY_CACHED_DASHBOARD, null) ?: return getSampleDashboardData()
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
            val lcStats = LeetCodeStats(
                username = lcObj.optString("username", leetCodeUsername),
                totalSolved = lcObj.optInt("totalSolved", 0),
                easySolved = lcObj.optInt("easySolved", 0),
                mediumSolved = lcObj.optInt("mediumSolved", 0),
                hardSolved = lcObj.optInt("hardSolved", 0),
                submissionCalendar = lcCal,
                recentSubmissions = lcSubs
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
            val ghStats = GitHubStats(
                username = ghObj.optString("username", gitHubUsername),
                totalContributions = ghObj.optInt("totalContributions", 0),
                commitsCount = ghObj.optInt("commitsCount", 0),
                pullRequestsCount = ghObj.optInt("pullRequestsCount", 0),
                issuesCount = ghObj.optInt("issuesCount", 0),
                reviewsCount = ghObj.optInt("reviewsCount", 0),
                contributionDays = ghCal,
                recentEvents = ghEvents
            )

            WidgetDashboardData(
                leetCode = lcStats,
                gitHub = ghStats,
                lastUpdatedMillis = updated
            )
        } catch (e: Exception) {
            getSampleDashboardData()
        }
    }

    fun getSampleDashboardData(): WidgetDashboardData {
        val now = System.currentTimeMillis() / 1000
        val sampleLcCal = mutableMapOf<Long, Int>()
        // Generate realistic 14 weeks of activity
        val secondsInDay = 86400L
        for (i in 0..98) {
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
        for (i in 0..98) {
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

        return WidgetDashboardData(
            leetCode = LeetCodeStats(
                username = if (leetCodeUsername.isNotEmpty()) leetCodeUsername else "sangrechy",
                totalSolved = 485,
                easySolved = 192,
                mediumSolved = 238,
                hardSolved = 55,
                submissionCalendar = sampleLcCal,
                recentSubmissions = sampleLcSubs
            ),
            gitHub = GitHubStats(
                username = if (gitHubUsername.isNotEmpty()) gitHubUsername else "sangrechy",
                totalContributions = 842,
                commitsCount = 590,
                pullRequestsCount = 84,
                issuesCount = 38,
                reviewsCount = 130,
                contributionDays = sampleGhCal,
                recentEvents = sampleGhEvents
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
    }
}
