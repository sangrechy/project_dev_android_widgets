package com.sangrechy.devwidgets.ui.problems

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sangrechy.devwidgets.data.model.CustomNote
import com.sangrechy.devwidgets.data.model.OfflineProblem
import com.sangrechy.devwidgets.data.model.ProblemDifficultyFilter
import com.sangrechy.devwidgets.data.model.ProblemNote
import com.sangrechy.devwidgets.data.model.ProblemsPlatform
import com.sangrechy.devwidgets.data.model.ProblemsSubTab
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProblemsScreenContent(
    platform: ProblemsPlatform,
    subTab: ProblemsSubTab,
    searchQuery: String,
    difficultyFilter: ProblemDifficultyFilter,
    problems: List<OfflineProblem>,
    isLoadingProblems: Boolean,
    selectedProblemDetail: OfflineProblem?,
    activeProblemNoteIndex: Int,
    generalNotes: List<CustomNote>,
    selectedGeneralNote: CustomNote?,
    backupRepoInput: String,
    isSyncingBackupRepo: Boolean,
    onPlatformSelected: (ProblemsPlatform) -> Unit,
    onSubTabSelected: (ProblemsSubTab) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onDifficultyFilterSelected: (ProblemDifficultyFilter) -> Unit,
    onSelectProblem: (OfflineProblem?) -> Unit,
    onActiveProblemNoteIndexChanged: (Int) -> Unit,
    onDownloadProblemOffline: (folderName: String, titleSlug: String) -> Unit,
    onDeleteProblemOffline: (folderName: String) -> Unit,
    onSaveProblemNote: (folderName: String, filename: String, content: String) -> Unit,
    onDeleteProblemNote: (folderName: String, filename: String) -> Unit,
    onBackupRepoChanged: (String) -> Unit,
    onSaveBackupRepo: (String) -> Unit,
    onSyncFromGitHubBackup: () -> Unit,
    onSelectGeneralNote: (CustomNote?) -> Unit,
    onSaveGeneralNote: (CustomNote) -> Unit,
    onDeleteGeneralNote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showNewGeneralNoteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Platform Switcher (LeetCode vs Codeforces)
        PlatformToggleCard(
            selectedPlatform = platform,
            onPlatformSelected = onPlatformSelected
        )

        // Sub-tab Navigation (Problems vs Notes)
        SubTabCard(
            selectedSubTab = subTab,
            onSubTabSelected = onSubTabSelected
        )

        when (subTab) {
            ProblemsSubTab.PROBLEMS -> {
                // LeetCode Problem Bank Section
                LeetCodeProblemsSection(
                    searchQuery = searchQuery,
                    difficultyFilter = difficultyFilter,
                    problems = problems,
                    isLoadingProblems = isLoadingProblems,
                    backupRepoInput = backupRepoInput,
                    isSyncingBackupRepo = isSyncingBackupRepo,
                    onSearchQueryChanged = onSearchQueryChanged,
                    onDifficultyFilterSelected = onDifficultyFilterSelected,
                    onBackupRepoChanged = onBackupRepoChanged,
                    onSaveBackupRepo = onSaveBackupRepo,
                    onSyncFromGitHubBackup = onSyncFromGitHubBackup,
                    onSelectProblem = onSelectProblem,
                    onDownloadProblemOffline = onDownloadProblemOffline,
                    onDeleteProblemOffline = onDeleteProblemOffline
                )
            }

            ProblemsSubTab.NOTES -> {
                // Standalone Developer Notes Section
                GeneralNotesSection(
                    notes = generalNotes,
                    onSelectNote = onSelectGeneralNote,
                    onCreateNewNote = { showNewGeneralNoteDialog = true }
                )
            }
        }
    }

    // Problem Detail Dialog (when a problem is selected)
    selectedProblemDetail?.let { problem ->
        LeetCodeProblemDetailDialog(
            problem = problem,
            activeNoteIndex = activeProblemNoteIndex,
            onDismiss = { onSelectProblem(null) },
            onActiveNoteIndexChanged = onActiveProblemNoteIndexChanged,
            onDownloadOffline = { onDownloadProblemOffline(problem.folderName, problem.titleSlug) },
            onDeleteOffline = { onDeleteProblemOffline(problem.folderName) },
            onSaveNote = { filename, content -> onSaveProblemNote(problem.folderName, filename, content) },
            onDeleteNote = { filename -> onDeleteProblemNote(problem.folderName, filename) }
        )
    }

    // General Note Editor Dialog (when clicked or creating)
    if (selectedGeneralNote != null || showNewGeneralNoteDialog) {
        val noteToEdit = selectedGeneralNote ?: CustomNote(
            id = "",
            title = "New Note",
            content = "# Notes\n\n- Key algorithm steps\n- Time & Space complexity\n",
            category = "General",
            lastModifiedMillis = System.currentTimeMillis()
        )

        GeneralNoteEditorDialog(
            note = noteToEdit,
            isNew = selectedGeneralNote == null,
            onDismiss = {
                onSelectGeneralNote(null)
                showNewGeneralNoteDialog = false
            },
            onSave = { updated ->
                onSaveGeneralNote(updated)
                onSelectGeneralNote(null)
                showNewGeneralNoteDialog = false
            },
            onDelete = { id ->
                onDeleteGeneralNote(id)
                onSelectGeneralNote(null)
                showNewGeneralNoteDialog = false
            }
        )
    }
}

