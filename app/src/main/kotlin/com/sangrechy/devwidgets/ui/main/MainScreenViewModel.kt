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

enum class AppScreen(val title: String) {
    HOME("Home"),
    WIDGETS("Widgets"),
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
        val user = _leetCodeInput.value.trim()
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
        val user = _gitHubInput.value.trim()
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
                leetCode = _leetCodeInput.value.trim(),
                gitHub = _gitHubInput.value.trim(),
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
                    _syncMessage.value = "You are on the latest version (${BuildConfig.VERSION_NAME})!"
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

    fun dismissMessage() {
        _syncMessage.value = null
    }
}
