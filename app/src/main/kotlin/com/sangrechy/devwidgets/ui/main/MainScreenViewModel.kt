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
import com.sangrechy.devwidgets.data.model.NoteTreeEntry
import com.sangrechy.devwidgets.data.model.OfflineProblem
import com.sangrechy.devwidgets.data.model.OpenedNoteFile
import com.sangrechy.devwidgets.data.model.ProblemDifficultyFilter
import com.sangrechy.devwidgets.data.model.ProblemNote
import com.sangrechy.devwidgets.data.model.ProblemsPlatform
import com.sangrechy.devwidgets.data.model.ProblemsSubTab
import com.sangrechy.devwidgets.data.WallpaperManager
import com.sangrechy.devwidgets.data.WallpaperPreset
import com.sangrechy.devwidgets.widget.WidgetManagerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

enum class AppScreen(val title: String) {
    HOME("Home"),
    PROBLEMS("Problems"),
    NOTES("Notes"),
    WIDGETS("Widgets"),
    WALLPAPER("Wallpaper"),
    SETTINGS("Settings")
}

enum class HomeTab {
    LEETCODE,
    GITHUB
}

enum class FeedLimit(val count: Int, val label: String) {
    LIMIT_5(5, "Last 5"),
    LIMIT_10(10, "Last 10"),
    LIMIT_50(50, "Last 50")
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

    private val _backupRepoInput = MutableStateFlow(repository.getGitHubBackupRepo().ifBlank { "leetcode" })
    val backupRepoInput: StateFlow<String> = _backupRepoInput.asStateFlow()

    private val _problemsTokenInput = MutableStateFlow(repository.getGitHubProblemsToken())
    val problemsTokenInput: StateFlow<String> = _problemsTokenInput.asStateFlow()

    private val _isSyncingBackupRepo = MutableStateFlow(false)
    val isSyncingBackupRepo: StateFlow<Boolean> = _isSyncingBackupRepo.asStateFlow()

    // Dedicated Notes Menu (res/ hierarchical file manager with separate repo & token)
    private val notesRepo = repository.notesRepository

    private val _notesRepoInput = MutableStateFlow(repository.getGitHubNotesRepo().ifBlank { "notes" })
    val notesRepoInput: StateFlow<String> = _notesRepoInput.asStateFlow()

    private val _notesTokenInput = MutableStateFlow(repository.getGitHubNotesToken())
    val notesTokenInput: StateFlow<String> = _notesTokenInput.asStateFlow()

    private val _notesCurrentPath = MutableStateFlow("")
    val notesCurrentPath: StateFlow<String> = _notesCurrentPath.asStateFlow()

    private val _notesEntries = MutableStateFlow<List<NoteTreeEntry>>(emptyList())
    val notesEntries: StateFlow<List<NoteTreeEntry>> = _notesEntries.asStateFlow()

    private val _notesSearchQuery = MutableStateFlow("")
    val notesSearchQuery: StateFlow<String> = _notesSearchQuery.asStateFlow()

    private val _isSyncingNotes = MutableStateFlow(false)
    val isSyncingNotes: StateFlow<Boolean> = _isSyncingNotes.asStateFlow()

    private val _openedNoteFile = MutableStateFlow<OpenedNoteFile?>(null)
    val openedNoteFile: StateFlow<OpenedNoteFile?> = _openedNoteFile.asStateFlow()

    private val _isLoadingNoteFile = MutableStateFlow(false)
    val isLoadingNoteFile: StateFlow<Boolean> = _isLoadingNoteFile.asStateFlow()

    private val _isSavingNoteFile = MutableStateFlow(false)
    val isSavingNoteFile: StateFlow<Boolean> = _isSavingNoteFile.asStateFlow()

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

