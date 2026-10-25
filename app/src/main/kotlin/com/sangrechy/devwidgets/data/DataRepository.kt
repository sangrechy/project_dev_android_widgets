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

interface DataRepository {
    val dashboardData: Flow<WidgetDashboardData>
    fun getSavedUsernames(): Triple<String, String, String> // leetCode, gitHub, token
    suspend fun saveUsernames(leetCode: String, gitHub: String, token: String)
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
        prefs.leetCodeUsername = leetCode
        prefs.gitHubUsername = gitHub
        prefs.gitHubToken = token
    }

    override suspend fun syncData(): Result<WidgetDashboardData> = withContext(Dispatchers.IO) {
        val lcUser = prefs.leetCodeUsername
        val ghUser = prefs.gitHubUsername
        val ghToken = prefs.gitHubToken

        if (lcUser.isBlank() && ghUser.isBlank()) {
            val sample = prefs.getSampleDashboardData()
            _dashboardData.value = sample
            return@withContext Result.success(sample)
        }

        try {
            val (lcStats, ghStats) = coroutineScope {
                val lcDeferred = async {
                    if (lcUser.isNotBlank()) apiService.fetchLeetCodeStats(lcUser)
                    else prefs.getSampleDashboardData().leetCode
                }
                val ghDeferred = async {
                    if (ghUser.isNotBlank()) apiService.fetchGitHubStats(ghUser, ghToken)
                    else prefs.getSampleDashboardData().gitHub
                }
                Pair(lcDeferred.await(), ghDeferred.await())
            }

            // Fallback to sample if API returned zeroed-out results
            val finalLc = if (lcStats.totalSolved == 0 && lcStats.recentSubmissions.isEmpty()) {
                prefs.getSampleDashboardData().leetCode.copy(username = lcUser.ifBlank { "dev" })
            } else {
                lcStats
            }

            val finalGh = if (ghStats.totalContributions == 0 && ghStats.recentEvents.isEmpty()) {
                prefs.getSampleDashboardData().gitHub.copy(username = ghUser.ifBlank { "dev" })
            } else {
                ghStats
            }

            val updatedData = WidgetDashboardData(
                leetCode = finalLc,
                gitHub = finalGh,
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
