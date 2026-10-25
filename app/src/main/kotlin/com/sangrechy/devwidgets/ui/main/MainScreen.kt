package com.sangrechy.devwidgets.ui.main

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.sangrechy.devwidgets.data.DefaultDataRepository
import com.sangrechy.devwidgets.data.model.WidgetDashboardData
import com.sangrechy.devwidgets.widget.DevActivityWidgetProvider
import com.sangrechy.devwidgets.widget.WidgetBitmapRenderer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: MainScreenViewModel = viewModel {
        MainScreenViewModel(DefaultDataRepository(context.applicationContext))
    }

    val leetCodeInput by viewModel.leetCodeInput.collectAsStateWithLifecycle()
    val gitHubInput by viewModel.gitHubInput.collectAsStateWithLifecycle()
    val tokenInput by viewModel.gitHubTokenInput.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    val dashboardData by viewModel.dashboardData.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Dev Activity Widgets",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "LeetCode & GitHub Home Screen Dashboard",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Status Banner if any
            syncMessage?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // SECTION 1: User Profiles Configuration Prompt
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Configure Developer Profiles",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Enter your handles below. The widget will automatically load your solved count, activity heatmaps, and latest submissions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // LeetCode Username Input
                    OutlinedTextField(
                        value = leetCodeInput,
                        onValueChange = viewModel::onLeetCodeInputChanged,
                        label = { Text("LeetCode Username") },
                        placeholder = { Text("e.g. sangrechy") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    // GitHub Username Input
                    OutlinedTextField(
                        value = gitHubInput,
                        onValueChange = viewModel::onGitHubInputChanged,
                        label = { Text("GitHub Username") },
                        placeholder = { Text("e.g. sangrechy") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    // Optional GitHub Token
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = viewModel::onGitHubTokenChanged,
                        label = { Text("GitHub Personal Access Token (Optional)") },
                        placeholder = { Text("ghp_... for private stats & high rate limits") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.saveAndSync()
                                DevActivityWidgetProvider.enqueueImmediateSync(context)
                            },
                            enabled = !isSyncing,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Syncing...")
                            } else {
                                Text("Save & Sync Widget")
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.useSampleData()
                                DevActivityWidgetProvider.updateAllWidgets(
                                    context,
                                    dashboardData
                                )
                            },
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Text("Use Sample")
                        }
                    }
                }
            }

            // SECTION 2: Live Widget Preview (Matching ui_idea.png)
            Text(
                text = "Home Screen Widget Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            WidgetVisualPreview(data = dashboardData)

            // SECTION 3: Pin Widget Shortcut
            Button(
                onClick = {
                    pinWidgetToHomeScreen(context)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Pin Widget to Home Screen")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun WidgetVisualPreview(data: WidgetDashboardData) {
    val lcCircle = remember(data.leetCode) {
        WidgetBitmapRenderer.renderLeetCodeCircle(data.leetCode, sizePx = 320).asImageBitmap()
    }
    val lcHeatmap = remember(data.leetCode.submissionCalendar) {
        WidgetBitmapRenderer.renderHeatmap(
            data.leetCode.submissionCalendar,
            isLeetCode = true,
            widthPx = 600,
            heightPx = 220,
            weeksCount = 18
        ).asImageBitmap()
    }
    val ghTypeMap = remember(data.gitHub) {
        WidgetBitmapRenderer.renderGitHubTypeMap(data.gitHub, sizePx = 320).asImageBitmap()
    }
    val ghHeatmap = remember(data.gitHub.contributionDays) {
        WidgetBitmapRenderer.renderHeatmap(
            data.gitHub.contributionDays,
            isLeetCode = false,
            widthPx = 600,
            heightPx = 220,
            weeksCount = 18
        ).asImageBitmap()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(androidx.compose.ui.graphics.Color(0xD9101824))
            .border(
                1.5.dp,
                androidx.compose.ui.graphics.Color(0x55FFFFFF),
                RoundedCornerShape(26.dp)
            )
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DEV DASHBOARD",
                    color = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "Live Glass Preview",
                    color = androidx.compose.ui.graphics.Color(0xFFCBD5E1),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Top Card: LeetCode Glass Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(androidx.compose.ui.graphics.Color(0x26FFFFFF))
                    .border(
                        1.dp,
                        androidx.compose.ui.graphics.Color(0x38FFFFFF),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LEETCODE STATS & PROGRESS",
                            color = androidx.compose.ui.graphics.Color(0xFFFFA116),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "@${data.leetCode.username.ifBlank { "dev" }}",
                            color = androidx.compose.ui.graphics.Color(0xFF94A3B8),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Top Visual Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(94.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = lcCircle,
                            contentDescription = "LeetCode Count",
                            modifier = Modifier
                                .size(94.dp)
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(androidx.compose.ui.graphics.Color(0x26FFFFFF))
                        )
                        Image(
                            bitmap = lcHeatmap,
                            contentDescription = "LeetCode Heatmap",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }

                    // Bottom Submissions Full-Width Rows
                    val subs = data.leetCode.recentSubmissions.take(3)
                    subs.forEach { sub ->
                        val isAc = sub.statusDisplay.contains("Accept", ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(androidx.compose.ui.graphics.Color(0x26FFFFFF))
                                .border(
                                    0.8.dp,
                                    androidx.compose.ui.graphics.Color(0x30FFFFFF),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(
                                                if (isAc) androidx.compose.ui.graphics.Color(0x3310B981)
                                                else androidx.compose.ui.graphics.Color(0x33EF4444)
                                            )
                                            .border(
                                                1.dp,
                                                if (isAc) androidx.compose.ui.graphics.Color(0x8034D399)
                                                else androidx.compose.ui.graphics.Color(0x80F87171),
                                                RoundedCornerShape(999.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isAc) "AC" else "WA",
                                            color = if (isAc) androidx.compose.ui.graphics.Color(0xFF34D399)
                                            else androidx.compose.ui.graphics.Color(0xFFF87171),
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = sub.title,
                                        color = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = sub.lang,
                                    color = androidx.compose.ui.graphics.Color(0xFF94A3B8),
                                    fontSize = 8.5.sp
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Card: GitHub Glass Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(androidx.compose.ui.graphics.Color(0x26FFFFFF))
                    .border(
                        1.dp,
                        androidx.compose.ui.graphics.Color(0x38FFFFFF),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GITHUB ACTIVITY & CONTRIBUTIONS",
                            color = androidx.compose.ui.graphics.Color(0xFF3FB950),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "@${data.gitHub.username.ifBlank { "dev" }}",
                            color = androidx.compose.ui.graphics.Color(0xFF94A3B8),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Top Visual Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(94.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = ghTypeMap,
                            contentDescription = "GitHub Type Map",
                            modifier = Modifier
                                .size(94.dp)
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(androidx.compose.ui.graphics.Color(0x26FFFFFF))
                        )
                        Image(
                            bitmap = ghHeatmap,
                            contentDescription = "GitHub Heatmap",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }

                    // Bottom Events Full-Width Rows
                    val events = data.gitHub.recentEvents.take(3)
                    events.forEach { ev ->
                        val tag = when (ev.type) {
                            "PushEvent" -> "PUSH"
                            "PullRequestEvent" -> "PR"
                            "IssuesEvent" -> "ISSUE"
                            else -> "ACT"
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(androidx.compose.ui.graphics.Color(0x26FFFFFF))
                                .border(
                                    0.8.dp,
                                    androidx.compose.ui.graphics.Color(0x30FFFFFF),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(androidx.compose.ui.graphics.Color(0x336366F1))
                                            .border(
                                                1.dp,
                                                androidx.compose.ui.graphics.Color(0x80818CF8),
                                                RoundedCornerShape(999.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = tag,
                                            color = androidx.compose.ui.graphics.Color(0xFFA5B4FC),
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = ev.repoName,
                                        color = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = ev.actionSummary,
                                    color = androidx.compose.ui.graphics.Color(0xFF94A3B8),
                                    fontSize = 8.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun pinWidgetToHomeScreen(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val provider = ComponentName(context, DevActivityWidgetProvider::class.java)
        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            appWidgetManager.requestPinAppWidget(provider, null, null)
            Toast.makeText(context, "Pinning widget to home screen...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                context,
                "Add widget manually from your home screen widget menu",
                Toast.LENGTH_LONG
            ).show()
        }
    } else {
        Toast.makeText(
            context,
            "Long-press your home screen and select DevWidgets",
            Toast.LENGTH_LONG
        ).show()
    }
}