    private val _widgetSpecificWallpapers = MutableStateFlow<Map<String, String>>(emptyMap())
    val widgetSpecificWallpapers: StateFlow<Map<String, String>> = _widgetSpecificWallpapers.asStateFlow()

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
            if (_backupRepoInput.value.isNotBlank()) {
                syncFromGitHubBackupRepo()
            }
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
                    if (_backupRepoInput.value.isNotBlank()) {
                        syncFromGitHubBackupRepo()
                    } else {
                        loadProblems()
                        loadGeneralNotes()
                    }
                    val result = repository.syncData()
                    _isSyncing.value = false
                    _syncMessage.value = "Problem bank & notes refreshed!"
                }
                AppScreen.NOTES -> {
                    _isSyncing.value = false
                    syncNotesFromGitHub()
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
        if (screen != AppScreen.PROBLEMS) {
            _selectedProblemDetail.value = null
            _selectedGeneralNote.value = null
        }
        if (screen == AppScreen.NOTES) {
            loadNotesEntries(_notesCurrentPath.value)
        } else {
            _openedNoteFile.value = null
        }
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
    }

    fun setHeatmapOrientation(orientation: String) {
        _heatmapOrientation.value = orientation
        repository.setHeatmapOrientation(orientation)
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

    private var searchDebounceJob: Job? = null
    private var activeSearchJob: Job? = null

    fun setProblemsSearchQuery(query: String) {
        _problemsSearchQuery.value = query
        searchDebounceJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            searchDebounceJob = viewModelScope.launch {
                delay(200)
                searchLeetCode(trimmed)
            }
        } else {
            clearLeetCodeSearchResults()
        }
    }

    fun setDifficultyFilter(filter: ProblemDifficultyFilter) {
        _difficultyFilter.value = filter
    }

    fun selectProblem(problem: OfflineProblem?) {
        _selectedProblemDetail.value = problem
        _activeProblemNoteIndex.value = 0
        if (problem != null) {
            viewModelScope.launch {
                val full = problemRepo.getProblemDetail(problem.folderName)
                if (full != null && _selectedProblemDetail.value?.folderName == problem.folderName) {
                    _selectedProblemDetail.value = full
                }
            }
        }
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
                if (updated != null && _currentScreen.value == AppScreen.PROBLEMS && _selectedProblemDetail.value?.folderName == folderName) {
                    _selectedProblemDetail.value = updated
                }
            } else {
                _syncMessage.value = "Failed to download offline: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
            }
        }
    }

    fun searchLeetCode(query: String) {
        val q = query.trim()
        activeSearchJob?.cancel()
        activeSearchJob = viewModelScope.launch {
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
        searchDebounceJob?.cancel()
        activeSearchJob?.cancel()
        _leetCodeSearchResults.value = emptyList()
        _isSearchingLeetCode.value = false
    }

    fun addProblemFromLeetCode(input: String, onDone: ((Boolean) -> Unit)? = null) {
        addMultipleProblemsFromLeetCode(listOf(input), onDone)
    }

    fun addMultipleProblemsFromLeetCode(inputs: List<String>, onDone: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            if (inputs.isEmpty()) {
                onDone?.invoke(false)
                return@launch
            }
            _isAddingProblem.value = true
            var successCount = 0
            var lastProblem: com.sangrechy.devwidgets.data.model.OfflineProblem? = null

            for ((idx, slug) in inputs.withIndex()) {
                _syncMessage.value = "Downloading (${idx + 1}/${inputs.size}): $slug..."
                val result = problemRepo.addProblemFromLeetCode(slug)
                if (result.isSuccess) {
                    successCount++
                    lastProblem = result.getOrNull()
                }
            }
            _isAddingProblem.value = false
            loadProblems()
            if (inputs.size == 1 && lastProblem != null) {
                _selectedProblemDetail.value = lastProblem
            } else if (lastProblem != null && _currentScreen.value == AppScreen.PROBLEMS && _selectedProblemDetail.value?.folderName == lastProblem.folderName) {
                _selectedProblemDetail.value = lastProblem
            }
            if (successCount > 0) {
                clearLeetCodeSearchResults()
                _syncMessage.value = "Downloaded $successCount of ${inputs.size} problems offline!"
                onDone?.invoke(true)
            } else {
                _syncMessage.value = "Failed to download selected problem(s)"
                onDone?.invoke(false)
            }
        }
    }

    fun deleteProblemOffline(folderName: String, silent: Boolean = false) {
        viewModelScope.launch {
            val success = problemRepo.deleteProblemOffline(folderName)
            if (success) {
                if (!silent) {
                    _syncMessage.value = "Removed $folderName from offline storage"
                }
                loadProblems()
                if (_selectedProblemDetail.value?.folderName == folderName) {
                    _selectedProblemDetail.value = null
                }
            }
        }
    }

    fun deleteMultipleProblemsOffline(folderNames: List<String>) {
        if (folderNames.isEmpty()) return
        viewModelScope.launch {
            folderNames.forEach { folderName ->
                problemRepo.deleteProblemOffline(folderName)
                if (_selectedProblemDetail.value?.folderName == folderName) {
                    _selectedProblemDetail.value = null
                }
            }
            loadProblems()
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
            _syncMessage.value = when (savedNote.syncStatus) {
                "SYNCED" -> "Successfully saved to GitHub & device"
                "LOCAL_ONLY" -> "Saved locally (GitHub backup not configured)"
                "FAILED" -> "Saved locally, but failed to push to GitHub"
                else -> "Successfully saved"
            }
        }
    }

    fun uploadProblemDocument(folderName: String, filename: String, bytes: ByteArray) {
        viewModelScope.launch {
            val savedNote = problemRepo.saveProblemDocument(folderName, filename, bytes)
            val updated = problemRepo.getProblemDetail(folderName)
            if (updated != null) {
                _selectedProblemDetail.value = updated
                val idx = updated.notes.indexOfFirst { it.filename == savedNote.filename }
                if (idx != -1) _activeProblemNoteIndex.value = idx
            }
            loadProblems()
            _syncMessage.value = when (savedNote.syncStatus) {
                "SYNCED" -> "Uploaded & backed up to GitHub"
                "LOCAL_ONLY" -> "Uploaded locally (GitHub backup not configured)"
                "FAILED" -> "Uploaded locally, but failed to push to GitHub"
                else -> "Uploaded document '${savedNote.filename}'"
            }
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

    fun onProblemsTokenInputChanged(token: String) {
        _problemsTokenInput.value = token
    }

    fun saveBackupRepo(repo: String) {
        val trimmed = repo.trim()
        _backupRepoInput.value = trimmed
        repository.setGitHubBackupRepo(trimmed)
        repository.setGitHubProblemsToken(_problemsTokenInput.value.trim())
        _syncMessage.value = "Problems repository & token saved ('$trimmed')"
        if (trimmed.isNotBlank()) {
            syncFromGitHubBackupRepo()
        }
    }

    fun syncFromGitHubBackupRepo() {
        val repo = _backupRepoInput.value.trim().ifBlank { repository.getGitHubBackupRepo().ifBlank { "leetcode" } }
        val owner = _gitHubInput.value.trim().removePrefix("@")
        val token = _problemsTokenInput.value.trim().ifBlank { _gitHubTokenInput.value.trim() }

        val targetOwner = if (repo.contains("/")) repo.substringBefore("/").trim() else owner.ifBlank { "sangrechy" }
        val targetRepo = if (repo.contains("/")) repo.substringAfter("/").trim() else repo

        viewModelScope.launch {
            _isSyncingBackupRepo.value = true
            _syncMessage.value = "Syncing with GitHub ($targetOwner/$targetRepo)..."
            val result = problemRepo.syncFromGitHubBackupRepo(targetOwner, targetRepo, token)
            _isSyncingBackupRepo.value = false
            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                _syncMessage.value = "Successfully synced $count files with $targetOwner/$targetRepo!"
                loadProblems()
            } else {
                _syncMessage.value = "Git sync failed: ${result.exceptionOrNull()?.message ?: "Check repository name or permissions"}"
            }
        }
    }

    fun syncSingleProblemFromGitHub(folderName: String, silent: Boolean = false) {
        viewModelScope.launch {
            _isSyncingBackupRepo.value = true
            if (!silent) {
                _syncMessage.value = "Syncing code and notes for $folderName from GitHub..."
            }
            val result = problemRepo.syncSingleProblemFromGitHub(folderName)
            _isSyncingBackupRepo.value = false
            if (result.isSuccess) {
                val updated = result.getOrNull()
                if (updated != null && _currentScreen.value == AppScreen.PROBLEMS && _selectedProblemDetail.value?.folderName == folderName) {
                    _selectedProblemDetail.value = updated
                }
                loadProblems()
                if (!silent) {
                    _syncMessage.value = "Synced code and notes from GitHub!"
                }
            } else if (!silent) {
                _syncMessage.value = "Sync failed: ${result.exceptionOrNull()?.message ?: "Check repository or network"}"
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
            _syncMessage.value = when (saved.syncStatus) {
                "SYNCED" -> "Successfully saved to GitHub & device"
                "LOCAL_ONLY" -> "Saved locally (GitHub backup not configured)"
                "FAILED" -> "Saved locally, but failed to push to GitHub"
                else -> "Successfully saved"
            }
        }
    }

    fun uploadGeneralDocument(filename: String, bytes: ByteArray) {
        viewModelScope.launch {
            val saved = problemRepo.saveGeneralDocument(filename, bytes)
            loadGeneralNotes()
            _selectedGeneralNote.value = saved
            _syncMessage.value = when (saved.syncStatus) {
                "SYNCED" -> "Uploaded & backed up to GitHub"
                "LOCAL_ONLY" -> "Uploaded locally (GitHub backup not configured)"
                "FAILED" -> "Uploaded locally, but failed to push to GitHub"
                else -> "Uploaded document '$filename'"
            }
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

    fun deleteMultipleGeneralNotes(ids: List<String>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { id ->
                problemRepo.deleteGeneralNote(id)
                if (_selectedGeneralNote.value?.id == id) {
                    _selectedGeneralNote.value = null
                }
            }
            loadGeneralNotes()
        }
    }

    // --- Wallpaper Actions ---

    fun setAppWallpaperDefault() {
        repository.setAppWallpaperType("NONE")
        _appWallpaperType.value = "NONE"
    }

    fun setAppWallpaperPreset(presetId: String) {
        repository.setAppWallpaperType("PRESET")
        repository.setAppWallpaperPreset(presetId)
        _appWallpaperType.value = "PRESET"
        _appWallpaperPreset.value = presetId
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
    }

    fun getWidgetWallpaperTypeFor(widgetKey: String): String {
        val norm = widgetKey.trim().uppercase()
        return _widgetSpecificWallpapers.value[norm] ?: repository.getWidgetWallpaperTypeFor(widgetKey)
    }

    fun setWidgetWallpaperTypeFor(widgetKey: String, type: String) {
        val norm = widgetKey.trim().uppercase()
        repository.setWidgetWallpaperTypeFor(widgetKey, type)
        _widgetSpecificWallpapers.value = _widgetSpecificWallpapers.value + (norm to type)
        applyWidgetWallpaperToAllWidgets()
    }

    fun getWidgetElementOffsetX(widgetKey: String): Float = repository.getWidgetElementOffsetX(widgetKey)
    fun getWidgetElementOffsetY(widgetKey: String): Float = repository.getWidgetElementOffsetY(widgetKey)
    fun getWidgetElementRatioX(widgetKey: String): Float = repository.getWidgetElementRatioX(widgetKey)
    fun getWidgetElementRatioY(widgetKey: String): Float = repository.getWidgetElementRatioY(widgetKey)
    fun setWidgetElementOffset(widgetKey: String, dx: Float, dy: Float, ratioX: Float? = null, ratioY: Float? = null) {
        repository.setWidgetElementOffset(widgetKey, dx, dy, ratioX, ratioY)
        applyWidgetWallpaperToAllWidgets()
    }
    fun getWidgetOuterDivSize(widgetKey: String): String = repository.getWidgetOuterDivSize(widgetKey)
    fun setWidgetOuterDivSize(widgetKey: String, size: String) {
        repository.setWidgetOuterDivSize(widgetKey, size)
        applyWidgetWallpaperToAllWidgets()
    }
    fun getWidgetInsideDivColor(widgetKey: String): String = repository.getWidgetInsideDivColor(widgetKey)
    fun setWidgetInsideDivColor(widgetKey: String, color: String) {
        repository.setWidgetInsideDivColor(widgetKey, color)
        applyWidgetWallpaperToAllWidgets()
    }
    fun getWidgetShowHeading(widgetKey: String): Boolean = repository.getWidgetShowHeading(widgetKey)
    fun setWidgetShowHeading(widgetKey: String, show: Boolean) {
        repository.setWidgetShowHeading(widgetKey, show)
        applyWidgetWallpaperToAllWidgets()
    }
    fun getWidgetOrderReversed(widgetKey: String): Boolean = repository.getWidgetOrderReversed(widgetKey)
    fun setWidgetOrderReversed(widgetKey: String, reversed: Boolean) {
        repository.setWidgetOrderReversed(widgetKey, reversed)
        applyWidgetWallpaperToAllWidgets()
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

    // =========================================================================
    // NOTES MENU (res/ Hierarchical File Manager + Separate GitHub Repo & Token)
    // =========================================================================

    private fun resolveNotesOwnerAndRepo(): Pair<String, String> {
        val rawRepo = _notesRepoInput.value.trim().ifBlank { repository.getGitHubNotesRepo().ifBlank { "notes" } }
        val commonUser = _gitHubInput.value.trim().removePrefix("@")
        val owner = if (rawRepo.contains("/")) rawRepo.substringBefore("/").trim() else commonUser
        val repo = if (rawRepo.contains("/")) rawRepo.substringAfter("/").trim() else rawRepo
        return owner to repo
    }

    private fun resolveNotesToken(): String = _notesTokenInput.value.trim()

    fun onNotesRepoInputChanged(value: String) {
        _notesRepoInput.value = value
    }

    fun onNotesTokenInputChanged(value: String) {
        _notesTokenInput.value = value
    }

    fun saveNotesRepoConfig(repo: String = _notesRepoInput.value, token: String = _notesTokenInput.value) {
        val cleanRepo = repo.trim().ifBlank { "notes" }
        val cleanToken = token.trim()
        _notesRepoInput.value = cleanRepo
        _notesTokenInput.value = cleanToken
        repository.setGitHubNotesRepo(cleanRepo)
        repository.setGitHubNotesToken(cleanToken)
        _syncMessage.value = "Notes repository & token saved ('$cleanRepo')"
        syncNotesFromGitHub()
    }

    fun setNotesSearchQuery(query: String) {
        _notesSearchQuery.value = query
    }

    fun loadNotesEntries(path: String = _notesCurrentPath.value) {
        val cleanPath = path.trim('/')
        _notesCurrentPath.value = cleanPath
        viewModelScope.launch {
            _notesEntries.value = notesRepo.getEntries(cleanPath)
        }
    }

    fun navigateNotesTo(path: String) {
        _notesSearchQuery.value = ""
        _openedNoteFile.value = null
        loadNotesEntries(path)
    }

    fun navigateNotesUp(): Boolean {
        if (_openedNoteFile.value != null) {
            _openedNoteFile.value = null
            return true
        }
        val current = _notesCurrentPath.value.trim('/')
        if (current.isEmpty()) return false
        val parent = current.substringBeforeLast('/', "")
        navigateNotesTo(parent)
        return true
    }

    fun syncNotesFromGitHub() {
        val (owner, repo) = resolveNotesOwnerAndRepo()
        val token = resolveNotesToken()
        if (owner.isBlank() || repo.isBlank()) {
            _syncMessage.value = "Set your GitHub username and Notes repo in Settings first"
            return
        }
        viewModelScope.launch {
            _isSyncingNotes.value = true
            _syncMessage.value = "Syncing Notes ($owner/$repo · res/)..."
            val result = notesRepo.syncFromGitHub(owner, repo, token)
            _isSyncingNotes.value = false
            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                _syncMessage.value = "Synced $count files from $owner/$repo/res"
                loadNotesEntries(_notesCurrentPath.value)
            } else {
                _syncMessage.value = "Notes sync failed: ${result.exceptionOrNull()?.message ?: "Check repo & token"}"
            }
        }
    }

    fun openNoteFile(entry: NoteTreeEntry) {
        if (entry.isFolder) {
            navigateNotesTo(entry.relativePath)
            return
        }
        val (owner, repo) = resolveNotesOwnerAndRepo()
        val token = resolveNotesToken()
        viewModelScope.launch {
            _isLoadingNoteFile.value = true
            val result = notesRepo.openFile(entry.relativePath, owner, repo, token)
            _isLoadingNoteFile.value = false
            if (result.isSuccess) {
                _openedNoteFile.value = result.getOrNull()
                loadNotesEntries(_notesCurrentPath.value)
            } else {
                _syncMessage.value = "Failed to open '${entry.name}': ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun closeNoteFile() {
        _openedNoteFile.value = null
    }

    fun saveOpenedNoteFile(newContent: String) {
        val current = _openedNoteFile.value ?: return
        val (owner, repo) = resolveNotesOwnerAndRepo()
        val token = resolveNotesToken()
        viewModelScope.launch {
            _isSavingNoteFile.value = true
            val result = notesRepo.saveTextFile(current.entry.relativePath, newContent, owner, repo, token)
            _isSavingNoteFile.value = false
            if (result.isSuccess) {
                _openedNoteFile.value = result.getOrNull()
                _syncMessage.value = if (token.isNotBlank()) "Saved & pushed '${current.entry.name}' to GitHub" else "Saved '${current.entry.name}' locally"
                loadNotesEntries(_notesCurrentPath.value)
            } else {
                _openedNoteFile.value = current.copy(textContent = newContent)
                _syncMessage.value = result.exceptionOrNull()?.message ?: "Saved locally"
                loadNotesEntries(_notesCurrentPath.value)
            }
        }
    }

    fun createNoteFolder(folderName: String) {
        val (owner, repo) = resolveNotesOwnerAndRepo()
        val token = resolveNotesToken()
        viewModelScope.launch {
            val result = notesRepo.createFolder(_notesCurrentPath.value, folderName, owner, repo, token)
            if (result.isSuccess) {
                _syncMessage.value = "Created folder '$folderName'"
                loadNotesEntries(_notesCurrentPath.value)
            } else {
                _syncMessage.value = "Could not create folder: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun createMarkdownNote(filename: String, initialContent: String = "") {
        val trimmed = filename.trim()
        if (trimmed.isBlank()) return
        val finalName = if (trimmed.contains('.')) trimmed else "$trimmed.md"
        val parent = _notesCurrentPath.value.trim('/')
        val relPath = if (parent.isEmpty()) finalName else "$parent/$finalName"
        val (owner, repo) = resolveNotesOwnerAndRepo()
        val token = resolveNotesToken()
        val content = initialContent.ifBlank { "# ${finalName.substringBeforeLast('.')}\n\n" }
        viewModelScope.launch {
            _isSavingNoteFile.value = true
            val result = notesRepo.saveTextFile(relPath, content, owner, repo, token)
            _isSavingNoteFile.value = false
            if (result.isSuccess) {
                _openedNoteFile.value = result.getOrNull()
                _syncMessage.value = "Created '$finalName'"
                loadNotesEntries(_notesCurrentPath.value)
            } else {
                _syncMessage.value = result.exceptionOrNull()?.message ?: "Created locally"
                loadNotesEntries(_notesCurrentPath.value)
            }
        }
    }

    fun uploadNoteDocument(filename: String, bytes: ByteArray) {
        val (owner, repo) = resolveNotesOwnerAndRepo()
        val token = resolveNotesToken()
        viewModelScope.launch {
            _isSyncingNotes.value = true
            val result = notesRepo.uploadFile(_notesCurrentPath.value, filename, bytes, owner, repo, token)
            _isSyncingNotes.value = false
            if (result.isSuccess) {
                _syncMessage.value = "Uploaded '$filename'"
                loadNotesEntries(_notesCurrentPath.value)
            } else {
                _syncMessage.value = result.exceptionOrNull()?.message ?: "Upload failed"
                loadNotesEntries(_notesCurrentPath.value)
            }
        }
    }

    fun deleteNoteEntry(entry: NoteTreeEntry) {
        val (owner, repo) = resolveNotesOwnerAndRepo()
        val token = resolveNotesToken()
        viewModelScope.launch {
            val result = notesRepo.deleteEntry(entry.relativePath, entry.isFolder, owner, repo, token)
            if (result.isSuccess) {
                if (_openedNoteFile.value?.entry?.relativePath == entry.relativePath) {
                    _openedNoteFile.value = null
                }
                _syncMessage.value = "Deleted '${entry.name}'"
                loadNotesEntries(_notesCurrentPath.value)
            } else {
                _syncMessage.value = "Delete failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun dismissMessage() {
        _syncMessage.value = null
    }
}
