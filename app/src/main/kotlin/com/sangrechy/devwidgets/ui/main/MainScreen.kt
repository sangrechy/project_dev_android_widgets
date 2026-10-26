package com.sangrechy.devwidgets.ui.main

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.sangrechy.devwidgets.BuildConfig
import com.sangrechy.devwidgets.R
import com.sangrechy.devwidgets.data.DefaultDataRepository
import com.sangrechy.devwidgets.data.model.AccountStatus
import com.sangrechy.devwidgets.data.model.GitHubEvent
import com.sangrechy.devwidgets.data.model.GitHubLanguageStat
import com.sangrechy.devwidgets.data.model.GitHubStats
import com.sangrechy.devwidgets.data.model.LeetCodeStats
import com.sangrechy.devwidgets.data.model.LeetCodeSubmission
import com.sangrechy.devwidgets.data.model.ProblemTopicTag
import com.sangrechy.devwidgets.data.model.WidgetDashboardData
import com.sangrechy.devwidgets.widget.DevActivityWidgetProvider
import com.sangrechy.devwidgets.widget.WidgetBitmapRenderer
import kotlinx.coroutines.launch
import java.time.Instant

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

    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val currentHomeTab by viewModel.currentHomeTab.collectAsStateWithLifecycle()
    val feedLimit by viewModel.feedLimit.collectAsStateWithLifecycle()
    val selectedInterval by viewModel.selectedInterval.collectAsStateWithLifecycle()

    val leetCodeInput by viewModel.leetCodeInput.collectAsStateWithLifecycle()
    val gitHubInput by viewModel.gitHubInput.collectAsStateWithLifecycle()
    val tokenInput by viewModel.gitHubTokenInput.collectAsStateWithLifecycle()

    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val isVerifyingLc by viewModel.isVerifyingLeetCode.collectAsStateWithLifecycle()
    val isVerifyingGh by viewModel.isVerifyingGitHub.collectAsStateWithLifecycle()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsStateWithLifecycle()

    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    val dashboardData by viewModel.dashboardData.collectAsStateWithLifecycle()

    val showUpdateDialog by viewModel.showUpdateDialog.collectAsStateWithLifecycle()
    val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()
    val showLogoutDialog by viewModel.showLogoutDialog.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Check updates silently on initial launch
    LaunchedEffect(Unit) {
        viewModel.checkForUpdates(manual = false)
    }

    val bgGradient = remember {
        Brush.verticalGradient(
            listOf(
                Color(0xFF090D16),
                Color(0xFF0F172A),
                Color(0xFF090D16)
            )
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF0D1424),
                drawerContentColor = Color.White,
                modifier = Modifier.width(300.dp)
            ) {
                // Drawer Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_skill_jane),
                        contentDescription = "Chibi Jane Logo",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, Color(0x80FFFFFF), CircleShape)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Dev Life",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "v${BuildConfig.VERSION_NAME} • Multi-Platform Hub",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Accounts Mini Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MiniAccountChip(
                            name = "LeetCode",
                            status = dashboardData.leetCode.status,
                            color = Color(0xFFFFA116)
                        )
                        MiniAccountChip(
                            name = "GitHub",
                            status = dashboardData.gitHub.status,
                            color = Color(0xFF3FB950)
                        )
                    }
                }

                HorizontalDivider(color = Color(0x26FFFFFF))
                Spacer(modifier = Modifier.height(8.dp))

                // Navigation Items
                NavigationDrawerItem(
                    label = { Text("Home", fontWeight = FontWeight.SemiBold) },
                    selected = currentScreen == AppScreen.HOME,
                    onClick = {
                        viewModel.setScreen(AppScreen.HOME)
                        coroutineScope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color(0x333B82F6),
                        selectedTextColor = Color(0xFF93C5FD),
                        selectedIconColor = Color(0xFF60A5FA),
                        unselectedTextColor = Color(0xFFCBD5E1),
                        unselectedIconColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Widgets", fontWeight = FontWeight.SemiBold) },
                    selected = currentScreen == AppScreen.WIDGETS,
                    onClick = {
                        viewModel.setScreen(AppScreen.WIDGETS)
                        coroutineScope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Widgets, contentDescription = "Widgets") },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color(0x333B82F6),
                        selectedTextColor = Color(0xFF93C5FD),
                        selectedIconColor = Color(0xFF60A5FA),
                        unselectedTextColor = Color(0xFFCBD5E1),
                        unselectedIconColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                    selected = currentScreen == AppScreen.SETTINGS,
                    onClick = {
                        viewModel.setScreen(AppScreen.SETTINGS)
                        coroutineScope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color(0x333B82F6),
                        selectedTextColor = Color(0xFF93C5FD),
                        selectedIconColor = Color(0xFF60A5FA),
                        unselectedTextColor = Color(0xFFCBD5E1),
                        unselectedIconColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider(color = Color(0x26FFFFFF))

                NavigationDrawerItem(
                    label = { Text("Logout All", fontWeight = FontWeight.Bold, color = Color(0xFFF87171)) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.showLogoutConfirmation()
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Color(0xFFF87171)) },
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgGradient)
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Top App Bar with Left Menu Button (PINNED AT TOP)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xD9101824))
                            .border(1.dp, Color(0x38FFFFFF), RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Navigation Menu",
                                tint = Color.White
                            )
                        }

                        Image(
                            painter = painterResource(R.drawable.ic_skill_jane),
                            contentDescription = "Chibi Jane",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color(0x55FFFFFF), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dev Life",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = when (currentScreen) {
                                    AppScreen.HOME -> "User Insights & Activity"
                                    AppScreen.WIDGETS -> "Widget Gallery & Sync"
                                    AppScreen.SETTINGS -> "Accounts & Configuration"
                                },
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }

                        // Quick Sync Icon Button
                        IconButton(
                            onClick = {
                                viewModel.saveAndSync()
                                DevActivityWidgetProvider.enqueueImmediateSync(context)
                            },
                            enabled = !isSyncing
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF60A5FA)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Quick Sync",
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    // Status Message Banner (PINNED)
                    syncMessage?.let { msg ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x2E3B82F6))
                                .border(1.dp, Color(0x5560A5FA), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = msg,
                                    color = Color(0xFF93C5FD),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = viewModel::dismissMessage,
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = Color(0xFF93C5FD),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Scrollable Content Column
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Content Based on Selected Screen
                        when (currentScreen) {
                        AppScreen.HOME -> HomeScreenContent(
                            dashboardData = dashboardData,
                            currentTab = currentHomeTab,
                            feedLimit = feedLimit,
                            onTabSelected = viewModel::setHomeTab,
                            onFeedLimitSelected = viewModel::setFeedLimit
                        )

                        AppScreen.WIDGETS -> WidgetsScreenContent(
                            dashboardData = dashboardData,
                            selectedInterval = selectedInterval,
                            onIntervalSelected = viewModel::setRefreshInterval,
                            onPinWidget = { pinWidgetToHomeScreen(context) }
                        )

                        AppScreen.SETTINGS -> SettingsScreenContent(
                            dashboardData = dashboardData,
                            leetCodeInput = leetCodeInput,
                            gitHubInput = gitHubInput,
                            tokenInput = tokenInput,
                            isSyncing = isSyncing,
                            isVerifyingLc = isVerifyingLc,
                            isVerifyingGh = isVerifyingGh,
                            isCheckingUpdate = isCheckingUpdate,
                            onLeetCodeChanged = viewModel::onLeetCodeInputChanged,
                            onGitHubChanged = viewModel::onGitHubInputChanged,
                            onTokenChanged = viewModel::onGitHubTokenChanged,
                            onVerifyLeetCode = {
                                viewModel.verifyLeetCode()
                                DevActivityWidgetProvider.updateAllWidgets(context, dashboardData)
                            },
                            onVerifyGitHub = {
                                viewModel.verifyGitHub()
                                DevActivityWidgetProvider.updateAllWidgets(context, dashboardData)
                            },
                            onSyncAll = {
                                viewModel.saveAndSync()
                                DevActivityWidgetProvider.enqueueImmediateSync(context)
                            },
                            onUseSample = {
                                viewModel.useSampleData()
                                DevActivityWidgetProvider.updateAllWidgets(context, dashboardData)
                            },
                            onCheckForUpdate = { viewModel.checkForUpdates(manual = true) },
                            onLogoutAll = viewModel::showLogoutConfirmation
                        )
                    }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // App Update Dialog
    if (showUpdateDialog && updateInfo != null) {
        val info = updateInfo!!
        AlertDialog(
            onDismissRequest = viewModel::dismissUpdateDialog,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Update",
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "New Version ${info.latestVersion}",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (info.releaseName.isNotBlank()) info.releaseName else "Dev Life Update",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF93C5FD),
                        fontSize = 13.sp
                    )
                    if (info.releaseNotes.isNotBlank()) {
                        Text(
                            text = info.releaseNotes.take(300),
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "Download the latest APK release directly from our GitHub repository.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetUrl = info.downloadUrl.ifBlank { info.releaseHtmlUrl }
                        if (targetUrl.isNotBlank()) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }
                        viewModel.dismissUpdateDialog()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download APK")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = viewModel::ignoreCurrentUpdate) {
                        Text("Never ask again", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    TextButton(onClick = viewModel::dismissUpdateDialog) {
                        Text("Later", color = Color(0xFFCBD5E1))
                    }
                }
            },
            containerColor = Color(0xFF1E293B),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissLogoutConfirmation,
            title = {
                Text(
                    text = "Logout from All Accounts?",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "This will clear your saved LeetCode and GitHub usernames, private access token, and all cached widget telemetry.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmLogout,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626),
                        contentColor = Color.White
                    )
                ) {
                    Text("Logout All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissLogoutConfirmation) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// =========================================================================
// 1. HOME SCREEN (LeetCode & GitHub Deep Insights)
// =========================================================================

@Composable
private fun HomeScreenContent(
    dashboardData: WidgetDashboardData,
    currentTab: HomeTab,
    feedLimit: FeedLimit,
    onTabSelected: (HomeTab) -> Unit,
    onFeedLimitSelected: (FeedLimit) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Top Segmented Switcher [ LeetCode | GitHub ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xD9101824))
                .border(1.dp, Color(0x38FFFFFF), RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // LeetCode Tab Button
            val isLcSelected = currentTab == HomeTab.LEETCODE
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isLcSelected) Color(0x33FFA116) else Color.Transparent)
                    .border(
                        1.dp,
                        if (isLcSelected) Color(0xFFFFA116) else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onTabSelected(HomeTab.LEETCODE) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_leetcode_logo),
                        contentDescription = "LeetCode",
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "LeetCode",
                        color = if (isLcSelected) Color(0xFFFFA116) else Color(0xFF94A3B8),
                        fontWeight = if (isLcSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }

            // GitHub Tab Button
            val isGhSelected = currentTab == HomeTab.GITHUB
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isGhSelected) Color(0x333FB950) else Color.Transparent)
                    .border(
                        1.dp,
                        if (isGhSelected) Color(0xFF3FB950) else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onTabSelected(HomeTab.GITHUB) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_github_logo),
                        contentDescription = "GitHub",
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "GitHub",
                        color = if (isGhSelected) Color(0xFF3FB950) else Color(0xFF94A3B8),
                        fontWeight = if (isGhSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Selected Platform View
        when (currentTab) {
            HomeTab.LEETCODE -> LeetCodeDetailView(
                stats = dashboardData.leetCode,
                feedLimit = feedLimit,
                onFeedLimitSelected = onFeedLimitSelected
            )
            HomeTab.GITHUB -> GitHubDetailView(
                stats = dashboardData.gitHub,
                feedLimit = feedLimit,
                onFeedLimitSelected = onFeedLimitSelected
            )
        }
    }
}

