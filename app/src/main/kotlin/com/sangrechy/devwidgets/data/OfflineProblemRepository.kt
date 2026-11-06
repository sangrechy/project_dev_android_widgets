package com.sangrechy.devwidgets.data

import android.content.Context
import com.sangrechy.devwidgets.data.model.CustomNote
import com.sangrechy.devwidgets.data.model.LeetCodeSearchResult
import com.sangrechy.devwidgets.data.model.OfflineProblem
import com.sangrechy.devwidgets.data.model.ProblemNote
import com.sangrechy.devwidgets.data.network.DevApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

interface ProblemRepository {
    suspend fun getProblems(): List<OfflineProblem>
    suspend fun getProblemDetail(folderName: String): OfflineProblem?
    suspend fun downloadProblemOffline(folderName: String, titleSlug: String): Result<OfflineProblem>
    suspend fun searchLeetCode(query: String): Result<List<LeetCodeSearchResult>>
    suspend fun addProblemFromLeetCode(input: String): Result<OfflineProblem>
    suspend fun deleteProblemOffline(folderName: String): Boolean
    suspend fun saveProblemNote(folderName: String, filename: String, content: String): ProblemNote
    suspend fun saveProblemDocument(folderName: String, filename: String, bytes: ByteArray): ProblemNote
    suspend fun deleteProblemNote(folderName: String, filename: String): Boolean
    suspend fun syncFromGitHubBackupRepo(owner: String, repo: String, token: String = ""): Result<Int>
    suspend fun syncSingleProblemFromGitHub(folderName: String): Result<OfflineProblem>
    suspend fun getGeneralNotes(): List<CustomNote>
    suspend fun saveGeneralNote(note: CustomNote): CustomNote
    suspend fun saveGeneralDocument(filename: String, bytes: ByteArray): CustomNote
    suspend fun deleteGeneralNote(id: String): Boolean
}

