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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.layout.ContentScale
import com.sangrechy.devwidgets.data.WallpaperManager
import com.sangrechy.devwidgets.data.WallpaperPreset
import androidx.activity.compose.BackHandler
import com.sangrechy.devwidgets.ui.problems.LeetCodeProblemDetailView
import com.sangrechy.devwidgets.ui.problems.GeneralNoteEditorView
import com.sangrechy.devwidgets.util.ChangelogParser
import com.sangrechy.devwidgets.ui.problems.ProblemsScreenContent
import com.sangrechy.devwidgets.ui.wallpaper.WallpaperScreenContent
import com.sangrechy.devwidgets.ui.theme.*
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
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
import android.appwidget.AppWidgetProvider
import com.sangrechy.devwidgets.data.model.GitHubStats
import com.sangrechy.devwidgets.data.model.LeetCodeStats
import com.sangrechy.devwidgets.data.model.LeetCodeSubmission
import com.sangrechy.devwidgets.data.model.ProblemTopicTag
import com.sangrechy.devwidgets.data.model.WidgetDashboardData
import com.sangrechy.devwidgets.ui.component.ContinuousCalendarHeatmap
import com.sangrechy.devwidgets.widget.AutoRefreshScheduler
import com.sangrechy.devwidgets.widget.CombinedHeatmapWidgetProvider
import com.sangrechy.devwidgets.widget.DevActivityWidgetProvider
import com.sangrechy.devwidgets.widget.GitHubHeatmapWidgetProvider
import com.sangrechy.devwidgets.widget.GitHubWidgetProvider
import com.sangrechy.devwidgets.widget.LeetCodeCircleWidgetProvider
import com.sangrechy.devwidgets.widget.GitHubCircleWidgetProvider
import com.sangrechy.devwidgets.widget.LeetCodeHeatmapWidgetProvider
import com.sangrechy.devwidgets.widget.LeetCodeWidgetProvider
import com.sangrechy.devwidgets.widget.WidgetBitmapRenderer
import com.sangrechy.devwidgets.widget.WidgetManagerHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
    val heatmapOrientation by viewModel.heatmapOrientation.collectAsStateWithLifecycle()

    val leetCodeInput by viewModel.leetCodeInput.collectAsStateWithLifecycle()
    val gitHubInput by viewModel.gitHubInput.collectAsStateWithLifecycle()
    val tokenInput by viewModel.gitHubTokenInput.collectAsStateWithLifecycle()

    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val isVerifyingLc by viewModel.isVerifyingLeetCode.collectAsStateWithLifecycle()
    val isVerifyingGh by viewModel.isVerifyingGitHub.collectAsStateWithLifecycle()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsStateWithLifecycle()

    val problemsPlatform by viewModel.problemsPlatform.collectAsStateWithLifecycle()
    val problemsSubTab by viewModel.problemsSubTab.collectAsStateWithLifecycle()
    val problemsSearchQuery by viewModel.problemsSearchQuery.collectAsStateWithLifecycle()
    val difficultyFilter by viewModel.difficultyFilter.collectAsStateWithLifecycle()
    val problemsList by viewModel.problemsList.collectAsStateWithLifecycle()
    val isLoadingProblems by viewModel.isLoadingProblems.collectAsStateWithLifecycle()
    val selectedProblemDetail by viewModel.selectedProblemDetail.collectAsStateWithLifecycle()
    val activeProblemNoteIndex by viewModel.activeProblemNoteIndex.collectAsStateWithLifecycle()
    val generalNotesList by viewModel.generalNotesList.collectAsStateWithLifecycle()
    val selectedGeneralNote by viewModel.selectedGeneralNote.collectAsStateWithLifecycle()
    val backupRepoInput by viewModel.backupRepoInput.collectAsStateWithLifecycle()
    val isSyncingBackupRepo by viewModel.isSyncingBackupRepo.collectAsStateWithLifecycle()
    val leetCodeSearchResults by viewModel.leetCodeSearchResults.collectAsStateWithLifecycle()
    val isSearchingLeetCode by viewModel.isSearchingLeetCode.collectAsStateWithLifecycle()
    val isAddingProblem by viewModel.isAddingProblem.collectAsStateWithLifecycle()

    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    val dashboardData by viewModel.dashboardData.collectAsStateWithLifecycle()

    LaunchedEffect(syncMessage) {
        if (syncMessage != null) {
            kotlinx.coroutines.delay(3500)
            viewModel.dismissMessage()
        }
    }

    val showUpdateDialog by viewModel.showUpdateDialog.collectAsStateWithLifecycle()
    val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()
    val showLogoutDialog by viewModel.showLogoutDialog.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Check updates silently on initial launch
    LaunchedEffect(Unit) {
        viewModel.checkForUpdates(manual = false)
    }

    // Active foreground auto-refresh loop matching chosen cadence
    LaunchedEffect(selectedInterval) {
        val delayMs = AutoRefreshScheduler.parseIntervalToMillis(selectedInterval)
        while (isActive) {
            delay(delayMs)
            if (!isSyncing && (leetCodeInput.isNotBlank() || gitHubInput.isNotBlank())) {
                viewModel.syncSilently()
                DevActivityWidgetProvider.enqueueImmediateSync(context)
            }
        }
    }

    val bgGradient = remember {
        Brush.verticalGradient(
            listOf(
                ThemeBgTop,
                ThemeBgMid,
                ThemeBgBottom
            )
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = ThemeDrawerBg,
                drawerContentColor = ThemeTextPrimary,
                modifier = Modifier.width(300.dp)
            ) {
                // Drawer Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ThemeDrawerHeaderBg)
                        .padding(20.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_skill_jane),
                        contentDescription = "Chibi Jane Logo",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, ThemeCardBorder, CircleShape)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Dev Life",
                        color = ThemeTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "v${BuildConfig.VERSION_NAME} • Multi-Platform Hub",
                        color = ThemeTextSecondary,
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
                            color = ThemeAccentAmber
                        )
                        MiniAccountChip(
                            name = "GitHub",
                            status = dashboardData.gitHub.status,
                            color = ThemeAccentGreen
                        )
                    }
                }

                HorizontalDivider(color = ThemeCardBorder)
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
                        selectedContainerColor = ThemeDrawerItemSelected,
                        selectedTextColor = ThemeTextPrimary,
                        selectedIconColor = ThemeAccentBlue,
                        unselectedTextColor = ThemeTextSecondary,
                        unselectedIconColor = ThemeTextMuted
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Problems", fontWeight = FontWeight.SemiBold) },
                    selected = currentScreen == AppScreen.PROBLEMS,
                    onClick = {
                        viewModel.setScreen(AppScreen.PROBLEMS)
                        coroutineScope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Code, contentDescription = "Problems") },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = ThemeDrawerItemSelected,
                        selectedTextColor = ThemeTextPrimary,
                        selectedIconColor = ThemeAccentBlue,
                        unselectedTextColor = ThemeTextSecondary,
                        unselectedIconColor = ThemeTextMuted
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
                        selectedContainerColor = ThemeDrawerItemSelected,
                        selectedTextColor = ThemeTextPrimary,
                        selectedIconColor = ThemeAccentBlue,
                        unselectedTextColor = ThemeTextSecondary,
                        unselectedIconColor = ThemeTextMuted
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Wallpaper", fontWeight = FontWeight.SemiBold) },
                    selected = currentScreen == AppScreen.WALLPAPER,
                    onClick = {
                        viewModel.setScreen(AppScreen.WALLPAPER)
                        coroutineScope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Wallpaper, contentDescription = "Wallpaper") },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = ThemeDrawerItemSelected,
                        selectedTextColor = ThemeTextPrimary,
                        selectedIconColor = ThemeAccentBlue,
                        unselectedTextColor = ThemeTextSecondary,
                        unselectedIconColor = ThemeTextMuted
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
                        selectedContainerColor = ThemeDrawerItemSelected,
                        selectedTextColor = ThemeTextPrimary,
                        selectedIconColor = ThemeAccentBlue,
                        unselectedTextColor = ThemeTextSecondary,
                        unselectedIconColor = ThemeTextMuted
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider(color = ThemeCardBorder)

                NavigationDrawerItem(
                    label = { Text("Logout All", fontWeight = FontWeight.Bold, color = ThemeAccentRed) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.showLogoutConfirmation()
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = ThemeAccentRed) },
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color.Transparent
        ) { innerPadding ->
            val appWallpaperType by viewModel.appWallpaperType.collectAsState()
            val appWallpaperPreset by viewModel.appWallpaperPreset.collectAsState()
            val appWallpaperCustomPath by viewModel.appWallpaperCustomPath.collectAsState()
            val appWallpaperDim by viewModel.appWallpaperDim.collectAsState()
            val widgetWallpaperType by viewModel.widgetWallpaperType.collectAsState()
            val widgetSpecificWallpapers by viewModel.widgetSpecificWallpapers.collectAsState()

            val wallpaperBitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
                initialValue = null,
                appWallpaperType, appWallpaperPreset, appWallpaperCustomPath
            ) {
                value = withContext(Dispatchers.IO) {
                    WallpaperManager.getAppWallpaperBitmap(context, 1080, 2400)?.asImageBitmap()
                }
            }

            val currentWallpaperBitmap = wallpaperBitmap
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgGradient)
            ) {
                if (appWallpaperType != "NONE" && currentWallpaperBitmap != null) {
                    Image(
                        bitmap = currentWallpaperBitmap,
                        contentDescription = "App Wallpaper Background",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = appWallpaperDim))
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
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
                            .background(ThemeTopBarBg)
                            .border(
                                1.dp,
                                ThemeTopBarBorder,
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Navigation Menu",
                                tint = ThemeTextPrimary
                            )
                        }

                        Image(
                            painter = painterResource(R.drawable.ic_skill_jane),
                            contentDescription = "Chibi Jane",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .border(1.dp, ThemeCardBorder, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dev Life",
                                color = ThemeTextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = when (currentScreen) {
                                    AppScreen.HOME -> "User Insights & Activity"
                                    AppScreen.PROBLEMS -> "Offline Problem Bank & Notes"
                                    AppScreen.WIDGETS -> "Widget Gallery & Sync"
                                    AppScreen.WALLPAPER -> "App & Widget Wallpaper Studio"
                                    AppScreen.SETTINGS -> "Accounts & Configuration"
                                },
                                color = ThemeTextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        // Quick Sync Icon Button
                        IconButton(
                            onClick = {
                                viewModel.refreshCurrentScreen()
                                DevActivityWidgetProvider.enqueueImmediateSync(context)
                            },
                            enabled = !isSyncing
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = ThemeAccentBlue
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Quick Sync",
                                    tint = ThemeTextSecondary
                                )
                            }
                        }
                    }

                    // Scrollable Content Column
                    val contentScrollState = rememberScrollState()
                    LaunchedEffect(currentScreen) {
                        contentScrollState.scrollTo(0)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(contentScrollState),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Content Based on Selected Screen
                        when (currentScreen) {
                        AppScreen.HOME -> HomeScreenContent(
                            dashboardData = dashboardData,
                            currentTab = currentHomeTab,
                            feedLimit = feedLimit,
                            onTabSelected = viewModel::setHomeTab,
                            onFeedLimitSelected = viewModel::setFeedLimit,
                            onNavigateToSettings = { viewModel.setScreen(AppScreen.SETTINGS) }
                        )

                        AppScreen.PROBLEMS -> ProblemsScreenContent(
                            platform = problemsPlatform,
                            subTab = problemsSubTab,
                            searchQuery = problemsSearchQuery,
                            difficultyFilter = difficultyFilter,
                            problems = problemsList,
                            isLoadingProblems = isLoadingProblems,
                            selectedProblemDetail = selectedProblemDetail,
                            activeProblemNoteIndex = activeProblemNoteIndex,
                            generalNotes = generalNotesList,
                            selectedGeneralNote = selectedGeneralNote,
                            backupRepoInput = backupRepoInput,
                            isSyncingBackupRepo = isSyncingBackupRepo,
                            leetCodeSearchResults = leetCodeSearchResults,
                            isSearchingLeetCode = isSearchingLeetCode,
                            isAddingProblem = isAddingProblem,
                            onPlatformSelected = viewModel::setProblemsPlatform,
                            onSubTabSelected = viewModel::setProblemsSubTab,
                            onSearchQueryChanged = viewModel::setProblemsSearchQuery,
                            onDifficultyFilterSelected = viewModel::setDifficultyFilter,
                            onSelectProblem = viewModel::selectProblem,
                            onActiveProblemNoteIndexChanged = viewModel::setActiveProblemNoteIndex,
                            onDownloadProblemOffline = viewModel::downloadProblemOffline,
                            onDeleteProblemOffline = viewModel::deleteProblemOffline,
                            onSearchLeetCode = viewModel::searchLeetCode,
                            onClearLeetCodeSearch = viewModel::clearLeetCodeSearchResults,
                            onAddProblemFromLeetCode = viewModel::addProblemFromLeetCode,
                            onSaveProblemNote = viewModel::saveProblemNote,
                            onDeleteProblemNote = viewModel::deleteProblemNote,
                            onBackupRepoChanged = viewModel::onBackupRepoInputChanged,
                            onSaveBackupRepo = viewModel::saveBackupRepo,
                            onSyncFromGitHubBackup = viewModel::syncFromGitHubBackupRepo,
                            onSelectGeneralNote = viewModel::selectGeneralNote,
                            onSaveGeneralNote = viewModel::saveGeneralNote,
                            onDeleteGeneralNote = viewModel::deleteGeneralNote
                        )

                        AppScreen.WIDGETS -> WidgetsScreenContent(
                            dashboardData = dashboardData,
                            selectedInterval = selectedInterval,
                            heatmapOrientation = heatmapOrientation,
                            widgetWallpaperType = widgetWallpaperType,
                            widgetSpecificWallpapers = widgetSpecificWallpapers,
                            onIntervalSelected = viewModel::setRefreshInterval,
                            onOrientationSelected = viewModel::setHeatmapOrientation,
                            onWidgetWallpaperTypeSelected = viewModel::setWidgetWallpaperType,
                            onSetWidgetSpecificWallpaper = viewModel::setWidgetWallpaperTypeFor,
                            getWidgetSpecificWallpaper = { key ->
                                widgetSpecificWallpapers[key.trim().uppercase()] ?: viewModel.getWidgetWallpaperTypeFor(key)
                            },
                            onSetWidgetElementOffset = viewModel::setWidgetElementOffset,
                            getWidgetElementOffset = { key ->
                                viewModel.getWidgetElementOffsetX(key) to viewModel.getWidgetElementOffsetY(key)
                            },
                            onPinWidget = { providerClass -> pinWidgetToHomeScreen(context, providerClass) }
                        )

                        AppScreen.WALLPAPER -> WallpaperScreenContent(
                            viewModel = viewModel,
                            dashboardData = dashboardData,
                            heatmapOrientation = heatmapOrientation
                        )

                        AppScreen.SETTINGS -> SettingsScreenContent(
                            dashboardData = dashboardData,
                            leetCodeInput = leetCodeInput,
                            gitHubInput = gitHubInput,
                            tokenInput = tokenInput,
                            backupRepoInput = backupRepoInput,
                            isSyncing = isSyncing,
                            isVerifyingLc = isVerifyingLc,
                            isVerifyingGh = isVerifyingGh,
                            isCheckingUpdate = isCheckingUpdate,
                            onLeetCodeChanged = viewModel::onLeetCodeInputChanged,
                            onGitHubChanged = viewModel::onGitHubInputChanged,
                            onTokenChanged = viewModel::onGitHubTokenChanged,
                            onBackupRepoChanged = viewModel::onBackupRepoInputChanged,
                            onSaveBackupRepo = viewModel::saveBackupRepo,
                            onVerifyLeetCode = viewModel::verifyLeetCode,
                            onVerifyGitHub = viewModel::verifyGitHub,
                            onSyncAll = {
                                viewModel.saveAndSync()
                                DevActivityWidgetProvider.enqueueImmediateSync(context)
                            },
                            onCheckForUpdate = { viewModel.checkForUpdates(manual = true) },
                            onOpenWallpaper = { viewModel.setScreen(AppScreen.WALLPAPER) },
                            onLogoutAll = viewModel::showLogoutConfirmation
                        )
                    }
                }
            }
        }

        // Floating Top-Right Notification Sticker (Overlay, zero layout shift)
        androidx.compose.animation.AnimatedVisibility(
            visible = syncMessage != null,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 56.dp, end = 16.dp)
        ) {
            syncMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .shadow(12.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xF0181B24))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(ThemeAccentBlue)
                        )
                        Text(
                            text = msg,
                            color = ThemeTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        IconButton(
                            onClick = viewModel::dismissMessage,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = ThemeTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // Problem Detail In-Hierarchy View (Wallpaper completely visible behind outer margins)
                selectedProblemDetail?.let { problem ->
                    BackHandler(onBack = { viewModel.selectProblem(null) })
                    LeetCodeProblemDetailView(
                        problem = problem,
                        activeNoteIndex = activeProblemNoteIndex,
                        onDismiss = { viewModel.selectProblem(null) },
                        onActiveNoteIndexChanged = viewModel::setActiveProblemNoteIndex,
                        onDownloadOffline = { viewModel.downloadProblemOffline(problem.folderName, problem.titleSlug) },
                        onDeleteOffline = { viewModel.deleteProblemOffline(problem.folderName) },
                        onSaveNote = { filename, content -> viewModel.saveProblemNote(problem.folderName, filename, content) },
                        onDeleteNote = { filename -> viewModel.deleteProblemNote(problem.folderName, filename) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 28.dp, bottom = 12.dp, start = 12.dp, end = 12.dp)
                    )
                }

                // General Note Editor In-Hierarchy View (Wallpaper completely visible behind outer margins)
                selectedGeneralNote?.let { note ->
                    BackHandler(onBack = { viewModel.selectGeneralNote(null) })
                    GeneralNoteEditorView(
                        note = note,
                        isNew = note.id.isBlank(),
                        onDismiss = { viewModel.selectGeneralNote(null) },
                        onSave = { updated ->
                            viewModel.saveGeneralNote(updated)
                            viewModel.selectGeneralNote(null)
                        },
                        onDelete = { id ->
                            viewModel.deleteGeneralNote(id)
                            viewModel.selectGeneralNote(null)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 28.dp, bottom = 12.dp, start = 12.dp, end = 12.dp)
                    )
                }
            }
        }
    }

    // App Update Dialog with Markdown / What's New Catcher
    if (showUpdateDialog && updateInfo != null) {
        val info = updateInfo!!
        val sections = remember(info.releaseNotes) {
            val parsed = ChangelogParser.parse(info.releaseNotes)
            if (parsed.isEmpty() || parsed.all { it.items.isEmpty() }) {
                ChangelogParser.getFallbackChangelog()
            } else {
                parsed
            }
        }
        AlertDialog(
            onDismissRequest = viewModel::dismissUpdateDialog,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(ThemePillBg)
                            .border(1.dp, ThemeCardBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Update",
                            tint = ThemeTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Update Available",
                            style = BalancedHeading.copy(fontSize = 16.sp),
                            color = ThemeTextPrimary
                        )
                        Text(
                            text = "Version ${info.latestVersion}",
                            style = BalancedParagraph.copy(fontSize = 12.sp, color = ThemeTextSecondary)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (info.releaseName.isNotBlank() && info.releaseName != info.latestVersion) {
                        Text(
                            text = info.releaseName,
                            style = BalancedParagraph.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = ThemeTextPrimary,
                                fontSize = 13.sp
                            )
                        )
                    }

                    sections.forEach { section ->
                        Text(
                            text = section.title,
                            style = BalancedParagraph.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = ThemeTextSecondary,
                                fontSize = 12.sp
                            )
                        )
                        section.items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, top = 2.dp, bottom = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(ThemeTextSecondary)
                                )
                                Text(
                                    text = item,
                                    style = BalancedParagraph.copy(
                                        color = ThemeTextPrimary.copy(alpha = 0.9f),
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp
                                    )
                                )
                            }
                        }
                    }
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
                        containerColor = ThemeTextPrimary,
                        contentColor = ThemeBgDark
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download APK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = viewModel::ignoreCurrentUpdate) {
                        Text("Never ask again", color = ThemeTextMuted, fontSize = 11.sp)
                    }
                    TextButton(onClick = viewModel::dismissUpdateDialog) {
                        Text("Later", color = ThemeTextSecondary, fontSize = 12.sp)
                    }
                }
            },
            containerColor = ThemeCardBg,
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
                    color = ThemeTextPrimary
                )
            },
            text = {
                Text(
                    text = "This will clear your saved LeetCode and GitHub usernames, private access token, and all cached widget telemetry.",
                    color = ThemeTextSecondary,
                    fontSize = 13.sp,
                    style = BalancedParagraph
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmLogout,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThemeAccentRed.copy(alpha = 0.2f),
                        contentColor = ThemeAccentRed
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ThemeCardBorder)
                ) {
                    Text("Logout All", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissLogoutConfirmation) {
                    Text("Cancel", color = ThemeTextSecondary)
                }
            },
            containerColor = ThemeCardBgElevated,
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
    onFeedLimitSelected: (FeedLimit) -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Top Segmented Switcher [ LeetCode | GitHub ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ThemeTopBarBg)
                .border(
                    1.dp,
                    ThemeTopBarBorder,
                    RoundedCornerShape(16.dp)
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // LeetCode Tab Button
            val isLcSelected = currentTab == HomeTab.LEETCODE
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isLcSelected) ThemeCardBgElevated else Color.Transparent)
                    .border(
                        1.dp,
                        if (isLcSelected) ThemeCardBorder else Color.Transparent,
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
                        color = if (isLcSelected) ThemeAccentAmber else ThemeTextSecondary,
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
                    .background(if (isGhSelected) ThemeCardBgElevated else Color.Transparent)
                    .border(
                        1.dp,
                        if (isGhSelected) ThemeCardBorder else Color.Transparent,
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
                        color = if (isGhSelected) ThemeAccentGreen else ThemeTextSecondary,
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
                onFeedLimitSelected = onFeedLimitSelected,
                onNavigateToSettings = onNavigateToSettings
            )
            HomeTab.GITHUB -> GitHubDetailView(
                stats = dashboardData.gitHub,
                feedLimit = feedLimit,
                onFeedLimitSelected = onFeedLimitSelected,
                onNavigateToSettings = onNavigateToSettings
            )
        }
    }
}