@Composable
private fun LeetCodeDetailView(
    stats: LeetCodeStats,
    feedLimit: FeedLimit,
    onFeedLimitSelected: (FeedLimit) -> Unit
) {
    val lcCircleBitmap = remember(stats) {
        WidgetBitmapRenderer.renderLeetCodeCircle(stats, sizePx = 420).asImageBitmap()
    }
    val lcHeatmapBitmap = remember(stats.submissionCalendar) {
        WidgetBitmapRenderer.renderHeatmap(
            stats.submissionCalendar,
            isLeetCode = true,
            widthPx = 680,
            heightPx = 520,
            weeksCount = 52
        ).asImageBitmap()
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // User Profile & Status Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xD9101824))
                .border(1.dp, Color(0x38FFFFFF), RoundedCornerShape(22.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_leetcode_logo),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (stats.username.isNotBlank()) "@${stats.username}" else "@not_linked",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Total Solved: ${stats.totalSolved} Problems Across All Tiers",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }

                AccountStatusBadge(
                    status = stats.status,
                    isVerifying = false,
                    connectedText = "Connected",
                    failedText = "Offline"
                )
            }
        }

        // 1. The 3/4 Circular Gauge Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xD9101824))
                .border(1.dp, Color(0x38FFFFFF), RoundedCornerShape(22.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Solved Problems Gauge",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        bitmap = lcCircleBitmap,
                        contentDescription = "LeetCode Progress Gauge",
                        modifier = Modifier.size(150.dp)
                    )

                    val maxTarget = stats.totalSolved.coerceAtLeast(100)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DifficultyPill(label = "Easy", solved = stats.easySolved, max = maxTarget, color = Color(0xFF00B8A3))
                        DifficultyPill(label = "Medium", solved = stats.mediumSolved, max = maxTarget, color = Color(0xFFFFC01E))
                        DifficultyPill(label = "Hard", solved = stats.hardSolved, max = maxTarget, color = Color(0xFFFF375F))
                    }
                }
            }
        }

        // 2. Down the Uniform Heatmap (Constant Month Shape)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xD9101824))
                .border(1.dp, Color(0x38FFFFFF), RoundedCornerShape(22.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "12-Month Submission Heatmap",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Uniform Month Blocks",
                        color = Color(0xFFFFA116),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Image(
                    bitmap = lcHeatmapBitmap,
                    contentDescription = "Uniform LeetCode Heatmap",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )

                Text(
                    text = "Each month consists of 7 columns (days of week) and 6 rows (weeks).",
                    color = Color(0xFF64748B),
                    fontSize = 10.5.sp
                )
            }
        }

        // 3. Bubble Map / Problem Topic Tags Cloud
        LeetCodeBubbleMapCard(topicTags = stats.topicTags)

        // 4. Recent Submissions Feed
        RecentSubmissionsCard(
            submissions = stats.recentSubmissions,
            feedLimit = feedLimit,
            onFeedLimitSelected = onFeedLimitSelected
        )
    }
}

