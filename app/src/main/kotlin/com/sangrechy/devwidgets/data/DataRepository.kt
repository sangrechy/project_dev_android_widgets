package com.sangrechy.devwidgets.data

import android.content.Context
import com.sangrechy.devwidgets.data.model.WidgetDashboardData
import com.sangrechy.devwidgets.data.network.DevApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

import com.sangrechy.devwidgets.data.model.AccountStatus
import com.sangrechy.devwidgets.BuildConfig
import com.sangrechy.devwidgets.data.model.AppUpdateInfo
import com.sangrechy.devwidgets.data.model.GitHubStats
import com.sangrechy.devwidgets.data.model.LeetCodeStats
import com.sangrechy.devwidgets.widget.DevActivityWidgetProvider

interface DataRepository {
    val dashboardData: Flow<WidgetDashboardData>
    fun getSavedUsernames(): Triple<String, String, String> // leetCode, gitHub, token
    suspend fun saveUsernames(leetCode: String, gitHub: String, token: String)
    suspend fun verifyLeetCode(username: String): LeetCodeStats
    suspend fun verifyGitHub(username: String, token: String = ""): GitHubStats
    suspend fun syncData(): Result<WidgetDashboardData>
    fun loadSampleData()
    suspend fun checkForUpdates(): AppUpdateInfo
    fun getIgnoredUpdateVersion(): String
    fun setIgnoredUpdateVersion(version: String)
    fun getRefreshInterval(): String
    fun setRefreshInterval(interval: String)
    fun getHeatmapOrientation(): String
    fun setHeatmapOrientation(orientation: String)
    suspend fun logoutAll()
}

class DefaultDataRepository(private val context: Context) : DataRepository {
    private val prefs = WidgetPreferences(context)
    private val apiService = DevApiService()
    private val _dashboardData = MutableStateFlow(prefs.getDashboardData())
    override val dashboardData: Flow<WidgetDashboardData> = _dashboardData.asStateFlow()

    override fun getSavedUsernames(): Triple<String, String, String> {
        return Triple(prefs.leetCodeUsername, prefs.gitHubUsername, prefs.gitHubToken)
    }

    override suspend fun saveUsernames(leetCode: String, gitHub: String, token: String) {
        prefs.leetCodeUsername = leetCode.trim()
        prefs.gitHubUsername = gitHub.trim()
        prefs.gitHubToken = token.trim()
    }

    override suspend fun verifyLeetCode(username: String): LeetCodeStats = withContext(Dispatchers.IO) {
        val trimmed = username.trim()
        val stats = apiService.fetchLeetCodeStats(trimmed)
        prefs.leetCodeUsername = trimmed
        val current = prefs.getDashboardData()
        val updated = current.copy(
            leetCode = stats,
            lastUpdatedMillis = System.currentTimeMillis()
        )
        prefs.saveDashboardData(updated)
        _dashboardData.value = updated
        com.sangrechy.devwidgets.widget.WidgetManagerHelper.updateAll(context, updated)
        stats
    }

    override suspend fun verifyGitHub(username: String, token: String): GitHubStats = withContext(Dispatchers.IO) {
        val trimmed = username.trim()
        val trimmedToken = token.trim()
        val stats = apiService.fetchGitHubStats(trimmed, trimmedToken)
        prefs.gitHubUsername = trimmed
        prefs.gitHubToken = trimmedToken
        val current = prefs.getDashboardData()
        val updated = current.copy(
            gitHub = stats,
            lastUpdatedMillis = System.currentTimeMillis()
        )
        prefs.saveDashboardData(updated)
        _dashboardData.value = updated
        com.sangrechy.devwidgets.widget.WidgetManagerHelper.updateAll(context, updated)
        stats
    }

    override suspend fun syncData(): Result<WidgetDashboardData> = withContext(Dispatchers.IO) {
        val lcUser = prefs.leetCodeUsername.trim()
        val ghUser = prefs.gitHubUsername.trim()
        val ghToken = prefs.gitHubToken.trim()

        if (lcUser.isBlank() && ghUser.isBlank()) {
            val emptyData = WidgetDashboardData(
                leetCode = LeetCodeStats(status = AccountStatus.NOT_CONFIGURED),
                gitHub = GitHubStats(status = AccountStatus.NOT_CONFIGURED),
                lastUpdatedMillis = System.currentTimeMillis()
            )
            prefs.saveDashboardData(emptyData)
            _dashboardData.value = emptyData
            return@withContext Result.success(emptyData)
        }

        try {
            val (lcStats, ghStats) = coroutineScope {
                val lcDeferred = async {
                    if (lcUser.isNotBlank()) apiService.fetchLeetCodeStats(lcUser)
                    else LeetCodeStats(status = AccountStatus.NOT_CONFIGURED)
                }
                val ghDeferred = async {
                    if (ghUser.isNotBlank()) apiService.fetchGitHubStats(ghUser, ghToken)
                    else GitHubStats(status = AccountStatus.NOT_CONFIGURED)
                }
                Pair(lcDeferred.await(), ghDeferred.await())
            }

            // Real stats used directly without fake sample replacement!
            val updatedData = WidgetDashboardData(
                leetCode = lcStats,
                gitHub = ghStats,
                lastUpdatedMillis = System.currentTimeMillis()
            )

            prefs.saveDashboardData(updatedData)
            _dashboardData.value = updatedData
            com.sangrechy.devwidgets.widget.WidgetManagerHelper.updateAll(context, updatedData)
            Result.success(updatedData)
        } catch (e: Exception) {
            val fallback = prefs.getDashboardData()
            _dashboardData.value = fallback
            Result.failure(e)
        }
    }

    override fun loadSampleData() {
        val sample = prefs.getSampleDashboardData()
        prefs.saveDashboardData(sample)
        _dashboardData.value = sample
        com.sangrechy.devwidgets.widget.WidgetManagerHelper.updateAll(context, sample)
    }

    override suspend fun checkForUpdates(): AppUpdateInfo {
        val currentVer = BuildConfig.VERSION_NAME
        return apiService.checkForAppUpdate(currentVer)
    }

    override fun getIgnoredUpdateVersion(): String {
        return prefs.ignoredUpdateVersion
    }

    override fun setIgnoredUpdateVersion(version: String) {
        prefs.ignoredUpdateVersion = version
    }

    override fun getRefreshInterval(): String {
        return prefs.autoRefreshInterval
    }

    override fun setRefreshInterval(interval: String) {
        prefs.autoRefreshInterval = interval
        com.sangrechy.devwidgets.widget.AutoRefreshScheduler.schedule(context, interval)
    }

    override fun getHeatmapOrientation(): String {
        return prefs.heatmapOrientation
    }

    override fun setHeatmapOrientation(orientation: String) {
        prefs.heatmapOrientation = orientation
        com.sangrechy.devwidgets.widget.WidgetManagerHelper.updateAll(context, _dashboardData.value)
    }

    override suspend fun logoutAll() = withContext(Dispatchers.IO) {
        prefs.clearAll()
        val emptyData = WidgetDashboardData(
            leetCode = LeetCodeStats(status = AccountStatus.NOT_CONFIGURED),
            gitHub = GitHubStats(status = AccountStatus.NOT_CONFIGURED),
            lastUpdatedMillis = System.currentTimeMillis()
        )
        prefs.saveDashboardData(emptyData)
        _dashboardData.value = emptyData
        com.sangrechy.devwidgets.widget.WidgetManagerHelper.updateAll(context, emptyData)
    }
}
