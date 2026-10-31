package com.sangrechy.devwidgets.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sangrechy.devwidgets.data.DataRepository
import com.sangrechy.devwidgets.data.model.AccountStatus
import com.sangrechy.devwidgets.data.model.WidgetDashboardData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.sangrechy.devwidgets.BuildConfig
import com.sangrechy.devwidgets.data.model.AppUpdateInfo
import com.sangrechy.devwidgets.data.model.CustomNote
import com.sangrechy.devwidgets.data.model.LeetCodeSearchResult
import com.sangrechy.devwidgets.data.model.OfflineProblem
import com.sangrechy.devwidgets.data.model.ProblemDifficultyFilter
import com.sangrechy.devwidgets.data.model.ProblemNote
import com.sangrechy.devwidgets.data.model.ProblemsPlatform
import com.sangrechy.devwidgets.data.model.ProblemsSubTab
import com.sangrechy.devwidgets.data.WallpaperManager
import com.sangrechy.devwidgets.data.WallpaperPreset
import com.sangrechy.devwidgets.widget.WidgetManagerHelper
import kotlinx.coroutines.Dispatchers

enum class AppScreen(val title: String) {
    HOME("Home"),
    PROBLEMS("Problems"),
    WIDGETS("Widgets"),
    WALLPAPER("Wallpaper"),
    SETTINGS("Settings")
}

enum class HomeTab {
    LEETCODE,
    GITHUB
}

enum class FeedLimit(val count: Int, val label: String) {
    LIMIT_10(10, "Last 10"),
    LIMIT_50(50, "Last 50"),
    LIMIT_100(100, "Last 100")
}

class MainScreenViewModel(private val repository: DataRepository) : ViewModel() {

    companion object {
        val REFRESH_INTERVALS = listOf(
            "1s", "5s", "15s", "30s",
            "1m", "2m", "5m", "10m", "15m", "30m",
            "1h", "2h", "6h", "12h", "1d"
        )
    }

    private val saved = repository.getSavedUsernames()

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _currentHomeTab = MutableStateFlow(HomeTab.LEETCODE)
    val currentHomeTab: StateFlow<HomeTab> = _currentHomeTab.asStateFlow()

    private val _feedLimit = MutableStateFlow(FeedLimit.LIMIT_10)
    val feedLimit: StateFlow<FeedLimit> = _feedLimit.asStateFlow()

    private val _selectedInterval = MutableStateFlow(repository.getRefreshInterval())
    val selectedInterval: StateFlow<String> = _selectedInterval.asStateFlow()

    private val _heatmapOrientation = MutableStateFlow(repository.getHeatmapOrientation())
    val heatmapOrientation: StateFlow<String> = _heatmapOrientation.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _updateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val updateInfo: StateFlow<AppUpdateInfo?> = _updateInfo.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private val _showLogoutDialog = MutableStateFlow(false)
    val showLogoutDialog: StateFlow<Boolean> = _showLogoutDialog.asStateFlow()

    private val _leetCodeInput = MutableStateFlow(saved.first)
    val leetCodeInput: StateFlow<String> = _leetCodeInput.asStateFlow()

    private val _gitHubInput = MutableStateFlow(saved.second)
    val gitHubInput: StateFlow<String> = _gitHubInput.asStateFlow()

    private val _gitHubTokenInput = MutableStateFlow(saved.third)
    val gitHubTokenInput: StateFlow<String> = _gitHubTokenInput.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isVerifyingLeetCode = MutableStateFlow(false)
    val isVerifyingLeetCode: StateFlow<Boolean> = _isVerifyingLeetCode.asStateFlow()