@Composable
private fun DifficultyPill(label: String, solved: Int, max: Int, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = "$solved solved", color = Color(0xFFCBD5E1), fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        val fraction = if (max > 0) (solved.toFloat() / max.toFloat()).coerceIn(0f, 1f) else 0f
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(Color(0x33FFFFFF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LeetCodeBubbleMapCard(topicTags: List<ProblemTopicTag>) {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Fundamental", "Intermediate", "Advanced")

    val filteredTags = remember(topicTags, selectedCategory) {
        if (selectedCategory == "All") topicTags
        else topicTags.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xD9101824))
            .border(1.dp, Color(0x38FFFFFF), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Skills & Problem Topics Cloud",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${filteredTags.size} Topics",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }

            // Category filter pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0x33FFA116) else Color(0x1AFFFFFF))
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFFFFA116) else Color(0x26FFFFFF),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) Color(0xFFFFA116) else Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Flow of topic bubbles
            if (filteredTags.isEmpty()) {
                Text(
                    text = "No topic data available yet. Link and sync your LeetCode account.",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredTags.forEach { tag ->
                        TopicBubbleChip(tag = tag)
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicBubbleChip(tag: ProblemTopicTag) {
    val chipBorderColor = when (tag.category.lowercase()) {
        "advanced" -> Color(0xFFF43F5E)
        "intermediate" -> Color(0xFFFFA116)
        "fundamental" -> Color(0xFF38BDF8)
        else -> Color(0x55FFFFFF)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x241E293B))
            .border(1.dp, chipBorderColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = tag.name,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(chipBorderColor.copy(alpha = 0.25f))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "${tag.solvedCount}",
                    color = chipBorderColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RecentSubmissionsCard(
    submissions: List<LeetCodeSubmission>,
    feedLimit: FeedLimit,
    onFeedLimitSelected: (FeedLimit) -> Unit
) {
    val displayed: List<LeetCodeSubmission> = remember(submissions, feedLimit) {
        submissions.take(feedLimit.count)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xD9101824))
            .border(1.dp, Color(0x38FFFFFF), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Submissions",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                // Limit switcher: Last 10 | 50 | 100
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x20FFFFFF))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    FeedLimit.values().forEach { limit ->
                        val isSelected = feedLimit == limit
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Color(0xFFFFA116) else Color.Transparent)
                                .clickable { onFeedLimitSelected(limit) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = limit.label.replace("Last ", ""),
                                color = if (isSelected) Color.Black else Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (displayed.isEmpty()) {
                Text(
                    text = "No recent submissions logged. Sync your profile to view history.",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    displayed.forEach { sub ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x1AFFFFFF))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sub.title,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${sub.lang} • ${formatTimestamp(sub.timestamp)}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.5.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x3322C55E))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = sub.statusDisplay.ifBlank { "Accepted" },
                                    color = Color(0xFF4ADE80),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// GitHub Detail Screen
// -------------------------------------------------------------------------

@Composable
private fun GitHubDetailView(
    stats: GitHubStats,
    feedLimit: FeedLimit,
    onFeedLimitSelected: (FeedLimit) -> Unit
) {
    val ghTypeMapBitmap = remember(stats) {
        WidgetBitmapRenderer.renderGitHubTypeMap(stats, sizePx = 420).asImageBitmap()
    }
    val ghHeatmapBitmap = remember(stats.contributionDays) {
        WidgetBitmapRenderer.renderHeatmap(
            stats.contributionDays,
            isLeetCode = false,
            widthPx = 680,
            heightPx = 520,
            weeksCount = 52
        ).asImageBitmap()
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // User Profile & Status Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xD9101824))
                .border(1.dp, Color(0x38FFFFFF), RoundedCornerShape(22.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_github_logo),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (stats.username.isNotBlank()) "@${stats.username}" else "@not_linked",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Total Contributions: ${stats.totalContributions} (Yearly Activity)",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }

                AccountStatusBadge(
                    status = stats.status,
                    isVerifying = false,
                    connectedText = "Connected",
                    failedText = "Offline"
                )
            }
        }

        // 1. Spider Radar Activity Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xD9101824))
                .border(1.dp, Color(0x38FFFFFF), RoundedCornerShape(22.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Activity Radar & Contribution Types",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        bitmap = ghTypeMapBitmap,
                        contentDescription = "GitHub Spider Radar",
                        modifier = Modifier.size(150.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ContribMetricItem(label = "Commits", count = stats.commitsCount, color = Color(0xFF3FB950))
                        ContribMetricItem(label = "Pull Requests", count = stats.pullRequestsCount, color = Color(0xFF8957E5))
                        ContribMetricItem(label = "Issues", count = stats.issuesCount, color = Color(0xFFD29922))
                        ContribMetricItem(label = "Code Reviews", count = stats.reviewsCount, color = Color(0xFF388BFD))
                    }
                }
            }
        }

        // 2. Down the Uniform Heatmap (Constant Month Shape)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xD9101824))
                .border(1.dp, Color(0x38FFFFFF), RoundedCornerShape(22.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "12-Month GitHub Contribution Heatmap",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Uniform Month Blocks",
                        color = Color(0xFF3FB950),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Image(
                    bitmap = ghHeatmapBitmap,
                    contentDescription = "Uniform GitHub Heatmap",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )

                Text(
                    text = "Each month consists of 7 columns (days of week) and 6 rows (weeks).",
                    color = Color(0xFF64748B),
                    fontSize = 10.5.sp
                )
            }
        }

        // 3. Top Languages Breakdown
        GitHubLanguagesCard(languages = stats.topLanguages)

        // 4. Recent GitHub Events Feed
        RecentGitHubEventsCard(
            events = stats.recentEvents,
            feedLimit = feedLimit,
            onFeedLimitSelected = onFeedLimitSelected
        )
    }
}

@Composable
private fun ContribMetricItem(label: String, count: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(text = label, color = Color(0xFFCBD5E1), fontSize = 12.sp)
        }
        Text(text = "$count", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GitHubLanguagesCard(languages: List<GitHubLanguageStat>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xD9101824))
            .border(1.dp, Color(0x38FFFFFF), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Top Languages & Technologies",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${languages.size} Tracked",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }

            if (languages.isEmpty()) {
                Text(
                    text = "No public language statistics available. Sync your profile to populate repositories.",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    languages.forEach { lang ->
                        val parsedColor = try {
                            Color(android.graphics.Color.parseColor(lang.colorHex))
                        } catch (_: Exception) {
                            Color(0xFF38BDF8)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x241E293B))
                                .border(1.dp, parsedColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(parsedColor)
                                )
                                Text(
                                    text = lang.name,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(parsedColor.copy(alpha = 0.25f))
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${lang.repoCount} repos",
                                        color = parsedColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
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
private fun RecentGitHubEventsCard(
    events: List<GitHubEvent>,
    feedLimit: FeedLimit,
    onFeedLimitSelected: (FeedLimit) -> Unit
) {
    val displayed: List<GitHubEvent> = remember(events, feedLimit) {
        events.take(feedLimit.count)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xD9101824))
            .border(1.dp, Color(0x38FFFFFF), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent GitHub Activity",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                // Limit switcher: Last 10 | 50 | 100
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x20FFFFFF))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    FeedLimit.values().forEach { limit ->
                        val isSelected = feedLimit == limit
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Color(0xFF3FB950) else Color.Transparent)
                                .clickable { onFeedLimitSelected(limit) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = limit.label.replace("Last ", ""),
                                color = if (isSelected) Color.Black else Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (displayed.isEmpty()) {
                Text(
                    text = "No recent public events recorded. Sync your profile to load contributions.",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    displayed.forEach { event ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x1AFFFFFF))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = event.repoName,
                                    color = Color(0xFF60A5FA),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${event.actionSummary.ifBlank { event.type }} • ${formatIsoDate(event.createdAt)}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x263FB950))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = event.type.replace("Event", ""),
                                    color = Color(0xFF4ADE80),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 2. WIDGETS SCREEN (Auto-refresh 15 options & Multiple Widgets Gallery)
// =========================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WidgetsScreenContent(
    dashboardData: WidgetDashboardData,
    selectedInterval: String,
    onIntervalSelected: (String) -> Unit,
    onPinWidget: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 1. Auto-Refresh Frequency Box (15 options: 1s to 1d)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xD9101824))
                .border(1.dp, Color(0x38FFFFFF), RoundedCornerShape(22.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Auto-Refresh Frequency",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select sync cadence (15 options: 1s to 1d)",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x333B82F6))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Current: $selectedInterval",
                            color = Color(0xFF93C5FD),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 15 Options FlowRow
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MainScreenViewModel.REFRESH_INTERVALS.forEach { interval ->
                        val isSelected = selectedInterval == interval
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF2563EB) else Color(0x1FFFFFFF))
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF60A5FA) else Color(0x2EFFFFFF),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onIntervalSelected(interval) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = interval,
                                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Text(
                    text = "Controls background WorkManager cadence and widget telemetry refresh.",
                    color = Color(0xFF64748B),
                    fontSize = 10.5.sp
                )
            }
        }

        // 2. Widget Gallery Header
        Text(
            text = "Multiple Widget Styles (Click to Pin)",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        // Multiple Widgets Available
        WidgetGalleryCard(
            title = "Master Dev Life Dashboard (4x5)",
            subtitle = "Combined LeetCode gauge + GitHub radar + dual 12-month uniform heatmaps",
            tag = "Full Suite",
            onClick = onPinWidget,
            content = { WidgetVisualPreview(data = dashboardData) }
        )

        WidgetGalleryCard(
            title = "LeetCode Focus Widget (4x2)",
            subtitle = "Dedicated 3/4 speedometer circle gauge and 12-month problem calendar",
            tag = "LeetCode",
            onClick = onPinWidget,
            content = {
                val lcCircle = remember(dashboardData.leetCode) {
                    WidgetBitmapRenderer.renderLeetCodeCircle(dashboardData.leetCode, sizePx = 320).asImageBitmap()
                }
                val lcHeatmap = remember(dashboardData.leetCode.submissionCalendar) {
                    WidgetBitmapRenderer.renderHeatmap(
                        dashboardData.leetCode.submissionCalendar,
                        isLeetCode = true,
                        widthPx = 450,
                        heightPx = 420,
                        weeksCount = 52
                    ).asImageBitmap()
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(bitmap = lcCircle, contentDescription = null, modifier = Modifier.weight(0.42f).fillMaxHeight())
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0x26FFFFFF)))
                    Image(bitmap = lcHeatmap, contentDescription = null, modifier = Modifier.weight(0.58f).fillMaxHeight())
                }
            }
        )

        WidgetGalleryCard(
            title = "GitHub Activity Widget (4x2)",
            subtitle = "Dedicated contribution radar polygon and 12-month uniform commit heatmap",
            tag = "GitHub",
            onClick = onPinWidget,
            content = {
                val ghRadar = remember(dashboardData.gitHub) {
                    WidgetBitmapRenderer.renderGitHubTypeMap(dashboardData.gitHub, sizePx = 320).asImageBitmap()
                }
                val ghHeatmap = remember(dashboardData.gitHub.contributionDays) {
                    WidgetBitmapRenderer.renderHeatmap(
                        dashboardData.gitHub.contributionDays,
                        isLeetCode = false,
                        widthPx = 450,
                        heightPx = 420,
                        weeksCount = 52
                    ).asImageBitmap()
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(bitmap = ghRadar, contentDescription = null, modifier = Modifier.weight(0.42f).fillMaxHeight())
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0x26FFFFFF)))
                    Image(bitmap = ghHeatmap, contentDescription = null, modifier = Modifier.weight(0.58f).fillMaxHeight())
                }
            }
        )
    }
}

