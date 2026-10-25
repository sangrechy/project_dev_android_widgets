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

class MainScreenViewModel(private val repository: DataRepository) : ViewModel() {

    private val saved = repository.getSavedUsernames()

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

    fun dismissMessage() {
        _syncMessage.value = null
    }
}
