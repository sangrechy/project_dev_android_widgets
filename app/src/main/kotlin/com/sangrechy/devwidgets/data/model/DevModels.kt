package com.sangrechy.devwidgets.data.model

enum class AccountStatus {
    NOT_CONFIGURED,
    CONNECTING,
    CONNECTED,
    FAILED
}

data class ProblemTopicTag(
    val name: String,
    val solvedCount: Int,
    val category: String = "General" // Advanced, Intermediate, Fundamental
)

data class LeetCodeSubmission(
    val title: String,
    val titleSlug: String,
    val statusDisplay: String, // e.g., "Accepted", "Wrong Answer"
    val lang: String,
    val timestamp: Long
)

data class LeetCodeStats(
    val username: String = "",
    val totalSolved: Int = 0,
    val easySolved: Int = 0,
    val mediumSolved: Int = 0,
    val hardSolved: Int = 0,
    val submissionCalendar: Map<Long, Int> = emptyMap(), // epoch seconds -> count
    val recentSubmissions: List<LeetCodeSubmission> = emptyList(),
    val topicTags: List<ProblemTopicTag> = emptyList(),
    val status: AccountStatus = AccountStatus.NOT_CONFIGURED,
    val statusMessage: String = ""
)

data class GitHubEvent(
    val type: String, // e.g., "PushEvent", "PullRequestEvent"
    val repoName: String,
    val actionSummary: String,
    val createdAt: String
)

data class GitHubLanguageStat(
    val name: String,
    val repoCount: Int,
    val colorHex: String = "#3B82F6"
)

data class GitHubStats(
    val username: String = "",
    val totalContributions: Int = 0,
    val commitsCount: Int = 0,
    val pullRequestsCount: Int = 0,
    val issuesCount: Int = 0,
    val reviewsCount: Int = 0,
    val contributionDays: Map<String, Int> = emptyMap(), // "YYYY-MM-DD" -> count
    val recentEvents: List<GitHubEvent> = emptyList(),
    val topLanguages: List<GitHubLanguageStat> = emptyList(),
    val status: AccountStatus = AccountStatus.NOT_CONFIGURED,
    val statusMessage: String = ""
)

data class AppUpdateInfo(
    val hasUpdate: Boolean = false,
    val latestVersion: String = "",
    val releaseName: String = "",
    val releaseNotes: String = "",
    val downloadUrl: String = "",
    val releaseHtmlUrl: String = ""
)

data class WidgetDashboardData(
    val leetCode: LeetCodeStats = LeetCodeStats(),
    val gitHub: GitHubStats = GitHubStats(),
    val lastUpdatedMillis: Long = System.currentTimeMillis(),
    val isSyncing: Boolean = false
)