@Composable
private fun LeetCodeDetailView(
    stats: LeetCodeStats,
    feedLimit: FeedLimit,
    onFeedLimitSelected: (FeedLimit) -> Unit,
    onNavigateToSettings: () -> Unit = {}
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
        // User Profile & Status Card (Responsive & Collision Free with Dedicated Div Boxes)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(ThemeCardBg)
                .border(
                    1.dp,
                    ThemeCardBorder,
                    RoundedCornerShape(22.dp)
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Div Box 1: User Handle Container
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ThemeInnerBoxBg)
                        .border(1.dp, ThemeInnerBoxBorder, RoundedCornerShape(14.dp))
                        .clickable { onNavigateToSettings() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_leetcode_logo),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (stats.username.isNotBlank()) "@${stats.username}" else "@not_linked",
                            color = ThemeTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                AccountStatusBadge(
                    status = stats.status,
                    isVerifying = false,
                    connectedText = "Connected",
                    failedText = "Offline",
                    onClick = onNavigateToSettings
                )
            }
        }

        // Setup Banner when Not Connected
        if (stats.status != AccountStatus.CONNECTED) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(ThemeInnerBoxBg)
                    .border(1.dp, ThemeCardBorder, RoundedCornerShape(18.dp))
                    .clickable { onNavigateToSettings() }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Link your LeetCode Account",
                            color = ThemeAccentAmber,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Tap here to set your username in Settings & view live solved problems & heatmap.",
                            color = ThemeTextSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ThemeAccentAmber)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Setup →",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 1. The 3/4 Circular Gauge Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(ThemeCardBg)
                .border(
                    1.dp,
                    ThemeCardBorder,
                    RoundedCornerShape(22.dp)
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Solved Problems Gauge",
                    color = ThemeTextPrimary,
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
                        DifficultyPill(label = "Easy", solved = stats.easySolved, max = maxTarget, color = Color(0xFFFFFFFF))
                        DifficultyPill(label = "Medium", solved = stats.mediumSolved, max = maxTarget, color = Color(0xFFCCCCCC))
                        DifficultyPill(label = "Hard", solved = stats.hardSolved, max = maxTarget, color = Color(0xFF888888))
                    }
                }
            }
        }

        // 2. Submission Heatmap (Authentic Continuous Calendar)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(ThemeCardBg)
                .border(
                    1.dp,
                    ThemeCardBorder,
                    RoundedCornerShape(22.dp)
                )
                .padding(16.dp)
        ) {
            ContinuousCalendarHeatmap(
                rawDaysMap = stats.submissionCalendar.toDaysMap(),
                isLeetCode = true
            )
        }

        // 3. Bubble Map / Problem Topic Tags Cloud
        LeetCodeBubbleMapCard(
            topicTags = stats.topicTags,
            isConnected = stats.status == AccountStatus.CONNECTED
        )

        // 4. Recent Submissions Feed
        RecentSubmissionsCard(
            submissions = stats.recentSubmissions,
            feedLimit = feedLimit,
            onFeedLimitSelected = onFeedLimitSelected,
            isConnected = stats.status == AccountStatus.CONNECTED
        )
    }
}