class OfflineProblemRepository(
    private val context: Context,
    private val okHttpClient: OkHttpClient = DevApiService.defaultClient()
) : ProblemRepository {

    private val prefs by lazy { WidgetPreferences(context) }

    private val baseProblemsDir: File
        get() = File(context.filesDir, "problems/leetcode").apply { mkdirs() }

    private val rootNotesDir: File
        get() = File(context.filesDir, "notes").apply { mkdirs() }

    private fun getProblemNotesDir(folderName: String): File {
        return File(rootNotesDir, folderName).apply { mkdirs() }
    }

    private val generalNotesDir: File
        get() = File(rootNotesDir, "general").apply { mkdirs() }

    init {
        // Ensure default seed data exists on initial launch
        migrateNotesToUnifiedRoot()
        seedDefaultProblemsIfEmpty()
        seedDefaultGeneralNotesIfEmpty()
        repairProblemDirectories()
    }

    private fun getLanguageExtensions(lang: String): List<String> = when (lang.lowercase().trim()) {
        "cpp", "c++" -> listOf(".cpp", ".cc", ".cxx", ".hpp", ".h")
        "python", "python3", "py" -> listOf(".py", ".py3")
        "java" -> listOf(".java")
        "javascript", "js" -> listOf(".js", ".mjs")
        "typescript", "ts" -> listOf(".ts")
        "c" -> listOf(".c", ".h")
        "csharp", "c#" -> listOf(".cs")
        "golang", "go" -> listOf(".go")
        "rust", "rs" -> listOf(".rs")
        "kotlin", "kt" -> listOf(".kt", ".kts")
        "swift" -> listOf(".swift")
        "ruby", "rb" -> listOf(".rb")
        "scala" -> listOf(".scala")
        "sql" -> listOf(".sql")
        "php" -> listOf(".php")
        "dart" -> listOf(".dart")
        else -> if (lang.isNotBlank()) listOf(".$lang") else emptyList()
    }

    private fun inferLanguageFromExtension(ext: String): String = when (ext.lowercase()) {
        "cpp", "cc", "cxx" -> "cpp"
        "c" -> "c"
        "py", "py3" -> "python3"
        "java" -> "java"
        "js", "mjs" -> "javascript"
        "ts" -> "typescript"
        "cs" -> "csharp"
        "go" -> "golang"
        "rs" -> "rust"
        "kt", "kts" -> "kotlin"
        "swift" -> "swift"
        "rb" -> "ruby"
        "scala" -> "scala"
        "sql" -> "sql"
        "php" -> "php"
        "dart" -> "dart"
        else -> ext.lowercase()
    }

    private fun isDefaultStubFile(file: File): Boolean {
        if (!file.exists() || !file.isFile) return false
        val text = try { file.readText() } catch (_: Exception) { return false }
        val trimmed = text.trim()
        return (trimmed.startsWith("# LeetCode #") || trimmed.startsWith("// LeetCode #")) &&
            (trimmed.endsWith("pass") || trimmed.endsWith("return null;") || trimmed.endsWith("return 0;") ||
             trimmed.lines().size <= 10)
    }

    private fun isBinaryFileName(filename: String): Boolean {
        val ext = filename.substringAfterLast('.', "").lowercase()
        return ext in setOf(
            "pdf", "png", "jpg", "jpeg", "webp", "gif", "bmp", "doc", "docx",
            "xls", "xlsx", "ppt", "pptx", "zip", "rar", "tar", "gz", "7z", "bin"
        )
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        return "%.1f %s".format(bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    private fun migrateNotesToUnifiedRoot() {
        try {
            // 1. Migrate problem-specific notes from problems/leetcode/<folder>/notes -> notes/<folder>/
            val dirs = baseProblemsDir.listFiles { file -> file.isDirectory } ?: emptyArray()
            for (dir in dirs) {
                val oldNotes = File(dir, "notes")
                if (oldNotes.exists() && oldNotes.isDirectory) {
                    val destDir = getProblemNotesDir(dir.name)
                    oldNotes.listFiles()?.forEach { nf ->
                        val target = File(destDir, nf.name)
                        if (!target.exists()) nf.copyTo(target)
                        nf.delete()
                    }
                    oldNotes.delete()
                }
            }

            // 2. Migrate legacy general_notes -> notes/general/
            val legacyGeneral = File(context.filesDir, "general_notes")
            if (legacyGeneral.exists() && legacyGeneral.isDirectory) {
                legacyGeneral.listFiles()?.forEach { f ->
                    val target = File(generalNotesDir, f.name)
                    if (!target.exists()) f.copyTo(target)
                    f.delete()
                }
                legacyGeneral.delete()
            }
        } catch (_: Exception) {}
    }

    private data class LeetCodeQuestionDetails(
        val questionId: String,
        val frontendId: String,
        val title: String,
        val titleSlug: String,
        val difficulty: String,
        val contentHtml: String,
        val topicTags: List<String>,
        val hints: List<String>,
        val codeSnippets: Map<String, String>
    )

    private fun repairProblemDirectories() {
        val dirs = baseProblemsDir.listFiles { file -> file.isDirectory } ?: return
        for (dir in dirs) {
            try {
                val folderName = dir.name
                val metaFile = File(dir, "metadata.json")
                val meta = if (metaFile.exists()) {
                    try { JSONObject(metaFile.readText()) } catch (_: Exception) { JSONObject() }
                } else JSONObject()

                var fid = meta.optString("frontend_id").toIntOrNull()
                val slug = meta.optString("problem_slug").ifBlank {
                    if (folderName.contains('-') && folderName.substringBefore('-').all { it.isDigit() }) {
                        folderName.substringAfter('-')
                    } else folderName
                }

                if (fid == null) {
                    when (slug) {
                        "two-sum" -> fid = 1
                        "3sum" -> fid = 15
                        "valid-parentheses" -> 20
                        "best-time-to-buy-and-sell-stock" -> 121
                        "trapping-rain-water" -> 42
                    }
                }

                if (fid != null && slug.isNotBlank()) {
                    val canonicalName = "%04d-%s".format(fid, slug)
                    if (folderName != canonicalName) {
                        val canonicalDir = File(baseProblemsDir, canonicalName)
                        if (!canonicalDir.exists()) {
                            dir.renameTo(canonicalDir)
                            val cMetaFile = File(canonicalDir, "metadata.json")
                            val cMeta = if (cMetaFile.exists()) {
                                try { JSONObject(cMetaFile.readText()) } catch (_: Exception) { JSONObject() }
                            } else JSONObject()
                            cMeta.put("frontend_id", fid.toString())
                            cMeta.put("problem_slug", slug)
                            if (!cMeta.has("problem_title")) {
                                cMeta.put("problem_title", slug.split('-').joinToString(" ") { word ->
                                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                })
                            }
                            cMetaFile.writeText(cMeta.toString(2))

                            val oldNotesDir = getProblemNotesDir(folderName)
                            val newNotesDir = getProblemNotesDir(canonicalName)
                            if (oldNotesDir.exists() && oldNotesDir != newNotesDir) {
                                oldNotesDir.listFiles()?.forEach { nf ->
                                    val target = File(newNotesDir, nf.name)
                                    if (!target.exists()) nf.copyTo(target)
                                }
                                oldNotesDir.deleteRecursively()
                            }
                        } else {
                            // Canonical already exists (e.g. 0015-3sum vs 3sum), migrate notes and delete duplicate
                            val oldNotesDir = getProblemNotesDir(folderName)
                            val newNotesDir = getProblemNotesDir(canonicalName)
                            oldNotesDir.listFiles()?.forEach { nf ->
                                val targetNf = File(newNotesDir, nf.name)
                                if (!targetNf.exists()) nf.copyTo(targetNf)
                            }
                            oldNotesDir.deleteRecursively()
                            dir.deleteRecursively()
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun fetchLeetCodeQuestionDetails(titleSlug: String): LeetCodeQuestionDetails? {
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
                codeSnippets {
                  lang
                  langSlug
                  code
                }
              }
            }
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("query", query)
            put("variables", JSONObject().apply { put("titleSlug", titleSlug) })
        }

        val request = Request.Builder()
            .url("https://leetcode.com/graphql")
            .header("Content-Type", "application/json")
            .header("Referer", "https://leetcode.com/problems/$titleSlug/")
            .header("Origin", "https://leetcode.com")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val root = JSONObject(body)
                val q = root.optJSONObject("data")?.optJSONObject("question") ?: return null

                val qId = q.optString("questionId", "")
                val fId = q.optString("questionFrontendId", "")
                val title = q.optString("title", "")
                val slug = q.optString("titleSlug", titleSlug)
                val content = q.optString("content", "")
                val diff = q.optString("difficulty", "Medium")

                val tags = mutableListOf<String>()
                val tagArr = q.optJSONArray("topicTags")
                if (tagArr != null) {
                    for (i in 0 until tagArr.length()) {
                        val tName = tagArr.getJSONObject(i).optString("name")
                        if (tName.isNotBlank()) tags.add(tName)
                    }
                }

                val hints = mutableListOf<String>()
                val hintArr = q.optJSONArray("hints")
                if (hintArr != null) {
                    for (i in 0 until hintArr.length()) {
                        hints.add(hintArr.getString(i))
                    }
                }

                val snippets = mutableMapOf<String, String>()
                val snipArr = q.optJSONArray("codeSnippets")
                if (snipArr != null) {
                    for (i in 0 until snipArr.length()) {
                        val sObj = snipArr.getJSONObject(i)
                        val langSlug = sObj.optString("langSlug")
                        val code = sObj.optString("code")
                        if (langSlug.isNotBlank() && code.isNotBlank()) {
                            snippets[langSlug] = code
                        }
                    }
                }

                LeetCodeQuestionDetails(
                    questionId = qId,
                    frontendId = fId,
                    title = title,
                    titleSlug = slug,
                    difficulty = diff,
                    contentHtml = content,
                    topicTags = tags,
                    hints = hints,
                    codeSnippets = snippets
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private val catalogFile: File
        get() = File(context.filesDir, "leetcode_catalog.json")

    private val inMemoryCatalog = mutableMapOf<String, LeetCodeSearchResult>()
    private var isCatalogSeeded = false

    private fun ensureCatalogLoaded(): List<LeetCodeSearchResult> {
        if (!isCatalogSeeded) {
            // 1. Read from catalogFile if present
            if (catalogFile.exists()) {
                try {
                    val arr = JSONArray(catalogFile.readText())
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val fid = obj.optString("frontendId")
                        val title = obj.optString("title")
                        val slug = obj.optString("titleSlug")
                        val diff = obj.optString("difficulty", "Medium")
                        val tArr = obj.optJSONArray("topicTags")
                        val tags = mutableListOf<String>()
                        if (tArr != null) {
                            for (j in 0 until tArr.length()) {
                                tags.add(tArr.getString(j))
                            }
                        }
                        if (slug.isNotBlank() && title.isNotBlank()) {
                            inMemoryCatalog[slug] = LeetCodeSearchResult(
                                frontendId = fid,
                                title = title,
                                titleSlug = slug,
                                difficulty = diff,
                                topicTags = tags
                            )
                        }
                    }
                } catch (_: Exception) {}
            }

            // 2. Also inject all currently saved problems into catalog
            val savedDirs = baseProblemsDir.listFiles { f -> f.isDirectory } ?: emptyArray()
            for (dir in savedDirs) {
                try {
                    val metaFile = File(dir, "metadata.json")
                    if (metaFile.exists()) {
                        val meta = JSONObject(metaFile.readText())
                        val fid = meta.optString("frontend_id", "")
                        val title = meta.optString("problem_title", "")
                        val slug = meta.optString("problem_slug", "")
                        val diff = meta.optString("difficulty", "Medium")
                        if (slug.isNotBlank() && title.isNotBlank() && !inMemoryCatalog.containsKey(slug)) {
                            inMemoryCatalog[slug] = LeetCodeSearchResult(
                                frontendId = fid,
                                title = title,
                                titleSlug = slug,
                                difficulty = diff
                            )
                        }
                    }
                } catch (_: Exception) {}
            }

            // 3. Seed classic popular problems if catalog is small
            if (inMemoryCatalog.size < 50) {
                seedInitialProblemCatalog()
            }

            isCatalogSeeded = true
        }
        return inMemoryCatalog.values.sortedWith(
            compareBy({ it.frontendId.toIntOrNull() ?: 999999 }, { it.title })
        )
    }

    private fun persistCatalog() {
        try {
            val arr = JSONArray()
            inMemoryCatalog.values.forEach { item ->
                val obj = JSONObject().apply {
                    put("frontendId", item.frontendId)
                    put("title", item.title)
                    put("titleSlug", item.titleSlug)
                    put("difficulty", item.difficulty)
                    val tArr = JSONArray()
                    item.topicTags.forEach { tArr.put(it) }
                    put("topicTags", tArr)
                }
                arr.put(obj)
            }
            catalogFile.writeText(arr.toString())
        } catch (_: Exception) {}
    }

    private fun seedInitialProblemCatalog() {
        fun s(fid: String, title: String, slug: String, diff: String, vararg tags: String) {
            inMemoryCatalog[slug] = LeetCodeSearchResult(fid, title, slug, diff, tags.toList())
        }
        s("1", "Two Sum", "two-sum", "Easy", "Array", "Hash Table")
        s("2", "Add Two Numbers", "add-two-numbers", "Medium", "Linked List", "Math")
        s("3", "Longest Substring Without Repeating Characters", "longest-substring-without-repeating-characters", "Medium", "Hash Table", "String", "Sliding Window")
        s("4", "Median of Two Sorted Arrays", "median-of-two-sorted-arrays", "Hard", "Array", "Binary Search")
        s("5", "Longest Palindromic Substring", "longest-palindromic-substring", "Medium", "String", "Dynamic Programming")
        s("6", "Zigzag Conversion", "zigzag-conversion", "Medium", "String")
        s("7", "Reverse Integer", "reverse-integer", "Medium", "Math")
        s("8", "String to Integer (atoi)", "string-to-integer-atoi", "Medium", "String")
        s("9", "Palindrome Number", "palindrome-number", "Easy", "Math")
        s("10", "Regular Expression Matching", "regular-expression-matching", "Hard", "String", "Dynamic Programming")
        s("11", "Container With Most Water", "container-with-most-water", "Medium", "Array", "Two Pointers", "Greedy")
        s("12", "Integer to Roman", "integer-to-roman", "Medium", "Hash Table", "Math", "String")
        s("13", "Roman to Integer", "roman-to-integer", "Easy", "Hash Table", "Math", "String")
        s("14", "Longest Common Prefix", "longest-common-prefix", "Easy", "String", "Trie")
        s("15", "3Sum", "3sum", "Medium", "Array", "Two Pointers", "Sorting")
        s("16", "3Sum Closest", "3sum-closest", "Medium", "Array", "Two Pointers")
        s("17", "Letter Combinations of a Phone Number", "letter-combinations-of-a-phone-number", "Medium", "Hash Table", "String", "Backtracking")
        s("18", "4Sum", "4sum", "Medium", "Array", "Two Pointers", "Sorting")
        s("19", "Remove Nth Node From End of List", "remove-nth-node-from-end-of-list", "Medium", "Linked List", "Two Pointers")
        s("20", "Valid Parentheses", "valid-parentheses", "Easy", "String", "Stack")
        s("21", "Merge Two Sorted Lists", "merge-two-sorted-lists", "Easy", "Linked List", "Recursion")
        s("22", "Generate Parentheses", "generate-parentheses", "Medium", "String", "Dynamic Programming", "Backtracking")
        s("23", "Merge k Sorted Lists", "merge-k-sorted-lists", "Hard", "Linked List", "Divide and Conquer", "Heap")
        s("24", "Swap Nodes in Pairs", "swap-nodes-in-pairs", "Medium", "Linked List", "Recursion")
        s("25", "Reverse Nodes in k-Group", "reverse-nodes-in-k-group", "Hard", "Linked List", "Recursion")
        s("26", "Remove Duplicates from Sorted Array", "remove-duplicates-from-sorted-array", "Easy", "Array", "Two Pointers")
        s("27", "Remove Element", "remove-element", "Easy", "Array", "Two Pointers")
        s("28", "Find the Index of the First Occurrence in a String", "find-the-index-of-the-first-occurrence-in-a-string", "Easy", "Two Pointers", "String")
        s("33", "Search in Rotated Sorted Array", "search-in-rotated-sorted-array", "Medium", "Array", "Binary Search")
        s("34", "Find First and Last Position of Element in Sorted Array", "find-first-and-last-position-of-element-in-sorted-array", "Medium", "Array", "Binary Search")
        s("35", "Search Insert Position", "search-insert-position", "Easy", "Array", "Binary Search")
        s("39", "Combination Sum", "combination-sum", "Medium", "Array", "Backtracking")
        s("40", "Combination Sum II", "combination-sum-ii", "Medium", "Array", "Backtracking")
        s("42", "Trapping Rain Water", "trapping-rain-water", "Hard", "Array", "Two Pointers", "Dynamic Programming", "Stack")
        s("46", "Permutations", "permutations", "Medium", "Array", "Backtracking")
        s("48", "Rotate Image", "rotate-image", "Medium", "Array", "Math", "Matrix")
        s("49", "Group Anagrams", "group-anagrams", "Medium", "Array", "Hash Table", "String", "Sorting")
        s("50", "Pow(x, n)", "powx-n", "Medium", "Math", "Recursion")
        s("53", "Maximum Subarray", "maximum-subarray", "Medium", "Array", "Dynamic Programming")
        s("54", "Spiral Matrix", "spiral-matrix", "Medium", "Array", "Matrix")
        s("55", "Jump Game", "jump-game", "Medium", "Array", "Dynamic Programming", "Greedy")
        s("56", "Merge Intervals", "merge-intervals", "Medium", "Array", "Sorting")
        s("62", "Unique Paths", "unique-paths", "Medium", "Math", "Dynamic Programming")
        s("64", "Minimum Path Sum", "minimum-path-sum", "Medium", "Array", "Dynamic Programming")
        s("70", "Climbing Stairs", "climbing-stairs", "Easy", "Math", "Dynamic Programming")
        s("72", "Edit Distance", "edit-distance", "Medium", "String", "Dynamic Programming")
        s("74", "Search a 2D Matrix", "search-a-2d-matrix", "Medium", "Array", "Binary Search")
        s("75", "Sort Colors", "sort-colors", "Medium", "Array", "Two Pointers", "Sorting")
        s("76", "Minimum Window Substring", "minimum-window-substring", "Hard", "Hash Table", "String", "Sliding Window")
        s("78", "Subsets", "subsets", "Medium", "Array", "Backtracking")
        s("79", "Word Search", "word-search", "Medium", "Array", "String", "Backtracking")
        s("84", "Largest Rectangle in Histogram", "largest-rectangle-in-histogram", "Hard", "Array", "Stack")
        s("88", "Merge Sorted Array", "merge-sorted-array", "Easy", "Array", "Two Pointers", "Sorting")
        s("94", "Binary Tree Inorder Traversal", "binary-tree-inorder-traversal", "Easy", "Stack", "Tree", "Binary Tree")
        s("98", "Validate Binary Search Tree", "validate-binary-search-tree", "Medium", "Tree", "Binary Search Tree")
        s("100", "Same Tree", "same-tree", "Easy", "Tree", "Binary Tree")
        s("101", "Symmetric Tree", "symmetric-tree", "Easy", "Tree", "Binary Tree")
        s("102", "Binary Tree Level Order Traversal", "binary-tree-level-order-traversal", "Medium", "Tree", "Breadth-First Search")
        s("104", "Maximum Depth of Binary Tree", "maximum-depth-of-binary-tree", "Easy", "Tree", "Depth-First Search")
        s("105", "Construct Binary Tree from Preorder and Inorder Traversal", "construct-binary-tree-from-preorder-and-inorder-traversal", "Medium", "Array", "Tree")
        s("121", "Best Time to Buy and Sell Stock", "best-time-to-buy-and-sell-stock", "Easy", "Array", "Dynamic Programming")
        s("122", "Best Time to Buy and Sell Stock II", "best-time-to-buy-and-sell-stock-ii", "Medium", "Array", "Dynamic Programming")
        s("124", "Binary Tree Maximum Path Sum", "binary-tree-maximum-path-sum", "Hard", "Dynamic Programming", "Tree")
        s("128", "Longest Consecutive Sequence", "longest-consecutive-sequence", "Medium", "Array", "Hash Table")
        s("133", "Clone Graph", "clone-graph", "Medium", "Hash Table", "Depth-First Search", "Graph")
        s("136", "Single Number", "single-number", "Easy", "Array", "Bit Manipulation")
        s("138", "Copy List with Random Pointer", "copy-list-with-random-pointer", "Medium", "Hash Table", "Linked List")
        s("139", "Word Break", "word-break", "Medium", "Array", "Dynamic Programming")
        s("141", "Linked List Cycle", "linked-list-cycle", "Easy", "Hash Table", "Linked List")
        s("142", "Linked List Cycle II", "linked-list-cycle-ii", "Medium", "Hash Table", "Linked List")
        s("146", "LRU Cache", "lru-cache", "Medium", "Hash Table", "Linked List", "Design")
        s("150", "Evaluate Reverse Polish Notation", "evaluate-reverse-polish-notation", "Medium", "Array", "Stack")
        s("152", "Maximum Product Subarray", "maximum-product-subarray", "Medium", "Array", "Dynamic Programming")
        s("153", "Find Minimum in Rotated Sorted Array", "find-minimum-in-rotated-sorted-array", "Medium", "Array", "Binary Search")
        s("155", "Min Stack", "min-stack", "Medium", "Stack", "Design")
        s("160", "Intersection of Two Linked Lists", "intersection-of-two-linked-lists", "Easy", "Hash Table", "Linked List")
        s("169", "Majority Element", "majority-element", "Easy", "Array", "Hash Table")
        s("198", "House Robber", "house-robber", "Medium", "Array", "Dynamic Programming")
        s("200", "Number of Islands", "number-of-islands", "Medium", "Array", "Depth-First Search", "Graph")
        s("206", "Reverse Linked List", "reverse-linked-list", "Easy", "Linked List", "Recursion")
        s("207", "Course Schedule", "course-schedule", "Medium", "Depth-First Search", "Graph")
        s("208", "Implement Trie (Prefix Tree)", "implement-trie-prefix-tree", "Medium", "Hash Table", "Trie")
        s("215", "Kth Largest Element in an Array", "kth-largest-element-in-an-array", "Medium", "Array", "Sorting", "Heap")
        s("226", "Invert Binary Tree", "invert-binary-tree", "Easy", "Tree", "Binary Tree")
        s("230", "Kth Smallest Element in a BST", "kth-smallest-element-in-a-bst", "Medium", "Tree", "Binary Search Tree")
        s("236", "Lowest Common Ancestor of a Binary Tree", "lowest-common-ancestor-of-a-binary-tree", "Medium", "Tree", "Binary Tree")
        s("238", "Product of Array Except Self", "product-of-array-except-self", "Medium", "Array", "Prefix Sum")
        s("242", "Valid Anagram", "valid-anagram", "Easy", "Hash Table", "String")
        s("295", "Find Median from Data Stream", "find-median-from-data-stream", "Hard", "Heap", "Design")
        s("297", "Serialize and Deserialize Binary Tree", "serialize-and-deserialize-binary-tree", "Hard", "Tree", "Design")
        s("300", "Longest Increasing Subsequence", "longest-increasing-subsequence", "Medium", "Array", "Dynamic Programming")
        s("322", "Coin Change", "coin-change", "Medium", "Array", "Dynamic Programming")
        s("347", "Top K Frequent Elements", "top-k-frequent-elements", "Medium", "Array", "Hash Table", "Heap")
        s("416", "Partition Equal Subset Sum", "partition-equal-subset-sum", "Medium", "Array", "Dynamic Programming")
        s("435", "Non-overlapping Intervals", "non-overlapping-intervals", "Medium", "Array", "Greedy")
        s("704", "Binary Search", "binary-search", "Easy", "Array", "Binary Search")
        s("1143", "Longest Common Subsequence", "longest-common-subsequence", "Medium", "String", "Dynamic Programming")
    }

    private fun tryFetchOnlineQuestions(limit: Int, clean: String): List<LeetCodeSearchResult> {
        return try {
            val gqlQuery = """
                query problemsetQuestionList(${'$'}categorySlug: String, ${'$'}limit: Int, ${'$'}skip: Int, ${'$'}filters: QuestionListFilterInput) {
                  problemsetQuestionList: questionList(categorySlug: ${'$'}categorySlug, limit: ${'$'}limit, skip: ${'$'}skip, filters: ${'$'}filters) {
                    total: totalNum
                    questions: data {
                      frontendQuestionId: questionFrontendId
                      title
                      titleSlug
                      difficulty
                      topicTags {
                        name
                        slug
                      }
                    }
                  }
                }
            """.trimIndent()

            val variables = JSONObject().apply {
                put("categorySlug", "")
                put("skip", 0)
                put("limit", limit)
                val filters = JSONObject()
                if (clean.isNotBlank()) {
                    filters.put("searchKeywords", clean)
                }
                put("filters", filters)
            }

            val jsonBody = JSONObject().apply {
                put("query", gqlQuery)
                put("variables", variables)
            }

            val request = Request.Builder()
                .url("https://leetcode.com/graphql")
                .header("Content-Type", "application/json")
                .header("Referer", "https://leetcode.com/problemset/all/")
                .header("Origin", "https://leetcode.com")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: ""
                val root = JSONObject(body)
                val listData = root.optJSONObject("data")?.optJSONObject("problemsetQuestionList")
                val questionsArr = listData?.optJSONArray("questions") ?: JSONArray()
                val list = mutableListOf<LeetCodeSearchResult>()

                for (i in 0 until questionsArr.length()) {
                    val q = questionsArr.getJSONObject(i)
                    val fid = q.optString("frontendQuestionId", "")
                    val title = q.optString("title", "")
                    val slug = q.optString("titleSlug", "")
                    val diff = q.optString("difficulty", "Medium")
                    val tags = mutableListOf<String>()
                    val tArr = q.optJSONArray("topicTags")
                    if (tArr != null) {
                        for (j in 0 until tArr.length()) {
                            val tn = tArr.getJSONObject(j).optString("name")
                            if (tn.isNotBlank()) tags.add(tn)
                        }
                    }
                    if (slug.isNotBlank() && title.isNotBlank()) {
                        val item = LeetCodeSearchResult(
                            frontendId = fid,
                            title = title,
                            titleSlug = slug,
                            difficulty = diff,
                            topicTags = tags
                        )
                        list.add(item)
                        inMemoryCatalog[slug] = item
                    }
                }
                list
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun searchLeetCode(query: String): Result<List<LeetCodeSearchResult>> = withContext(Dispatchers.IO) {
        val clean = query.trim().removePrefix("#")
        ensureCatalogLoaded()

        if (clean.isBlank()) {
            // When query is blank, show all problems in numbering order (#1, #2, #3...)
            if (inMemoryCatalog.size < 100) {
                tryFetchOnlineQuestions(limit = 100, clean = "")
                persistCatalog()
            }
            val sorted = inMemoryCatalog.values.sortedWith(
                compareBy({ it.frontendId.toIntOrNull() ?: 999999 }, { it.title })
            )
            return@withContext Result.success(sorted)
        }

        // 1. Instant local offline search on phone
        val localMatches = mutableListOf<LeetCodeSearchResult>()
        for (item in inMemoryCatalog.values) {
            val matchFid = item.frontendId == clean || item.frontendId.startsWith(clean)
            val matchTitle = item.title.contains(clean, ignoreCase = true)
            val matchSlug = item.titleSlug.contains(clean, ignoreCase = true)
            val matchTag = item.topicTags.any { it.contains(clean, ignoreCase = true) }
            if (matchFid || matchTitle || matchSlug || matchTag) {
                localMatches.add(item)
            }
        }

        // 2. Fetch live results from LeetCode GraphQL online
        val onlineMatches = tryFetchOnlineQuestions(limit = 30, clean = clean)

        // 3. Merge results (avoiding duplicates by titleSlug)
        val combined = (localMatches + onlineMatches).distinctBy { it.titleSlug }.toMutableList()

        // 4. Sort: exact frontendId match first, then numeric prefix, then ascending order
        combined.sortWith(
            compareBy(
                { it.frontendId != clean },
                { !it.frontendId.startsWith(clean) },
                { it.frontendId.toIntOrNull() ?: 999999 },
                { it.title }
            )
        )

        persistCatalog()
        Result.success(combined)
    }

    override suspend fun addProblemFromLeetCode(input: String): Result<OfflineProblem> = withContext(Dispatchers.IO) {
        val clean = input.trim()
        if (clean.isBlank()) return@withContext Result.failure(Exception("Please enter a problem number, title, or URL"))

        val slugOrQuery = when {
            clean.contains("leetcode.com/problems/") -> {
                clean.substringAfter("leetcode.com/problems/").substringBefore("/").substringBefore("?").trim()
            }
            clean.contains("/") -> {
                clean.removeSuffix("/").substringAfterLast("/").trim()
            }
            else -> clean.removePrefix("#").trim()
        }

        ensureCatalogLoaded()
        val catalogItem = inMemoryCatalog[slugOrQuery] ?: inMemoryCatalog.values.find {
            it.frontendId == slugOrQuery || it.title.equals(slugOrQuery, ignoreCase = true)
        }
        val targetSlug = catalogItem?.titleSlug ?: slugOrQuery

        downloadProblemOffline(folderName = targetSlug, titleSlug = targetSlug)
    }

    override suspend fun getProblems(): List<OfflineProblem> = withContext(Dispatchers.IO) {
        repairProblemDirectories()
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
        val problem = loadProblemFromDir(dir) ?: return@withContext null

        // If statement content.html is missing (e.g. problem synced from Git without HTML statement),
        // fetch official statement on demand from LeetCode GraphQL
        if (problem.contentHtml.isBlank() && problem.titleSlug.isNotBlank()) {
            val details = fetchLeetCodeQuestionDetails(problem.titleSlug)
            if (details != null && details.contentHtml.isNotBlank()) {
                File(dir, "content.html").writeText(details.contentHtml)
                val metaFile = File(dir, "metadata.json")
                if (metaFile.exists()) {
                    try {
                        val metaObj = JSONObject(metaFile.readText())
                        if (!metaObj.has("hints") && details.hints.isNotEmpty()) {
                            metaObj.put("hints", JSONArray(details.hints))
                        }
                        if (!metaObj.has("topic_tags") && details.topicTags.isNotEmpty()) {
                            metaObj.put("topic_tags", JSONArray(details.topicTags))
                        }
                        metaFile.writeText(metaObj.toString(2))
                    } catch (_: Exception) {}
                }
                return@withContext loadProblemFromDir(dir)
            }
        }

        problem
    }

    override suspend fun downloadProblemOffline(folderName: String, titleSlug: String): Result<OfflineProblem> = withContext(Dispatchers.IO) {
        try {
            val rawSlug = titleSlug.ifBlank { folderName.substringAfter('-', folderName) }.trim()

            // 1. Try direct fetch if not purely numeric
            var details: LeetCodeQuestionDetails? = if (rawSlug.isNotBlank() && !rawSlug.all { it.isDigit() }) {
                fetchLeetCodeQuestionDetails(rawSlug)
            } else null

            // 2. If direct fetch returned null, search LeetCode website to resolve
            if (details == null) {
                val searchRes = searchLeetCode(rawSlug.ifBlank { folderName })
                val best = searchRes.getOrNull()?.firstOrNull()
                if (best != null) {
                    details = fetchLeetCodeQuestionDetails(best.titleSlug)
                }
            }

            if (details == null) {
                return@withContext Result.failure(Exception("Could not find problem '$rawSlug' on LeetCode."))
            }

            val frontendId = details.frontendId.ifBlank { "0" }
            val officialTitle = details.title
            val officialSlug = details.titleSlug
            val num = frontendId.toIntOrNull()
            val canonicalFolderName = if (num != null) "%04d-%s".format(num, officialSlug) else "$frontendId-$officialSlug"
            val targetDir = File(baseProblemsDir, canonicalFolderName).apply { mkdirs() }

            // Clean up / migrate from any non-canonical or unpadded folder names
            val candidateOldNames = listOf(folderName, officialSlug, "$num-$officialSlug").distinct()
            for (oldName in candidateOldNames) {
                if (oldName.isNotBlank() && oldName != canonicalFolderName) {
                    val oldDir = File(baseProblemsDir, oldName)
                    if (oldDir.exists() && oldDir.isDirectory) {
                        val oldNotesDir = File(oldDir, "notes")
                        if (oldNotesDir.exists()) {
                            val newNotesDir = getProblemNotesDir(canonicalFolderName)
                            oldNotesDir.listFiles()?.forEach { nf ->
                                val dest = File(newNotesDir, nf.name)
                                if (!dest.exists()) nf.copyTo(dest)
                            }
                        }
                        val oldSol = File(oldDir, "solution.py")
                        val newSol = File(targetDir, "solution.py")
                        if (oldSol.exists() && !newSol.exists()) {
                            oldSol.copyTo(newSol)
                        }
                        oldDir.deleteRecursively()
                    }
                }
            }

            // Write metadata.json with authentic LeetCode properties
            val metaFile = File(targetDir, "metadata.json")
            val metaObj = if (metaFile.exists()) {
                try { JSONObject(metaFile.readText()) } catch (_: Exception) { JSONObject() }
            } else JSONObject()

            metaObj.put("frontend_id", frontendId)
            metaObj.put("problem_id", details.questionId.ifBlank { frontendId })
            metaObj.put("problem_title", officialTitle)
            metaObj.put("problem_slug", officialSlug)
            metaObj.put("difficulty", details.difficulty)
            metaObj.put("problem_url", "https://leetcode.com/problems/$officialSlug/")
            metaObj.put("is_downloaded_offline", true)
            metaObj.put("last_synced_millis", System.currentTimeMillis())

            if (details.topicTags.isNotEmpty()) {
                val tagsArr = JSONArray()
                details.topicTags.forEach { tagsArr.put(it) }
                metaObj.put("topic_tags", tagsArr)
            }
            if (details.hints.isNotEmpty()) {
                val hintArr = JSONArray()
                details.hints.forEach { hintArr.put(it) }
                metaObj.put("hints", hintArr)
            }
            metaFile.writeText(metaObj.toString(2))

            // Save official content.html
            if (details.contentHtml.isNotBlank()) {
                File(targetDir, "content.html").writeText(details.contentHtml)
            }

            // Starter solution code if not already created
            val hasExistingSolution = targetDir.listFiles { f ->
                f.isFile && (f.name.startsWith("solution.", ignoreCase = true) || f.name.startsWith("Solution.", ignoreCase = true))
            }?.any { !isDefaultStubFile(it) } == true

            if (!hasExistingSolution) {
                // Try syncing solution code and notes from GitHub backup repo if configured
                syncSingleProblemFromGitHub(canonicalFolderName)
            }

            val hasSolutionAfterSync = targetDir.listFiles { f ->
                f.isFile && (f.name.startsWith("solution.", ignoreCase = true) || f.name.startsWith("Solution.", ignoreCase = true))
            }?.any { !isDefaultStubFile(it) } == true

            if (!hasSolutionAfterSync) {
                val solFile = File(targetDir, "solution.py")
                if (!solFile.exists()) {
                    val pySnippet = details.codeSnippets["python3"]
                        ?: details.codeSnippets["python"]
                        ?: details.codeSnippets["java"]
                        ?: details.codeSnippets["cpp"]
                        ?: ""
                    val starterCode = if (pySnippet.isNotBlank()) {
                        "# LeetCode #$frontendId: $officialTitle\n# https://leetcode.com/problems/$officialSlug/\n\n$pySnippet"
                    } else {
                        "# LeetCode #$frontendId: $officialTitle\n# https://leetcode.com/problems/$officialSlug/\n\nclass Solution:\n    pass\n"
                    }
                    solFile.writeText(starterCode)
                }
            }

            // Starter notes in notes/<problemFolder>/notes.md
            val notesDir = getProblemNotesDir(canonicalFolderName)
            val defaultNote = File(notesDir, "notes.md")
            if (!defaultNote.exists()) {
                val tagsStr = if (details.topicTags.isNotEmpty()) details.topicTags.joinToString(", ") else "None"
                defaultNote.writeText("""
                    # #$frontendId $officialTitle Notes

                    ## Intuition
                    <!-- Write your initial thoughts, key observations, and edge cases here -->

                    ## Approach
                    - Time Complexity: O(?)
                    - Space Complexity: O(?)

                    ## Key Insights
                    - Tags: $tagsStr
                """.trimIndent())
                launchGitHubPush("notes/$canonicalFolderName/notes.md", defaultNote.readBytes())
            }

            val updated = loadProblemFromDir(targetDir)
                ?: return@withContext Result.failure(Exception("Failed to reload downloaded problem"))

            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProblemOffline(folderName: String): Boolean = withContext(Dispatchers.IO) {
        val dir = File(baseProblemsDir, folderName)
        val pNotes = getProblemNotesDir(folderName)
        val dirDeleted = if (dir.exists()) dir.deleteRecursively() else false
        if (pNotes.exists()) pNotes.deleteRecursively()
        dirDeleted
    }

    override suspend fun saveProblemNote(folderName: String, filename: String, content: String): ProblemNote = withContext(Dispatchers.IO) {
        val safeName = filename.trim().ifBlank { "notes.md" }
            .let { if (!it.contains('.')) "$it.md" else it }
        val notesDir = getProblemNotesDir(folderName)
        val noteFile = File(notesDir, safeName)
        noteFile.writeText(content)

        launchGitHubPush("notes/$folderName/$safeName", content.toByteArray(Charsets.UTF_8))

        val title = safeName.substringBeforeLast('.').replace('_', ' ').replace('-', ' ')
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        ProblemNote(
            filename = safeName,
            title = title,
            content = content,
            lastModifiedMillis = noteFile.lastModified(),
            isBinaryDoc = false,
            localFilePath = noteFile.absolutePath
        )
    }

    override suspend fun saveProblemDocument(folderName: String, filename: String, bytes: ByteArray): ProblemNote = withContext(Dispatchers.IO) {
        val safeName = filename.trim().ifBlank { "document_${System.currentTimeMillis()}" }
        val notesDir = getProblemNotesDir(folderName)
        val docFile = File(notesDir, safeName)
        docFile.writeBytes(bytes)

        launchGitHubPush("notes/$folderName/$safeName", bytes)

        val isBinary = isBinaryFileName(safeName)
        val title = safeName.substringBeforeLast('.').replace('_', ' ').replace('-', ' ')
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        val content = if (isBinary) {
            "[Binary Document: $safeName (${formatFileSize(docFile.length())})]"
        } else {
            try { String(bytes, Charsets.UTF_8) } catch (_: Exception) { "[Document file: $safeName]" }
        }

        ProblemNote(
            filename = safeName,
            title = title,
            content = content,
            lastModifiedMillis = docFile.lastModified(),
            isBinaryDoc = isBinary,
            localFilePath = docFile.absolutePath
        )
    }

    override suspend fun deleteProblemNote(folderName: String, filename: String): Boolean = withContext(Dispatchers.IO) {
        val noteFile = File(getProblemNotesDir(folderName), filename)
        val deleted = if (noteFile.exists()) {
            noteFile.delete()
        } else false
        if (deleted) {
            launchGitHubDelete("notes/$folderName/$filename")
        }
        deleted
    }

    // --- GitHub API Push & Delete Helpers ---

    private fun encodeGitHubPath(path: String): String {
        return path.trimStart('/').split('/').joinToString("/") { part ->
            URLEncoder.encode(part, "UTF-8").replace("+", "%20")
        }
    }

    private fun pushFileToGitHubDirect(
        subPath: String,
        bytes: ByteArray,
        owner: String,
        repo: String,
        token: String
    ): Boolean {
        if (token.isBlank() || owner.isBlank() || repo.isBlank()) return false
        val cleanPath = subPath.trimStart('/')
        val encodedPath = encodeGitHubPath(cleanPath)
        val url = "https://api.github.com/repos/$owner/$repo/contents/$encodedPath"

        var existingSha: String? = null
        try {
            val getReq = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "DevLife-Android")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            okHttpClient.newCall(getReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val obj = JSONObject(resp.body?.string() ?: "{}")
                    existingSha = obj.optString("sha").ifBlank { null }
                }
            }
        } catch (_: Exception) {}

        val base64Content = java.util.Base64.getEncoder().encodeToString(bytes)
        val putBody = JSONObject().apply {
            put("message", "Sync note $cleanPath")
            put("content", base64Content)
            if (existingSha != null) {
                put("sha", existingSha)
            }
        }

        val putReq = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github.v3+json")
            .header("User-Agent", "DevLife-Android")
            .header("Authorization", "Bearer $token")
            .put(putBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            okHttpClient.newCall(putReq).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    private fun launchGitHubPush(subPath: String, bytes: ByteArray) {
        val repoConfig = prefs.gitHubBackupRepo.trim().ifBlank { "leetcode" }
        val token = DevApiService.normalizeToken(prefs.gitHubToken)
        if (token.isBlank()) return
        val owner = if (repoConfig.contains('/')) repoConfig.substringBefore('/').trim() else prefs.gitHubUsername.trim().removePrefix("@")
        val repo = if (repoConfig.contains('/')) repoConfig.substringAfter('/').trim() else repoConfig
        if (owner.isBlank() || repo.isBlank()) return

        CoroutineScope(Dispatchers.IO).launch {
            pushFileToGitHubDirect(subPath, bytes, owner, repo, token)
        }
    }

    private fun launchGitHubDelete(subPath: String) {
        val repoConfig = prefs.gitHubBackupRepo.trim().ifBlank { "leetcode" }
        val token = DevApiService.normalizeToken(prefs.gitHubToken)
        if (token.isBlank()) return
        val owner = if (repoConfig.contains('/')) repoConfig.substringBefore('/').trim() else prefs.gitHubUsername.trim().removePrefix("@")
        val repo = if (repoConfig.contains('/')) repoConfig.substringAfter('/').trim() else repoConfig
        if (owner.isBlank() || repo.isBlank()) return

        val cleanPath = subPath.trimStart('/')
        val encodedPath = encodeGitHubPath(cleanPath)
        val url = "https://api.github.com/repos/$owner/$repo/contents/$encodedPath"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                var existingSha: String? = null
                val getReq = Request.Builder()
                    .url(url)
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "DevLife-Android")
                    .header("Authorization", "Bearer $token")
                    .get()
                    .build()

                try {
                    okHttpClient.newCall(getReq).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val obj = JSONObject(resp.body?.string() ?: "{}")
                            existingSha = obj.optString("sha").ifBlank { null }
                        }
                    }
                } catch (_: Exception) {}

                if (existingSha != null) {
                    val delBody = JSONObject().apply {
                        put("message", "Delete note $cleanPath")
                        put("sha", existingSha)
                    }
                    val delReq = Request.Builder()
                        .url(url)
                        .header("Accept", "application/vnd.github.v3+json")
                        .header("User-Agent", "DevLife-Android")
                        .header("Authorization", "Bearer $token")
                        .delete(delBody.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                    okHttpClient.newCall(delReq).execute().close()
                }
            } catch (_: Exception) {}
        }
    }

    private fun downloadBlobToFile(blobUrl: String, token: String, targetFile: File) {
        try {
            val blobReqBuilder = Request.Builder()
                .url(blobUrl)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "DevLife-Android")
            if (token.isNotBlank()) {
                blobReqBuilder.header("Authorization", "Bearer $token")
            }

            okHttpClient.newCall(blobReqBuilder.build()).execute().use { blobResp ->
                if (blobResp.isSuccessful) {
                    val blobJson = JSONObject(blobResp.body?.string() ?: "")
                    val base64Content = blobJson.optString("content").replace("\n", "").replace("\r", "")
                    val bytes = java.util.Base64.getMimeDecoder().decode(base64Content)
                    targetFile.parentFile?.mkdirs()
                    targetFile.writeBytes(bytes)

                    // If we just wrote a real solution file, check if there's an obsolete stub in the directory and remove it!
                    if (targetFile.name.startsWith("solution.", ignoreCase = true) && !isDefaultStubFile(targetFile)) {
                        targetFile.parentFile?.listFiles { f ->
                            f.isFile && f.name.startsWith("solution.", ignoreCase = true) && f.name != targetFile.name && isDefaultStubFile(f)
                        }?.forEach { it.delete() }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    override suspend fun syncFromGitHubBackupRepo(
        owner: String,
        repo: String,
        token: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        val (cleanOwner, cleanRepo) = if (repo.contains("/")) {
            val pOwner = repo.substringBefore("/").trim()
            val pRepo = repo.substringAfter("/").trim()
            Pair(pOwner.ifBlank { owner.trim().removePrefix("@") }, pRepo)
        } else {
            Pair(
                owner.trim().removePrefix("@").ifBlank { prefs.gitHubUsername.trim().removePrefix("@") },
                repo.trim().ifBlank { prefs.gitHubBackupRepo.ifBlank { "leetcode" } }
            )
        }

        if (cleanOwner.isBlank() || cleanRepo.isBlank()) {
            return@withContext Result.failure(Exception("GitHub username/owner and repository name are required."))
        }

        val cleanToken = DevApiService.normalizeToken(token.ifBlank { prefs.gitHubToken })

        try {
            // 1. Detect default branch from repository info
            val repoInfoReq = Request.Builder()
                .url("https://api.github.com/repos/$cleanOwner/$cleanRepo")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "DevLife-Android")
                .apply { if (cleanToken.isNotBlank()) header("Authorization", "Bearer $cleanToken") }
                .build()

            val detectedBranch = try {
                okHttpClient.newCall(repoInfoReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val obj = JSONObject(resp.body?.string() ?: "{}")
                        obj.optString("default_branch", "main").ifBlank { "main" }
                    } else "main"
                }
            } catch (_: Exception) { "main" }

            var importedCount = 0
            val remoteNotesMap = mutableMapOf<String, ByteArray>()
            var zipDownloadSuccessful = false
            var lastHttpCode = 0

            // 2. Fast Path: Download entire repository as single ZIP archive in 1 HTTP call
            val zipUrlsToTry = listOf(
                "https://api.github.com/repos/$cleanOwner/$cleanRepo/zipball/$detectedBranch",
                "https://github.com/$cleanOwner/$cleanRepo/archive/refs/heads/$detectedBranch.zip",
                "https://api.github.com/repos/$cleanOwner/$cleanRepo/zipball/main",
                "https://github.com/$cleanOwner/$cleanRepo/archive/refs/heads/main.zip"
            ).distinct()

            for (zipUrl in zipUrlsToTry) {
                val reqBuilder = Request.Builder()
                    .url(zipUrl)
                    .header("User-Agent", "DevLife-Android")
                if (cleanToken.isNotBlank() && zipUrl.contains("api.github.com")) {
                    reqBuilder.header("Authorization", "Bearer $cleanToken")
                }

                try {
                    okHttpClient.newCall(reqBuilder.build()).execute().use { resp ->
                        lastHttpCode = resp.code
                        if (resp.isSuccessful && resp.body != null) {
                            ZipInputStream(resp.body!!.byteStream()).use { zis ->
                                var entry = zis.nextEntry
                                while (entry != null) {
                                    if (!entry.isDirectory) {
                                        val rawName = entry.name
                                        val slashIdx = rawName.indexOf('/')
                                        val relPath = if (slashIdx != -1) rawName.substring(slashIdx + 1) else rawName
                                        val bytes = zis.readBytes()

                                        if (relPath.startsWith("notes/")) {
                                            val noteSub = relPath.removePrefix("notes/")
                                            val targetFile = File(rootNotesDir, noteSub)
                                            targetFile.parentFile?.mkdirs()
                                            if (!targetFile.exists() || !targetFile.readBytes().contentEquals(bytes)) {
                                                targetFile.writeBytes(bytes)
                                            }
                                            remoteNotesMap[relPath] = bytes
                                            importedCount++
                                        } else if (relPath.startsWith("leetcode_problem/")) {
                                            val parts = relPath.split('/')
                                            if (parts.size >= 3) {
                                                val folderName = parts[1]
                                                val sub = parts.subList(2, parts.size).joinToString("/")
                                                val targetFile = File(baseProblemsDir, "$folderName/$sub")
                                                targetFile.parentFile?.mkdirs()
                                                targetFile.writeBytes(bytes)
                                                importedCount++
                                            }
                                        } else {
                                            val parts = relPath.split('/')
                                            if (parts.size >= 2 && parts[0].matches(Regex("^\\d{4}-.*"))) {
                                                val folderName = parts[0]
                                                val sub = parts.subList(1, parts.size).joinToString("/")
                                                val targetFile = File(baseProblemsDir, "$folderName/$sub")
                                                targetFile.parentFile?.mkdirs()
                                                targetFile.writeBytes(bytes)
                                                importedCount++
                                            }
                                        }
                                    }
                                    zis.closeEntry()
                                    entry = zis.nextEntry
                                }
                            }
                            zipDownloadSuccessful = true
                            return@use
                        }
                    }
                } catch (_: Exception) {}

                if (zipDownloadSuccessful) break
            }

            // 3. Fallback: If zipball was blocked, use Git Tree API
            if (!zipDownloadSuccessful) {
                val branchesToTry = listOf(detectedBranch, "main", "master").distinct()
                var treeBody: String? = null

                for (branch in branchesToTry) {
                    val treeUrl = "https://api.github.com/repos/$cleanOwner/$cleanRepo/git/trees/$branch?recursive=1"
                    val reqBuilder = Request.Builder()
                        .url(treeUrl)
                        .header("Accept", "application/vnd.github.v3+json")
                        .header("User-Agent", "DevLife-Android")
                    if (cleanToken.isNotBlank()) {
                        reqBuilder.header("Authorization", "Bearer $cleanToken")
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
                        403 -> "GitHub API rate limit exceeded. Check token permissions in Settings."
                        404 -> "Repository '$cleanOwner/$cleanRepo' not found or is private. Check repository name in Settings."
                        else -> "Failed to access repository '$cleanOwner/$cleanRepo' (HTTP $lastHttpCode)"
                    }
                    return@withContext Result.failure(Exception(msg))
                }

                val root = JSONObject(treeBody ?: "{}")
                val tree = root.optJSONArray("tree") ?: JSONArray()

                for (i in 0 until tree.length()) {
                    val entry = tree.getJSONObject(i)
                    if (entry.optString("type") != "blob") continue
                    val path = entry.optString("path")
                    val blobUrl = entry.optString("url")
                    if (blobUrl.isBlank()) continue

                    val targetFile = when {
                        path.startsWith("notes/") -> File(rootNotesDir, path.removePrefix("notes/"))
                        path.startsWith("leetcode_problem/") -> {
                            val parts = path.split('/')
                            if (parts.size >= 3) File(baseProblemsDir, "${parts[1]}/${parts.subList(2, parts.size).joinToString("/")}") else null
                        }
                        path.matches(Regex("^\\d{4}-.*")) -> File(baseProblemsDir, path)
                        else -> null
                    }

                    if (targetFile != null) {
                        downloadBlobToFile(blobUrl, cleanToken, targetFile)
                        if (targetFile.exists()) {
                            if (path.startsWith("notes/")) {
                                remoteNotesMap[path] = targetFile.readBytes()
                            }
                            importedCount++
                        }
                    }
                }
            }

            // Post-sync cleanup: remove any obsolete default stubs where a real solution was synced
            baseProblemsDir.listFiles { f -> f.isDirectory }?.forEach { probDir ->
                val realSol = probDir.listFiles { f ->
                    f.isFile && (f.name.startsWith("solution.", ignoreCase = true) || f.name.startsWith("Solution.", ignoreCase = true)) && !isDefaultStubFile(f)
                }?.firstOrNull()
                if (realSol != null) {
                    probDir.listFiles { f ->
                        f.isFile && f.name.startsWith("solution.", ignoreCase = true) && f.name != realSol.name && isDefaultStubFile(f)
                    }?.forEach { it.delete() }
                }
            }

            // 4. Bidirectional Sync: PUSH local notes to GitHub that are missing or newer
            var pushedCount = 0
            if (cleanToken.isNotBlank()) {
                rootNotesDir.walkTopDown().filter { it.isFile && !it.name.startsWith(".") }.forEach { localFile ->
                    val relPath = "notes/" + localFile.relativeTo(rootNotesDir).path.replace('\\', '/')
                    val localBytes = try { localFile.readBytes() } catch (_: Exception) { null }
                    if (localBytes != null && localBytes.isNotEmpty()) {
                        val remoteBytes = remoteNotesMap[relPath]
                        if (remoteBytes == null || !remoteBytes.contentEquals(localBytes)) {
                            val pushed = pushFileToGitHubDirect(relPath, localBytes, cleanOwner, cleanRepo, cleanToken)
                            if (pushed) pushedCount++
                        }
                    }
                }
            }

            Result.success(importedCount + pushedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncSingleProblemFromGitHub(folderName: String): Result<OfflineProblem> = withContext(Dispatchers.IO) {
        val repoConfig = prefs.gitHubBackupRepo.trim().ifBlank { "leetcode" }
        val token = DevApiService.normalizeToken(prefs.gitHubToken)
        val owner = if (repoConfig.contains('/')) repoConfig.substringBefore('/').trim() else prefs.gitHubUsername.trim().removePrefix("@")
        val repo = if (repoConfig.contains('/')) repoConfig.substringAfter('/').trim() else repoConfig

        if (owner.isBlank() || repo.isBlank()) {
            return@withContext Result.failure(Exception("GitHub backup repository is not configured in settings."))
        }

        val targetDir = File(baseProblemsDir, folderName).apply { mkdirs() }
        val pNotesDir = getProblemNotesDir(folderName).apply { mkdirs() }

        // Candidate folder names (e.g. "1411-number-of-ways-to-paint-n-3-grid", "0001-two-sum", "1-two-sum")
        val dashIdx = folderName.indexOf('-')
        val numPart = if (dashIdx != -1) folderName.substring(0, dashIdx) else ""
        val slugPart = if (dashIdx != -1) folderName.substring(dashIdx + 1) else folderName
        val candidateNames = mutableListOf(folderName)
        val num = numPart.toIntOrNull()
        if (num != null) {
            candidateNames.add("%04d-%s".format(num, slugPart))
            candidateNames.add("$num-$slugPart")
        }
        val distinctCandidates = candidateNames.distinct()

        var downloadedFiles = 0

        fun fetchAndSaveDir(remotePath: String, localDir: File) {
            val encodedRemotePath = encodeGitHubPath(remotePath)
            val url = "https://api.github.com/repos/$owner/$repo/contents/$encodedRemotePath"
            val req = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "DevLife-Android")
                .apply { if (token.isNotBlank()) header("Authorization", "Bearer $token") }
                .build()

            try {
                okHttpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: ""
                        if (body.trimStart().startsWith("[")) {
                            val arr = JSONArray(body)
                            for (i in 0 until arr.length()) {
                                val item = arr.getJSONObject(i)
                                val type = item.optString("type")
                                val gitUrl = item.optString("git_url")
                                val downloadUrl = item.optString("download_url")
                                val name = item.optString("name")
                                if (type == "file" && name.isNotBlank()) {
                                    val targetFile = File(localDir, name)
                                    var written = false
                                    if (downloadUrl.isNotBlank()) {
                                        try {
                                            val rawReq = Request.Builder()
                                                .url(downloadUrl)
                                                .header("User-Agent", "DevLife-Android")
                                                .apply { if (token.isNotBlank()) header("Authorization", "Bearer $token") }
                                                .build()
                                            okHttpClient.newCall(rawReq).execute().use { rawResp ->
                                                if (rawResp.isSuccessful && rawResp.body != null) {
                                                    targetFile.parentFile?.mkdirs()
                                                    targetFile.writeBytes(rawResp.body!!.bytes())
                                                    written = true
                                                    downloadedFiles++
                                                }
                                            }
                                        } catch (_: Exception) {}
                                    }
                                    if (!written && gitUrl.isNotBlank()) {
                                        downloadBlobToFile(gitUrl, token, targetFile)
                                        if (targetFile.exists()) downloadedFiles++
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 1. Fetch problem files from leetcode_problem/<cand> or root <cand>
        for (cand in distinctCandidates) {
            fetchAndSaveDir("leetcode_problem/$cand", targetDir)
            if (downloadedFiles > 0) break
            fetchAndSaveDir(cand, targetDir)
            if (downloadedFiles > 0) break
        }

        // 2. Fetch notes from notes/<cand>, leetcode_problem/<cand>/notes, or <cand>/notes
        for (cand in distinctCandidates) {
            fetchAndSaveDir("notes/$cand", pNotesDir)
            fetchAndSaveDir("leetcode_problem/$cand/notes", pNotesDir)
            fetchAndSaveDir("$cand/notes", pNotesDir)
        }

        // 3. Purge stub file if a real submitted solution was downloaded
        val realSol = targetDir.listFiles { f ->
            f.isFile && (f.name.startsWith("solution.", ignoreCase = true) || f.name.startsWith("Solution.", ignoreCase = true)) && !isDefaultStubFile(f)
        }?.firstOrNull()
        if (realSol != null) {
            targetDir.listFiles { f ->
                f.isFile && f.name.startsWith("solution.", ignoreCase = true) && f.name != realSol.name && isDefaultStubFile(f)
            }?.forEach { it.delete() }
        }

        // 4. Ensure problem statement HTML is present
        val metaFile = File(targetDir, "metadata.json")
        val slug = if (metaFile.exists()) {
            try { JSONObject(metaFile.readText()).optString("problem_slug", slugPart) } catch (_: Exception) { slugPart }
        } else slugPart

        if (!File(targetDir, "content.html").exists() && slug.isNotBlank()) {
            try {
                val details = fetchLeetCodeQuestionDetails(slug)
                if (details != null && details.contentHtml.isNotBlank()) {
                    File(targetDir, "content.html").writeText(details.contentHtml)
                }
            } catch (_: Exception) {}
        }

        // 5. Push local notes for this problem to GitHub if token available
        if (token.isNotBlank()) {
            pNotesDir.listFiles { f -> f.isFile && !f.name.startsWith(".") }?.forEach { nFile ->
                try {
                    val bytes = nFile.readBytes()
                    pushFileToGitHubDirect("notes/$folderName/${nFile.name}", bytes, owner, repo, token)
                } catch (_: Exception) {}
            }
        }

        val reloaded = loadProblemFromDir(targetDir)
        if (reloaded != null) {
            Result.success(reloaded)
        } else {
            Result.failure(Exception("Failed to load problem after sync from GitHub."))
        }
    }

    // --- General Notes Operations ---

    override suspend fun getGeneralNotes(): List<CustomNote> = withContext(Dispatchers.IO) {
        migrateNotesToUnifiedRoot()
        val files = generalNotesDir.listFiles { f -> f.isFile && !f.name.startsWith(".") }
            ?: emptyArray()
        val list = mutableListOf<CustomNote>()

        for (file in files) {
            try {
                val isBinary = isBinaryFileName(file.name)
                val content = if (isBinary) {
                    "[Binary Document: ${file.name} (${formatFileSize(file.length())})]"
                } else {
                    try { file.readText() } catch (_: Exception) { "[Document file: ${file.name}]" }
                }
                val id = file.name
                val title = if (isBinary) {
                    file.name
                } else {
                    val firstLine = content.lines().firstOrNull { it.isNotBlank() } ?: file.nameWithoutExtension
                    firstLine.removePrefix("#").trim().ifBlank { file.nameWithoutExtension.replace('_', ' ') }
                }
                list.add(
                    CustomNote(
                        id = id,
                        title = title,
                        content = content,
                        category = "General",
                        lastModifiedMillis = file.lastModified(),
                        isBinaryDoc = isBinary,
                        localFilePath = file.absolutePath
                    )
                )
            } catch (_: Exception) {}
        }

        list.sortedByDescending { it.lastModifiedMillis }
    }

    override suspend fun saveGeneralNote(note: CustomNote): CustomNote = withContext(Dispatchers.IO) {
        val safeId = note.id.ifBlank { UUID.randomUUID().toString() }
        val fileName = if (!safeId.contains('.')) "$safeId.md" else safeId
        val file = File(generalNotesDir, fileName)
        file.writeText(note.content)

        launchGitHubPush("notes/general/$fileName", note.content.toByteArray(Charsets.UTF_8))

        note.copy(id = fileName, lastModifiedMillis = file.lastModified(), localFilePath = file.absolutePath)
    }

    override suspend fun saveGeneralDocument(filename: String, bytes: ByteArray): CustomNote = withContext(Dispatchers.IO) {
        val safeName = filename.trim().ifBlank { "document_${System.currentTimeMillis()}" }
        val file = File(generalNotesDir, safeName)
        file.writeBytes(bytes)

        launchGitHubPush("notes/general/$safeName", bytes)

        val isBinary = isBinaryFileName(safeName)
        val title = safeName.substringBeforeLast('.').replace('_', ' ').replace('-', ' ')
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        val content = if (isBinary) {
            "[Binary Document: $safeName (${formatFileSize(file.length())})]"
        } else {
            try { String(bytes, Charsets.UTF_8) } catch (_: Exception) { "[Document file: $safeName]" }
        }

        CustomNote(
            id = safeName,
            title = title,
            content = content,
            category = "General",
            lastModifiedMillis = file.lastModified(),
            isBinaryDoc = isBinary,
            localFilePath = file.absolutePath
        )
    }

    override suspend fun deleteGeneralNote(id: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(generalNotesDir, id).let { if (it.exists()) it else File(generalNotesDir, "$id.md") }
        val deleted = if (file.exists()) file.delete() else false
        if (deleted) {
            launchGitHubDelete("notes/general/${file.name}")
        }
        deleted
    }

    // --- Helpers ---

    private fun loadProblemFromDir(dir: File): OfflineProblem? {
        val folderName = dir.name
        val metaFile = File(dir, "metadata.json")
        val meta = if (metaFile.exists()) {
            try { JSONObject(metaFile.readText()) } catch (_: Exception) { JSONObject() }
        } else JSONObject()

        val dashIdx = folderName.indexOf('-')
        val firstPrefix = if (dashIdx != -1) folderName.substring(0, dashIdx).trimStart('0') else ""
        val parsedId = if (firstPrefix.isNotEmpty() && firstPrefix.all { it.isDigit() }) firstPrefix else "0"
        val parsedSlug = if (dashIdx != -1 && firstPrefix.all { it.isDigit() }) folderName.substring(dashIdx + 1) else folderName
        val parsedTitle = parsedSlug.split('-').joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        val metaFid = meta.optString("frontend_id").ifBlank { meta.optString("problem_id") }
        val frontendId = if (metaFid.isNotBlank()) metaFid else parsedId
        val title = meta.optString("problem_title").ifBlank { parsedTitle }
        val titleSlug = meta.optString("problem_slug").ifBlank { parsedSlug }
        val difficulty = meta.optString("difficulty", "Medium")
        val language = meta.optString("language", "").trim()
        val runtime = meta.optString("runtime", "")
        val rawRuntimePct = if (meta.isNull("runtime_percentile")) "" else meta.optString("runtime_percentile", "")
        val runtimePercentile = if (rawRuntimePct == "null") "" else rawRuntimePct
        val memory = meta.optString("memory", "")
        val rawMemoryPct = if (meta.isNull("memory_percentile")) "" else meta.optString("memory_percentile", "")
        val memoryPercentile = if (rawMemoryPct == "null") "" else rawMemoryPct
        val submittedAt = meta.optString("submitted_at", "")
        val problemUrl = meta.optString("problem_url", "https://leetcode.com/problems/$titleSlug/")
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

        // Solution code file selection matching user's submitted language
        val candidateSolutionFiles = dir.listFiles { f ->
            f.isFile && (f.name.startsWith("solution.", ignoreCase = true) || f.name.startsWith("Solution.", ignoreCase = true))
        }?.toList() ?: emptyList()

        val metaLang = language.trim()
        val metaExts = getLanguageExtensions(metaLang)

        var chosenSolutionFile: File? = null
        if (metaExts.isNotEmpty()) {
            chosenSolutionFile = candidateSolutionFiles.firstOrNull { f ->
                val ext = "." + f.extension.lowercase()
                metaExts.contains(ext)
            }
        }

        // If not found or if the chosen one is a default stub while another non-stub exists:
        if (chosenSolutionFile == null || isDefaultStubFile(chosenSolutionFile)) {
            val nonStub = candidateSolutionFiles.firstOrNull { !isDefaultStubFile(it) }
            if (nonStub != null) {
                // If there was a stub (like solution.py) and a real submitted solution (like solution.cpp), delete the stub
                if (chosenSolutionFile != null && isDefaultStubFile(chosenSolutionFile) && chosenSolutionFile != nonStub) {
                    chosenSolutionFile.delete()
                }
                chosenSolutionFile = nonStub
            }
        }

        if (chosenSolutionFile == null) {
            chosenSolutionFile = candidateSolutionFiles.firstOrNull()
        }

        val solutionCode = chosenSolutionFile?.readText() ?: meta.optString("solution_code", "")
        val resolvedLanguage = if (chosenSolutionFile != null) {
            val ext = chosenSolutionFile.extension.lowercase()
            if (metaLang.isNotBlank() && metaExts.contains(".$ext")) metaLang else inferLanguageFromExtension(ext)
        } else if (metaLang.isNotBlank()) {
            metaLang
        } else {
            "python3"
        }

        val hasRealSolution = chosenSolutionFile != null && !isDefaultStubFile(chosenSolutionFile)
        val isOffline = meta.optBoolean("is_downloaded_offline", false) ||
            File(dir, "content.html").exists() ||
            hasRealSolution

        // Migrate any legacy notes inside problem directory
        val legacyNotesDir = File(dir, "notes")
        val pNotesDir = getProblemNotesDir(folderName)
        if (legacyNotesDir.exists() && legacyNotesDir.isDirectory) {
            legacyNotesDir.listFiles()?.forEach { nf ->
                val target = File(pNotesDir, nf.name)
                if (!target.exists()) nf.copyTo(target)
                nf.delete()
            }
            legacyNotesDir.delete()
        }

        // Dedicated root notes/<folderName>/ directory
        val notes = mutableListOf<ProblemNote>()
        if (pNotesDir.exists() && pNotesDir.isDirectory) {
            val noteFiles = pNotesDir.listFiles { f -> f.isFile && !f.name.startsWith(".") }
                ?: emptyArray()
            for (nf in noteFiles) {
                val isBinary = isBinaryFileName(nf.name)
                val nContent = if (isBinary) {
                    "[Binary Document: ${nf.name} (${formatFileSize(nf.length())})]"
                } else {
                    try { nf.readText() } catch (_: Exception) { "[Document file: ${nf.name}]" }
                }
                val nTitle = nf.nameWithoutExtension.replace('_', ' ').replace('-', ' ')
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                notes.add(
                    ProblemNote(
                        filename = nf.name,
                        title = nTitle,
                        content = nContent,
                        lastModifiedMillis = nf.lastModified(),
                        isBinaryDoc = isBinary,
                        localFilePath = nf.absolutePath
                    )
                )
            }
        }

        // If no note files exist but metadata had a legacy note, seed notes/<folderName>/general.md
        if (notes.isEmpty() && meta.has("notes") && meta.getString("notes").isNotBlank()) {
            val legacyContent = meta.getString("notes")
            val defaultNote = File(pNotesDir, "general.md")
            defaultNote.writeText(legacyContent)
            notes.add(
                ProblemNote(
                    filename = "general.md",
                    title = "General Notes",
                    content = legacyContent,
                    lastModifiedMillis = defaultNote.lastModified(),
                    isBinaryDoc = false,
                    localFilePath = defaultNote.absolutePath
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
            solutionLanguage = resolvedLanguage,
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
        val twoSumNotes = getProblemNotesDir("0001-two-sum")
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
        val threeSumNotes = getProblemNotesDir("0015-3sum")
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
        val validParenNotes = getProblemNotesDir("0020-valid-parentheses")
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
