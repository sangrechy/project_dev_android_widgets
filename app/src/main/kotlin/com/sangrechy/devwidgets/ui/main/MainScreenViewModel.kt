package com.sangrechy.devwidgets.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sangrechy.devwidgets.data.DataRepository
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

    fun saveAndSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "Saving usernames and syncing live data..."
            repository.saveUsernames(
                leetCode = _leetCodeInput.value.trim(),
                gitHub = _gitHubInput.value.trim(),
                token = _gitHubTokenInput.value.trim()
            )
            val result = repository.syncData()
            _isSyncing.value = false
            if (result.isSuccess) {
                _syncMessage.value = "Synced successfully! Widget updated."
            } else {
                _syncMessage.value = "Sync completed with offline/cached fallback."
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
