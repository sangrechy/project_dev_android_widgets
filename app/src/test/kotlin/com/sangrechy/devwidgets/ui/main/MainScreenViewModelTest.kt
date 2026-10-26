package com.sangrechy.devwidgets.ui.main

import com.sangrechy.devwidgets.data.DataRepository
import com.sangrechy.devwidgets.data.model.AppUpdateInfo
import com.sangrechy.devwidgets.data.model.GitHubStats
import com.sangrechy.devwidgets.data.model.LeetCodeStats
import com.sangrechy.devwidgets.data.model.WidgetDashboardData
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun usernameInputs_canBeUpdated() = runTest {
        val fakeRepo = FakeDataRepository()
        val viewModel = MainScreenViewModel(fakeRepo)

        viewModel.onLeetCodeInputChanged("neetcode")
        assertEquals("neetcode", viewModel.leetCodeInput.value)

        viewModel.onGitHubInputChanged("torvalds")
        assertEquals("torvalds", viewModel.gitHubInput.value)

        viewModel.onGitHubTokenChanged("ghp_test123")
        assertEquals("ghp_test123", viewModel.gitHubTokenInput.value)
    }

    @Test
    fun sampleData_loadsSuccessfully() = runTest {
        val fakeRepo = FakeDataRepository()
        val viewModel = MainScreenViewModel(fakeRepo)

        viewModel.useSampleData()
        assertEquals("sangrechy", viewModel.leetCodeInput.value)
        assertEquals("sangrechy", viewModel.gitHubInput.value)
    }

    @Test
    fun screenNavigation_switchesProperly() = runTest {
        val fakeRepo = FakeDataRepository()
        val viewModel = MainScreenViewModel(fakeRepo)

        assertEquals(AppScreen.HOME, viewModel.currentScreen.value)

        viewModel.setScreen(AppScreen.WIDGETS)
        assertEquals(AppScreen.WIDGETS, viewModel.currentScreen.value)

        viewModel.setScreen(AppScreen.SETTINGS)
        assertEquals(AppScreen.SETTINGS, viewModel.currentScreen.value)
    }

    @Test
    fun homeTabs_switchesProperly() = runTest {
        val fakeRepo = FakeDataRepository()
        val viewModel = MainScreenViewModel(fakeRepo)

        assertEquals(HomeTab.LEETCODE, viewModel.currentHomeTab.value)

        viewModel.setHomeTab(HomeTab.GITHUB)
        assertEquals(HomeTab.GITHUB, viewModel.currentHomeTab.value)
    }

    @Test
    fun feedLimit_updatesProperly() = runTest {
        val fakeRepo = FakeDataRepository()
        val viewModel = MainScreenViewModel(fakeRepo)

        assertEquals(FeedLimit.LIMIT_10, viewModel.feedLimit.value)

        viewModel.setFeedLimit(FeedLimit.LIMIT_50)
        assertEquals(FeedLimit.LIMIT_50, viewModel.feedLimit.value)

        viewModel.setFeedLimit(FeedLimit.LIMIT_100)
        assertEquals(FeedLimit.LIMIT_100, viewModel.feedLimit.value)
    }

    @Test
    fun refreshIntervals_contains15OptionsAndCanBeSet() = runTest {
        val fakeRepo = FakeDataRepository()
        val viewModel = MainScreenViewModel(fakeRepo)

        assertEquals(15, MainScreenViewModel.REFRESH_INTERVALS.size)
        assertTrue(MainScreenViewModel.REFRESH_INTERVALS.contains("1s"))
        assertTrue(MainScreenViewModel.REFRESH_INTERVALS.contains("1d"))

        viewModel.setRefreshInterval("10m")
        assertEquals("10m", viewModel.selectedInterval.value)
        assertEquals("10m", fakeRepo.interval)
    }

    @Test
    fun logoutConfirmation_clearsInputsAndReturnsHome() = runTest {
        val fakeRepo = FakeDataRepository()
        val viewModel = MainScreenViewModel(fakeRepo)

        viewModel.onLeetCodeInputChanged("user1")
        viewModel.onGitHubInputChanged("user2")
        viewModel.setScreen(AppScreen.SETTINGS)

        viewModel.showLogoutConfirmation()
        assertTrue(viewModel.showLogoutDialog.value)

        viewModel.confirmLogout()
        assertFalse(viewModel.showLogoutDialog.value)
        assertEquals("", viewModel.leetCodeInput.value)
        assertEquals("", viewModel.gitHubInput.value)
        assertEquals(AppScreen.HOME, viewModel.currentScreen.value)
        assertTrue(fakeRepo.isLoggedOut)
    }
}

private class FakeDataRepository : DataRepository {
    override val dashboardData: Flow<WidgetDashboardData> = flowOf(WidgetDashboardData())
    var interval = "1h"
    var ignoredVersion = ""
    var isLoggedOut = false

    override fun getSavedUsernames(): Triple<String, String, String> {
        return Triple("", "", "")
    }

    override suspend fun saveUsernames(leetCode: String, gitHub: String, token: String) {}

    override suspend fun verifyLeetCode(username: String): LeetCodeStats {
        return LeetCodeStats(username = username)
    }

    override suspend fun verifyGitHub(username: String, token: String): GitHubStats {
        return GitHubStats(username = username)
    }

    override suspend fun syncData(): Result<WidgetDashboardData> {
        return Result.success(WidgetDashboardData())
    }

    override fun loadSampleData() {}

    override suspend fun checkForUpdates(): AppUpdateInfo {
        return AppUpdateInfo(hasUpdate = false, latestVersion = "1.1.0")
    }

    override fun getIgnoredUpdateVersion(): String = ignoredVersion

    override fun setIgnoredUpdateVersion(version: String) {
        ignoredVersion = version
    }

    override fun getRefreshInterval(): String = interval

    override fun setRefreshInterval(interval: String) {
        this.interval = interval
    }

    override suspend fun logoutAll() {
        isLoggedOut = true
    }
}
