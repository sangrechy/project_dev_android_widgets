package com.sangrechy.devwidgets.data

import android.content.Context
import com.sangrechy.devwidgets.data.model.CustomNote
import com.sangrechy.devwidgets.data.model.OfflineProblem
import com.sangrechy.devwidgets.data.model.ProblemNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

interface ProblemRepository {
    suspend fun getProblems(): List<OfflineProblem>
    suspend fun getProblemDetail(folderName: String): OfflineProblem?
    suspend fun downloadProblemOffline(folderName: String, titleSlug: String): Result<OfflineProblem>
    suspend fun deleteProblemOffline(folderName: String): Boolean
    suspend fun saveProblemNote(folderName: String, filename: String, content: String): ProblemNote
    suspend fun deleteProblemNote(folderName: String, filename: String): Boolean
    suspend fun syncFromGitHubBackupRepo(owner: String, repo: String, token: String = ""): Result<Int>
    suspend fun getGeneralNotes(): List<CustomNote>
    suspend fun saveGeneralNote(note: CustomNote): CustomNote
    suspend fun deleteGeneralNote(id: String): Boolean
}

class OfflineProblemRepository(
    private val context: Context,
    private val okHttpClient: OkHttpClient = defaultClient()
) : ProblemRepository {
    companion object {
        private fun defaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()
        }
    }

    private val baseProblemsDir: File
        get() = File(context.filesDir, "problems/leetcode").apply { mkdirs() }

    private val generalNotesDir: File
        get() = File(context.filesDir, "general_notes").apply { mkdirs() }

    init {
        // Ensure default seed data exists on initial launch
        seedDefaultProblemsIfEmpty()
        seedDefaultGeneralNotesIfEmpty()
    }

    override suspend fun getProblems(): List<OfflineProblem> = withContext(Dispatchers.IO) {
        val dirs = baseProblemsDir.listFiles { file -> file.isDirectory } ?: emptyArray()
        val list = mutableListOf<OfflineProblem>()

        for (dir in dirs) {
            val problem = loadProblemFromDir(dir)
            if (problem != null) {
                list.add(problem)
            }
        }

        // Sort by frontendId numeric order if possible
        list.sortedWith(compareBy({ it.frontendId.toIntOrNull() ?: 999999 }, { it.title }))
    }

    override suspend fun getProblemDetail(folderName: String): OfflineProblem? = withContext(Dispatchers.IO) {
        val dir = File(baseProblemsDir, folderName)
        if (!dir.exists() || !dir.isDirectory) return@withContext null
        loadProblemFromDir(dir)
    }

    override suspend fun downloadProblemOffline(folderName: String, titleSlug: String): Result<OfflineProblem> = withContext(Dispatchers.IO) {
        val dir = File(baseProblemsDir, folderName).apply { mkdirs() }
        val slug = titleSlug.ifBlank { folderName.substringAfter('-', folderName) }

        try {
            // Query public LeetCode GraphQL for authentic content, examples, constraints, hints
            val query = """
                query questionContent(${'$'}titleSlug: String!) {
                  question(titleSlug: ${'$'}titleSlug) {
                    questionId
                    questionFrontendId
                    title
                    titleSlug
                    content
                    difficulty
                    topicTags {
                      name
                      slug
                    }
                    hints
                  }
                }
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("query", query)
                put("variables", JSONObject().apply { put("titleSlug", slug) })
            }

            val request = Request.Builder()
                .url("https://leetcode.com/graphql")
                .header("Content-Type", "application/json")
                .header("Referer", "https://leetcode.com/problems/$slug/")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val (contentHtml, fetchedDifficulty, fetchedTags, fetchedHints) = okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val root = JSONObject(body)
                    val q = root.optJSONObject("data")?.optJSONObject("question")
                    val content = q?.optString("content") ?: ""
                    val diff = q?.optString("difficulty") ?: "Medium"
                    val tags = mutableListOf<String>()
                    val tagArr = q?.optJSONArray("topicTags")
                    if (tagArr != null) {
                        for (i in 0 until tagArr.length()) {
                            val tName = tagArr.getJSONObject(i).optString("name")
                            if (tName.isNotBlank()) tags.add(tName)
                        }
                    }
                    val hints = mutableListOf<String>()
                    val hintArr = q?.optJSONArray("hints")
                    if (hintArr != null) {
                        for (i in 0 until hintArr.length()) {
                            hints.add(hintArr.getString(i))
                        }
                    }
                    Quadruple(content, diff, tags, hints)
                } else {
                    Quadruple("", "Medium", emptyList<String>(), emptyList<String>())
                }
            }

            // Save content.html
            if (contentHtml.isNotBlank()) {
                File(dir, "content.html").writeText(contentHtml)
            }

            // Load existing metadata.json or create one
            val metaFile = File(dir, "metadata.json")
            val metaObj = if (metaFile.exists()) {
                try { JSONObject(metaFile.readText()) } catch (_: Exception) { JSONObject() }
            } else JSONObject()

            metaObj.put("is_downloaded_offline", true)
            metaObj.put("last_synced_millis", System.currentTimeMillis())
            if (fetchedDifficulty.isNotBlank() && !metaObj.has("difficulty")) {
                metaObj.put("difficulty", fetchedDifficulty)
            }
            if (fetchedTags.isNotEmpty()) {
                val tagsArr = JSONArray()
                fetchedTags.forEach { tagsArr.put(it) }
                metaObj.put("topic_tags", tagsArr)
            }
            if (fetchedHints.isNotEmpty()) {
                val hintArr = JSONArray()
                fetchedHints.forEach { hintArr.put(it) }
                metaObj.put("hints", hintArr)
            }
            metaFile.writeText(metaObj.toString(2))

            // Ensure notes directory exists
            File(dir, "notes").mkdirs()

            val updated = loadProblemFromDir(dir)
                ?: return@withContext Result.failure(Exception("Failed to reload downloaded problem"))

            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProblemOffline(folderName: String): Boolean = withContext(Dispatchers.IO) {
        val dir = File(baseProblemsDir, folderName)
        if (dir.exists()) {
            dir.deleteRecursively()
        } else false
    }

    override suspend fun saveProblemNote(folderName: String, filename: String, content: String): ProblemNote = withContext(Dispatchers.IO) {
        val safeName = filename.trim().ifBlank { "notes.md" }
            .let { if (!it.contains('.')) "$it.md" else it }
        val notesDir = File(File(baseProblemsDir, folderName), "notes").apply { mkdirs() }
        val noteFile = File(notesDir, safeName)
        noteFile.writeText(content)

        val title = safeName.substringBeforeLast('.').replace('_', ' ').replace('-', ' ')
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        ProblemNote(
            filename = safeName,
            title = title,
            content = content,
            lastModifiedMillis = noteFile.lastModified()
        )
    }

    override suspend fun deleteProblemNote(folderName: String, filename: String): Boolean = withContext(Dispatchers.IO) {
        val noteFile = File(File(File(baseProblemsDir, folderName), "notes"), filename)
        if (noteFile.exists()) {
            noteFile.delete()
        } else false
    }

    override suspend fun syncFromGitHubBackupRepo(
        owner: String,
        repo: String,
        token: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        val (cleanOwner, cleanRepo) = if (repo.contains("/")) {
            val pOwner = repo.substringBefore("/").trim()
            val pRepo = repo.substringAfter("/").trim()
            Pair(pOwner.ifBlank { owner.trim() }, pRepo)
        } else {
            Pair(owner.trim(), repo.trim())
        }

        if (cleanOwner.isBlank() || cleanRepo.isBlank()) {
            return@withContext Result.failure(Exception("GitHub username/owner and repository name are required."))
        }

        try {
            // 1. Detect default branch from repository info
            val repoInfoReq = Request.Builder()
                .url("https://api.github.com/repos/$cleanOwner/$cleanRepo")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "DevLife-Android")
                .apply { if (token.isNotBlank()) header("Authorization", "Bearer ${token.trim()}") }
                .build()

            val detectedBranch = try {
                okHttpClient.newCall(repoInfoReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val obj = JSONObject(resp.body?.string() ?: "{}")
                        obj.optString("default_branch", "main").ifBlank { "main" }
                    } else "main"
                }
            } catch (_: Exception) { "main" }

            // 2. Fetch Git tree recursively
            val branchesToTry = listOf(detectedBranch, "main", "master").distinct()
            var treeBody: String? = null
            var lastHttpCode = 0

            for (branch in branchesToTry) {
                val treeUrl = "https://api.github.com/repos/$cleanOwner/$cleanRepo/git/trees/$branch?recursive=1"
                val reqBuilder = Request.Builder()
                    .url(treeUrl)
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "DevLife-Android")
                if (token.isNotBlank()) {
                    reqBuilder.header("Authorization", "Bearer ${token.trim()}")
                }

                try {
                    okHttpClient.newCall(reqBuilder.build()).execute().use { resp ->
                        lastHttpCode = resp.code
                        if (resp.isSuccessful) {
                            treeBody = resp.body?.string()
                            return@use
                        }
                    }
                } catch (_: Exception) {}

                if (treeBody != null) break
            }

            if (treeBody == null) {
                val msg = when (lastHttpCode) {
                    401 -> "GitHub token is invalid or unauthorized."
                    403 -> "GitHub API rate limit exceeded. Check token permissions."
                    404 -> "Repository '$cleanOwner/$cleanRepo' not found or is private."
                    else -> "Failed to access repository '$cleanOwner/$cleanRepo' (HTTP $lastHttpCode)"
                }
                return@withContext Result.failure(Exception(msg))
            }

            val root = JSONObject(treeBody ?: "{}")
            val tree = root.optJSONArray("tree") ?: JSONArray()
            var importedCount = 0

            // Filter entries starting with "leetcode_problem/"
            for (i in 0 until tree.length()) {
                val entry = tree.getJSONObject(i)
                val path = entry.optString("path")
                val type = entry.optString("type")
                if (type != "blob" || !path.startsWith("leetcode_problem/")) continue

                val parts = path.split('/')
                if (parts.size < 3) continue
                val folderName = parts[1] // e.g. "0001-two-sum"
                val subPath = parts.subList(2, parts.size).joinToString("/") // e.g. "solution.py" or "notes/approach1.md"

                val blobUrl = entry.optString("url")
                if (blobUrl.isBlank()) continue

                try {
                    // Download blob content
                    val blobReqBuilder = Request.Builder()
                        .url(blobUrl)
                        .header("Accept", "application/vnd.github.v3+json")
                        .header("User-Agent", "DevLife-Android")
                    if (token.isNotBlank()) {
                        blobReqBuilder.header("Authorization", "Bearer ${token.trim()}")
                    }

                    okHttpClient.newCall(blobReqBuilder.build()).execute().use { blobResp ->
                        if (blobResp.isSuccessful) {
                            val blobJson = JSONObject(blobResp.body?.string() ?: "")
                            val base64Content = blobJson.optString("content").replace("\n", "").replace("\r", "")
                            val bytes = android.util.Base64.decode(base64Content, android.util.Base64.DEFAULT)
                            val textContent = String(bytes, Charsets.UTF_8)

                            val targetFile = File(baseProblemsDir, "$folderName/$subPath")
                            targetFile.parentFile?.mkdirs()
                            targetFile.writeText(textContent)
                            importedCount++
                        }
                    }
                } catch (_: Exception) {}
            }

            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- General Notes Operations ---

    override suspend fun getGeneralNotes(): List<CustomNote> = withContext(Dispatchers.IO) {
        val files = generalNotesDir.listFiles { f -> f.isFile && (f.name.endsWith(".md") || f.name.endsWith(".txt")) }
            ?: emptyArray()
        val list = mutableListOf<CustomNote>()

        for (file in files) {
            try {
                val content = file.readText()
                val id = file.nameWithoutExtension
                val firstLine = content.lines().firstOrNull { it.isNotBlank() } ?: "Untitled Note"
                val title = firstLine.removePrefix("#").trim().ifBlank { id.replace('_', ' ') }
                list.add(
                    CustomNote(
                        id = id,
                        title = title,
                        content = content,
                        category = "General",
                        lastModifiedMillis = file.lastModified()
                    )
                )
            } catch (_: Exception) {}
        }

        list.sortedByDescending { it.lastModifiedMillis }
    }

    override suspend fun saveGeneralNote(note: CustomNote): CustomNote = withContext(Dispatchers.IO) {
        val safeId = note.id.ifBlank { UUID.randomUUID().toString() }
        val file = File(generalNotesDir, "$safeId.md")
        file.writeText(note.content)
        note.copy(id = safeId, lastModifiedMillis = file.lastModified())
    }

    override suspend fun deleteGeneralNote(id: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(generalNotesDir, "$id.md")
        if (file.exists()) file.delete() else false
    }

    // --- Helpers ---

    private fun loadProblemFromDir(dir: File): OfflineProblem? {
        val folderName = dir.name
        val metaFile = File(dir, "metadata.json")
        val meta = if (metaFile.exists()) {
            try { JSONObject(metaFile.readText()) } catch (_: Exception) { JSONObject() }
        } else JSONObject()

        val dashIdx = folderName.indexOf('-')
        val parsedId = if (dashIdx != -1) folderName.substring(0, dashIdx).trimStart('0').ifBlank { "0" } else "0"
        val parsedSlug = if (dashIdx != -1) folderName.substring(dashIdx + 1) else folderName
        val parsedTitle = parsedSlug.split('-').joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        val frontendId = meta.optString("frontend_id", meta.optString("problem_id", parsedId))
        val title = meta.optString("problem_title", parsedTitle)
        val titleSlug = meta.optString("problem_slug", parsedSlug)
        val difficulty = meta.optString("difficulty", "Medium")
        val language = meta.optString("language", "python3")
        val runtime = meta.optString("runtime", "")
        val runtimePercentile = meta.optString("runtime_percentile", "")
        val memory = meta.optString("memory", "")
        val memoryPercentile = meta.optString("memory_percentile", "")
        val submittedAt = meta.optString("submitted_at", "")
        val problemUrl = meta.optString("problem_url", "https://leetcode.com/problems/$titleSlug/")
        val isOffline = meta.optBoolean("is_downloaded_offline", false) || File(dir, "content.html").exists()
        val lastSynced = meta.optLong("last_synced_millis", dir.lastModified())

        // Topic tags
        val topicTags = mutableListOf<String>()
        val tagsArr = meta.optJSONArray("topic_tags")
        if (tagsArr != null) {
            for (i in 0 until tagsArr.length()) {
                topicTags.add(tagsArr.getString(i))
            }
        }

        // Hints
        val hints = mutableListOf<String>()
        val hintArr = meta.optJSONArray("hints")
        if (hintArr != null) {
            for (i in 0 until hintArr.length()) {
                hints.add(hintArr.getString(i))
            }
        }

        // Problem statement content (content.html or fallback description.txt)
        val contentHtml = when {
            File(dir, "content.html").exists() -> File(dir, "content.html").readText()
            File(dir, "description.txt").exists() -> File(dir, "description.txt").readText()
            else -> ""
        }

        // Solution code file
        val solutionFile = dir.listFiles { f ->
            f.isFile && (f.name.startsWith("solution.", ignoreCase = true) || f.name.startsWith("Solution.", ignoreCase = true))
        }?.firstOrNull()
        val solutionCode = solutionFile?.readText() ?: meta.optString("solution_code", "")

        // Dedicated notes/ folder for multiple note files
        val notesDir = File(dir, "notes")
        val notes = mutableListOf<ProblemNote>()
        if (notesDir.exists() && notesDir.isDirectory) {
            val noteFiles = notesDir.listFiles { f -> f.isFile && (f.name.endsWith(".md") || f.name.endsWith(".txt")) }
                ?: emptyArray()
            for (nf in noteFiles) {
                val nContent = nf.readText()
                val nTitle = nf.nameWithoutExtension.replace('_', ' ').replace('-', ' ')
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                notes.add(
                    ProblemNote(
                        filename = nf.name,
                        title = nTitle,
                        content = nContent,
                        lastModifiedMillis = nf.lastModified()
                    )
                )
            }
        }

        // If no note files exist but metadata had a legacy note, seed notes/general.md
        if (notes.isEmpty() && meta.has("notes") && meta.getString("notes").isNotBlank()) {
            val legacyContent = meta.getString("notes")
            val defaultNote = File(notesDir.apply { mkdirs() }, "general.md")
            defaultNote.writeText(legacyContent)
            notes.add(
                ProblemNote(
                    filename = "general.md",
                    title = "General Notes",
                    content = legacyContent,
                    lastModifiedMillis = defaultNote.lastModified()
                )
            )
        }

        return OfflineProblem(
            folderName = folderName,
            frontendId = frontendId,
            title = title,
            titleSlug = titleSlug,
            difficulty = difficulty,
            contentHtml = contentHtml,
            topicTags = topicTags,
            hints = hints,
            solutionCode = solutionCode,
            solutionLanguage = language,
            runtime = runtime,
            runtimePercentile = runtimePercentile,
            memory = memory,
            memoryPercentile = memoryPercentile,
            submittedAt = submittedAt,
            problemUrl = problemUrl,
            notes = notes,
            isDownloadedOffline = isOffline,
            lastSyncedMillis = lastSynced
        )
    }

    private fun seedDefaultProblemsIfEmpty() {
        val dirs = baseProblemsDir.listFiles { file -> file.isDirectory }
        if (!dirs.isNullOrEmpty()) return

        // Seed 1: Two Sum
        val twoSumDir = File(baseProblemsDir, "0001-two-sum").apply { mkdirs() }
        File(twoSumDir, "metadata.json").writeText(
            """
            {
              "submission_id": "11823901",
              "problem_id": "1",
              "frontend_id": "1",
              "problem_title": "Two Sum",
              "problem_slug": "two-sum",
              "difficulty": "Easy",
              "language": "python3",
              "runtime": "36 ms",
              "runtime_percentile": "91.2%",
              "memory": "17.4 MB",
              "memory_percentile": "84.5%",
              "submitted_at": "2026-10-15T14:20:00Z",
              "topic_tags": ["Array", "Hash Table"],
              "hints": ["A really brute force way would be to search for all possible pairs of numbers but that would be too slow.", "Can we use extra space such as a hash map to look up complements in O(1)?"],
              "is_downloaded_offline": true
            }
            """.trimIndent()
        )
        File(twoSumDir, "solution.py").writeText(
            """
            class Solution:
                def twoSum(self, nums: List[int], target: int) -> List[int]:
                    seen = {}
                    for i, x in enumerate(nums):
                        complement = target - x
                        if complement in seen:
                            return [seen[complement], i]
                        seen[x] = i
                    return []
            """.trimIndent()
        )
        File(twoSumDir, "content.html").writeText(
            """
            <p>Given an array of integers <code>nums</code>&nbsp;and an integer <code>target</code>, return <em>indices of the two numbers such that they add up to <code>target</code></em>.</p>
            <p>You may assume that each input would have <strong><em>exactly</em> one solution</strong>, and you may not use the <em>same</em> element twice.</p>
            <p>You can return the answer in any order.</p>

            <p>&nbsp;</p>
            <p><strong class="example">Example 1:</strong></p>
            <pre><strong>Input:</strong> nums = [2,7,11,15], target = 9
<strong>Output:</strong> [0,1]
<strong>Explanation:</strong> Because nums[0] + nums[1] == 9, we return [0, 1].</pre>

            <p><strong class="example">Example 2:</strong></p>
            <pre><strong>Input:</strong> nums = [3,2,4], target = 6
<strong>Output:</strong> [1,2]</pre>

            <p><strong class="example">Example 3:</strong></p>
            <pre><strong>Input:</strong> nums = [3,3], target = 6
<strong>Output:</strong> [0,1]</pre>

            <p>&nbsp;</p>
            <p><strong>Constraints:</strong></p>
            <ul>
                <li><code>2 &lt;= nums.length &lt;= 10<sup>4</sup></code></li>
                <li><code>-10<sup>9</sup> &lt;= nums[i] &lt;= 10<sup>9</sup></code></li>
                <li><code>-10<sup>9</sup> &lt;= target &lt;= 10<sup>9</sup></code></li>
                <li><strong>Only one valid answer exists.</strong></li>
            </ul>
            """.trimIndent()
        )
        val twoSumNotes = File(twoSumDir, "notes").apply { mkdirs() }
        File(twoSumNotes, "intuition.md").writeText(
            """
            # Intuition & Hash Map Lookup
            Instead of checking every pair with O(N^2) brute force, we trade space for time using a single-pass hash map:
            - Store each number along with its index as we iterate.
            - Check if `target - num` already exists in our dictionary.
            - Time Complexity: O(N)
            - Space Complexity: O(N)
            """.trimIndent()
        )
        File(twoSumNotes, "edge_cases.md").writeText(
            """
            # Edge Cases to Watch Out For
            1. Negative numbers in `nums`: Works seamlessly with hash map complement arithmetic.
            2. Two duplicate numbers summing to target (e.g. `[3, 3]`, target `6`): Hash map handles this if we check for complement *before* recording current number!
            """.trimIndent()
        )

        // Seed 2: 3Sum
        val threeSumDir = File(baseProblemsDir, "0015-3sum").apply { mkdirs() }
        File(threeSumDir, "metadata.json").writeText(
            """
            {
              "submission_id": "11902451",
              "problem_id": "15",
              "frontend_id": "15",
              "problem_title": "3Sum",
              "problem_slug": "3sum",
              "difficulty": "Medium",
              "language": "cpp",
              "runtime": "48 ms",
              "runtime_percentile": "89.6%",
              "memory": "24.1 MB",
              "memory_percentile": "82.0%",
              "submitted_at": "2026-10-18T10:15:00Z",
              "topic_tags": ["Array", "Two Pointers", "Sorting"],
              "hints": ["So, we essentially need to find three numbers x, y, and z such that they add up to the given value.", "If we sort the array, can we fix x and use two pointers to find y and z?"],
              "is_downloaded_offline": true
            }
            """.trimIndent()
        )
        File(threeSumDir, "solution.cpp").writeText(
            """
            #include <vector>
            #include <algorithm>

            class Solution {
            public:
                std::vector<std::vector<int>> threeSum(std::vector<int>& nums) {
                    std::vector<std::vector<int>> res;
                    std::sort(nums.begin(), nums.end());
                    int n = nums.size();
                    for (int i = 0; i < n - 2; ++i) {
                        if (i > 0 && nums[i] == nums[i - 1]) continue;
                        if (nums[i] > 0) break;
                        int l = i + 1, r = n - 1;
                        while (l < r) {
                            int sum = nums[i] + nums[l] + nums[r];
                            if (sum == 0) {
                                res.push_back({nums[i], nums[l], nums[r]});
                                while (l < r && nums[l] == nums[l + 1]) l++;
                                while (l < r && nums[r] == nums[r - 1]) r--;
                                l++; r--;
                            } else if (sum < 0) {
                                l++;
                            } else {
                                r--;
                            }
                        }
                    }
                    return res;
                }
            };
            """.trimIndent()
        )
        File(threeSumDir, "content.html").writeText(
            """
            <p>Given an integer array nums, return all the triplets <code>[nums[i], nums[j], nums[k]]</code> such that <code>i != j</code>, <code>i != k</code>, and <code>j != k</code>, and <code>nums[i] + nums[j] + nums[k] == 0</code>.</p>
            <p>Notice that the solution set must not contain duplicate triplets.</p>

            <p>&nbsp;</p>
            <p><strong class="example">Example 1:</strong></p>
            <pre><strong>Input:</strong> nums = [-1,0,1,2,-1,-4]
<strong>Output:</strong> [[-1,-1,2],[-1,0,1]]
<strong>Explanation:</strong> 
nums[0] + nums[1] + nums[2] = (-1) + 0 + 1 = 0.
nums[1] + nums[2] + nums[4] = 0 + 1 + (-1) = 0.
The distinct triplets are [-1,0,1] and [-1,-1,2].</pre>

            <p><strong class="example">Example 2:</strong></p>
            <pre><strong>Input:</strong> nums = [0,1,1]
<strong>Output:</strong> []
<strong>Explanation:</strong> The only possible triplet does not sum up to 0.</pre>

            <p>&nbsp;</p>
            <p><strong>Constraints:</strong></p>
            <ul>
                <li><code>3 &lt;= nums.length &lt;= 3000</code></li>
                <li><code>-10<sup>5</sup> &lt;= nums[i] &lt;= 10<sup>5</sup></code></li>
            </ul>
            """.trimIndent()
        )
        val threeSumNotes = File(threeSumDir, "notes").apply { mkdirs() }
        File(threeSumNotes, "two_pointers_strategy.md").writeText(
            """
            # Two Pointers Strategy
            - Sorting takes O(N log N).
            - Fix element i and use two pointers (`left` and `right`) from i+1 to N-1.
            - Skip duplicate elements for both i, left, and right to avoid duplicate triplets.
            - Total time: O(N^2).
            """.trimIndent()
        )

        // Seed 3: Valid Parentheses
        val validParenDir = File(baseProblemsDir, "0020-valid-parentheses").apply { mkdirs() }
        File(validParenDir, "metadata.json").writeText(
            """
            {
              "submission_id": "11984210",
              "problem_id": "20",
              "frontend_id": "20",
              "problem_title": "Valid Parentheses",
              "problem_slug": "valid-parentheses",
              "difficulty": "Easy",
              "language": "python3",
              "runtime": "28 ms",
              "runtime_percentile": "96.4%",
              "memory": "16.4 MB",
              "memory_percentile": "90.1%",
              "submitted_at": "2026-10-20T16:45:00Z",
              "topic_tags": ["String", "Stack"],
              "hints": ["Use a stack of characters.", "When you encounter an opening bracket, push it to the top. When you encounter a closing bracket, check if the top matches."],
              "is_downloaded_offline": true
            }
            """.trimIndent()
        )
        File(validParenDir, "solution.py").writeText(
            """
            class Solution:
                def isValid(self, s: str) -> bool:
                    stack = []
                    pairs = {')': '(', '}': '{', ']': '['}
                    for ch in s:
                        if ch in pairs:
                            if not stack or stack[-1] != pairs[ch]:
                                return False
                            stack.pop()
                        else:
                            stack.append(ch)
                    return len(stack) == 0
            """.trimIndent()
        )
        File(validParenDir, "content.html").writeText(
            """
            <p>Given a string <code>s</code> containing just the characters <code>'('</code>, <code>')'</code>, <code>'{'</code>, <code>'}'</code>, <code>'['</code> and <code>']'</code>, determine if the input string is valid.</p>
            <p>An input string is valid if:</p>
            <ol>
                <li>Open brackets must be closed by the same type of brackets.</li>
                <li>Open brackets must be closed in the correct order.</li>
                <li>Every close bracket has a corresponding open bracket of the same type.</li>
            </ol>

            <p>&nbsp;</p>
            <p><strong class="example">Example 1:</strong></p>
            <pre><strong>Input:</strong> s = "()"
<strong>Output:</strong> true</pre>

            <p><strong class="example">Example 2:</strong></p>
            <pre><strong>Input:</strong> s = "()[]{}"
<strong>Output:</strong> true</pre>

            <p><strong class="example">Example 3:</strong></p>
            <pre><strong>Input:</strong> s = "(]"
<strong>Output:</strong> false</pre>

            <p>&nbsp;</p>
            <p><strong>Constraints:</strong></p>
            <ul>
                <li><code>1 &lt;= s.length &lt;= 10<sup>4</sup></code></li>
                <li><code>s</code> consists of parentheses only <code>'()[]{}'</code>.</li>
            </ul>
            """.trimIndent()
        )
        val validParenNotes = File(validParenDir, "notes").apply { mkdirs() }
        File(validParenNotes, "stack_invariants.md").writeText(
            """
            # Stack Invariants
            - Every closing bracket must match the most recently opened bracket.
            - If stack is empty when receiving closing bracket -> immediately invalid.
            - At end of loop, `len(stack) == 0` must hold true.
            """.trimIndent()
        )
    }

    private fun seedDefaultGeneralNotesIfEmpty() {
        val files = generalNotesDir.listFiles()
        if (!files.isNullOrEmpty()) return

        val dpNote = File(generalNotesDir, "dp_patterns.md")
        dpNote.writeText(
            """
            # Dynamic Programming Framework & Patterns

            ### 1. 0/1 Knapsack Pattern
            - State: `dp[i][w]` represents maximum value using first `i` items with capacity `w`.
            - Transition: `dp[i][w] = max(dp[i-1][w], dp[i-1][w - weights[i]] + values[i])`.
            - Space optimization: 1D array traversed backwards.

            ### 2. Longest Common Subsequence (LCS)
            - If `s1[i] == s2[j]`: `dp[i][j] = 1 + dp[i-1][j-1]`
            - Else: `dp[i][j] = max(dp[i-1][j], dp[i][j-1])`
            """.trimIndent()
        )

        val bsNote = File(generalNotesDir, "binary_search_templates.md")
        bsNote.writeText(
            """
            # Binary Search Invariants Template

            ```python
            def binary_search(nums, target):
                left, right = 0, len(nums) - 1
                while left <= right:
                    mid = left + (right - left) // 2
                    if nums[mid] == target:
                        return mid
                    elif nums[mid] < target:
                        left = mid + 1
                    else:
                        right = mid - 1
                return -1
            ```
            - Upper bound vs Lower bound search invariants.
            """.trimIndent()
        )
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