@Composable
private fun DifficultyPill(label: String, solved: Int, max: Int, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ThemeInnerBoxBg)
            .border(1.dp, ThemeInnerBoxBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Text(text = label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text(text = "$solved", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(5.dp))
            val fraction = if (max > 0) (solved.toFloat() / max.toFloat()).coerceIn(0f, 1f) else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color(0x26FFFFFF))
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LeetCodeBubbleMapCard(
    topicTags: List<ProblemTopicTag>,
    isConnected: Boolean = false
) {
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
            .background(ThemeCardBg)
            .border(
                1.dp,
                ThemeCardBorder,
                RoundedCornerShape(22.dp)
            )
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
                    color = ThemeTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${filteredTags.size} Topics",
                    color = ThemeTextSecondary,
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
                            .background(if (isSelected) ThemeAccentAmber.copy(alpha = 0.2f) else ThemeInnerBoxBg)
                            .border(
                                1.dp,
                                if (isSelected) ThemeAccentAmber else ThemeInnerBoxBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) ThemeAccentAmber else ThemeTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Flow of topic bubbles
            if (filteredTags.isEmpty()) {
                val emptyMessage = if (isConnected) {
                    if (topicTags.isEmpty()) "No solved problem topics found for this profile yet."
                    else "No topic data available in category '$selectedCategory'."
                } else {
                    "No topic data available yet. Link and sync your LeetCode account in Settings."
                }
                Text(
                    text = emptyMessage,
                    color = ThemeTextMuted,
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
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ThemeInnerBoxBg)
            .border(1.dp, ThemeInnerBoxBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = tag.name,
                color = ThemeTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(ThemePillBg)
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "${tag.solvedCount}",
                    color = ThemeTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun RecentSubmissionsCard(
    submissions: List<LeetCodeSubmission>,
    feedLimit: FeedLimit,
    onFeedLimitSelected: (FeedLimit) -> Unit,
    isConnected: Boolean = false
) {
    val displayed: List<LeetCodeSubmission> = remember(submissions, feedLimit) {
        submissions.take(feedLimit.count)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(ThemeCardBg)
            .border(
                1.dp,
                ThemeCardBorder,
                RoundedCornerShape(22.dp)
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "Recent Submissions",
                        color = ThemeTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Showing ${displayed.size} submissions",
                        color = ThemeTextSecondary,
                        fontSize = 11.sp
                    )
                }

                // Limit switcher: 10 | 50 | 100
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ThemeInnerBoxBg)
                        .border(1.dp, ThemeInnerBoxBorder, RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    FeedLimit.values().forEach { limit ->
                        val isSelected = feedLimit == limit
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) ThemeTextPrimary else Color.Transparent)
                                .clickable { onFeedLimitSelected(limit) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${limit.count}",
                                color = if (isSelected) ThemeBgDark else ThemeTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (displayed.isEmpty()) {
                val emptyMessage = if (isConnected) {
                    "No recent submissions found for this profile."
                } else {
                    "No recent submissions logged. Sync your profile to view history."
                }
                Text(
                    text = emptyMessage,
                    color = ThemeTextMuted,
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
                                .background(ThemeInnerBoxBg)
                                .border(1.dp, ThemeInnerBoxBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sub.title,
                                    color = ThemeTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${sub.lang} • ${formatTimestamp(sub.timestamp)}",
                                    color = ThemeTextSecondary,
                                    fontSize = 10.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ThemePillBg)
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = sub.statusDisplay.ifBlank { "Accepted" },
                                    color = ThemeTextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
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
    onFeedLimitSelected: (FeedLimit) -> Unit,
    onNavigateToSettings: () -> Unit = {}
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
        // User Profile & Status Card (Responsive & Collision Free with Dedicated Div Boxes)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(ThemeCardBg)
                .border(
                    1.dp,
                    ThemeCardBorder,
                    RoundedCornerShape(22.dp)
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Div Box 1: User Handle Container
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ThemeInnerBoxBg)
                        .border(1.dp, ThemeInnerBoxBorder, RoundedCornerShape(14.dp))
                        .clickable { onNavigateToSettings() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_github_logo),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (stats.username.isNotBlank()) "@${stats.username}" else "@not_linked",
                            color = ThemeTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                AccountStatusBadge(
                    status = stats.status,
                    isVerifying = false,
                    connectedText = if (stats.isAuthenticated) "Connected (Token)" else "Connected",
                    failedText = "Offline",
                    onClick = onNavigateToSettings
                )
            }
        }

        // Setup Banner when Not Connected
        if (stats.status != AccountStatus.CONNECTED) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(ThemeInnerBoxBg)
                    .border(1.dp, ThemeCardBorder, RoundedCornerShape(18.dp))
                    .clickable { onNavigateToSettings() }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Link your GitHub Account",
                            color = ThemeAccentGreen,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Tap here to configure your username & token in Settings to view radar & commits.",
                            color = ThemeTextSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ThemeAccentGreen)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Setup →",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 1. Spider Radar Activity Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(ThemeCardBg)
                .border(
                    1.dp,
                    ThemeCardBorder,
                    RoundedCornerShape(22.dp)
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Activity Radar & Contribution Types",
                    color = ThemeTextPrimary,
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
                        ContribMetricItem(label = "Commits", count = stats.commitsCount, color = Color(0xFFFFFFFF))
                        ContribMetricItem(label = "Pull Requests", count = stats.pullRequestsCount, color = Color(0xFFE0E0E0))
                        ContribMetricItem(label = "Issues", count = stats.issuesCount, color = Color(0xFFCCCCCC))
                        ContribMetricItem(label = "Code Reviews", count = stats.reviewsCount, color = Color(0xFFA0A0A0))
                    }
                }
            }
        }

        // 2. Contribution Heatmap (Authentic Continuous Calendar)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(ThemeCardBg)
                .border(
                    1.dp,
                    ThemeCardBorder,
                    RoundedCornerShape(22.dp)
                )
                .padding(16.dp)
        ) {
            ContinuousCalendarHeatmap(
                rawDaysMap = stats.contributionDays,
                isLeetCode = false
            )
        }

        // 3. Top Languages Breakdown
        GitHubLanguagesCard(
            languages = stats.topLanguages,
            isConnected = stats.status == AccountStatus.CONNECTED
        )

        // 4. Recent GitHub Events Feed
        RecentGitHubEventsCard(
            events = stats.recentEvents,
            feedLimit = feedLimit,
            onFeedLimitSelected = onFeedLimitSelected,
            isConnected = stats.status == AccountStatus.CONNECTED
        )
    }
}