// -----------------------------------------------------------------------------
// Platform Toggle Card
// -----------------------------------------------------------------------------

@Composable
private fun PlatformToggleCard(
    selectedPlatform: ProblemsPlatform,
    onPlatformSelected: (ProblemsPlatform) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x28182438))
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color(0x38FFFFFF), Color(0x12FFFFFF))),
                RoundedCornerShape(20.dp)
            )
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // LeetCode Platform Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .then(
                        if (selectedPlatform == ProblemsPlatform.LEETCODE) {
                            Modifier.background(Brush.horizontalGradient(listOf(Color(0xFFFFA116), Color(0xFFFF7A00))))
                        } else {
                            Modifier.background(Color.Transparent)
                        }
                    )
                    .clickable { onPlatformSelected(ProblemsPlatform.LEETCODE) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "LeetCode",
                        tint = if (selectedPlatform == ProblemsPlatform.LEETCODE) Color.Black else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "LeetCode",
                        color = if (selectedPlatform == ProblemsPlatform.LEETCODE) Color.Black else Color(0xFFCBD5E1),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Codeforces (Coming Soon)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x14FFFFFF))
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Codeforces",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x3364748B))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Soon",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Sub-tab Card (Problems vs Notes)
// -----------------------------------------------------------------------------

@Composable
private fun SubTabCard(
    selectedSubTab: ProblemsSubTab,
    onSubTabSelected: (ProblemsSubTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x1F1E293B))
            .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ProblemsSubTab.entries.forEach { tab ->
            val isSelected = tab == selectedSubTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) Color(0x383B82F6) else Color.Transparent
                    )
                    .clickable { onSubTabSelected(tab) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (tab == ProblemsSubTab.PROBLEMS) Icons.Default.Description else Icons.Default.NoteAlt,
                        contentDescription = tab.name,
                        tint = if (isSelected) Color(0xFF93C5FD) else Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (tab == ProblemsSubTab.PROBLEMS) "Problems Bank" else "Notes",
                        color = if (isSelected) Color(0xFF93C5FD) else Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// LeetCode Problems Section
// -----------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LeetCodeProblemsSection(
    searchQuery: String,
    difficultyFilter: ProblemDifficultyFilter,
    problems: List<OfflineProblem>,
    isLoadingProblems: Boolean,
    backupRepoInput: String,
    isSyncingBackupRepo: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onDifficultyFilterSelected: (ProblemDifficultyFilter) -> Unit,
    onBackupRepoChanged: (String) -> Unit,
    onSaveBackupRepo: (String) -> Unit,
    onSyncFromGitHubBackup: () -> Unit,
    onSelectProblem: (OfflineProblem) -> Unit,
    onDownloadProblemOffline: (folderName: String, titleSlug: String) -> Unit,
    onDeleteProblemOffline: (folderName: String) -> Unit
) {
    var showBackupSettings by remember { mutableStateOf(false) }

    // Backup Repository Sync Card
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x28182438))
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color(0x38FFFFFF), Color(0x12FFFFFF))),
                RoundedCornerShape(18.dp)
            )
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0x263FB950)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync",
                            tint = Color(0xFF3FB950),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "GitHub Backup Sync",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Repo: ${backupRepoInput.ifBlank { "project_git_the_leet" }}",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = { showBackupSettings = !showBackupSettings },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (showBackupSettings) Icons.Default.ExpandLess else Icons.Default.Edit,
                            contentDescription = "Edit Repo",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Button(
                        onClick = onSyncFromGitHubBackup,
                        enabled = !isSyncingBackupRepo,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        if (isSyncingBackupRepo) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text("Sync", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (showBackupSettings) {
                HorizontalDivider(color = Color(0x1FFFFFFF))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = backupRepoInput,
                        onValueChange = onBackupRepoChanged,
                        label = { Text("Backup Repo Name", fontSize = 11.sp) },
                        placeholder = { Text("project_git_the_leet", fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0x40FFFFFF)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            onSaveBackupRepo(backupRepoInput)
                            showBackupSettings = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Save", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Search Bar
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChanged,
        placeholder = { Text("Search by problem name, ID, or slug...", fontSize = 13.sp, color = Color(0xFF64748B)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp)
            )
        },
        trailingIcon = {
            if (searchQuery.isNotBlank()) {
                IconButton(onClick = { onSearchQueryChanged("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = Color(0x28182438),
            unfocusedContainerColor = Color(0x28182438),
            focusedBorderColor = Color(0xFF3B82F6),
            unfocusedBorderColor = Color(0x2EFFFFFF)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    )

    // Difficulty Filter Pills
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProblemDifficultyFilter.entries.forEach { filter ->
            val isSelected = filter == difficultyFilter
            val chipColor = when (filter) {
                ProblemDifficultyFilter.ALL -> Color(0xFF3B82F6)
                ProblemDifficultyFilter.EASY -> Color(0xFF00B8A3)
                ProblemDifficultyFilter.MEDIUM -> Color(0xFFFFC01E)
                ProblemDifficultyFilter.HARD -> Color(0xFFFF375F)
                ProblemDifficultyFilter.DOWNLOADED -> Color(0xFFA855F7)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) chipColor.copy(alpha = 0.25f) else Color(0x14FFFFFF)
                    )
                    .border(
                        1.dp,
                        if (isSelected) chipColor else Color(0x24FFFFFF),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onDifficultyFilterSelected(filter) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = filter.label,
                    color = if (isSelected) chipColor else Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }

    // Filter problems according to search and difficulty
    val filteredProblems = remember(problems, searchQuery, difficultyFilter) {
        problems.filter { problem ->
            val matchesSearch = searchQuery.isBlank() ||
                problem.title.contains(searchQuery, ignoreCase = true) ||
                problem.frontendId.contains(searchQuery) ||
                problem.titleSlug.contains(searchQuery, ignoreCase = true) ||
                problem.topicTags.any { it.contains(searchQuery, ignoreCase = true) }

            val matchesDifficulty = when (difficultyFilter) {
                ProblemDifficultyFilter.ALL -> true
                ProblemDifficultyFilter.EASY -> problem.difficulty.equals("Easy", ignoreCase = true)
                ProblemDifficultyFilter.MEDIUM -> problem.difficulty.equals("Medium", ignoreCase = true)
                ProblemDifficultyFilter.HARD -> problem.difficulty.equals("Hard", ignoreCase = true)
                ProblemDifficultyFilter.DOWNLOADED -> problem.isDownloadedOffline
            }

            matchesSearch && matchesDifficulty
        }
    }

    // Problem Counter
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${filteredProblems.size} Problems (${problems.count { it.isDownloadedOffline }} Offline)",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }

    if (isLoadingProblems) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF3B82F6))
        }
    } else if (filteredProblems.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x1F1E293B))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = "No problems match your filter",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Sync from your GitHub backup repo or clear search filters.",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            filteredProblems.forEach { problem ->
                ProblemItemCard(
                    problem = problem,
                    onClick = { onSelectProblem(problem) },
                    onDownloadOffline = { onDownloadProblemOffline(problem.folderName, problem.titleSlug) },
                    onDeleteOffline = { onDeleteProblemOffline(problem.folderName) }
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Problem Item Card
// -----------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProblemItemCard(
    problem: OfflineProblem,
    onClick: () -> Unit,
    onDownloadOffline: () -> Unit,
    onDeleteOffline: () -> Unit
) {
    val diffColor = when (problem.difficulty.lowercase()) {
        "easy" -> Color(0xFF00B8A3)
        "hard" -> Color(0xFFFF375F)
        else -> Color(0xFFFFC01E)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x28182438))
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color(0x38FFFFFF), Color(0x12FFFFFF))),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header Row: Problem ID + Title + Difficulty Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "#${problem.frontendId}",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = problem.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Difficulty Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(diffColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = problem.difficulty,
                        color = diffColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Tags & Meta Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Topic tags excerpt
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    problem.topicTags.take(2).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x1FFFFFFF))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tag,
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.sp
                            )
                        }
                    }
                    if (problem.topicTags.size > 2) {
                        Text(
                            text = "+${problem.topicTags.size - 2}",
                            color = Color(0xFF64748B),
                            fontSize = 10.sp
                        )
                    }
                }

                // Offline Status & Language
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (problem.solutionLanguage.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x333B82F6))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = problem.solutionLanguage,
                                color = Color(0xFF93C5FD),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (problem.isDownloadedOffline) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Offline Available",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Offline",
                                color = Color(0xFF10B981),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onDownloadOffline,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download Offline",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Notes counter badge if problem has notes
            if (problem.notes.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NoteAlt,
                        contentDescription = "Notes",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "${problem.notes.size} note${if (problem.notes.size > 1) "s" else ""}: ${problem.notes.joinToString { it.title }}",
                        color = Color(0xFFFDE68A),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Authentic LeetCode Problem Detail Dialog
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LeetCodeProblemDetailDialog(
    problem: OfflineProblem,
    activeNoteIndex: Int,
    onDismiss: () -> Unit,
    onActiveNoteIndexChanged: (Int) -> Unit,
    onDownloadOffline: () -> Unit,
    onDeleteOffline: () -> Unit,
    onSaveNote: (filename: String, content: String) -> Unit,
    onDeleteNote: (filename: String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Description, 1 = Code, 2 = Notes
    var showNewNoteModal by remember { mutableStateOf(false) }
    var newNoteFilename by remember { mutableStateOf("") }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val diffColor = when (problem.difficulty.lowercase()) {
        "easy" -> Color(0xFF00B8A3)
        "hard" -> Color(0xFFFF375F)
        else -> Color(0xFFFFC01E)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 28.dp, bottom = 12.dp, start = 12.dp, end = 12.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E1E1E), // Authentic dark LeetCode background
            border = BorderStroke(1.dp, Color(0xFF333333))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF262626))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "#${problem.frontendId}. ${problem.title}",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(diffColor.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = problem.difficulty,
                                color = diffColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Offline download or remove button
                        if (problem.isDownloadedOffline) {
                            IconButton(
                                onClick = { showDeleteConfirmation = true },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove Offline Cache",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = onDownloadOffline,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download Offline",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Authentic 3-Tab Bar (Description | Solution Code | Notes)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF282828))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tabs = listOf("Description", "Code", "Notes (${problem.notes.size})")
                    tabs.forEachIndexed { index, tabTitle ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF3E3E3E) else Color.Transparent)
                                .clickable { selectedTab = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabTitle,
                                color = if (isSelected) Color.White else Color(0xFF9CA3AF),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF333333))

                // Tab Content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (selectedTab) {
                        0 -> LeetCodeDescriptionTab(problem = problem)
                        1 -> LeetCodeCodeTab(problem = problem)
                        2 -> LeetCodeNotesTab(
                            problem = problem,
                            activeNoteIndex = activeNoteIndex,
                            onActiveNoteIndexChanged = onActiveNoteIndexChanged,
                            onSaveNote = onSaveNote,
                            onDeleteNote = onDeleteNote,
                            onAddNewNote = { showNewNoteModal = true }
                        )
                    }
                }
            }
        }
    }

    // Modal to create a new note file
    if (showNewNoteModal) {
        AlertDialog(
            onDismissRequest = { showNewNoteModal = false },
            containerColor = Color(0xFF262626),
            title = { Text("New Note File", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter a file name for this problem's note (saved in notes/):",
                        color = Color(0xFFD1D5DB),
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = newNoteFilename,
                        onValueChange = { newNoteFilename = it },
                        placeholder = { Text("approach2.md, intuition.md...", color = Color(0xFF6B7280)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF4B5563)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newNoteFilename.trim().ifBlank { "notes_${System.currentTimeMillis()}.md" }
                        onSaveNote(name, "# ${name.substringBeforeLast('.')}\n\n- Write your thoughts here...")
                        newNoteFilename = ""
                        showNewNoteModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text("Create Note")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewNoteModal = false }) {
                    Text("Cancel", color = Color(0xFF9CA3AF))
                }
            }
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            containerColor = Color(0xFF262626),
            title = { Text("Remove Offline Problem?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to remove #${problem.frontendId} ${problem.title} from offline storage?",
                    color = Color(0xFFD1D5DB),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onDeleteOffline()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel", color = Color(0xFF9CA3AF))
                }
            }
        )
    }
}

