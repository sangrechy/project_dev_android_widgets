package com.sangrechy.devwidgets.ui.notes

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.sangrechy.devwidgets.data.model.NoteFileType
import com.sangrechy.devwidgets.data.model.NoteTreeEntry
import com.sangrechy.devwidgets.data.model.OpenedNoteFile
import com.sangrechy.devwidgets.ui.theme.ThemeAccentAmber
import com.sangrechy.devwidgets.ui.theme.ThemeAccentBlue
import com.sangrechy.devwidgets.ui.theme.ThemeAccentGreen
import com.sangrechy.devwidgets.ui.theme.ThemeAccentRed
import com.sangrechy.devwidgets.ui.theme.ThemeBgDark
import com.sangrechy.devwidgets.ui.theme.ThemeCardBg
import com.sangrechy.devwidgets.ui.theme.ThemeCardBgElevated
import com.sangrechy.devwidgets.ui.theme.ThemeCardBorder
import com.sangrechy.devwidgets.ui.theme.ThemeInnerBoxBg
import com.sangrechy.devwidgets.ui.theme.ThemeInnerBoxBorder
import com.sangrechy.devwidgets.ui.theme.ThemePillBg
import com.sangrechy.devwidgets.ui.theme.ThemeTextFieldBg
import com.sangrechy.devwidgets.ui.theme.ThemeTextMuted
import com.sangrechy.devwidgets.ui.theme.ThemeTextPrimary
import com.sangrechy.devwidgets.ui.theme.ThemeTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

private val FlashcardAccents = listOf(
    ThemeAccentBlue,
    ThemeAccentGreen,
    ThemeAccentAmber,
    Color(0xFFC084FC), // Soft Violet
    Color(0xFF38BDF8), // Sky Cyan
    Color(0xFFFB7185)  // Rose
)