@Composable
private fun WidgetGalleryCard(
    title: String,
    subtitle: String,
    tag: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xD9101824))
            .border(1.dp, Color(0x38FFFFFF), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x333B82F6))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tag,
                        color = Color(0xFF93C5FD),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Visual Preview Block
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x1FFFFFFF))
                    .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {
                content()
            }

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Widgets, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Widget to Home Screen", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// =========================================================================
// 3. SETTINGS SCREEN (Single Unified Box with All Connections, Updates, Logout)
// =========================================================================

@Composable
private fun SettingsScreenContent(
    dashboardData: WidgetDashboardData,
    leetCodeInput: String,
    gitHubInput: String,
    tokenInput: String,
    isSyncing: Boolean,
    isVerifyingLc: Boolean,
    isVerifyingGh: Boolean,
    isCheckingUpdate: Boolean,
    onLeetCodeChanged: (String) -> Unit,
    onGitHubChanged: (String) -> Unit,
    onTokenChanged: (String) -> Unit,
    onVerifyLeetCode: () -> Unit,
    onVerifyGitHub: () -> Unit,
    onSyncAll: () -> Unit,
    onUseSample: () -> Unit,
    onCheckForUpdate: () -> Unit,
    onLogoutAll: () -> Unit
) {
    // Single Unified Settings Box / Card
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xD9101824))
            .border(1.dp, Color(0x38FFFFFF), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Developer Accounts & Settings",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 1. LeetCode Connected Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x1FFFFFFF))
                    .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_leetcode_logo),
                                contentDescription = "LeetCode",
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "LeetCode",
                                color = Color(0xFFFFA116),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        AccountStatusBadge(
                            status = dashboardData.leetCode.status,
                            isVerifying = isVerifyingLc,
                            connectedText = "✓ Connected (${dashboardData.leetCode.totalSolved} solved)",
                            failedText = "✗ Failed"
                        )
                    }

                    if (dashboardData.leetCode.status == AccountStatus.FAILED && dashboardData.leetCode.statusMessage.isNotBlank()) {
                        Text(
                            text = dashboardData.leetCode.statusMessage,
                            color = Color(0xFFF87171),
                            fontSize = 11.sp
                        )
                    }

                    OutlinedTextField(
                        value = leetCodeInput,
                        onValueChange = onLeetCodeChanged,
                        label = { Text("username", color = Color(0xFF94A3B8)) },
                        placeholder = { Text("e.g. sangrechy", color = Color(0xFF64748B)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFA116),
                            unfocusedBorderColor = Color(0x40FFFFFF)
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    Button(
                        onClick = onVerifyLeetCode,
                        enabled = !isVerifyingLc && !isSyncing,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFA116),
                            contentColor = Color.Black
                        )
                    ) {
                        if (isVerifyingLc) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verifying...", fontWeight = FontWeight.Bold)
                        } else {
                            Text("Link & Verify LeetCode", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. GitHub Connected Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x1FFFFFFF))
                    .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_github_logo),
                                contentDescription = "GitHub",
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "GitHub",
                                color = Color(0xFF3FB950),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        AccountStatusBadge(
                            status = dashboardData.gitHub.status,
                            isVerifying = isVerifyingGh,
                            connectedText = "✓ Connected (${dashboardData.gitHub.totalContributions} contribs)",
                            failedText = "✗ Failed"
                        )
                    }

                    if (dashboardData.gitHub.status == AccountStatus.FAILED && dashboardData.gitHub.statusMessage.isNotBlank()) {
                        Text(
                            text = dashboardData.gitHub.statusMessage,
                            color = Color(0xFFF87171),
                            fontSize = 11.sp
                        )
                    }

                    OutlinedTextField(
                        value = gitHubInput,
                        onValueChange = onGitHubChanged,
                        label = { Text("username", color = Color(0xFF94A3B8)) },
                        placeholder = { Text("e.g. sangrechy", color = Color(0xFF64748B)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF3FB950),
                            unfocusedBorderColor = Color(0x40FFFFFF)
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = onTokenChanged,
                        label = { Text("token (optional)", color = Color(0xFF94A3B8)) },
                        placeholder = { Text("ghp_... for private contributions", color = Color(0xFF64748B)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF3FB950),
                            unfocusedBorderColor = Color(0x40FFFFFF)
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        )
                    )

                    // Token Status Indicator
                    val hasToken = tokenInput.isNotBlank()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (hasToken) Color(0xFF22C55E) else Color(0xFF94A3B8))
                        )
                        Text(
                            text = if (hasToken) "token: connected (private repo telemetry enabled)" else "token: disconnected (public contributions only)",
                            color = if (hasToken) Color(0xFF4ADE80) else Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = onVerifyGitHub,
                        enabled = !isVerifyingGh && !isSyncing,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF22C55E),
                            contentColor = Color.Black
                        )
                    ) {
                        if (isVerifyingGh) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verifying...", fontWeight = FontWeight.Bold)
                        } else {
                            Text("Link & Verify GitHub", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Sync All & Use Sample Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSyncAll,
                    enabled = !isSyncing,
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    )
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Syncing...", fontWeight = FontWeight.Bold)
                    } else {
                        Text("Sync All & Update Widget", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onUseSample,
                    modifier = Modifier.weight(0.8f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1)),
                    border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color(0x60FFFFFF), Color(0x30FFFFFF))))
                ) {
                    Text("Use Sample")
                }
            }

            HorizontalDivider(color = Color(0x26FFFFFF))

            // Check for Update Button
            Button(
                onClick = onCheckForUpdate,
                enabled = !isCheckingUpdate,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isCheckingUpdate) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Checking GitHub Releases...", fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF60A5FA))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Check for Update (GitHub Releases)", fontWeight = FontWeight.SemiBold)
                }
            }

            HorizontalDivider(color = Color(0x26FFFFFF))

            // Logout All Button
            Button(
                onClick = onLogoutAll,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x33DC2626),
                    contentColor = Color(0xFFF87171)
                ),
                border = BorderStroke(1.dp, Color(0x66DC2626)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout All Accounts", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// =========================================================================
// SHARED UTILITY COMPONENTS & HELPERS
// =========================================================================

@Composable
private fun MiniAccountChip(
    name: String,
    status: AccountStatus,
    color: Color
) {
    val isConnected = status == AccountStatus.CONNECTED
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isConnected) color.copy(alpha = 0.2f) else Color(0x1FFFFFFF))
            .border(
                1.dp,
                if (isConnected) color.copy(alpha = 0.5f) else Color(0x2EFFFFFF),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isConnected) color else Color(0xFF94A3B8))
            )
            Text(
                text = name,
                color = if (isConnected) Color.White else Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AccountStatusBadge(
    status: AccountStatus,
    isVerifying: Boolean,
    connectedText: String,
    failedText: String
) {
    val (bgColor, borderColor, textColor, text) = when {
        isVerifying -> Quadruple(Color(0x333B82F6), Color(0x663B82F6), Color(0xFF93C5FD), "Verifying...")
        status == AccountStatus.CONNECTED -> Quadruple(Color(0x3322C55E), Color(0x6622C55E), Color(0xFF4ADE80), connectedText)
        status == AccountStatus.FAILED -> Quadruple(Color(0x33EF4444), Color(0x66EF4444), Color(0xFFF87171), failedText)
        else -> Quadruple(Color(0x26FFFFFF), Color(0x38FFFFFF), Color(0xFF94A3B8), "○ Not Linked")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun WidgetVisualPreview(data: WidgetDashboardData) {
    val lcCircle = remember(data.leetCode) {
        WidgetBitmapRenderer.renderLeetCodeCircle(data.leetCode, sizePx = 360).asImageBitmap()
    }
    val lcHeatmap = remember(data.leetCode.submissionCalendar) {
        WidgetBitmapRenderer.renderHeatmap(
            data.leetCode.submissionCalendar,
            isLeetCode = true,
            widthPx = 450,
            heightPx = 480,
            weeksCount = 52
        ).asImageBitmap()
    }
    val ghTypeMap = remember(data.gitHub) {
        WidgetBitmapRenderer.renderGitHubTypeMap(data.gitHub, sizePx = 360).asImageBitmap()
    }
    val ghHeatmap = remember(data.gitHub.contributionDays) {
        WidgetBitmapRenderer.renderHeatmap(
            data.gitHub.contributionDays,
            isLeetCode = false,
            widthPx = 450,
            heightPx = 480,
            weeksCount = 52
        ).asImageBitmap()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xD9101824))
            .border(
                1.5.dp,
                Color(0x55FFFFFF),
                RoundedCornerShape(26.dp)
            )
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header with Chibi Jane logo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.ic_skill_jane),
                        contentDescription = "Chibi Jane Logo",
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 6.dp)
                    )
                    Text(
                        text = "Dev Life",
                        color = Color(0xFFFFFFFF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    text = "Live Glass Preview",
                    color = Color(0xFFCBD5E1),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Top Card: LeetCode Glass Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x26FFFFFF))
                    .border(
                        1.dp,
                        Color(0x38FFFFFF),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_leetcode_logo),
                                contentDescription = "LeetCode",
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "LeetCode",
                                color = Color(0xFFFFA116),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (data.leetCode.status == AccountStatus.FAILED)
                                "@${data.leetCode.username.ifBlank { "dev" }} (Offline)"
                            else
                                "@${data.leetCode.username.ifBlank { "dev" }}",
                            color = Color(0xFF94A3B8),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Side-by-side: 3/4 Circular Gauge (Left 42%) + Divider + Month-Aware Heatmap (Right 58%)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = lcCircle,
                            contentDescription = "LeetCode Count",
                            modifier = Modifier
                                .weight(0.42f)
                                .fillMaxHeight()
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(Color(0x26FFFFFF))
                        )
                        Image(
                            bitmap = lcHeatmap,
                            contentDescription = "LeetCode Heatmap",
                            modifier = Modifier
                                .weight(0.58f)
                                .fillMaxHeight()
                        )
                    }
                }
            }

            // Bottom Card: GitHub Glass Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x26FFFFFF))
                    .border(
                        1.dp,
                        Color(0x38FFFFFF),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_github_logo),
                                contentDescription = "GitHub",
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "GitHub",
                                color = Color(0xFF3FB950),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (data.gitHub.status == AccountStatus.FAILED)
                                "@${data.gitHub.username.ifBlank { "dev" }} (Offline)"
                            else
                                "@${data.gitHub.username.ifBlank { "dev" }}",
                            color = Color(0xFF94A3B8),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Side-by-side: Spider Radar Activity (Left 42%) + Divider + Month-Aware Heatmap (Right 58%)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = ghTypeMap,
                            contentDescription = "GitHub Type Map",
                            modifier = Modifier
                                .weight(0.42f)
                                .fillMaxHeight()
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(Color(0x26FFFFFF))
                        )
                        Image(
                            bitmap = ghHeatmap,
                            contentDescription = "GitHub Heatmap",
                            modifier = Modifier
                                .weight(0.58f)
                                .fillMaxHeight()
                        )
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

private fun formatTimestamp(epochSeconds: Long): String {
    return try {
        if (epochSeconds <= 0L) return "Recently"
        val millis = if (epochSeconds < 100000000000L) epochSeconds * 1000 else epochSeconds
        val diff = System.currentTimeMillis() - millis
        val minutes = diff / (1000 * 60)
        val hours = diff / (1000 * 60 * 60)
        val days = diff / (1000 * 60 * 60 * 24)
        when {
            minutes < 1 -> "just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 30 -> "${days}d ago"
            else -> "${days / 30}mo ago"
        }
    } catch (_: Exception) {
        "Recently"
    }
}

private fun formatIsoDate(isoString: String): String {
    return try {
        val instant = Instant.parse(isoString)
        val diff = System.currentTimeMillis() - instant.toEpochMilli()
        val minutes = diff / (1000 * 60)
        val hours = diff / (1000 * 60 * 60)
        val days = diff / (1000 * 60 * 60 * 24)
        when {
            minutes < 1 -> "just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 30 -> "${days}d ago"
            else -> "${days / 30}mo ago"
        }
    } catch (_: Exception) {
        isoString.take(10)
    }
}
