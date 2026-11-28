package com.sangrechy.devwidgets.data

import android.content.Context
import android.util.Base64
import com.sangrechy.devwidgets.data.model.NoteFileType
import com.sangrechy.devwidgets.data.model.NoteTreeEntry
import com.sangrechy.devwidgets.data.model.OpenedNoteFile
import com.sangrechy.devwidgets.data.network.DevApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.zip.ZipInputStream

interface NotesRepository {
    suspend fun getEntries(parentPath: String = ""): List<NoteTreeEntry>
    suspend fun syncFromGitHub(owner: String, repo: String, token: String): Result<Int>
    suspend fun openFile(relativePath: String, owner: String, repo: String, token: String): Result<OpenedNoteFile>
    suspend fun saveTextFile(relativePath: String, content: String, owner: String, repo: String, token: String): Result<OpenedNoteFile>
    suspend fun createFolder(parentPath: String, folderName: String, owner: String, repo: String, token: String): Result<NoteTreeEntry>
    suspend fun uploadFile(parentPath: String, filename: String, bytes: ByteArray, owner: String, repo: String, token: String): Result<NoteTreeEntry>
    suspend fun deleteEntry(relativePath: String, isFolder: Boolean, owner: String, repo: String, token: String): Result<Boolean>
}

class OfflineNotesRepository(
    private val context: Context,
    private val okHttpClient: OkHttpClient = DevApiService.defaultClient()
) : NotesRepository {

    companion object {
        const val REMOTE_ROOT = "res"
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }

    private val repoBaseDir: File
        get() = File(context.filesDir, "notes_repo").apply { mkdirs() }

    private val localResDir: File
        get() = File(repoBaseDir, REMOTE_ROOT).apply { mkdirs() }

    private val indexFile: File
        get() = File(repoBaseDir, "tree_index.json")

    private data class CachedMeta(
        val relativePath: String,
        val isFolder: Boolean,
        val sha: String,
        val sizeBytes: Long
    )

    private fun readIndex(): MutableMap<String, CachedMeta> {
        val map = linkedMapOf<String, CachedMeta>()
        if (!indexFile.exists()) return map
        try {
            val arr = JSONArray(indexFile.readText())
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val path = obj.optString("path", "").trim('/')
                if (path.isBlank()) continue
                map[path] = CachedMeta(
                    relativePath = path,
                    isFolder = obj.optBoolean("isFolder", false),
                    sha = obj.optString("sha", ""),
                    sizeBytes = obj.optLong("size", 0L)
                )
            }
        } catch (_: Exception) {
        }
        return map
    }

    private fun writeIndex(map: Map<String, CachedMeta>) {
        try {
            val arr = JSONArray()
            map.values.forEach { meta ->
                arr.put(
                    JSONObject().apply {
                        put("path", meta.relativePath)
                        put("isFolder", meta.isFolder)
                        put("sha", meta.sha)
                        put("size", meta.sizeBytes)
                    }
                )
            }
            indexFile.writeText(arr.toString())
        } catch (_: Exception) {
        }
    }

    private fun localFileFor(relativePath: String): File {
        val clean = relativePath.trim('/').replace("..", "")
        return if (clean.isEmpty()) localResDir else File(localResDir, clean)
    }

    private fun encodeGitHubPath(path: String): String {
        return path.trim('/')
            .split("/")
            .filter { it.isNotBlank() }
            .joinToString("/") { segment ->
                URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
            }
    }

    private fun parentOf(relativePath: String): String {
        val clean = relativePath.trim('/')
        val idx = clean.lastIndexOf('/')
        return if (idx == -1) "" else clean.substring(0, idx)
    }

    private fun leafOf(relativePath: String): String {
        val clean = relativePath.trim('/')
        val idx = clean.lastIndexOf('/')
        return if (idx == -1) clean else clean.substring(idx + 1)
    }

    private fun buildGitHubRequest(url: String, token: String): Request.Builder {
        val builder = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "DevWidgets-Android")
        if (token.isNotBlank()) {
            builder.header("Authorization", "Bearer ${token.trim()}")
        }
        return builder
    }

    private fun collectAllMergedPaths(): Map<String, CachedMeta> {
        val merged = readIndex()

        // Also scan local filesystem so offline-created folders/files are always visible
        fun walk(dir: File, prefix: String) {
            val children = dir.listFiles() ?: return
            for (child in children) {
                if (child.name == ".gitkeep" || child.name.startsWith(".")) continue
                val rel = if (prefix.isEmpty()) child.name else "$prefix/${child.name}"
                if (child.isDirectory) {
                    if (!merged.containsKey(rel)) {
                        merged[rel] = CachedMeta(rel, isFolder = true, sha = "", sizeBytes = 0L)
                    }
                    walk(child, rel)
                } else {
                    val existing = merged[rel]
                    merged[rel] = CachedMeta(
                        relativePath = rel,
                        isFolder = false,
                        sha = existing?.sha ?: "",
                        sizeBytes = child.length()
                    )
                }
            }
        }
        walk(localResDir, "")
        return merged
    }

    override suspend fun getEntries(parentPath: String): List<NoteTreeEntry> = withContext(Dispatchers.IO) {
        val normalizedParent = parentPath.trim('/')
        val all = collectAllMergedPaths()

        // Ensure implicit parent folders exist for all paths
        val withImplicitFolders = linkedMapOf<String, CachedMeta>()
        withImplicitFolders.putAll(all)
        for (path in all.keys.toList()) {
            var p = parentOf(path)
            while (p.isNotEmpty()) {
                if (!withImplicitFolders.containsKey(p)) {
                    withImplicitFolders[p] = CachedMeta(p, isFolder = true, sha = "", sizeBytes = 0L)
                }
                p = parentOf(p)
            }
        }

        // Direct children of normalizedParent
        val directChildren = withImplicitFolders.values.filter { meta ->
            val name = leafOf(meta.relativePath)
            name != ".gitkeep" && !name.startsWith(".") && parentOf(meta.relativePath) == normalizedParent
        }

        directChildren.map { meta ->
            val name = leafOf(meta.relativePath)
            val local = localFileFor(meta.relativePath)
            val childCount = if (meta.isFolder) {
                val prefix = "${meta.relativePath}/"
                withImplicitFolders.values.count { other ->
                    other.relativePath.startsWith(prefix) &&
                        !other.isFolder &&
                        leafOf(other.relativePath) != ".gitkeep" &&
                        !leafOf(other.relativePath).startsWith(".")
                }
            } else 0

            NoteTreeEntry(
                relativePath = meta.relativePath,
                name = name,
                parentPath = normalizedParent,
                isFolder = meta.isFolder,
                fileType = NoteFileType.fromFilename(name, meta.isFolder),
                sizeBytes = if (!meta.isFolder && local.exists()) local.length() else meta.sizeBytes,
                sha = meta.sha,
                isDownloaded = if (meta.isFolder) local.exists() else (local.exists() && local.isFile),
                childCount = childCount,
                lastModified = if (local.exists()) local.lastModified() else 0L
            )
        }.sortedWith(compareByDescending<NoteTreeEntry> { it.isFolder }.thenBy { it.name.lowercase() })
    }

    override suspend fun syncFromGitHub(owner: String, repo: String, token: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val cleanOwner = owner.trim().removePrefix("@")
            val cleanRepo = repo.trim().substringAfterLast('/')
            if (cleanOwner.isBlank() || cleanRepo.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Configure GitHub username and Notes repo in Settings"))
            }

            // 1. Fetch repository metadata to resolve default_branch
            val repoReq = buildGitHubRequest("https://api.github.com/repos/$cleanOwner/$cleanRepo", token).get().build()
            val defaultBranch = okHttpClient.newCall(repoReq).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val msg = when (resp.code) {
                        401 -> "Invalid Notes GitHub token (401 Unauthorized)"
                        403 -> "GitHub API rate limit or permission denied (403)"
                        404 -> "Notes repo '$cleanOwner/$cleanRepo' not found (404)"
                        else -> "GitHub error ${resp.code}"
                    }
                    return@withContext Result.failure(Exception(msg))
                }
                val json = JSONObject(resp.body?.string() ?: "{}")
                json.optString("default_branch", "main").ifBlank { "main" }
            }

            // 2. Fetch full recursive git tree in a single API call
            val treeUrl = "https://api.github.com/repos/$cleanOwner/$cleanRepo/git/trees/$defaultBranch?recursive=1"
            val treeReq = buildGitHubRequest(treeUrl, token).get().build()
            val treeJson = okHttpClient.newCall(treeReq).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed to load repo tree (HTTP ${resp.code})"))
                }
                JSONObject(resp.body?.string() ?: "{}")
            }

            val treeArr = treeJson.optJSONArray("tree") ?: JSONArray()
            val oldIndex = readIndex()
            val newIndex = linkedMapOf<String, CachedMeta>()
            val prefix = "$REMOTE_ROOT/"

            for (i in 0 until treeArr.length()) {
                val item = treeArr.getJSONObject(i)
                val fullPath = item.optString("path", "")
                if (!fullPath.startsWith(prefix)) continue

                val relPath = fullPath.removePrefix(prefix).trim('/')
                if (relPath.isBlank()) continue
                val type = item.optString("type", "")
                val sha = item.optString("sha", "")
                val size = item.optLong("size", 0L)

                if (type == "tree") {
                    newIndex[relPath] = CachedMeta(relPath, isFolder = true, sha = sha, sizeBytes = 0L)
                    localFileFor(relPath).mkdirs()
                } else if (type == "blob") {
                    val leaf = leafOf(relPath)
                    if (leaf == ".gitkeep") {
                        val parentDir = parentOf(relPath)
                        if (parentDir.isNotBlank() && !newIndex.containsKey(parentDir)) {
                            newIndex[parentDir] = CachedMeta(parentDir, isFolder = true, sha = "", sizeBytes = 0L)
                            localFileFor(parentDir).mkdirs()
                        }
                        continue
                    }
                    newIndex[relPath] = CachedMeta(relPath, isFolder = false, sha = sha, sizeBytes = size)

                    // Invalidate stale local cached file if remote SHA changed
                    val prev = oldIndex[relPath]
                    val local = localFileFor(relPath)
                    if (prev != null && prev.sha.isNotBlank() && prev.sha != sha && local.exists() && local.isFile) {
                        local.delete()
                    }
                }
            }

            // Remove local cached files that no longer exist remotely (if previously indexed)
            for ((oldPath, oldMeta) in oldIndex) {
                if (!newIndex.containsKey(oldPath)) {
                    val local = localFileFor(oldPath)
                    if (local.exists()) {
                        if (oldMeta.isFolder) local.deleteRecursively() else local.delete()
                    }
                }
            }

            writeIndex(newIndex)
            val fileCount = newIndex.values.count { !it.isFolder }
            Result.success(fileCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun openFile(relativePath: String, owner: String, repo: String, token: String): Result<OpenedNoteFile> = withContext(Dispatchers.IO) {
        try {
            val cleanRel = relativePath.trim('/')
            val name = leafOf(cleanRel)
            val fileType = NoteFileType.fromFilename(name, isFolder = false)
            val local = localFileFor(cleanRel)
            val index = readIndex()
            val meta = index[cleanRel]

            if (!local.exists() || !local.isFile) {
                val cleanOwner = owner.trim().removePrefix("@")
                val cleanRepo = repo.trim().substringAfterLast('/')
                if (cleanOwner.isBlank() || cleanRepo.isBlank()) {
                    return@withContext Result.failure(Exception("File not cached locally and GitHub Notes repo is not configured"))
                }

                val bytes = downloadRemoteFileBytes(cleanOwner, cleanRepo, cleanRel, meta?.sha ?: "", token)
                    ?: return@withContext Result.failure(Exception("Failed to download '$name' from GitHub"))

                local.parentFile?.mkdirs()
                local.writeBytes(bytes)
            }

            val entry = NoteTreeEntry(
                relativePath = cleanRel,
                name = name,
                parentPath = parentOf(cleanRel),
                isFolder = false,
                fileType = fileType,
                sizeBytes = local.length(),
                sha = meta?.sha ?: "",
                isDownloaded = true,
                lastModified = local.lastModified()
            )

            val textContent = if (fileType.isTextEditable) {
                try { local.readText() } catch (_: Exception) { "" }
            } else ""

            val extractedDocText = if (fileType == NoteFileType.DOCX) {
                extractDocxPlainText(local)
            } else ""

            Result.success(
                OpenedNoteFile(
                    entry = entry,
                    textContent = textContent,
                    localFilePath = local.absolutePath,
                    extractedDocText = extractedDocText
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun downloadRemoteFileBytes(
        owner: String,
        repo: String,
        relativePath: String,
        sha: String,
        token: String
    ): ByteArray? {
        val remotePath = "$REMOTE_ROOT/$relativePath"
        val encoded = encodeGitHubPath(remotePath)

        // Try Contents API first (gives raw bytes directly via raw Accept header, or base64 JSON)
        val contentsUrl = "https://api.github.com/repos/$owner/$repo/contents/$encoded"
        val rawReq = Request.Builder()
            .url(contentsUrl)
            .header("Accept", "application/vnd.github.raw+json")
            .header("User-Agent", "DevWidgets-Android")
            .apply { if (token.isNotBlank()) header("Authorization", "Bearer ${token.trim()}") }
            .get()
            .build()

        okHttpClient.newCall(rawReq).execute().use { resp ->
            if (resp.isSuccessful) {
                return resp.body?.bytes()
            }
        }

        // Fallback to Git Blobs API (supports files > 1MB up to 100MB)
        if (sha.isNotBlank()) {
            val blobUrl = "https://api.github.com/repos/$owner/$repo/git/blobs/$sha"
            val blobReq = buildGitHubRequest(blobUrl, token).get().build()
            okHttpClient.newCall(blobReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val json = JSONObject(resp.body?.string() ?: "{}")
                    val b64 = json.optString("content", "").replace("\n", "").replace("\r", "")
                    if (b64.isNotBlank()) {
                        return Base64.decode(b64, Base64.DEFAULT)
                    }
                }
            }
        }
        return null
    }

    private fun fetchRemoteFileSha(owner: String, repo: String, remotePath: String, token: String): String? {
        return try {
            val encoded = encodeGitHubPath(remotePath)
            val url = "https://api.github.com/repos/$owner/$repo/contents/$encoded"
            val req = buildGitHubRequest(url, token).get().build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val json = JSONObject(resp.body?.string() ?: "{}")
                json.optString("sha", "").ifBlank { null }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun putFileToGitHub(
        owner: String,
        repo: String,
        remotePath: String,
        bytes: ByteArray,
        commitMessage: String,
        knownSha: String,
        token: String
    ): Result<String> {
        val cleanOwner = owner.trim().removePrefix("@")
        val cleanRepo = repo.trim().substringAfterLast('/')
        if (cleanOwner.isBlank() || cleanRepo.isBlank() || token.isBlank()) {
            return Result.success(knownSha) // Offline save succeeded; skip push if no token
        }
        return try {
            val encoded = encodeGitHubPath(remotePath)
            val url = "https://api.github.com/repos/$cleanOwner/$cleanRepo/contents/$encoded"
            val currentSha = fetchRemoteFileSha(cleanOwner, cleanRepo, remotePath, token) ?: knownSha.ifBlank { null }

            val payload = JSONObject().apply {
                put("message", commitMessage)
                put("content", Base64.encodeToString(bytes, Base64.NO_WRAP))
                if (!currentSha.isNullOrBlank()) {
                    put("sha", currentSha)
                }
            }

            val req = buildGitHubRequest(url, token)
                .put(payload.toString().toRequestBody(JSON_MEDIA))
                .build()

            okHttpClient.newCall(req).execute().use { resp ->
                val bodyStr = resp.body?.string() ?: ""
                if (!resp.isSuccessful) {
                    return Result.failure(Exception("GitHub push failed (HTTP ${resp.code})"))
                }
                val newSha = try {
                    JSONObject(bodyStr).optJSONObject("content")?.optString("sha", "") ?: ""
                } catch (_: Exception) { "" }
                Result.success(newSha)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveTextFile(
        relativePath: String,
        content: String,
        owner: String,
        repo: String,
        token: String
    ): Result<OpenedNoteFile> = withContext(Dispatchers.IO) {
        try {
            val cleanRel = relativePath.trim('/')
            val local = localFileFor(cleanRel)
            local.parentFile?.mkdirs()
            local.writeText(content)

            val index = readIndex()
            val prevSha = index[cleanRel]?.sha ?: ""
            val remotePath = "$REMOTE_ROOT/$cleanRel"
            val pushRes = putFileToGitHub(
                owner = owner,
                repo = repo,
                remotePath = remotePath,
                bytes = content.toByteArray(Charsets.UTF_8),
                commitMessage = "notes: update $cleanRel",
                knownSha = prevSha,
                token = token
            )

            val newSha = pushRes.getOrElse { prevSha }
            index[cleanRel] = CachedMeta(cleanRel, isFolder = false, sha = newSha, sizeBytes = local.length())
            writeIndex(index)

            val name = leafOf(cleanRel)
            val entry = NoteTreeEntry(
                relativePath = cleanRel,
                name = name,
                parentPath = parentOf(cleanRel),
                isFolder = false,
                fileType = NoteFileType.fromFilename(name, isFolder = false),
                sizeBytes = local.length(),
                sha = newSha,
                isDownloaded = true,
                lastModified = local.lastModified()
            )
            val opened = OpenedNoteFile(
                entry = entry,
                textContent = content,
                localFilePath = local.absolutePath
            )
            if (pushRes.isFailure) {
                Result.failure(Exception("Saved locally, but GitHub push failed: ${pushRes.exceptionOrNull()?.message}"))
            } else {
                Result.success(opened)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createFolder(
        parentPath: String,
        folderName: String,
        owner: String,
        repo: String,
        token: String
    ): Result<NoteTreeEntry> = withContext(Dispatchers.IO) {
        try {
            val cleanName = folderName.trim().replace("/", "_").replace("..", "")
            if (cleanName.isBlank()) return@withContext Result.failure(IllegalArgumentException("Folder name cannot be empty"))

            val cleanParent = parentPath.trim('/')
            val relPath = if (cleanParent.isEmpty()) cleanName else "$cleanParent/$cleanName"
            val localDir = localFileFor(relPath)
            localDir.mkdirs()

            val index = readIndex()
            index[relPath] = CachedMeta(relPath, isFolder = true, sha = "", sizeBytes = 0L)
            writeIndex(index)

            // Push a .gitkeep so GitHub tracks the new folder immediately
            if (owner.isNotBlank() && repo.isNotBlank() && token.isNotBlank()) {
                putFileToGitHub(
                    owner = owner,
                    repo = repo,
                    remotePath = "$REMOTE_ROOT/$relPath/.gitkeep",
                    bytes = ByteArray(0),
                    commitMessage = "notes: create folder $relPath",
                    knownSha = "",
                    token = token
                )
            }

            Result.success(
                NoteTreeEntry(
                    relativePath = relPath,
                    name = cleanName,
                    parentPath = cleanParent,
                    isFolder = true,
                    fileType = NoteFileType.FOLDER,
                    isDownloaded = true,
                    lastModified = localDir.lastModified()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadFile(
        parentPath: String,
        filename: String,
        bytes: ByteArray,
        owner: String,
        repo: String,
        token: String
    ): Result<NoteTreeEntry> = withContext(Dispatchers.IO) {
        try {
            val cleanName = filename.trim().replace("/", "_").replace("..", "")
            if (cleanName.isBlank()) return@withContext Result.failure(IllegalArgumentException("Filename cannot be empty"))

            val cleanParent = parentPath.trim('/')
            val relPath = if (cleanParent.isEmpty()) cleanName else "$cleanParent/$cleanName"
            val local = localFileFor(relPath)
            local.parentFile?.mkdirs()
            local.writeBytes(bytes)

            val index = readIndex()
            val prevSha = index[relPath]?.sha ?: ""
            val pushRes = putFileToGitHub(
                owner = owner,
                repo = repo,
                remotePath = "$REMOTE_ROOT/$relPath",
                bytes = bytes,
                commitMessage = "notes: upload $relPath",
                knownSha = prevSha,
                token = token
            )
            val newSha = pushRes.getOrElse { prevSha }
            index[relPath] = CachedMeta(relPath, isFolder = false, sha = newSha, sizeBytes = local.length())
            writeIndex(index)

            val entry = NoteTreeEntry(
                relativePath = relPath,
                name = cleanName,
                parentPath = cleanParent,
                isFolder = false,
                fileType = NoteFileType.fromFilename(cleanName, isFolder = false),
                sizeBytes = local.length(),
                sha = newSha,
                isDownloaded = true,
                lastModified = local.lastModified()
            )
            if (pushRes.isFailure) {
                Result.failure(Exception("Saved locally, but GitHub upload failed: ${pushRes.exceptionOrNull()?.message}"))
            } else {
                Result.success(entry)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteEntry(
        relativePath: String,
        isFolder: Boolean,
        owner: String,
        repo: String,
        token: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val cleanRel = relativePath.trim('/')
            val local = localFileFor(cleanRel)
            val index = readIndex()

            val pathsToDeleteRemotely = mutableListOf<String>()
            if (isFolder) {
                val prefix = "$cleanRel/"
                index.values.filter { !it.isFolder && it.relativePath.startsWith(prefix) }
                    .forEach { pathsToDeleteRemotely.add(it.relativePath) }
                pathsToDeleteRemotely.add("$cleanRel/.gitkeep")
                index.keys.removeAll { it == cleanRel || it.startsWith(prefix) }
                if (local.exists()) local.deleteRecursively()
            } else {
                pathsToDeleteRemotely.add(cleanRel)
                index.remove(cleanRel)
                if (local.exists()) local.delete()
            }
            writeIndex(index)

            val cleanOwner = owner.trim().removePrefix("@")
            val cleanRepo = repo.trim().substringAfterLast('/')
            if (cleanOwner.isNotBlank() && cleanRepo.isNotBlank() && token.isNotBlank()) {
                for (rel in pathsToDeleteRemotely) {
                    val remotePath = "$REMOTE_ROOT/$rel"
                    val sha = fetchRemoteFileSha(cleanOwner, cleanRepo, remotePath, token) ?: continue
                    val encoded = encodeGitHubPath(remotePath)
                    val url = "https://api.github.com/repos/$cleanOwner/$cleanRepo/contents/$encoded"
                    val payload = JSONObject().apply {
                        put("message", "notes: delete $rel")
                        put("sha", sha)
                    }
                    val req = buildGitHubRequest(url, token)
                        .delete(payload.toString().toRequestBody(JSON_MEDIA))
                        .build()
                    try {
                        okHttpClient.newCall(req).execute().close()
                    } catch (_: Exception) {
                    }
                }
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lightweight zero-dependency DOCX text extractor (reads word/document.xml paragraphs)
     * so users get instant in-app preview alongside the "Open in External App" button.
     */
    private fun extractDocxPlainText(file: File): String {
        return try {
            ZipInputStream(file.inputStream().buffered()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") {
                        val xml = zis.readBytes().toString(Charsets.UTF_8)
                        val paragraphs = xml.split("</w:p>")
                        val sb = StringBuilder()
                        val tagRegex = Regex("<[^>]+>")
                        for (p in paragraphs) {
                            val text = tagRegex.replace(p, "")
                                .replace("&amp;", "&")
                                .replace("&lt;", "<")
                                .replace("&gt;", ">")
                                .replace("&quot;", "\"")
                                .replace("&apos;", "'")
                                .trim()
                            if (text.isNotEmpty()) {
                                sb.append(text).append("\n\n")
                            }
                        }
                        return sb.toString().trim()
                    }
                    entry = zis.nextEntry
                }
            }
            ""
        } catch (_: Exception) {
            ""
        }
    }
}