@Composable
fun NotesScreenContent(
    gitHubUsername: String,
    notesRepoInput: String,
    notesTokenInput: String,
    currentPath: String,
    entries: List<NoteTreeEntry>,
    searchQuery: String,
    isSyncing: Boolean,
    isLoadingFile: Boolean,
    onNotesRepoChanged: (String) -> Unit,
    onNotesTokenChanged: (String) -> Unit,
    onSaveNotesConfig: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onNavigateTo: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onSyncNotes: () -> Unit,
    onOpenFile: (NoteTreeEntry) -> Unit,
    onCreateFolder: (String) -> Unit,
    onCreateMarkdownNote: (String, String) -> Unit,
    onUploadDocument: (String, ByteArray) -> Unit,
    onDeleteEntry: (NoteTreeEntry) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showConfigPanel by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCreateNoteDialog by remember { mutableStateOf(false) }
    var entryPendingDelete by remember { mutableStateOf<NoteTreeEntry?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val displayName = queryDisplayName(context, uri) ?: "document_${System.currentTimeMillis()}"
                        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            ?: error("Unable to read selected file")
                        displayName to bytes
                    }
                }
                result.onSuccess { (filename, bytes) ->
                    onUploadDocument(filename, bytes)
                }.onFailure { err ->
                    Toast.makeText(context, err.message ?: "Failed to read file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val folders = remember(entries) { entries.filter { it.isFolder } }
    val files = remember(entries) { entries.filter { !it.isFolder } }
    val isRootFlashcardMode = currentPath.isBlank() && searchQuery.isBlank()
    val folderPairs = remember(folders) { folders.chunked(2) }
    val repoDisplay = remember(gitHubUsername, notesRepoInput) {
        val repo = notesRepoInput.trim()
        when {
            repo.contains("/") -> repo
            gitHubUsername.isNotBlank() && repo.isNotBlank() -> "${gitHubUsername.trim()}/$repo"
            repo.isNotBlank() -> repo
            else -> "Not configured"
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Header Card (Repo badge + Sync + Config toggle)
        item(key = "notes_header") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(ThemeCardBg)
                    .border(1.dp, ThemeCardBorder, RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ThemeAccentBlue.copy(alpha = 0.16f))
                                    .border(1.dp, ThemeAccentBlue.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = "Notes",
                                    tint = ThemeAccentBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Notes Vault",
                                    color = ThemeTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$repoDisplay  •  res/",
                                    color = ThemeTextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = { showConfigPanel = !showConfigPanel },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ThemeInnerBoxBg)
                                    .border(1.dp, ThemeInnerBoxBorder, RoundedCornerShape(10.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Notes Repo Settings",
                                    tint = if (showConfigPanel) ThemeAccentBlue else ThemeTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Button(
                                onClick = onSyncNotes,
                                enabled = !isSyncing,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ThemeAccentBlue.copy(alpha = 0.2f),
                                    contentColor = ThemeAccentBlue
                                ),
                                border = BorderStroke(1.dp, ThemeAccentBlue.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = ThemeAccentBlue
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Sync",
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isSyncing) "Syncing" else "Sync",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Inline quick-setup panel (collapsible or shown if repo/user missing)
                    if (showConfigPanel || (notesRepoInput.isBlank() && entries.isEmpty())) {
                        HorizontalDivider(color = ThemeCardBorder)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Notes Repository (reads res/ folder)",
                                    color = ThemeTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (gitHubUsername.isBlank()) {
                                    Text(
                                        text = "Set Username →",
                                        color = ThemeAccentAmber,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { onNavigateToSettings() }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = notesRepoInput,
                                onValueChange = onNotesRepoChanged,
                                label = { Text("Notes Repo Name", color = ThemeTextSecondary, fontSize = 12.sp) },
                                placeholder = { Text("e.g. notes or username/notes", color = ThemeTextMuted) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = notesTextFieldColors()
                            )

                            OutlinedTextField(
                                value = notesTokenInput,
                                onValueChange = onNotesTokenChanged,
                                label = { Text("Notes GitHub Token (PAT)", color = ThemeTextSecondary, fontSize = 12.sp) },
                                placeholder = { Text("ghp_... (for private repo & push edits)", color = ThemeTextMuted) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                colors = notesTextFieldColors()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        onSaveNotesConfig()
                                        onSyncNotes()
                                        showConfigPanel = false
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ThemeTextPrimary,
                                        contentColor = ThemeBgDark
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Save & Sync", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = onNavigateToSettings,
                                    border = BorderStroke(1.dp, ThemeCardBorder),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("All Settings", color = ThemeTextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Search & File Actions Bar
        item(key = "notes_actions") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = { Text("Search subjects, notes, PDFs, docs...", color = ThemeTextMuted, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = ThemeTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = ThemeTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = notesTextFieldColors()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NotesActionChip(
                        icon = Icons.Default.CreateNewFolder,
                        label = if (currentPath.isBlank()) "+ Subject" else "+ Folder",
                        accent = ThemeAccentAmber,
                        onClick = { showCreateFolderDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                    NotesActionChip(
                        icon = Icons.Default.NoteAdd,
                        label = "+ Note",
                        accent = ThemeAccentGreen,
                        onClick = { showCreateNoteDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                    NotesActionChip(
                        icon = Icons.Default.UploadFile,
                        label = "Upload",
                        accent = ThemeAccentBlue,
                        onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Breadcrumbs when inside a subfolder
        if (currentPath.isNotBlank()) {
            item(key = "notes_breadcrumbs") {
                NotesBreadcrumbBar(
                    currentPath = currentPath,
                    onNavigateUp = onNavigateUp,
                    onNavigateTo = onNavigateTo
                )
            }
        }

        // Loading indicator when opening a file
        if (isLoadingFile) {
            item(key = "notes_loading_file") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ThemeCardBg)
                        .border(1.dp, ThemeCardBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = ThemeAccentBlue
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Opening file...", color = ThemeTextSecondary, fontSize = 12.sp)
                }
            }
        }

        // 4. Content: Empty State OR Subject Flashcards (at res/ root) OR Hierarchical List
        if (entries.isEmpty()) {
            item(key = "notes_empty") {
                EmptyNotesCard(
                    currentPath = currentPath,
                    isSearching = searchQuery.isNotBlank(),
                    onSync = onSyncNotes,
                    onCreateFolder = { showCreateFolderDialog = true },
                    onCreateNote = { showCreateNoteDialog = true }
                )
            }
        } else if (isRootFlashcardMode) {
            // Top-level Subjects inside res/ rendered as 2-column Flashcards
            if (folders.isNotEmpty()) {
                item(key = "subjects_label") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SUBJECTS IN RES/",
                            color = ThemeTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${folders.size} ${if (folders.size == 1) "subject" else "subjects"}",
                            color = ThemeTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                items(
                    items = folderPairs,
                    key = { pair -> pair.joinToString("|") { it.relativePath } }
                ) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEachIndexed { colIdx, folder ->
                            val globalIdx = folders.indexOf(folder).coerceAtLeast(colIdx)
                            val accent = FlashcardAccents[globalIdx % FlashcardAccents.size]
                            SubjectFlashcard(
                                entry = folder,
                                accent = accent,
                                onClick = { onNavigateTo(folder.relativePath) },
                                onDelete = { entryPendingDelete = folder },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Root-level files directly inside res/ (if any)
            if (files.isNotEmpty()) {
                item(key = "root_files_label") {
                    Text(
                        text = "FILES IN RES/",
                        color = ThemeTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 4.dp, start = 2.dp)
                    )
                }

                items(items = files, key = { it.relativePath }) { fileEntry ->
                    NoteTreeEntryRow(
                        entry = fileEntry,
                        showParentPath = false,
                        onClick = { onOpenFile(fileEntry) },
                        onDelete = { entryPendingDelete = fileEntry }
                    )
                }
            }
        } else {
            // Subfolder or Search Results: Hierarchical list (folders first, then files)
            item(key = "section_count_label") {
                val label = if (searchQuery.isNotBlank()) {
                    "SEARCH RESULTS (${entries.size})"
                } else {
                    val folderName = currentPath.substringAfterLast('/')
                    "${folderName.uppercase(Locale.US)}  •  ${entries.size} ITEMS"
                }
                Text(
                    text = label,
                    color = ThemeTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }

            items(items = entries, key = { it.relativePath }) { entry ->
                NoteTreeEntryRow(
                    entry = entry,
                    showParentPath = searchQuery.isNotBlank(),
                    onClick = {
                        if (entry.isFolder) onNavigateTo(entry.relativePath)
                        else onOpenFile(entry)
                    },
                    onDelete = { entryPendingDelete = entry }
                )
            }
        }
    }

    // Create Folder Dialog
    if (showCreateFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            containerColor = ThemeCardBgElevated,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = if (currentPath.isBlank()) "New Subject Folder" else "New Folder",
                    color = ThemeTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (currentPath.isBlank()) "Creates a subject folder inside res/" else "Creates a subfolder inside res/$currentPath/",
                        color = ThemeTextSecondary,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        label = { Text("Folder name", color = ThemeTextSecondary) },
                        placeholder = { Text("e.g. CS Fundamentals", color = ThemeTextMuted) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = notesTextFieldColors()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            onCreateFolder(folderName.trim())
                            showCreateFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeAccentAmber, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Create", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel", color = ThemeTextSecondary)
                }
            }
        )
    }

    // Create Markdown Note Dialog
    if (showCreateNoteDialog) {
        var noteTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateNoteDialog = false },
            containerColor = ThemeCardBgElevated,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "New Markdown Note",
                    color = ThemeTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Saved in res/${if (currentPath.isBlank()) "" else "$currentPath/"} (syncs to GitHub if token is set)",
                        color = ThemeTextSecondary,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("Note filename", color = ThemeTextSecondary) },
                        placeholder = { Text("e.g. Operating_Systems.md", color = ThemeTextMuted) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = notesTextFieldColors()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = noteTitle.trim()
                        if (trimmed.isNotBlank()) {
                            val displayTitle = trimmed.removeSuffix(".md").removeSuffix(".markdown")
                            val template = "# $displayTitle\n\n## Key Points\n- \n\n## Summary\n"
                            onCreateMarkdownNote(trimmed, template)
                            showCreateNoteDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeAccentGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Create & Open", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateNoteDialog = false }) {
                    Text("Cancel", color = ThemeTextSecondary)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    entryPendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { entryPendingDelete = null },
            containerColor = ThemeCardBgElevated,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Delete ${if (target.isFolder) "Folder" else "File"}?",
                    color = ThemeTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "Delete \"${target.name}\" from offline cache${if (notesTokenInput.isNotBlank()) " and GitHub repo" else ""}?",
                    color = ThemeTextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteEntry(target)
                        entryPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeAccentRed, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryPendingDelete = null }) {
                    Text("Cancel", color = ThemeTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun NotesActionChip(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ThemeCardBg)
            .border(1.dp, ThemeCardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = accent,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = ThemeTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NotesBreadcrumbBar(
    currentPath: String,
    onNavigateUp: () -> Unit,
    onNavigateTo: (String) -> Unit
) {
    val segments = remember(currentPath) { currentPath.split('/').filter { it.isNotBlank() } }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ThemeCardBg)
            .border(1.dp, ThemeCardBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onNavigateUp,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = ThemeTextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "res",
                color = ThemeAccentBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onNavigateTo("") }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )

            segments.forEachIndexed { index, seg ->
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = ThemeTextMuted,
                    modifier = Modifier.size(14.dp)
                )
                val targetPath = segments.take(index + 1).joinToString("/")
                val isLast = index == segments.lastIndex
                Text(
                    text = seg,
                    color = if (isLast) ThemeTextPrimary else ThemeAccentBlue,
                    fontSize = 12.sp,
                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(enabled = !isLast) { onNavigateTo(targetPath) }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SubjectFlashcard(
    entry: NoteTreeEntry,
    accent: Color,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(126.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(ThemeCardBg)
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(accent.copy(alpha = 0.16f))
                        .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = entry.name,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete subject",
                        tint = ThemeTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = entry.name,
                    color = ThemeTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accent.copy(alpha = 0.14f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${entry.childCount} ${if (entry.childCount == 1) "item" else "items"}",
                        color = accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteTreeEntryRow(
    entry: NoteTreeEntry,
    showParentPath: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val (icon, tint) = iconAndColorFor(entry.fileType)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ThemeCardBg)
            .border(1.dp, ThemeCardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(tint.copy(alpha = 0.15f))
                .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = entry.fileType.badge,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = entry.name,
                color = ThemeTextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(tint.copy(alpha = 0.15f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = entry.fileType.badge,
                        color = tint,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (entry.isFolder) {
                    Text(
                        text = "${entry.childCount} ${if (entry.childCount == 1) "item" else "items"}",
                        color = ThemeTextSecondary,
                        fontSize = 11.sp
                    )
                } else {
                    if (entry.sizeBytes > 0L) {
                        Text(
                            text = formatFileSize(entry.sizeBytes),
                            color = ThemeTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    if (entry.isDownloaded) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(ThemeAccentGreen)
                        )
                        Text(
                            text = "Offline",
                            color = ThemeAccentGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (showParentPath && entry.parentPath.isNotBlank()) {
                    Text(
                        text = "• ${entry.parentPath}",
                        color = ThemeTextMuted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(30.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = ThemeTextMuted,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
private fun EmptyNotesCard(
    currentPath: String,
    isSearching: Boolean,
    onSync: () -> Unit,
    onCreateFolder: () -> Unit,
    onCreateNote: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ThemeCardBg)
            .border(1.dp, ThemeCardBorder, RoundedCornerShape(20.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = ThemeTextMuted,
                modifier = Modifier.size(38.dp)
            )
            Text(
                text = when {
                    isSearching -> "No matching files or folders"
                    currentPath.isBlank() -> "No subjects in res/ yet"
                    else -> "This folder is empty"
                },
                color = ThemeTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = when {
                    isSearching -> "Try a different search keyword."
                    currentPath.isBlank() -> "Sync your Notes GitHub repo (reads res/) or create a subject folder to get started."
                    else -> "Add a subfolder, write a Markdown note, or upload PDF / DOCX files."
                },
                color = ThemeTextSecondary,
                fontSize = 12.sp
            )
            if (!isSearching) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onSync,
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeAccentBlue, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Sync Repo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = if (currentPath.isBlank()) onCreateFolder else onCreateNote,
                        border = BorderStroke(1.dp, ThemeCardBorder),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = ThemeTextPrimary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (currentPath.isBlank()) "New Subject" else "New Note",
                            color = ThemeTextPrimary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// IN-APP NOTE FILE VIEWER & EDITOR OVERLAY
// =========================================================================

@Composable
fun NoteFileViewerOverlay(
    openedFile: OpenedNoteFile,
    isSaving: Boolean,
    onClose: () -> Unit,
    onSaveText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val entry = openedFile.entry
    val isEditable = entry.fileType.isTextEditable

    var isEditMode by remember(entry.relativePath) {
        mutableStateOf(isEditable && openedFile.textContent.isBlank())
    }
    var draftContent by remember(entry.relativePath, openedFile.textContent) {
        mutableStateOf(openedFile.textContent)
    }
    val hasUnsavedChanges = isEditable && draftContent != openedFile.textContent

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(ThemeBgDark.copy(alpha = 0.97f))
            .border(1.dp, ThemeCardBorder, RoundedCornerShape(22.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ThemeCardBg)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Close",
                        tint = ThemeTextPrimary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.name,
                        color = ThemeTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "res/${entry.relativePath}",
                        color = ThemeTextSecondary,
                        fontSize = 10.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isEditable) {
                    // Read / Edit toggle
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(ThemeInnerBoxBg)
                            .border(1.dp, ThemeInnerBoxBorder, RoundedCornerShape(10.dp))
                            .clickable { isEditMode = !isEditMode }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Default.Visibility else Icons.Default.Edit,
                            contentDescription = if (isEditMode) "Preview" else "Edit",
                            tint = ThemeAccentBlue,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (isEditMode) "Read" else "Edit",
                            color = ThemeTextPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Save & Push button
                    Button(
                        onClick = { onSaveText(draftContent) },
                        enabled = !isSaving && (hasUnsavedChanges || isEditMode),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasUnsavedChanges) ThemeAccentGreen else ThemeCardBgElevated,
                            contentColor = if (hasUnsavedChanges) Color.Black else ThemeTextSecondary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = Color.Black
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Save",
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // External Open Button for PDF / DOCX / Images / Other
                    Button(
                        onClick = { openFileExternally(context, openedFile.localFilePath, entry.name) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThemeAccentBlue,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open Externally",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(color = ThemeCardBorder)

            // Body Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (entry.fileType) {
                    NoteFileType.MARKDOWN -> {
                        if (isEditMode) {
                            OutlinedTextField(
                                value = draftContent,
                                onValueChange = { draftContent = it },
                                placeholder = { Text("Write Markdown here...", color = ThemeTextMuted) },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    color = ThemeTextPrimary
                                ),
                                colors = notesTextFieldColors()
                            )
                        } else {
                            MarkdownWebViewPreview(
                                markdown = draftContent,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    NoteFileType.TEXT -> {
                        if (isEditMode) {
                            OutlinedTextField(
                                value = draftContent,
                                onValueChange = { draftContent = it },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    color = ThemeTextPrimary
                                ),
                                colors = notesTextFieldColors()
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = draftContent.ifBlank { "(Empty file)" },
                                    color = ThemeTextPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.5.sp,
                                    lineHeight = 19.sp
                                )
                            }
                        }
                    }

                    NoteFileType.PDF -> {
                        PdfDocumentViewer(
                            localFilePath = openedFile.localFilePath,
                            onOpenExternal = { openFileExternally(context, openedFile.localFilePath, entry.name) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    NoteFileType.DOCX -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(ThemeCardBg)
                                    .border(1.dp, ThemeCardBorder, RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "DOCX Quick Preview",
                                        color = ThemeTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "For full formatting & editing, open in Docs / Office.",
                                        color = ThemeTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { openFileExternally(context, openedFile.localFilePath, entry.name) },
                                    border = BorderStroke(1.dp, ThemeAccentBlue),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Open App", color = ThemeAccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(ThemeInnerBoxBg)
                                    .border(1.dp, ThemeInnerBoxBorder, RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = openedFile.extractedDocText.ifBlank { "No readable text found in this DOCX file. Tap Open App above to view it." },
                                    color = ThemeTextPrimary,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }

                    NoteFileType.IMAGE -> {
                        val imageBitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
                            initialValue = null,
                            openedFile.localFilePath
                        ) {
                            value = withContext(Dispatchers.IO) {
                                runCatching {
                                    BitmapFactory.decodeFile(openedFile.localFilePath)?.asImageBitmap()
                                }.getOrNull()
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val bmp = imageBitmap
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp,
                                    contentDescription = entry.name,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(14.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                CircularProgressIndicator(color = ThemeAccentBlue)
                            }
                        }
                    }

                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                    contentDescription = null,
                                    tint = ThemeAccentBlue,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = entry.name,
                                    color = ThemeTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Downloaded offline (${formatFileSize(entry.sizeBytes)}). Open with an installed document viewer.",
                                    color = ThemeTextSecondary,
                                    fontSize = 12.sp
                                )
                                Button(
                                    onClick = { openFileExternally(context, openedFile.localFilePath, entry.name) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ThemeAccentBlue, contentColor = Color.Black),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Externally", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfDocumentViewer(
    localFilePath: String,
    onOpenExternal: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pageCount by remember(localFilePath) { mutableStateOf(0) }
    var loadError by remember(localFilePath) { mutableStateOf<String?>(null) }
    var renderedPages by remember(localFilePath) { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isRendering by remember(localFilePath) { mutableStateOf(true) }

    LaunchedEffect(localFilePath) {
        isRendering = true
        loadError = null
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val file = File(localFilePath)
                if (!file.exists()) error("PDF file not found locally")
                val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fd)
                val total = renderer.pageCount
                val maxPreviewPages = total.coerceAtMost(30)
                val bitmaps = mutableListOf<Bitmap>()
                try {
                    for (i in 0 until maxPreviewPages) {
                        val page = renderer.openPage(i)
                        try {
                            val scale = 2
                            val w = (page.width * scale).coerceIn(400, 1440)
                            val h = (page.height * scale).coerceIn(400, 2200)
                            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            bmp.eraseColor(android.graphics.Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bitmaps.add(bmp)
                        } finally {
                            page.close()
                        }
                    }
                } finally {
                    renderer.close()
                    fd.close()
                }
                total to bitmaps
            }
        }
        result.onSuccess { (total, bitmaps) ->
            pageCount = total
            renderedPages = bitmaps
        }.onFailure { err ->
            loadError = err.message ?: "Unable to render PDF"
        }
        isRendering = false
    }

    DisposableEffect(localFilePath) {
        onDispose {
            renderedPages.forEach { if (!it.isRecycled) it.recycle() }
        }
    }

    when {
        isRendering -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(color = ThemeAccentBlue)
                    Text("Rendering PDF pages...", color = ThemeTextSecondary, fontSize = 12.sp)
                }
            }
        }

        loadError != null -> {
            Box(modifier = modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(loadError ?: "Failed to open PDF", color = ThemeAccentRed, fontSize = 13.sp)
                    Button(onClick = onOpenExternal) {
                        Text("Open in PDF App")
                    }
                }
            }
        }

        else -> {
            LazyColumn(
                modifier = modifier.padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(renderedPages.size) { index ->
                    val bmp = renderedPages[index]
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Page ${index + 1} of $pageCount",
                            color = ThemeTextMuted,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "PDF page ${index + 1}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, ThemeCardBorder, RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
                if (pageCount > renderedPages.size) {
                    item {
                        OutlinedButton(
                            onClick = onOpenExternal,
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, ThemeAccentBlue)
                        ) {
                            Text(
                                "Showing first ${renderedPages.size} of $pageCount pages • Open Full PDF",
                                color = ThemeAccentBlue,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownWebViewPreview(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val html = remember(markdown) { markdownToDarkHtml(markdown) }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                settings.javaScriptEnabled = false
                settings.allowFileAccess = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = false
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        }
    )
}

private fun markdownToDarkHtml(markdown: String): String {
    val lines = markdown.replace("\r\n", "\n").split("\n")
    val body = StringBuilder()
    var inCodeBlock = false
    var inList = false

    fun closeList() {
        if (inList) {
            body.append("</ul>")
            inList = false
        }
    }

    fun escapeHtml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    fun formatInline(raw: String): String {
        var s = escapeHtml(raw)
        s = Regex("`([^`]+)`").replace(s) { "<code>${it.groupValues[1]}</code>" }
        s = Regex("\\*\\*([^*]+)\\*\\*").replace(s) { "<strong>${it.groupValues[1]}</strong>" }
        s = Regex("__([^_]+)__").replace(s) { "<strong>${it.groupValues[1]}</strong>" }
        s = Regex("\\*([^*]+)\\*").replace(s) { "<em>${it.groupValues[1]}</em>" }
        s = Regex("\\[([^\\]]+)]\\(([^)]+)\\)").replace(s) {
            "<a href=\"${it.groupValues[2]}\">${it.groupValues[1]}</a>"
        }
        return s
    }

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("```")) {
            closeList()
            if (!inCodeBlock) {
                body.append("<pre><code>")
                inCodeBlock = true
            } else {
                body.append("</code></pre>")
                inCodeBlock = false
            }
            continue
        }
        if (inCodeBlock) {
            body.append(escapeHtml(line)).append("\n")
            continue
        }
        when {
            trimmed.isEmpty() -> {
                closeList()
            }
            trimmed.startsWith("### ") -> {
                closeList()
                body.append("<h3>").append(formatInline(trimmed.drop(4))).append("</h3>")
            }
            trimmed.startsWith("## ") -> {
                closeList()
                body.append("<h2>").append(formatInline(trimmed.drop(3))).append("</h2>")
            }
            trimmed.startsWith("# ") -> {
                closeList()
                body.append("<h1>").append(formatInline(trimmed.drop(2))).append("</h1>")
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                if (!inList) {
                    body.append("<ul>")
                    inList = true
                }
                body.append("<li>").append(formatInline(trimmed.drop(2))).append("</li>")
            }
            trimmed.startsWith("> ") -> {
                closeList()
                body.append("<blockquote>").append(formatInline(trimmed.drop(2))).append("</blockquote>")
            }
            trimmed == "---" || trimmed == "***" -> {
                closeList()
                body.append("<hr/>")
            }
            else -> {
                closeList()
                body.append("<p>").append(formatInline(line)).append("</p>")
            }
        }
    }
    if (inCodeBlock) body.append("</code></pre>")
    closeList()

    return """
        <!DOCTYPE html>
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
        <style>
          body {
            background: transparent;
            color: #E6EDF3;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            font-size: 14px;
            line-height: 1.65;
            padding: 14px 16px 28px 16px;
            margin: 0;
            word-wrap: break-word;
          }
          h1, h2, h3 { color: #FFFFFF; margin-top: 18px; margin-bottom: 8px; }
          h1 { font-size: 20px; border-bottom: 1px solid #30363D; padding-bottom: 6px; }
          h2 { font-size: 17px; }
          h3 { font-size: 15px; color: #79C0FF; }
          p { margin: 8px 0; }
          ul { padding-left: 20px; margin: 8px 0; }
          li { margin: 4px 0; }
          code {
            background: rgba(110, 118, 129, 0.25);
            color: #79C0FF;
            padding: 2px 5px;
            border-radius: 5px;
            font-family: monospace;
            font-size: 12.5px;
          }
          pre {
            background: #0D1117;
            border: 1px solid #30363D;
            border-radius: 10px;
            padding: 12px;
            overflow-x: auto;
          }
          pre code { background: transparent; padding: 0; color: #E6EDF3; }
          blockquote {
            border-left: 3px solid #58A6FF;
            margin: 10px 0;
            padding: 4px 12px;
            color: #8B949E;
            background: rgba(88, 166, 255, 0.08);
            border-radius: 0 8px 8px 0;
          }
          hr { border: none; border-top: 1px solid #30363D; margin: 14px 0; }
          a { color: #58A6FF; text-decoration: none; }
        </style>
        </head>
        <body>${body}</body>
        </html>
    """.trimIndent()
}

@Composable
private fun notesTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = ThemeTextPrimary,
    unfocusedTextColor = ThemeTextPrimary,
    focusedBorderColor = ThemeAccentBlue,
    unfocusedBorderColor = ThemeInnerBoxBorder,
    focusedContainerColor = ThemeTextFieldBg,
    unfocusedContainerColor = ThemeTextFieldBg,
    cursorColor = ThemeAccentBlue
)

private fun iconAndColorFor(type: NoteFileType): Pair<ImageVector, Color> = when (type) {
    NoteFileType.FOLDER -> Icons.Default.Folder to ThemeAccentAmber
    NoteFileType.MARKDOWN -> Icons.Default.Description to ThemeAccentBlue
    NoteFileType.TEXT -> Icons.Default.Description to ThemeAccentGreen
    NoteFileType.PDF -> Icons.Default.PictureAsPdf to ThemeAccentRed
    NoteFileType.DOCX -> Icons.Default.Description to Color(0xFF60A5FA)
    NoteFileType.IMAGE -> Icons.Default.Image to Color(0xFFC084FC)
    NoteFileType.DOCUMENT -> Icons.AutoMirrored.Filled.InsertDriveFile to ThemeAccentAmber
    NoteFileType.OTHER -> Icons.AutoMirrored.Filled.InsertDriveFile to ThemeTextSecondary
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
        else -> String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
}

private fun openFileExternally(context: Context, localFilePath: String, filename: String) {
    val file = File(localFilePath)
    if (!file.exists()) {
        Toast.makeText(context, "File not cached yet", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val ext = filename.substringAfterLast('.', "").lowercase(Locale.US)
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Open $filename"))
    } catch (e: Exception) {
        Toast.makeText(context, "No installed app can open $filename", Toast.LENGTH_SHORT).show()
    }
}