    private val _isVerifyingGitHub = MutableStateFlow(false)
    val isVerifyingGitHub: StateFlow<Boolean> = _isVerifyingGitHub.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    val dashboardData: StateFlow<WidgetDashboardData> = repository.dashboardData
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            WidgetDashboardData()
        )

    private val problemRepo = repository.problemRepository

    private val _problemsPlatform = MutableStateFlow(ProblemsPlatform.LEETCODE)
    val problemsPlatform: StateFlow<ProblemsPlatform> = _problemsPlatform.asStateFlow()

    private val _problemsSubTab = MutableStateFlow(ProblemsSubTab.PROBLEMS)
    val problemsSubTab: StateFlow<ProblemsSubTab> = _problemsSubTab.asStateFlow()

    private val _problemsSearchQuery = MutableStateFlow("")
    val problemsSearchQuery: StateFlow<String> = _problemsSearchQuery.asStateFlow()

    private val _difficultyFilter = MutableStateFlow(ProblemDifficultyFilter.ALL)
    val difficultyFilter: StateFlow<ProblemDifficultyFilter> = _difficultyFilter.asStateFlow()

    private val _problemsList = MutableStateFlow<List<OfflineProblem>>(emptyList())
    val problemsList: StateFlow<List<OfflineProblem>> = _problemsList.asStateFlow()

    private val _isLoadingProblems = MutableStateFlow(false)
    val isLoadingProblems: StateFlow<Boolean> = _isLoadingProblems.asStateFlow()

    private val _selectedProblemDetail = MutableStateFlow<OfflineProblem?>(null)
    val selectedProblemDetail: StateFlow<OfflineProblem?> = _selectedProblemDetail.asStateFlow()

    private val _activeProblemNoteIndex = MutableStateFlow(0)
    val activeProblemNoteIndex: StateFlow<Int> = _activeProblemNoteIndex.asStateFlow()

    private val _generalNotesList = MutableStateFlow<List<CustomNote>>(emptyList())
    val generalNotesList: StateFlow<List<CustomNote>> = _generalNotesList.asStateFlow()

    private val _selectedGeneralNote = MutableStateFlow<CustomNote?>(null)
    val selectedGeneralNote: StateFlow<CustomNote?> = _selectedGeneralNote.asStateFlow()

    private val _backupRepoInput = MutableStateFlow(repository.getGitHubBackupRepo())
    val backupRepoInput: StateFlow<String> = _backupRepoInput.asStateFlow()

    private val _isSyncingBackupRepo = MutableStateFlow(false)
    val isSyncingBackupRepo: StateFlow<Boolean> = _isSyncingBackupRepo.asStateFlow()

    private val _leetCodeSearchResults = MutableStateFlow<List<LeetCodeSearchResult>>(emptyList())
    val leetCodeSearchResults: StateFlow<List<LeetCodeSearchResult>> = _leetCodeSearchResults.asStateFlow()

    private val _isSearchingLeetCode = MutableStateFlow(false)
    val isSearchingLeetCode: StateFlow<Boolean> = _isSearchingLeetCode.asStateFlow()

    private val _isAddingProblem = MutableStateFlow(false)
    val isAddingProblem: StateFlow<Boolean> = _isAddingProblem.asStateFlow()

    // Wallpaper configuration flows
    private val _appWallpaperType = MutableStateFlow(repository.getAppWallpaperType())
    val appWallpaperType: StateFlow<String> = _appWallpaperType.asStateFlow()

    private val _appWallpaperPreset = MutableStateFlow(repository.getAppWallpaperPreset())
    val appWallpaperPreset: StateFlow<String> = _appWallpaperPreset.asStateFlow()

    private val _appWallpaperDim = MutableStateFlow(repository.getAppWallpaperDim())
    val appWallpaperDim: StateFlow<Float> = _appWallpaperDim.asStateFlow()

    private val _appWallpaperCustomPath = MutableStateFlow(repository.getAppWallpaperCustomPath())
    val appWallpaperCustomPath: StateFlow<String> = _appWallpaperCustomPath.asStateFlow()

    private val _widgetWallpaperType = MutableStateFlow(repository.getWidgetWallpaperType())
    val widgetWallpaperType: StateFlow<String> = _widgetWallpaperType.asStateFlow()

    private val _widgetWallpaperPreset = MutableStateFlow(repository.getWidgetWallpaperPreset())
    val widgetWallpaperPreset: StateFlow<String> = _widgetWallpaperPreset.asStateFlow()

    private val _widgetWallpaperDim = MutableStateFlow(repository.getWidgetWallpaperDim())
    val widgetWallpaperDim: StateFlow<Float> = _widgetWallpaperDim.asStateFlow()

    private val _widgetWallpaperCustomPath = MutableStateFlow(repository.getWidgetWallpaperCustomPath())
    val widgetWallpaperCustomPath: StateFlow<String> = _widgetWallpaperCustomPath.asStateFlow()

    init {
        loadProblems()
        loadGeneralNotes()
    }

    fun onLeetCodeInputChanged(value: String) {
        _leetCodeInput.value = value
    }

    fun onGitHubInputChanged(value: String) {
        _gitHubInput.value = value
    }

    fun onGitHubTokenChanged(value: String) {
        _gitHubTokenInput.value = value
    }

    fun verifyLeetCode() {
        val user = _leetCodeInput.value.trim().removePrefix("@")
        if (user.isBlank()) {
            _syncMessage.value = "Please enter a LeetCode username"
            return
        }
        viewModelScope.launch {
            _isVerifyingLeetCode.value = true
            val stats = repository.verifyLeetCode(user)
            _isVerifyingLeetCode.value = false
            if (stats.status == AccountStatus.CONNECTED) {
                _syncMessage.value = "LeetCode linked successfully! (${stats.totalSolved} solved)"
            } else {
                _syncMessage.value = "LeetCode verification failed: ${stats.statusMessage}"
            }
        }
    }

    fun verifyGitHub() {
        val user = _gitHubInput.value.trim().removePrefix("@")
        if (user.isBlank()) {
            _syncMessage.value = "Please enter a GitHub username"
            return
        }
        viewModelScope.launch {
            _isVerifyingGitHub.value = true
            val stats = repository.verifyGitHub(user, _gitHubTokenInput.value.trim())
            _isVerifyingGitHub.value = false
            if (stats.status == AccountStatus.CONNECTED) {
                _syncMessage.value = "GitHub linked successfully! (${stats.totalContributions} contributions)"
            } else {
                _syncMessage.value = "GitHub verification failed: ${stats.statusMessage}"
            }
        }
    }

    fun saveAndSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "Saving settings and syncing profiles..."
            repository.saveUsernames(
                leetCode = _leetCodeInput.value.trim().removePrefix("@"),
                gitHub = _gitHubInput.value.trim().removePrefix("@"),
                token = _gitHubTokenInput.value.trim()
            )
            val result = repository.syncData()
            _isSyncing.value = false
            if (result.isSuccess) {
                val data = result.getOrNull()
                val lcStatus = data?.leetCode?.status
                val ghStatus = data?.gitHub?.status
                val msg = when {
                    lcStatus == AccountStatus.CONNECTED && ghStatus == AccountStatus.CONNECTED ->
                        "Both LeetCode & GitHub synced successfully!"
                    lcStatus == AccountStatus.CONNECTED ->
                        "LeetCode synced! GitHub: ${data.gitHub.statusMessage.ifBlank { "Not linked" }}"
                    ghStatus == AccountStatus.CONNECTED ->
                        "GitHub synced! LeetCode: ${data.leetCode.statusMessage.ifBlank { "Not linked" }}"
                    else ->
                        "Sync completed. Check profile status above."
                }
                _syncMessage.value = msg
            } else {
                _syncMessage.value = "Sync failed: ${result.exceptionOrNull()?.localizedMessage ?: "Network error"}"
            }
        }
    }

    fun syncSilently() {
        viewModelScope.launch {
            repository.syncData()
        }
    }

    fun refreshCurrentScreen() {
        viewModelScope.launch {
            _isSyncing.value = true
            when (_currentScreen.value) {
                AppScreen.HOME, AppScreen.SETTINGS -> {
                    val result = repository.syncData()
                    _isSyncing.value = false
                    if (result.isSuccess) {
                        _syncMessage.value = "Profiles refreshed successfully!"
                    } else {
                        _syncMessage.value = "Sync failed: ${result.exceptionOrNull()?.localizedMessage ?: "Network error"}"
                    }
                }
                AppScreen.WIDGETS -> {
                    val result = repository.syncData()
                    _isSyncing.value = false
                    _syncMessage.value = "Widgets and data refreshed!"
                }
                AppScreen.PROBLEMS -> {
                    loadProblems()
                    loadGeneralNotes()
                    val result = repository.syncData()
                    _isSyncing.value = false
                    _syncMessage.value = "Problem bank & notes refreshed!"
                }
                AppScreen.WALLPAPER -> {
                    _isSyncing.value = false
                    applyWidgetWallpaperToAllWidgets()
                    _syncMessage.value = "Wallpaper & widgets synchronized!"
                }
            }
        }
    }

    fun useSampleData() {
        _leetCodeInput.value = "sangrechy"
        _gitHubInput.value = "sangrechy"
        repository.loadSampleData()
        _syncMessage.value = "Loaded sample developer activity data."
    }

    fun setScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setHomeTab(tab: HomeTab) {
        _currentHomeTab.value = tab
    }

    fun setFeedLimit(limit: FeedLimit) {
        _feedLimit.value = limit
    }

    fun setRefreshInterval(interval: String) {
        _selectedInterval.value = interval
        repository.setRefreshInterval(interval)
        _syncMessage.value = "Auto-refresh frequency set to $interval"
    }

    fun setHeatmapOrientation(orientation: String) {
        _heatmapOrientation.value = orientation
        repository.setHeatmapOrientation(orientation)
        _syncMessage.value = "Heatmap orientation set to $orientation"
    }

    fun checkForUpdates(manual: Boolean = true) {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            val info = repository.checkForUpdates()
            _isCheckingUpdate.value = false
            if (info.hasUpdate) {
                val ignored = repository.getIgnoredUpdateVersion()
                if (!manual && ignored == info.latestVersion) {
                    return@launch
                }
                _updateInfo.value = info
                _showUpdateDialog.value = true
            } else {
                if (manual) {
                    if (!info.error.isNullOrBlank()) {
                        _syncMessage.value = "Update check: ${info.error}"
                    } else {
                        _syncMessage.value = "You are on the latest version (${BuildConfig.VERSION_NAME})!"
                    }
                }
            }
        }
    }

    fun ignoreCurrentUpdate() {
        _updateInfo.value?.latestVersion?.let { ver ->
            repository.setIgnoredUpdateVersion(ver)
        }
        _showUpdateDialog.value = false
        _syncMessage.value = "Update notifications muted for this version."
    }

    fun dismissUpdateDialog() {
        _showUpdateDialog.value = false
    }

    fun showLogoutConfirmation() {
        _showLogoutDialog.value = true
    }

    fun dismissLogoutConfirmation() {
        _showLogoutDialog.value = false
    }

    fun confirmLogout() {
        viewModelScope.launch {
            repository.logoutAll()
            _leetCodeInput.value = ""
            _gitHubInput.value = ""
            _gitHubTokenInput.value = ""
            _showLogoutDialog.value = false
            _currentScreen.value = AppScreen.HOME
            _syncMessage.value = "Logged out from all developer accounts."
        }
    }

    fun loadProblems() {
        viewModelScope.launch {
            _isLoadingProblems.value = true
            _problemsList.value = problemRepo.getProblems()
            _isLoadingProblems.value = false
        }
    }

    fun loadGeneralNotes() {
        viewModelScope.launch {
            _generalNotesList.value = problemRepo.getGeneralNotes()
        }
    }

    fun setProblemsPlatform(platform: ProblemsPlatform) {
        _problemsPlatform.value = platform
    }

    fun setProblemsSubTab(subTab: ProblemsSubTab) {
        _problemsSubTab.value = subTab
    }

    fun setProblemsSearchQuery(query: String) {
        _problemsSearchQuery.value = query
    }

    fun setDifficultyFilter(filter: ProblemDifficultyFilter) {
        _difficultyFilter.value = filter
    }

    fun selectProblem(problem: OfflineProblem?) {
        _selectedProblemDetail.value = problem
        _activeProblemNoteIndex.value = 0
    }

    fun setActiveProblemNoteIndex(index: Int) {
        _activeProblemNoteIndex.value = index
    }

    fun downloadProblemOffline(folderName: String, titleSlug: String) {
        viewModelScope.launch {
            _syncMessage.value = "Downloading offline data for $folderName..."
            val result = problemRepo.downloadProblemOffline(folderName, titleSlug)
            if (result.isSuccess) {
                val updated = result.getOrNull()
                _syncMessage.value = "Downloaded #${updated?.frontendId ?: ""} ${updated?.title ?: folderName} for offline access!"
                loadProblems()
                if (updated != null) {
                    _selectedProblemDetail.value = updated
                }
            } else {
                _syncMessage.value = "Failed to download offline: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
            }
        }
    }

    fun searchLeetCode(query: String) {
        val q = query.trim()
        if (q.isBlank()) {
            _leetCodeSearchResults.value = emptyList()
            _isSearchingLeetCode.value = false
            return
        }
        viewModelScope.launch {
            _isSearchingLeetCode.value = true
            val res = problemRepo.searchLeetCode(q)
            _isSearchingLeetCode.value = false
            if (res.isSuccess) {
                _leetCodeSearchResults.value = res.getOrDefault(emptyList())
            } else {
                _leetCodeSearchResults.value = emptyList()
            }
        }
    }

    fun clearLeetCodeSearchResults() {
        _leetCodeSearchResults.value = emptyList()
        _isSearchingLeetCode.value = false
    }

    fun addProblemFromLeetCode(input: String, onDone: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            _isAddingProblem.value = true
            _syncMessage.value = "Fetching authentic data from LeetCode..."
            val result = problemRepo.addProblemFromLeetCode(input)
            _isAddingProblem.value = false
            if (result.isSuccess) {
                val problem = result.getOrNull()
                _syncMessage.value = "Downloaded #${problem?.frontendId} ${problem?.title} offline!"
                loadProblems()
                if (problem != null) {
                    _selectedProblemDetail.value = problem
                }
                clearLeetCodeSearchResults()
                onDone?.invoke(true)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to download problem"
                _syncMessage.value = "Error: $err"
                onDone?.invoke(false)
            }
        }
    }

    fun deleteProblemOffline(folderName: String) {
        viewModelScope.launch {
            val success = problemRepo.deleteProblemOffline(folderName)
            if (success) {
                _syncMessage.value = "Removed $folderName from offline storage"
                loadProblems()
                if (_selectedProblemDetail.value?.folderName == folderName) {
                    _selectedProblemDetail.value = null
                }
            }
        }
    }

    fun saveProblemNote(folderName: String, filename: String, content: String) {
        viewModelScope.launch {
            val savedNote = problemRepo.saveProblemNote(folderName, filename, content)
            val updated = problemRepo.getProblemDetail(folderName)
            if (updated != null) {
                _selectedProblemDetail.value = updated
                val idx = updated.notes.indexOfFirst { it.filename == savedNote.filename }
                if (idx != -1) _activeProblemNoteIndex.value = idx
            }
            loadProblems()
            _syncMessage.value = "Saved note '${savedNote.filename}'"
        }
    }

    fun deleteProblemNote(folderName: String, filename: String) {
        viewModelScope.launch {
            problemRepo.deleteProblemNote(folderName, filename)
            val updated = problemRepo.getProblemDetail(folderName)
            if (updated != null) {
                _selectedProblemDetail.value = updated
                _activeProblemNoteIndex.value = 0
            }
            loadProblems()
            _syncMessage.value = "Deleted note '$filename'"
        }
    }

    fun onBackupRepoInputChanged(value: String) {
        _backupRepoInput.value = value
    }

    fun saveBackupRepo(repo: String) {
        val trimmed = repo.trim()
        _backupRepoInput.value = trimmed
        repository.setGitHubBackupRepo(trimmed)
        _syncMessage.value = "Backup repository updated to '$trimmed'"
    }

    fun syncFromGitHubBackupRepo() {
        val repo = _backupRepoInput.value.trim()
        val owner = _gitHubInput.value.trim().removePrefix("@")
        val token = _gitHubTokenInput.value.trim()

        if (repo.isBlank()) {
            _syncMessage.value = "Please specify a backup repository name (e.g. project_git_the_leet or leetcode-backup)."
            return
        }

        val targetOwner = if (repo.contains("/")) repo.substringBefore("/").trim() else owner
        val targetRepo = if (repo.contains("/")) repo.substringAfter("/").trim() else repo

        if (targetOwner.isBlank()) {
            _syncMessage.value = "Please configure your GitHub username in Settings or enter owner/repo."
            return
        }

        viewModelScope.launch {
            _isSyncingBackupRepo.value = true
            _syncMessage.value = "Syncing LeetCode backup from GitHub ($targetOwner/$targetRepo)..."
            val result = problemRepo.syncFromGitHubBackupRepo(targetOwner, targetRepo, token)
            _isSyncingBackupRepo.value = false
            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                _syncMessage.value = "Successfully synced $count files from $owner/$repo!"
                loadProblems()
            } else {
                _syncMessage.value = "GitHub backup sync failed: ${result.exceptionOrNull()?.message ?: "Check repository name or permissions"}"
            }
        }
    }

    fun selectGeneralNote(note: CustomNote?) {
        _selectedGeneralNote.value = note
    }

    fun saveGeneralNote(note: CustomNote) {
        viewModelScope.launch {
            val saved = problemRepo.saveGeneralNote(note)
            loadGeneralNotes()
            _selectedGeneralNote.value = saved
            _syncMessage.value = "Saved note '${saved.title}'"
        }
    }

    fun deleteGeneralNote(id: String) {
        viewModelScope.launch {
            problemRepo.deleteGeneralNote(id)
            if (_selectedGeneralNote.value?.id == id) {
                _selectedGeneralNote.value = null
            }
            loadGeneralNotes()
            _syncMessage.value = "Deleted note"
        }
    }

    // --- Wallpaper Actions ---

    fun setAppWallpaperDefault() {
        repository.setAppWallpaperType("NONE")
        _appWallpaperType.value = "NONE"
        _syncMessage.value = "App wallpaper reset to Obsidian theme"
    }

    fun setAppWallpaperPreset(presetId: String) {
        repository.setAppWallpaperType("PRESET")
        repository.setAppWallpaperPreset(presetId)
        _appWallpaperType.value = "PRESET"
        _appWallpaperPreset.value = presetId
        _syncMessage.value = "Wallpaper updated!"
    }

    fun setAppWallpaperCustomFromUri(uri: android.net.Uri) {
        viewModelScope.launch {
            val path = repository.saveCustomWallpaperFromUri(uri, forWidget = false)
            if (path != null) {
                repository.setAppWallpaperType("CUSTOM")
                repository.setAppWallpaperCustomPath(path)
                _appWallpaperType.value = "CUSTOM"
                _appWallpaperCustomPath.value = path
                _syncMessage.value = "Custom wallpaper applied!"
            } else {
                _syncMessage.value = "Failed to load custom wallpaper image"
            }
        }
    }

    fun setAppWallpaperDim(dim: Float) {
        val clamped = dim.coerceIn(0.1f, 0.95f)
        repository.setAppWallpaperDim(clamped)
        _appWallpaperDim.value = clamped
    }

    fun setWidgetWallpaperType(type: String) {
        repository.setWidgetWallpaperType(type)
        _widgetWallpaperType.value = type
        applyWidgetWallpaperToAllWidgets()
        _syncMessage.value = "Widget background style updated!"
    }

    fun getWidgetWallpaperTypeFor(widgetKey: String): String {
        return repository.getWidgetWallpaperTypeFor(widgetKey)
    }

    fun setWidgetWallpaperTypeFor(widgetKey: String, type: String) {
        repository.setWidgetWallpaperTypeFor(widgetKey, type)
        applyWidgetWallpaperToAllWidgets()
        _syncMessage.value = "Updated $widgetKey wallpaper"
    }

    fun setWidgetWallpaperPreset(presetId: String) {
        repository.setWidgetWallpaperPreset(presetId)
        _widgetWallpaperPreset.value = presetId
        applyWidgetWallpaperToAllWidgets()
    }

    fun setWidgetWallpaperDim(dim: Float) {
        val clamped = dim.coerceIn(0.1f, 0.95f)
        repository.setWidgetWallpaperDim(clamped)
        _widgetWallpaperDim.value = clamped
        applyWidgetWallpaperToAllWidgets()
    }

    fun setWidgetWallpaperCustomFromUri(uri: android.net.Uri) {
        viewModelScope.launch {
            val path = repository.saveCustomWallpaperFromUri(uri, forWidget = true)
            if (path != null) {
                repository.setWidgetWallpaperType("CUSTOM")
                repository.setWidgetWallpaperCustomPath(path)
                _widgetWallpaperType.value = "CUSTOM"
                _widgetWallpaperCustomPath.value = path
                applyWidgetWallpaperToAllWidgets()
                _syncMessage.value = "Custom widget wallpaper applied!"
            } else {
                _syncMessage.value = "Failed to load custom widget image"
            }
        }
    }

    fun applyWidgetWallpaperToAllWidgets() {
        repository.updateAllWidgets()
    }

    fun dismissMessage() {
        _syncMessage.value = null
    }
}
