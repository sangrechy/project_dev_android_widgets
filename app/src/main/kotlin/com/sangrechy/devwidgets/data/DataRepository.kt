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
import com.sangrechy.devwidgets.data.model.GitHubStats
import com.sangrechy.devwidgets.data.model.LeetCodeStats

interface DataRepository {
    val dashboardData: Flow<WidgetDashboardData>
    fun getSavedUsernames(): Triple<String, String, String> // leetCode, gitHub, token
    suspend fun saveUsernames(leetCode: String, gitHub: String, token: String)
    suspend fun verifyLeetCode(username: String): LeetCodeStats
    suspend fun verifyGitHub(username: String, token: String = ""): GitHubStats
    suspend fun syncData(): Result<WidgetDashboardData>
    fun loadSampleData()
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
    }
}