// -----------------------------------------------------------------------------
// Tab 1: Authentic Description with Styled WebView
// -----------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LeetCodeDescriptionTab(problem: OfflineProblem) {
    val scrollState = rememberScrollState()
    var expandedHintIndex by remember { mutableIntStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Topics Pills
        if (problem.topicTags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                problem.topicTags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF2D2D2D))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tag,
                            color = Color(0xFFD1D5DB),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Problem Description HTML or Formatted Text
        val htmlContent = remember(problem.contentHtml) {
            val raw = problem.contentHtml.ifBlank {
                """
                <p><strong>${problem.title}</strong></p>
                <p>This problem is available offline. Download full problem statement using the download icon above if needed.</p>
                """.trimIndent()
            }

            """
            <!DOCTYPE html>
            <html>
            <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
            <style>
              body {
                background-color: #1E1E1E;
                color: #EFF1F6;
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                font-size: 14px;
                line-height: 1.6;
                padding: 0px;
                margin: 0;
              }
              code {
                background-color: #2D2D2D;
                color: #F87171;
                padding: 2px 5px;
                border-radius: 4px;
                font-family: monospace;
                font-size: 13px;
              }
              pre {
                background-color: #262626;
                border: 1px solid #3E3E3E;
                border-radius: 8px;
                padding: 12px;
                overflow-x: auto;
                font-family: monospace;
                font-size: 13px;
                color: #EFF1F6;
                line-height: 1.5;
              }
              strong {
                color: #FFFFFF;
                font-weight: 600;
              }
              ul, ol {
                padding-left: 20px;
              }
              li {
                margin-bottom: 6px;
              }
              p {
                margin-top: 8px;
                margin-bottom: 8px;
              }
            </style>
            </head>
            <body>
            $raw
            </body>
            </html>
            """.trimIndent()
        }

        // Render authentic LeetCode statement using styled WebView
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(0x00000000)
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = false
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = false
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL("https://leetcode.com", htmlContent, "text/html", "UTF-8", null)
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 280.dp, max = 1200.dp)
        )

        // Hints Accordion
        if (problem.hints.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Hints (${problem.hints.size})",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            problem.hints.forEachIndexed { index, hint ->
                val isExpanded = expandedHintIndex == index
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF282828))
                        .border(1.dp, Color(0xFF383838), RoundedCornerShape(8.dp))
                        .clickable { expandedHintIndex = if (isExpanded) -1 else index }
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = "Hint",
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Hint ${index + 1}",
                                    color = Color(0xFFD1D5DB),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (isExpanded) {
                            Text(
                                text = hint,
                                color = Color(0xFF9CA3AF),
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Tab 2: Backed-Up Code Tab
// -----------------------------------------------------------------------------

@Composable
private fun LeetCodeCodeTab(problem: OfflineProblem) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Stats Banner & Copy Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF2563EB))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = problem.solutionLanguage.ifBlank { "Code" }.uppercase(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (problem.runtime.isNotBlank()) {
                    Text(
                        text = "${problem.runtime} ${if (problem.runtimePercentile.isNotBlank()) "(${problem.runtimePercentile})" else ""}",
                        color = Color(0xFF10B981),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Copy to Clipboard
            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("LeetCode Solution", problem.solutionCode))
                    Toast.makeText(context, "Solution code copied!", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF4B5563)),
                modifier = Modifier.height(32.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = Color(0xFF93C5FD),
                        modifier = Modifier.size(14.dp)
                    )
                    Text("Copy Code", color = Color(0xFF93C5FD), fontSize = 11.sp)
                }
            }
        }

        // Monospace Dark Code View with Line Numbers
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF141414))
                .border(1.dp, Color(0xFF333333), RoundedCornerShape(10.dp))
                .verticalScroll(scrollState)
                .padding(12.dp)
        ) {
            val lines = problem.solutionCode.ifBlank { "# No backed up solution found for this problem." }.lines()

            Row(modifier = Modifier.fillMaxWidth()) {
                // Line numbers column
                Column(modifier = Modifier.padding(end = 12.dp)) {
                    lines.indices.forEach { index ->
                        Text(
                            text = "${index + 1}",
                            color = Color(0xFF4B5563),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Code lines column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState())
                ) {
                    lines.forEach { line ->
                        Text(
                            text = line,
                            color = Color(0xFFE5E7EB),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Tab 3: Dedicated Problem Notes (Multi-File Notes Support)
// -----------------------------------------------------------------------------

@Composable
private fun LeetCodeNotesTab(
    problem: OfflineProblem,
    activeNoteIndex: Int,
    onActiveNoteIndexChanged: (Int) -> Unit,
    onSaveNote: (filename: String, content: String) -> Unit,
    onDeleteNote: (filename: String) -> Unit,
    onAddNewNote: () -> Unit
) {
    val notes = problem.notes
    val currentNote = notes.getOrNull(activeNoteIndex) ?: notes.firstOrNull()
    var noteContent by remember(currentNote?.filename, currentNote?.content) {
        mutableStateOf(currentNote?.content ?: "")
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Multi-File Notes Selector Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            notes.forEachIndexed { index, note ->
                val isSelected = (index == activeNoteIndex)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF3B82F6) else Color(0xFF2D2D2D))
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFF60A5FA) else Color(0xFF404040),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onActiveNoteIndexChanged(index) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NoteAlt,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else Color(0xFF9CA3AF),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = note.filename,
                            color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // "+ Add Note" Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x1FFFFFFF))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                    .clickable(onClick = onAddNewNote)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Note",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(14.dp)
                    )
                    Text("+ Note", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (currentNote != null) {
            // Note Editor Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Editing: ${currentNote.filename}",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Delete Note button
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Note",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Save Note button
                    Button(
                        onClick = { onSaveNote(currentNote.filename, noteContent) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Save",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text("Save", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Note Content Editor
            OutlinedTextField(
                value = noteContent,
                onValueChange = { noteContent = it },
                placeholder = { Text("Write intuition, edge cases, complexity analysis here...", color = Color(0xFF6B7280)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFFE5E7EB),
                    unfocusedTextColor = Color(0xFFE5E7EB),
                    focusedContainerColor = Color(0xFF141414),
                    unfocusedContainerColor = Color(0xFF141414),
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFF333333)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            // No notes exist yet
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF141414))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NoteAlt,
                        contentDescription = null,
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "No notes created for this problem yet",
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp
                    )
                    Button(
                        onClick = onAddNewNote,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Create First Note")
                    }
                }
            }
        }
    }

    if (showDeleteConfirm && currentNote != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Color(0xFF262626),
            title = { Text("Delete Note?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete '${currentNote.filename}'?",
                    color = Color(0xFFD1D5DB),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteNote(currentNote.filename)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = Color(0xFF9CA3AF))
                }
            }
        )
    }
}

