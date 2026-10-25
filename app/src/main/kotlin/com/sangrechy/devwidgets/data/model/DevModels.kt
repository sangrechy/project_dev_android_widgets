package com.sangrechy.devwidgets.data.model

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
    val recentSubmissions: List<LeetCodeSubmission> = emptyList()
)

data class GitHubEvent(
    val type: String, // e.g., "PushEvent", "PullRequestEvent"
    val repoName: String,
    val actionSummary: String,
    val createdAt: String
)

data class GitHubStats(
    val username: String = "",
    val totalContributions: Int = 0,
    val commitsCount: Int = 0,
    val pullRequestsCount: Int = 0,
    val issuesCount: Int = 0,
    val reviewsCount: Int = 0,
    val contributionDays: Map<String, Int> = emptyMap(), // "YYYY-MM-DD" -> count
    val recentEvents: List<GitHubEvent> = emptyList()
)

data class WidgetDashboardData(
    val leetCode: LeetCodeStats = LeetCodeStats(),
    val gitHub: GitHubStats = GitHubStats(),
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)
