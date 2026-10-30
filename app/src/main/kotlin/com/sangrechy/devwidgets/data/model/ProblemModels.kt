package com.sangrechy.devwidgets.data.model

data class ProblemNote(
    val filename: String,
    val title: String,
    val content: String,
    val lastModifiedMillis: Long = System.currentTimeMillis()
)

data class OfflineProblem(
    val folderName: String, // e.g., "0001-two-sum"
    val frontendId: String, // e.g., "1" or "0001"
    val title: String, // e.g., "Two Sum"
    val titleSlug: String, // e.g., "two-sum"
    val difficulty: String = "Medium", // Easy, Medium, Hard
    val contentHtml: String = "", // Official LeetCode description & examples HTML
    val topicTags: List<String> = emptyList(),
    val hints: List<String> = emptyList(),
    val solutionCode: String = "", // Code backed up by project_git_the_leet
    val solutionLanguage: String = "python3",
    val runtime: String = "", // e.g. "38 ms"
    val runtimePercentile: String = "", // e.g. "88.4%"
    val memory: String = "", // e.g. "16.5 MB"
    val memoryPercentile: String = "", // e.g. "76.2%"
    val submittedAt: String = "",
    val problemUrl: String = "",
    val notes: List<ProblemNote> = emptyList(), // Multiple notes inside notes/ folder
    val isDownloadedOffline: Boolean = false,
    val lastSyncedMillis: Long = 0L
)

data class CustomNote(
    val id: String, // unique id or filename
    val title: String,
    val content: String,
    val category: String = "General", // General, Algorithms, Math, Templates
    val lastModifiedMillis: Long = System.currentTimeMillis()
)

enum class ProblemsPlatform(val label: String) {
    LEETCODE("LeetCode"),
    CODEFORCES("Codeforces (Coming Soon)")
}

enum class ProblemsSubTab(val label: String) {
    PROBLEMS("Problems"),
    NOTES("Notes")
}

enum class ProblemDifficultyFilter(val label: String) {
    ALL("All"),
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard"),
    DOWNLOADED("Downloaded")
}