@Composable
private fun ContribMetricItem(label: String, count: Int, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ThemeInnerBoxBg)
            .border(1.dp, ThemeInnerBoxBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Text(text = label, color = ThemeTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Text(text = "$count", color = ThemeTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GitHubLanguagesCard(
    languages: List<GitHubLanguageStat>,
    isConnected: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(ThemeCardBg)
            .border(
                1.dp,
                ThemeCardBorder,
                RoundedCornerShape(22.dp)
            )
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
                    color = ThemeTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${languages.size} Tracked",
                    color = ThemeTextSecondary,
                    fontSize = 11.sp
                )
            }

            if (languages.isEmpty()) {
                val emptyMessage = if (isConnected) {
                    "No public language statistics found for this profile yet."
                } else {
                    "No public language statistics available. Sync your profile to populate repositories."
                }
                Text(
                    text = emptyMessage,
                    color = ThemeTextMuted,
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
                            val c = android.graphics.Color.parseColor(lang.colorHex)
                            val lum = (0.299 * android.graphics.Color.red(c) + 0.587 * android.graphics.Color.green(c) + 0.114 * android.graphics.Color.blue(c)).toInt().coerceIn(120, 255)
                            Color(lum, lum, lum)
                        } catch (_: Exception) {
                            Color(0xFFD4D4D4)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(ThemeInnerBoxBg)
                                .border(1.dp, ThemeInnerBoxBorder, RoundedCornerShape(10.dp))
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
                                    color = ThemeTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(ThemePillBg)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${lang.repoCount} repos",
                                        color = ThemeTextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
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
    onFeedLimitSelected: (FeedLimit) -> Unit,
    isConnected: Boolean = false
) {
    val displayed: List<GitHubEvent> = remember(events, feedLimit) {
        events.take(feedLimit.count)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(ThemeCardBg)
            .border(
                1.dp,
                ThemeCardBorder,
                RoundedCornerShape(22.dp)
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "Recent GitHub Activity",
                        color = ThemeTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Showing ${displayed.size} events",
                        color = ThemeTextSecondary,
                        fontSize = 11.sp
                    )
                }

                // Limit switcher: 10 | 50 | 100
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ThemeInnerBoxBg)
                        .border(1.dp, ThemeInnerBoxBorder, RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    FeedLimit.values().forEach { limit ->
                        val isSelected = feedLimit == limit
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) ThemeTextPrimary else Color.Transparent)
                                .clickable { onFeedLimitSelected(limit) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${limit.count}",
                                color = if (isSelected) ThemeBgDark else ThemeTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (displayed.isEmpty()) {
                val emptyMessage = if (isConnected) {
                    "No recent public events recorded for this profile."
                } else {
                    "No recent public events recorded. Sync your profile to load contributions."
                }
                Text(
                    text = emptyMessage,
                    color = ThemeTextMuted,
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
                                .background(ThemeInnerBoxBg)
                                .border(1.dp, ThemeInnerBoxBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = event.repoName,
                                    color = ThemeTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${event.actionSummary.ifBlank { event.type }} • ${formatIsoDate(event.createdAt)}",
                                    color = ThemeTextSecondary,
                                    fontSize = 10.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ThemePillBg)
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = event.type.replace("Event", ""),
                                    color = ThemeTextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
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
    heatmapOrientation: String,
    widgetWallpaperType: String,
    widgetSpecificWallpapers: Map<String, String> = emptyMap(),
    onIntervalSelected: (String) -> Unit,
    onOrientationSelected: (String) -> Unit,
    onWidgetWallpaperTypeSelected: (String) -> Unit,
    onSetWidgetSpecificWallpaper: (widgetKey: String, type: String) -> Unit = { _, _ -> },
    getWidgetSpecificWallpaper: (widgetKey: String) -> String = { "DEFAULT" },
    onSetWidgetElementOffset: (widgetKey: String, dx: Float, dy: Float) -> Unit = { _, _, _ -> },
    getWidgetElementOffset: (widgetKey: String) -> Pair<Float, Float> = { 0f to 0f },
    onPinWidget: (Class<out AppWidgetProvider>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 1. Heatmap Orientation Selector Box (Horizontal vs Vertical 90°)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(ThemeCardBg)
                .border(
                    1.dp,
                    ThemeCardBorder,
                    RoundedCornerShape(22.dp)
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Heatmap Orientation",
                            color = ThemeTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Horizontal calendar grid or vertical 90° LeetCode style",
                            color = ThemeTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ThemeAccentBlue.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (heatmapOrientation == "vertical") "Vertical (90°)" else "Horizontal",
                            color = ThemeAccentBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                // Toggle Selector Pills: [ Horizontal ] | [ Vertical (90°) ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isHoriz = (heatmapOrientation == "horizontal")
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isHoriz) ThemeAccentBlue else ThemeInnerBoxBg)
                            .border(
                                1.dp,
                                if (isHoriz) ThemeAccentBlue else ThemeInnerBoxBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onOrientationSelected("horizontal") }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⊞ Horizontal",
                            color = if (isHoriz) Color.White else ThemeTextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isHoriz) FontWeight.Bold else FontWeight.Medium
                        )
                    }

                    val isVert = (heatmapOrientation == "vertical")
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isVert) ThemeAccentBlue else ThemeInnerBoxBg)
                            .border(
                                1.dp,
                                if (isVert) ThemeAccentBlue else ThemeInnerBoxBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onOrientationSelected("vertical") }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⊟ Vertical (90°)",
                            color = if (isVert) Color.White else ThemeTextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isVert) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                Text(
                    text = "Controls launcher widgets and gallery previews instantly.",
                    color = ThemeTextMuted,
                    fontSize = 11.sp
                )
            }
        }

        // 1.5 Widget Background & Wallpaper Chassis Selector Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(ThemeCardBg)
                .border(
                    1.dp,
                    ThemeCardBorder,
                    RoundedCornerShape(22.dp)
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Widget Chassis Background",
                            color = ThemeTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Solid obsidian, frosted translucent, transparent, or wallpaper",
                            color = ThemeTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ThemeAccentGreen.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when (widgetWallpaperType) {
                                "TRANSLUCENT" -> "Translucent Frost"
                                "TRANSPARENT" -> "Transparent Glass"
                                "MATCH_APP" -> "App Wallpaper"
                                "CUSTOM" -> "Custom Photo"
                                else -> "Obsidian Matte"
                            },
                            color = ThemeAccentGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                // Row of Pills: [ Obsidian ] | [ Frost ] | [ Clear ] | [ Wallpaper ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val styles = listOf(
                        "DEFAULT" to "Obsidian",
                        "TRANSLUCENT" to "Frost",
                        "TRANSPARENT" to "Clear",
                        "MATCH_APP" to "Wallpaper"
                    )
                    styles.forEach { (typeKey, label) ->
                        val isSelected = (widgetWallpaperType == typeKey)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) ThemeTextPrimary else ThemeInnerBoxBg)
                                .border(
                                    1.dp,
                                    if (isSelected) ThemeTextPrimary else ThemeInnerBoxBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onWidgetWallpaperTypeSelected(typeKey) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) ThemeBgDark else ThemeTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // 2. Auto-Refresh Frequency Box (Segmented Toggle: 1m | 5m | 1h | 4h)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(ThemeCardBg)
                .border(
                    1.dp,
                    ThemeCardBorder,
                    RoundedCornerShape(22.dp)
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Refresh Frequency",
                            color = ThemeTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Cadence for background widget and foreground sync",
                            color = ThemeTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ThemePillBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = selectedInterval,
                            color = ThemeTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }

                // 4-Option Segmented Toggle Switch: 1m | 5m | 1h | 4h
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ThemeInnerBoxBg)
                        .border(1.dp, ThemeInnerBoxBorder, RoundedCornerShape(12.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("1m", "5m", "1h", "4h").forEach { interval ->
                        val isSelected = (selectedInterval == interval)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) ThemeTextPrimary else Color.Transparent)
                                .clickable { onIntervalSelected(interval) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = interval,
                                color = if (isSelected) ThemeBgDark else ThemeTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Text(
                    text = "Controls app foreground loop and background launcher widget auto-refresh.",
                    color = ThemeTextMuted,
                    fontSize = 10.5.sp
                )
            }
        }

        // 3. Widget Gallery Header
        Text(
            text = "Official Widget Styles (7 Styles • Click to Pin)",
            color = ThemeTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        // 1. LeetCode Heatmap (Heatmap Only)
        WidgetGalleryCard(
            title = "LeetCode Heatmap (4x2)",
            subtitle = "Full-width dedicated LeetCode submission activity calendar",
            tag = "LC Heatmap",
            widgetKey = "leetcode_heatmap",
            currentWidgetWallpaper = getWidgetSpecificWallpaper("leetcode_heatmap"),
            onSelectWidgetWallpaper = { onSetWidgetSpecificWallpaper("leetcode_heatmap", it) },
            onClick = { onPinWidget(LeetCodeHeatmapWidgetProvider::class.java) },
            content = {
                val lcHeatmap = remember(dashboardData.leetCode.submissionCalendar, heatmapOrientation) {
                    WidgetBitmapRenderer.renderHeatmap(
                        dashboardData.leetCode.submissionCalendar,
                        isLeetCode = true,
                        widthPx = 680,
                        heightPx = 340,
                        weeksCount = 52,
                        orientation = heatmapOrientation
                    ).asImageBitmap()
                }
                Image(
                    bitmap = lcHeatmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(110.dp)
                )
            }
        )

        // 2. GitHub Heatmap (Heatmap Only)
        WidgetGalleryCard(
            title = "GitHub Heatmap (4x2)",
            subtitle = "Full-width dedicated GitHub emerald contribution activity calendar",
            tag = "GH Heatmap",
            widgetKey = "github_heatmap",
            currentWidgetWallpaper = getWidgetSpecificWallpaper("github_heatmap"),
            onSelectWidgetWallpaper = { onSetWidgetSpecificWallpaper("github_heatmap", it) },
            onClick = { onPinWidget(GitHubHeatmapWidgetProvider::class.java) },
            content = {
                val ghHeatmap = remember(dashboardData.gitHub.contributionDays, heatmapOrientation) {
                    WidgetBitmapRenderer.renderHeatmap(
                        dashboardData.gitHub.contributionDays,
                        isLeetCode = false,
                        widthPx = 680,
                        heightPx = 340,
                        weeksCount = 52,
                        orientation = heatmapOrientation
                    ).asImageBitmap()
                }
                Image(
                    bitmap = ghHeatmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(110.dp)
                )
            }
        )

        // 3. LeetCode Circle (Circle Only - Div Inside Div)
        WidgetGalleryCard(
            title = "LeetCode Solved Circle (2x2)",
            subtitle = "Dedicated 3/4 circular speedometer gauge showing solved problems breakdown",
            tag = "LC Circle",
            widgetKey = "leetcode_circle",
            currentWidgetWallpaper = getWidgetSpecificWallpaper("leetcode_circle"),
            onSelectWidgetWallpaper = { onSetWidgetSpecificWallpaper("leetcode_circle", it) },
            onClick = { onPinWidget(LeetCodeCircleWidgetProvider::class.java) },
            content = {
                val initialOffset = remember { getWidgetElementOffset("leetcode_circle") }
                var offsetX by remember { mutableStateOf(initialOffset.first) }
                var offsetY by remember { mutableStateOf(initialOffset.second) }
                val lcCircle = remember(dashboardData.leetCode) {
                    WidgetBitmapRenderer.renderLeetCodeCircle(dashboardData.leetCode, sizePx = 360).asImageBitmap()
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Compact floating element (div inside div)
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragEnd = {
                                        onSetWidgetElementOffset("leetcode_circle", offsetX, offsetY)
                                    }
                                ) { change, dragAmount ->
                                    change.consume()
                                    val newX = (offsetX + dragAmount.x).coerceIn(-65f, 65f)
                                    val newY = (offsetY + dragAmount.y).coerceIn(-35f, 35f)
                                    offsetX = newX
                                    offsetY = newY
                                }
                            }
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x2EFFFFFF))
                            .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Inner Card Header
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.ic_leetcode_logo),
                                        contentDescription = "LeetCode",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "LeetCode",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (dashboardData.leetCode.username.isNotBlank())
                                            "@${dashboardData.leetCode.username}"
                                        else "@dev",
                                        color = Color(0xFFAAAAAA),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }

                                // Directional Nudge Arrows + Reset
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    if (offsetX != 0f || offsetY != 0f) {
                                        Box(
                                            modifier = Modifier
                                                .size(19.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(Color(0x33FFFFFF))
                                                .clickable {
                                                    offsetX = 0f
                                                    offsetY = 0f
                                                    onSetWidgetElementOffset("leetcode_circle", 0f, 0f)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("↺", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(19.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0x26FFFFFF))
                                            .clickable {
                                                val newX = (offsetX - 10f).coerceIn(-65f, 65f)
                                                offsetX = newX
                                                onSetWidgetElementOffset("leetcode_circle", newX, offsetY)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("←", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(19.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0x26FFFFFF))
                                            .clickable {
                                                val newX = (offsetX + 10f).coerceIn(-65f, 65f)
                                                offsetX = newX
                                                onSetWidgetElementOffset("leetcode_circle", newX, offsetY)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("→", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(19.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0x26FFFFFF))
                                            .clickable {
                                                val newY = (offsetY - 8f).coerceIn(-35f, 35f)
                                                offsetY = newY
                                                onSetWidgetElementOffset("leetcode_circle", offsetX, newY)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("↑", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(19.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0x26FFFFFF))
                                            .clickable {
                                                val newY = (offsetY + 8f).coerceIn(-35f, 35f)
                                                offsetY = newY
                                                onSetWidgetElementOffset("leetcode_circle", offsetX, newY)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("↓", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Centered Circle Gauge (Compact 112dp)
                            Image(
                                bitmap = lcCircle,
                                contentDescription = "LeetCode Gauge",
                                modifier = Modifier.size(112.dp)
                            )
                        }
                    }
                }
            }
        )

        // 4. GitHub Circle (Circle Only - Div Inside Div)
        WidgetGalleryCard(
            title = "GitHub Contribution Circle (2x2)",
            subtitle = "Dedicated contribution radar polygon with commits, PRs, issues, reviews",
            tag = "GH Circle",
            widgetKey = "github_circle",
            currentWidgetWallpaper = getWidgetSpecificWallpaper("github_circle"),
            onSelectWidgetWallpaper = { onSetWidgetSpecificWallpaper("github_circle", it) },
            onClick = { onPinWidget(GitHubCircleWidgetProvider::class.java) },
            content = {
                val initialOffset = remember { getWidgetElementOffset("github_circle") }
                var offsetX by remember { mutableStateOf(initialOffset.first) }
                var offsetY by remember { mutableStateOf(initialOffset.second) }
                val ghCircle = remember(dashboardData.gitHub) {
                    WidgetBitmapRenderer.renderGitHubTypeMap(dashboardData.gitHub, sizePx = 360).asImageBitmap()
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Compact floating element (div inside div)
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragEnd = {
                                        onSetWidgetElementOffset("github_circle", offsetX, offsetY)
                                    }
                                ) { change, dragAmount ->
                                    change.consume()
                                    val newX = (offsetX + dragAmount.x).coerceIn(-65f, 65f)
                                    val newY = (offsetY + dragAmount.y).coerceIn(-35f, 35f)
                                    offsetX = newX
                                    offsetY = newY
                                }
                            }
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x2EFFFFFF))
                            .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Inner Card Header
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.ic_github_logo),
                                        contentDescription = "GitHub",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "GitHub",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (dashboardData.gitHub.username.isNotBlank())
                                            "@${dashboardData.gitHub.username}"
                                        else "@dev",
                                        color = Color(0xFFAAAAAA),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }

                                // Directional Nudge Arrows + Reset
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    if (offsetX != 0f || offsetY != 0f) {
                                        Box(
                                            modifier = Modifier
                                                .size(19.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(Color(0x33FFFFFF))
                                                .clickable {
                                                    offsetX = 0f
                                                    offsetY = 0f
                                                    onSetWidgetElementOffset("github_circle", 0f, 0f)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("↺", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(19.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0x26FFFFFF))
                                            .clickable {
                                                val newX = (offsetX - 10f).coerceIn(-65f, 65f)
                                                offsetX = newX
                                                onSetWidgetElementOffset("github_circle", newX, offsetY)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("←", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(19.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0x26FFFFFF))
                                            .clickable {
                                                val newX = (offsetX + 10f).coerceIn(-65f, 65f)
                                                offsetX = newX
                                                onSetWidgetElementOffset("github_circle", newX, offsetY)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("→", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(19.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0x26FFFFFF))
                                            .clickable {
                                                val newY = (offsetY - 8f).coerceIn(-35f, 35f)
                                                offsetY = newY
                                                onSetWidgetElementOffset("github_circle", offsetX, newY)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("↑", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(19.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0x26FFFFFF))
                                            .clickable {
                                                val newY = (offsetY + 8f).coerceIn(-35f, 35f)
                                                offsetY = newY
                                                onSetWidgetElementOffset("github_circle", offsetX, newY)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("↓", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Centered Radar Circle (Compact 112dp)
                            Image(
                                bitmap = ghCircle,
                                contentDescription = "GitHub Contribution Circle",
                                modifier = Modifier.size(112.dp)
                            )
                        }
                    }
                }
            }
        )

        // 5. LeetCode Full (Circle + Heatmap)
        WidgetGalleryCard(
            title = "LeetCode Full: Circle + Heatmap (4x2)",
            subtitle = "Speedometer circle gauge and 52-week submission calendar side-by-side",
            tag = "LC Full",
            widgetKey = "leetcode",
            currentWidgetWallpaper = getWidgetSpecificWallpaper("leetcode"),
            onSelectWidgetWallpaper = { onSetWidgetSpecificWallpaper("leetcode", it) },
            onClick = { onPinWidget(LeetCodeWidgetProvider::class.java) },
            content = {
                val lcCircle = remember(dashboardData.leetCode) {
                    WidgetBitmapRenderer.renderLeetCodeCircle(dashboardData.leetCode, sizePx = 320).asImageBitmap()
                }
                val lcHeatmap = remember(dashboardData.leetCode.submissionCalendar, heatmapOrientation) {
                    WidgetBitmapRenderer.renderHeatmap(
                        dashboardData.leetCode.submissionCalendar,
                        isLeetCode = true,
                        widthPx = 450,
                        heightPx = 420,
                        weeksCount = 52,
                        orientation = heatmapOrientation
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
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(ThemeCardBorder))
                    Image(bitmap = lcHeatmap, contentDescription = null, modifier = Modifier.weight(0.58f).fillMaxHeight())
                }
            }
        )

        // 6. GitHub Full (Circle + Heatmap)
        WidgetGalleryCard(
            title = "GitHub Full: Circle + Heatmap (4x2)",
            subtitle = "Contribution radar polygon and 52-week activity heatmap side-by-side",
            tag = "GH Full",
            widgetKey = "github",
            currentWidgetWallpaper = getWidgetSpecificWallpaper("github"),
            onSelectWidgetWallpaper = { onSetWidgetSpecificWallpaper("github", it) },
            onClick = { onPinWidget(GitHubWidgetProvider::class.java) },
            content = {
                val ghRadar = remember(dashboardData.gitHub) {
                    WidgetBitmapRenderer.renderGitHubTypeMap(dashboardData.gitHub, sizePx = 320).asImageBitmap()
                }
                val ghHeatmap = remember(dashboardData.gitHub.contributionDays, heatmapOrientation) {
                    WidgetBitmapRenderer.renderHeatmap(
                        dashboardData.gitHub.contributionDays,
                        isLeetCode = false,
                        widthPx = 450,
                        heightPx = 420,
                        weeksCount = 52,
                        orientation = heatmapOrientation
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
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(ThemeCardBorder))
                    Image(bitmap = ghHeatmap, contentDescription = null, modifier = Modifier.weight(0.58f).fillMaxHeight())
                }
            }
        )

        // 7. Combined All: Master Dev Life (4x4)
        WidgetGalleryCard(
            title = "Combined All: Master Dev Life (4x4)",
            subtitle = "Master dashboard with LeetCode & GitHub gauges and dual uniform heatmaps",
            tag = "Combined All",
            widgetKey = "dev_activity",
            currentWidgetWallpaper = getWidgetSpecificWallpaper("dev_activity"),
            onSelectWidgetWallpaper = { onSetWidgetSpecificWallpaper("dev_activity", it) },
            onClick = { onPinWidget(DevActivityWidgetProvider::class.java) },
            content = { WidgetVisualPreview(data = dashboardData, orientation = heatmapOrientation) }
        )
    }
}

@Composable
private fun WidgetGalleryCard(
    title: String,
    subtitle: String,
    tag: String,
    widgetKey: String? = null,
    currentWidgetWallpaper: String = "DEFAULT",
    onSelectWidgetWallpaper: (String) -> Unit = {},
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(ThemeCardBg)
            .border(
                1.dp,
                ThemeCardBorder,
                RoundedCornerShape(22.dp)
            )
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
                        color = ThemeTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = ThemeTextSecondary,
                        fontSize = 11.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(ThemePillBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tag,
                        color = ThemeTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Visual Preview Block (Chassis with Wallpaper / Style preview)
            val context = LocalContext.current
            val previewWallpaperBitmap = remember(currentWidgetWallpaper) {
                if (currentWidgetWallpaper == "MATCH_APP") {
                    WallpaperManager.getAppWallpaperBitmap(context, width = 640, height = 360)
                } else null
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .then(
                        when (currentWidgetWallpaper) {
                            "TRANSLUCENT" -> Modifier
                                .background(Brush.verticalGradient(listOf(Color(0x33262A36), Color(0x1F1A1C24))))
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp))
                            "TRANSPARENT" -> Modifier
                                .background(Color(0x0DFFFFFF))
                                .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(18.dp))
                            else -> Modifier
                                .background(ThemeInnerBoxBg)
                                .border(1.dp, ThemeInnerBoxBorder, RoundedCornerShape(18.dp))
                        }
                    )
            ) {
                if (previewWallpaperBitmap != null) {
                    Image(
                        bitmap = previewWallpaperBitmap.asImageBitmap(),
                        contentDescription = "Widget Wallpaper",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.55f))
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    content()
                }
            }

            // Per-Widget Wallpaper Selector Row
            if (widgetKey != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Wallpaper:",
                        color = ThemeTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    listOf(
                        "DEFAULT" to "Obsidian",
                        "TRANSLUCENT" to "Frost",
                        "TRANSPARENT" to "Clear",
                        "MATCH_APP" to "App"
                    ).forEach { (styleKey, label) ->
                        val isSelected = currentWidgetWallpaper == styleKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ThemeTextPrimary else ThemeInnerBoxBg)
                                .border(
                                    1.dp,
                                    if (isSelected) ThemeTextPrimary else ThemeInnerBoxBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectWidgetWallpaper(styleKey) }
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) ThemeBgDark else ThemeTextSecondary,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemeCardBgElevated,
                    contentColor = ThemeTextPrimary
                ),
                border = BorderStroke(1.dp, ThemeCardBorder),
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
    backupRepoInput: String,
    isSyncing: Boolean,
    isVerifyingLc: Boolean,
    isVerifyingGh: Boolean,
    isCheckingUpdate: Boolean,
    onLeetCodeChanged: (String) -> Unit,
    onGitHubChanged: (String) -> Unit,
    onTokenChanged: (String) -> Unit,
    onBackupRepoChanged: (String) -> Unit,
    onSaveBackupRepo: (String) -> Unit,
    onVerifyLeetCode: () -> Unit,
    onVerifyGitHub: () -> Unit,
    onSyncAll: () -> Unit,
    onCheckForUpdate: () -> Unit,
    onOpenWallpaper: () -> Unit = {},
    onLogoutAll: () -> Unit
) {
    // Single Unified Settings Box / Card
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(ThemeCardBg)
            .border(
                1.dp,
                ThemeCardBorder,
                RoundedCornerShape(22.dp)
            )
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
                    color = ThemeTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    color = ThemeTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 1. LeetCode Connected Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ThemeInnerBoxBg)
                    .border(1.dp, ThemeInnerBoxBorder, RoundedCornerShape(16.dp))
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
                                color = ThemeAccentAmber,
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
                            color = ThemeAccentRed,
                            fontSize = 11.sp
                        )
                    }

                    OutlinedTextField(
                        value = leetCodeInput,
                        onValueChange = onLeetCodeChanged,
                        label = { Text("username", color = ThemeTextSecondary) },
                        placeholder = { Text("e.g. sangrechy", color = ThemeTextMuted) },
                        trailingIcon = {
                            if (leetCodeInput.isNotEmpty()) {
                                IconButton(onClick = { onLeetCodeChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = ThemeTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ThemeTextPrimary,
                            unfocusedTextColor = ThemeTextPrimary,
                            focusedBorderColor = ThemeTextPrimary,
                            unfocusedBorderColor = ThemeInnerBoxBorder,
                            focusedContainerColor = ThemeTextFieldBg,
                            unfocusedContainerColor = ThemeTextFieldBg,
                            cursorColor = ThemeTextPrimary
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    Button(
                        onClick = onVerifyLeetCode,
                        enabled = !isVerifyingLc && !isSyncing,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThemeCardBgElevated,
                            contentColor = ThemeTextPrimary
                        ),
                        border = BorderStroke(1.dp, ThemeCardBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isVerifyingLc) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ThemeTextPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verifying...", fontWeight = FontWeight.SemiBold)
                        } else {
                            Text("Link & Verify LeetCode", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // 2. GitHub Connected Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ThemeInnerBoxBg)
                    .border(1.dp, ThemeInnerBoxBorder, RoundedCornerShape(16.dp))
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
                                color = ThemeTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val ghConnectedLabel = if (dashboardData.gitHub.isAuthenticated) {
                            "✓ Connected (Token • ${dashboardData.gitHub.totalContributions} contribs)"
                        } else {
                            "✓ Connected (${dashboardData.gitHub.totalContributions} contribs)"
                        }
                        AccountStatusBadge(
                            status = dashboardData.gitHub.status,
                            isVerifying = isVerifyingGh,
                            connectedText = ghConnectedLabel,
                            failedText = "✗ Failed"
                        )
                    }

                    if (dashboardData.gitHub.status == AccountStatus.FAILED && dashboardData.gitHub.statusMessage.isNotBlank()) {
                        Text(
                            text = dashboardData.gitHub.statusMessage,
                            color = ThemeAccentRed,
                            fontSize = 11.sp
                        )
                    }

                    OutlinedTextField(
                        value = gitHubInput,
                        onValueChange = onGitHubChanged,
                        label = { Text("username", color = ThemeTextSecondary) },
                        placeholder = { Text("e.g. sangrechy", color = ThemeTextMuted) },
                        trailingIcon = {
                            if (gitHubInput.isNotEmpty()) {
                                IconButton(onClick = { onGitHubChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = ThemeTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ThemeTextPrimary,
                            unfocusedTextColor = ThemeTextPrimary,
                            focusedBorderColor = ThemeTextPrimary,
                            unfocusedBorderColor = ThemeInnerBoxBorder,
                            focusedContainerColor = ThemeTextFieldBg,
                            unfocusedContainerColor = ThemeTextFieldBg,
                            cursorColor = ThemeTextPrimary
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = onTokenChanged,
                        label = { Text("token (optional)", color = ThemeTextSecondary) },
                        placeholder = { Text("ghp_... for private contributions & fast API sync", color = ThemeTextMuted) },
                        trailingIcon = {
                            if (tokenInput.isNotEmpty()) {
                                IconButton(onClick = { onTokenChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = ThemeTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ThemeTextPrimary,
                            unfocusedTextColor = ThemeTextPrimary,
                            focusedBorderColor = ThemeTextPrimary,
                            unfocusedBorderColor = ThemeInnerBoxBorder,
                            focusedContainerColor = ThemeTextFieldBg,
                            unfocusedContainerColor = ThemeTextFieldBg,
                            cursorColor = ThemeTextPrimary
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        )
                    )

                    // Token Status Indicator
                    val hasToken = tokenInput.isNotBlank()
                    val tokenAuthenticated = hasToken && dashboardData.gitHub.isAuthenticated && dashboardData.gitHub.status == AccountStatus.CONNECTED
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (tokenAuthenticated) ThemeAccentGreen else if (hasToken) ThemeTextSecondary else ThemeTextMuted)
                        )
                        Text(
                            text = when {
                                tokenAuthenticated -> "token: connected & authenticated (GraphQL API + private contributions enabled)"
                                hasToken -> "token: entered (click verify below to authenticate with GitHub)"
                                else -> "token: disconnected (public contributions only)"
                            },
                            color = ThemeTextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = onVerifyGitHub,
                        enabled = !isVerifyingGh && !isSyncing,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThemeCardBgElevated,
                            contentColor = ThemeTextPrimary
                        ),
                        border = BorderStroke(1.dp, ThemeCardBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isVerifyingGh) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ThemeTextPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verifying...", fontWeight = FontWeight.SemiBold)
                        } else {
                            Text("Link & Verify GitHub", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // 3. LeetCode Backup Repository (project_git_the_leet integration)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ThemeInnerBoxBg)
                    .border(1.dp, ThemeInnerBoxBorder, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LeetCode Backup Repository",
                            color = ThemeTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ThemePillBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Git The Leet",
                                color = ThemeTextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Text(
                        text = "GitHub repository storing your leetcode_problem/ folder and notes",
                        color = ThemeTextSecondary,
                        fontSize = 11.sp
                    )
                    OutlinedTextField(
                        value = backupRepoInput,
                        onValueChange = onBackupRepoChanged,
                        label = { Text("Repository Name", color = ThemeTextSecondary) },
                        placeholder = { Text("e.g. project_git_the_leet or username/repo", color = ThemeTextMuted) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ThemeTextPrimary,
                            unfocusedTextColor = ThemeTextPrimary,
                            focusedBorderColor = ThemeTextPrimary,
                            unfocusedBorderColor = ThemeInnerBoxBorder,
                            focusedContainerColor = ThemeTextFieldBg,
                            unfocusedContainerColor = ThemeTextFieldBg,
                            cursorColor = ThemeTextPrimary
                        )
                    )
                    Button(
                        onClick = { onSaveBackupRepo(backupRepoInput) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThemeCardBgElevated,
                            contentColor = ThemeTextPrimary
                        ),
                        border = BorderStroke(1.dp, ThemeCardBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Backup Repository", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Sync All Action (Full width)
            Button(
                onClick = onSyncAll,
                enabled = !isSyncing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemeTextPrimary,
                    contentColor = ThemeBgDark
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ThemeBgDark)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Syncing...", fontWeight = FontWeight.Bold)
                } else {
                    Text("Sync All & Update Widget", fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(color = ThemeCardBorder)

            // App & Widget Wallpaper Studio Button
            Button(
                onClick = onOpenWallpaper,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemeCardBgElevated,
                    contentColor = ThemeTextPrimary
                ),
                border = BorderStroke(1.dp, ThemeCardBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(16.dp), tint = ThemeTextSecondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("App & Widget Wallpaper Studio", fontWeight = FontWeight.SemiBold)
            }

            HorizontalDivider(color = ThemeCardBorder)

            // Check for Update Button
            Button(
                onClick = onCheckForUpdate,
                enabled = !isCheckingUpdate,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemeCardBgElevated,
                    contentColor = ThemeTextPrimary
                ),
                border = BorderStroke(1.dp, ThemeCardBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isCheckingUpdate) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ThemeTextPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Checking GitHub Releases...", fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp), tint = ThemeTextSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Check for Update (GitHub Releases)", fontWeight = FontWeight.SemiBold)
                }
            }

            HorizontalDivider(color = ThemeCardBorder)

            // Logout All Button
            Button(
                onClick = onLogoutAll,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemeCardBgElevated,
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, ThemeCardBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout All Accounts", fontWeight = FontWeight.SemiBold)
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
            .background(ThemeInnerBoxBg)
            .border(
                1.dp,
                ThemeInnerBoxBorder,
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
                    .background(if (isConnected) color else ThemeTextMuted)
            )
            Text(
                text = name,
                color = if (isConnected) ThemeTextPrimary else ThemeTextMuted,
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
    failedText: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val (dotColor, text) = when {
        isVerifying -> Pair(ThemeAccentBlue, "Verifying...")
        status == AccountStatus.CONNECTED -> Pair(ThemeAccentGreen, connectedText)
        status == AccountStatus.FAILED -> Pair(ThemeAccentRed, failedText)
        else -> Pair(ThemeTextMuted, "Not Linked")
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ThemeInnerBoxBg)
            .border(1.dp, ThemeInnerBoxBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = text,
            color = ThemeTextSecondary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun WidgetVisualPreview(data: WidgetDashboardData, orientation: String = "horizontal") {
    var isReversed by remember { mutableStateOf(false) }
    var lcOffsetX by remember { mutableStateOf(0f) }
    var lcOffsetY by remember { mutableStateOf(0f) }
    var ghOffsetX by remember { mutableStateOf(0f) }
    var ghOffsetY by remember { mutableStateOf(0f) }

    val lcCircle = remember(data.leetCode) {
        WidgetBitmapRenderer.renderLeetCodeCircle(data.leetCode, sizePx = 360).asImageBitmap()
    }
    val lcHeatmap = remember(data.leetCode.submissionCalendar, orientation) {
        WidgetBitmapRenderer.renderHeatmap(
            data.leetCode.submissionCalendar,
            isLeetCode = true,
            widthPx = 450,
            heightPx = 480,
            weeksCount = 52,
            orientation = orientation
        ).asImageBitmap()
    }
    val ghTypeMap = remember(data.gitHub) {
        WidgetBitmapRenderer.renderGitHubTypeMap(data.gitHub, sizePx = 360).asImageBitmap()
    }
    val ghHeatmap = remember(data.gitHub.contributionDays, orientation) {
        WidgetBitmapRenderer.renderHeatmap(
            data.gitHub.contributionDays,
            isLeetCode = false,
            widthPx = 450,
            heightPx = 480,
            weeksCount = 52,
            orientation = orientation
        ).asImageBitmap()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(ThemeCardBgElevated)
            .border(
                1.5.dp,
                ThemeCardBorder,
                RoundedCornerShape(26.dp)
            )
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header with Dev Life brand and Interactive Div Controls (Swap & Reset)
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
                        contentDescription = "Dev Life",
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 6.dp)
                    )
                    Text(
                        text = "Dev Life (Movable Divs)",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // ⇄ Swap Top / Bottom Div Positions
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x26FFFFFF))
                            .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(8.dp))
                            .clickable { isReversed = !isReversed }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isReversed) "⇅ GH on Top" else "⇅ LC on Top",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Reset button
                    if (lcOffsetX != 0f || lcOffsetY != 0f || ghOffsetX != 0f || ghOffsetY != 0f || isReversed) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x26FFFFFF))
                                .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(8.dp))
                                .clickable {
                                    lcOffsetX = 0f
                                    lcOffsetY = 0f
                                    ghOffsetX = 0f
                                    ghOffsetY = 0f
                                    isReversed = false
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Reset",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Composables for each Div
            val renderLeetCodeDiv: @Composable () -> Unit = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(lcOffsetX.roundToInt(), lcOffsetY.roundToInt()) }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                lcOffsetX = (lcOffsetX + dragAmount.x).coerceIn(-120f, 120f)
                                lcOffsetY = (lcOffsetY + dragAmount.y).coerceIn(-80f, 80f)
                            }
                        }
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
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (data.leetCode.status == AccountStatus.FAILED)
                                        "@${data.leetCode.username.ifBlank { "dev" }} (Offline)"
                                    else
                                        "@${data.leetCode.username.ifBlank { "dev" }}",
                                    color = Color(0xFFAAAAAA),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Directional Nudge Arrows
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x26FFFFFF))
                                        .clickable { lcOffsetX = (lcOffsetX - 15f).coerceIn(-120f, 120f) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("←", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x26FFFFFF))
                                        .clickable { lcOffsetX = (lcOffsetX + 15f).coerceIn(-120f, 120f) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("→", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x26FFFFFF))
                                        .clickable { lcOffsetY = (lcOffsetY - 10f).coerceIn(-80f, 80f) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("↑", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x26FFFFFF))
                                        .clickable { lcOffsetY = (lcOffsetY + 10f).coerceIn(-80f, 80f) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("↓", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Side-by-side: 3/4 Circular Gauge (Left 42%) + Divider + Month-Aware Heatmap (Right 58%)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(125.dp),
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
            }

            val renderGitHubDiv: @Composable () -> Unit = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(ghOffsetX.roundToInt(), ghOffsetY.roundToInt()) }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                ghOffsetX = (ghOffsetX + dragAmount.x).coerceIn(-120f, 120f)
                                ghOffsetY = (ghOffsetY + dragAmount.y).coerceIn(-80f, 80f)
                            }
                        }
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
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (data.gitHub.status == AccountStatus.FAILED)
                                        "@${data.gitHub.username.ifBlank { "dev" }} (Offline)"
                                    else
                                        "@${data.gitHub.username.ifBlank { "dev" }}",
                                    color = Color(0xFFAAAAAA),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Directional Nudge Arrows
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x26FFFFFF))
                                        .clickable { ghOffsetX = (ghOffsetX - 15f).coerceIn(-120f, 120f) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("←", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x26FFFFFF))
                                        .clickable { ghOffsetX = (ghOffsetX + 15f).coerceIn(-120f, 120f) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("→", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x26FFFFFF))
                                        .clickable { ghOffsetY = (ghOffsetY - 10f).coerceIn(-80f, 80f) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("↑", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x26FFFFFF))
                                        .clickable { ghOffsetY = (ghOffsetY + 10f).coerceIn(-80f, 80f) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("↓", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Side-by-side: Spider Radar Activity (Left 42%) + Divider + Month-Aware Heatmap (Right 58%)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(125.dp),
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

            // Display in configured order (Top div then Bottom div)
            if (!isReversed) {
                renderLeetCodeDiv()
                renderGitHubDiv()
            } else {
                renderGitHubDiv()
                renderLeetCodeDiv()
            }
        }
    }
}

private fun pinWidgetToHomeScreen(context: Context, providerClass: Class<*>? = null) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val targetClass = providerClass ?: DevActivityWidgetProvider::class.java
        val provider = ComponentName(context, targetClass)
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

private fun Map<Long, Int>.toDaysMap(): Map<String, Int> {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }
    val result = mutableMapOf<String, Int>()
    forEach { (ts, count) ->
        val millis = if (ts < 100000000000L) ts * 1000L else ts
        val dStr = sdf.format(java.util.Date(millis))
        result[dStr] = (result[dStr] ?: 0) + count
    }
    return result
}
