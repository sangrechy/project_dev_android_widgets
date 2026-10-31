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

    var gitHubBackupRepo: String
        get() = prefs.getString(KEY_GITHUB_BACKUP_REPO, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GITHUB_BACKUP_REPO, value.trim()).apply()

    var autoRefreshInterval: String
        get() = prefs.getString(KEY_REFRESH_INTERVAL, "1h") ?: "1h"
        set(value) = prefs.edit().putString(KEY_REFRESH_INTERVAL, value).apply()

    var heatmapOrientation: String
        get() = prefs.getString(KEY_HEATMAP_ORIENTATION, "horizontal") ?: "horizontal"
        set(value) = prefs.edit().putString(KEY_HEATMAP_ORIENTATION, value).apply()

    var ignoredUpdateVersion: String
        get() = prefs.getString(KEY_IGNORED_UPDATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_IGNORED_UPDATE, value).apply()

    // Wallpaper configuration
    var appWallpaperType: String
        get() = prefs.getString(KEY_APP_WALLPAPER_TYPE, "NONE") ?: "NONE"
        set(value) = prefs.edit().putString(KEY_APP_WALLPAPER_TYPE, value).apply()

    var appWallpaperPreset: String
        get() = prefs.getString(KEY_APP_WALLPAPER_PRESET, "OBSIDIAN_CORE") ?: "OBSIDIAN_CORE"
        set(value) = prefs.edit().putString(KEY_APP_WALLPAPER_PRESET, value).apply()

    var appWallpaperCustomPath: String
        get() = prefs.getString(KEY_APP_WALLPAPER_CUSTOM_PATH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_APP_WALLPAPER_CUSTOM_PATH, value).apply()

    var appWallpaperDim: Float
        get() = prefs.getFloat(KEY_APP_WALLPAPER_DIM, 0.65f)
        set(value) = prefs.edit().putFloat(KEY_APP_WALLPAPER_DIM, value).apply()

    var widgetWallpaperType: String
        get() = prefs.getString(KEY_WIDGET_WALLPAPER_TYPE, "DEFAULT") ?: "DEFAULT"
        set(value) = prefs.edit().putString(KEY_WIDGET_WALLPAPER_TYPE, value).apply()

    var widgetWallpaperPreset: String
        get() = prefs.getString(KEY_WIDGET_WALLPAPER_PRESET, "OBSIDIAN_CORE") ?: "OBSIDIAN_CORE"
        set(value) = prefs.edit().putString(KEY_WIDGET_WALLPAPER_PRESET, value).apply()

    var widgetWallpaperCustomPath: String
        get() = prefs.getString(KEY_WIDGET_WALLPAPER_CUSTOM_PATH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_WIDGET_WALLPAPER_CUSTOM_PATH, value).apply()

    var widgetWallpaperDim: Float
        get() = prefs.getFloat(KEY_WIDGET_WALLPAPER_DIM, 0.55f)
        set(value) = prefs.edit().putFloat(KEY_WIDGET_WALLPAPER_DIM, value).apply()

    fun getWidgetWallpaperTypeFor(widgetKey: String): String {
        val norm = widgetKey.trim().uppercase()
        return prefs.getString("KEY_WIDGET_WALLPAPER_TYPE_$norm", null)
            ?: prefs.getString("KEY_WIDGET_WALLPAPER_TYPE_$widgetKey", null)
            ?: widgetWallpaperType
    }
    fun setWidgetWallpaperTypeFor(widgetKey: String, type: String) {
        val norm = widgetKey.trim().uppercase()
        prefs.edit()
            .putString("KEY_WIDGET_WALLPAPER_TYPE_$norm", type)
            .putString("KEY_WIDGET_WALLPAPER_TYPE_$widgetKey", type)
            .apply()
    }

    fun getWidgetWallpaperPresetFor(widgetKey: String): String {
        val norm = widgetKey.trim().uppercase()
        return prefs.getString("KEY_WIDGET_WALLPAPER_PRESET_$norm", null)
            ?: prefs.getString("KEY_WIDGET_WALLPAPER_PRESET_$widgetKey", null)
            ?: widgetWallpaperPreset
    }
    fun setWidgetWallpaperPresetFor(widgetKey: String, preset: String) {
        val norm = widgetKey.trim().uppercase()
        prefs.edit()
            .putString("KEY_WIDGET_WALLPAPER_PRESET_$norm", preset)
            .putString("KEY_WIDGET_WALLPAPER_PRESET_$widgetKey", preset)
            .apply()
    }

    fun getWidgetWallpaperCustomPathFor(widgetKey: String): String {
        val norm = widgetKey.trim().uppercase()
        return prefs.getString("KEY_WIDGET_WALLPAPER_CUSTOM_PATH_$norm", null)
            ?: prefs.getString("KEY_WIDGET_WALLPAPER_CUSTOM_PATH_$widgetKey", null)
            ?: widgetWallpaperCustomPath
    }
    fun setWidgetWallpaperCustomPathFor(widgetKey: String, path: String) {
        val norm = widgetKey.trim().uppercase()
        prefs.edit()
            .putString("KEY_WIDGET_WALLPAPER_CUSTOM_PATH_$norm", path)
            .putString("KEY_WIDGET_WALLPAPER_CUSTOM_PATH_$widgetKey", path)
            .apply()
    }

    fun getWidgetWallpaperDimFor(widgetKey: String): Float {
        val norm = widgetKey.trim().uppercase()
        return prefs.getFloat("KEY_WIDGET_WALLPAPER_DIM_$norm", prefs.getFloat("KEY_WIDGET_WALLPAPER_DIM_$widgetKey", widgetWallpaperDim))
    }
    fun setWidgetWallpaperDimFor(widgetKey: String, dim: Float) {
        val norm = widgetKey.trim().uppercase()
        prefs.edit()
            .putFloat("KEY_WIDGET_WALLPAPER_DIM_$norm", dim)
            .putFloat("KEY_WIDGET_WALLPAPER_DIM_$widgetKey", dim)
            .apply()
    }

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
            put("isAuthenticated", data.gitHub.isAuthenticated)

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
                statusMessage = ghObj.optString("statusMessage", ""),
                isAuthenticated = ghObj.optBoolean("isAuthenticated", false)
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

        val problemTemplates = listOf(
            "Two Sum" to "two-sum",
            "LRU Cache" to "lru-cache",
            "Median of Two Sorted Arrays" to "median-of-two-sorted-arrays",
            "Trapping Rain Water" to "trapping-rain-water",
            "Merge k Sorted Lists" to "merge-k-sorted-lists",
            "Valid Parentheses" to "valid-parentheses",
            "Number of Islands" to "number-of-islands",
            "Coin Change" to "coin-change",
            "Word Break" to "word-break",
            "Longest Substring Without Repeating" to "longest-substring",
            "Climbing Stairs" to "climbing-stairs",
            "Reverse Linked List" to "reverse-linked-list",
            "Binary Tree Maximum Path Sum" to "binary-tree-maximum-path-sum",
            "Course Schedule" to "course-schedule",
            "Kth Largest Element in an Array" to "kth-largest-element-in-an-array",
            "Product of Array Except Self" to "product-of-array-except-self",
            "Top K Frequent Elements" to "top-k-frequent-elements",
            "Decode String" to "decode-string",
            "Daily Temperatures" to "daily-temperatures",
            "Find Minimum in Rotated Sorted Array" to "find-minimum-in-rotated-sorted-array"
        )
        val langs = listOf("Kotlin", "Java", "Python3", "C++", "Rust", "Go", "TypeScript")
        val statuses = listOf("Accepted", "Accepted", "Accepted", "Wrong Answer", "Time Limit Exceeded")

        val sampleLcSubs = (0 until 100).map { i ->
            val tmpl = problemTemplates[i % problemTemplates.size]
            val lang = langs[i % langs.size]
            val status = statuses[i % statuses.size]
            val ts = now - (i * 3600L * 4 + (i * 123L))
            LeetCodeSubmission(
                title = tmpl.first,
                titleSlug = tmpl.second,
                statusDisplay = status,
                lang = lang,
                timestamp = ts
            )
        }

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

        val eventRepos = listOf("project_dev_android_widgets", "awesome-android", "compose-multiplatform", "developer-tools", "portfolio", "algorithms-repo")
        val sampleGhEvents = (0 until 100).map { i ->
            val repo = eventRepos[i % eventRepos.size]
            val (type, summary) = when (i % 6) {
                0 -> "PushEvent" to "Pushed ${((i % 4) + 1)} commit(s) to main"
                1 -> "PullRequestEvent" to "Opened PR #${100 + i}: Feature enhancements"
                2 -> "IssuesEvent" to "Created issue #${50 + i}: UI performance optimization"
                3 -> "PullRequestReviewEvent" to "Approved PR #${80 + i}"
                4 -> "WatchEvent" to "Starred repository"
                else -> "ForkEvent" to "Forked repository"
            }
            val timeLabel = when {
                i == 0 -> "1h ago"
                i < 5 -> "${i * 2}h ago"
                i < 24 -> "${i}h ago"
                i < 48 -> "${i / 24}d ago"
                else -> "${i / 7}w ago"
            }
            GitHubEvent(
                type = type,
                repoName = repo,
                actionSummary = summary,
                createdAt = timeLabel
            )
        }

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
        private const val KEY_GITHUB_BACKUP_REPO = "github_backup_repo"
        private const val KEY_CACHED_DASHBOARD = "cached_dashboard_json"
        private const val KEY_REFRESH_INTERVAL = "auto_refresh_interval"
        private const val KEY_HEATMAP_ORIENTATION = "heatmap_orientation"
        private const val KEY_IGNORED_UPDATE = "ignored_update_version"
        private const val KEY_APP_WALLPAPER_TYPE = "app_wallpaper_type"
        private const val KEY_APP_WALLPAPER_PRESET = "app_wallpaper_preset"
        private const val KEY_APP_WALLPAPER_CUSTOM_PATH = "app_wallpaper_custom_path"
        private const val KEY_APP_WALLPAPER_DIM = "app_wallpaper_dim"
        private const val KEY_WIDGET_WALLPAPER_TYPE = "widget_wallpaper_type"
        private const val KEY_WIDGET_WALLPAPER_PRESET = "widget_wallpaper_preset"
        private const val KEY_WIDGET_WALLPAPER_CUSTOM_PATH = "widget_wallpaper_custom_path"
        private const val KEY_WIDGET_WALLPAPER_DIM = "widget_wallpaper_dim"
    }
}