// -----------------------------------------------------------------------------
// General Notes Section (Standalone Developer Notes)
// -----------------------------------------------------------------------------

@Composable
private fun GeneralNotesSection(
    notes: List<CustomNote>,
    onSelectNote: (CustomNote) -> Unit,
    onCreateNewNote: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Header with "+ New Note"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${notes.size} Standalone Notes",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Button(
                onClick = onCreateNewNote,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Note",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text("New Note", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x1F1E293B))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NoteAlt,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "No general notes yet",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Store custom cheatsheets, algorithm patterns, or interview templates.",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            notes.forEach { note ->
                GeneralNoteCard(note = note, onClick = { onSelectNote(note) })
            }
        }
    }
}

@Composable
private fun GeneralNoteCard(
    note: CustomNote,
    onClick: () -> Unit
) {
    val dateStr = remember(note.lastModifiedMillis) {
        val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
        sdf.format(Date(note.lastModifiedMillis))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x28182438))
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color(0x38FFFFFF), Color(0x12FFFFFF))),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dateStr,
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }

            // Preview excerpt
            val preview = note.content.lines().filter { it.isNotBlank() && !it.startsWith("#") }.take(2).joinToString(" ")
            Text(
                text = preview.ifBlank { "Click to view note content..." },
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
        }
    }
}

// -----------------------------------------------------------------------------
// General Note Editor Dialog
// -----------------------------------------------------------------------------

@Composable
fun GeneralNoteEditorDialog(
    note: CustomNote,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (CustomNote) -> Unit,
    onDelete: (String) -> Unit
) {
    var title by remember { mutableStateOf(note.title) }
    var content by remember { mutableStateOf(note.content) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF131D31),
            border = BorderStroke(1.dp, Color(0x3360A5FA))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isNew) "New Note" else "Edit Note",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!isNew) {
                            IconButton(onClick = { showDeleteConfirm = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color(0xFFEF4444)
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0x33FFFFFF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Content Editor
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Markdown / Text Content", fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0x33FFFFFF)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                // Bottom Save Button
                Button(
                    onClick = {
                        val updated = note.copy(
                            title = title.trim().ifBlank { "Untitled Note" },
                            content = content,
                            lastModifiedMillis = System.currentTimeMillis()
                        )
                        onSave(updated)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Note", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Delete Note?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete '$title'?",
                    color = Color(0xFFCBD5E1),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(note.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}
