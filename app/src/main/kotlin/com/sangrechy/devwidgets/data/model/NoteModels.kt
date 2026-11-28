package com.sangrechy.devwidgets.data.model

enum class NoteFileType(val badge: String) {
    FOLDER("DIR"),
    MARKDOWN("MD"),
    TEXT("TXT"),
    PDF("PDF"),
    DOCX("DOCX"),
    IMAGE("IMG"),
    DOCUMENT("DOC"),
    OTHER("FILE");

    companion object {
        fun fromFilename(filename: String, isFolder: Boolean = false): NoteFileType {
            if (isFolder) return FOLDER
            return when (filename.substringAfterLast('.', "").lowercase()) {
                "md", "markdown" -> MARKDOWN
                "txt", "kt", "java", "py", "cpp", "c", "h", "js", "ts", "json", "xml", "yaml", "yml", "sql", "sh", "html", "css", "csv" -> TEXT
                "pdf" -> PDF
                "docx" -> DOCX
                "png", "jpg", "jpeg", "webp", "gif", "bmp" -> IMAGE
                "doc", "ppt", "pptx", "xls", "xlsx", "odt", "rtf" -> DOCUMENT
                else -> OTHER
            }
        }
    }

    val isTextEditable: Boolean
        get() = this == MARKDOWN || this == TEXT
}

data class NoteTreeEntry(
    val relativePath: String, // Relative to res/, e.g. "CS Fundamentals" or "CS Fundamentals/OS/Deadlock.md"
    val name: String,
    val parentPath: String, // "" for top-level subject inside res/
    val isFolder: Boolean,
    val fileType: NoteFileType = NoteFileType.fromFilename(name, isFolder),
    val sizeBytes: Long = 0L,
    val sha: String = "",
    val isDownloaded: Boolean = false,
    val childCount: Int = 0,
    val lastModified: Long = 0L
)

data class OpenedNoteFile(
    val entry: NoteTreeEntry,
    val textContent: String = "",
    val localFilePath: String = "",
    val extractedDocText: String = ""
)
