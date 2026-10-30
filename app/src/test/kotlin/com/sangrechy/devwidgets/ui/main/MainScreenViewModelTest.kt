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
    fun heatmapOrientation_canBeToggled() = runTest {
        val fakeRepo = FakeDataRepository()
        val viewModel = MainScreenViewModel(fakeRepo)

        assertEquals("horizontal", viewModel.heatmapOrientation.value)

        viewModel.setHeatmapOrientation("vertical")
        assertEquals("vertical", viewModel.heatmapOrientation.value)
        assertEquals("vertical", fakeRepo.orientation)
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

    @Test
    fun usernameInputs_stripLeadingAtSymbolOnVerify() = runTest {
        val fakeRepo = FakeDataRepository()
        val viewModel = MainScreenViewModel(fakeRepo)

        viewModel.onLeetCodeInputChanged("@sangrechy")
        viewModel.onGitHubInputChanged("@sangrechy")

        viewModel.verifyLeetCode()
        assertEquals("sangrechy", fakeRepo.lastVerifiedLeetCode)

        viewModel.verifyGitHub()
        assertEquals("sangrechy", fakeRepo.lastVerifiedGitHub)
    }

    @Test
    fun testProblemsNavigationAndState() = runTest {
        val fakeRepo = FakeDataRepository()
        val viewModel = MainScreenViewModel(fakeRepo)

        viewModel.setScreen(AppScreen.PROBLEMS)
        assertEquals(AppScreen.PROBLEMS, viewModel.currentScreen.value)

        viewModel.setProblemsPlatform(com.sangrechy.devwidgets.data.model.ProblemsPlatform.LEETCODE)
        assertEquals(com.sangrechy.devwidgets.data.model.ProblemsPlatform.LEETCODE, viewModel.problemsPlatform.value)

        viewModel.setProblemsSubTab(com.sangrechy.devwidgets.data.model.ProblemsSubTab.NOTES)
        assertEquals(com.sangrechy.devwidgets.data.model.ProblemsSubTab.NOTES, viewModel.problemsSubTab.value)

        viewModel.setProblemsSearchQuery("Two Sum")
        assertEquals("Two Sum", viewModel.problemsSearchQuery.value)

        viewModel.setDifficultyFilter(com.sangrechy.devwidgets.data.model.ProblemDifficultyFilter.EASY)
        assertEquals(com.sangrechy.devwidgets.data.model.ProblemDifficultyFilter.EASY, viewModel.difficultyFilter.value)
    }

    @Test
    fun testProblemSelectionAndNotes() = runTest {
        val fakeRepo = FakeDataRepository()
        val viewModel = MainScreenViewModel(fakeRepo)

        val problem = fakeRepo.problemRepository.getProblems().first()
        viewModel.selectProblem(problem)
        assertEquals(problem.folderName, viewModel.selectedProblemDetail.value?.folderName)

        // Save a note
        viewModel.saveProblemNote(problem.folderName, "approach2.md", "Two Pointers solution")
        assertEquals(2, viewModel.selectedProblemDetail.value?.notes?.size)

        // Save general note
        val generalNote = com.sangrechy.devwidgets.data.model.CustomNote(
            id = "test-note",
            title = "Test Cheatsheet",
            content = "Binary search details",
            category = "General",
            lastModifiedMillis = 1000L
        )
        viewModel.saveGeneralNote(generalNote)
        assertEquals("Test Cheatsheet", viewModel.selectedGeneralNote.value?.title)

        // Update backup repository
        viewModel.saveBackupRepo("custom-leetcode-backup")
        assertEquals("custom-leetcode-backup", viewModel.backupRepoInput.value)
        assertEquals("custom-leetcode-backup", fakeRepo.getGitHubBackupRepo())
    }
}

private class FakeProblemRepository : com.sangrechy.devwidgets.data.ProblemRepository {
    val problems = mutableListOf(
        com.sangrechy.devwidgets.data.model.OfflineProblem(
            folderName = "0001-two-sum",
            frontendId = "1",
            title = "Two Sum",
            titleSlug = "two-sum",
            difficulty = "Easy",
            contentHtml = "<p>Two sum description</p>",
            topicTags = listOf("Array", "Hash Table"),
            hints = listOf("Use hash map"),
            solutionCode = "class Solution: pass",
            solutionLanguage = "python3",
            notes = listOf(
                com.sangrechy.devwidgets.data.model.ProblemNote(
                    filename = "intuition.md",
                    title = "Intuition",
                    content = "Hash map",
                    lastModifiedMillis = 1000L
                )
            ),
            isDownloadedOffline = true
        )
    )

    val generalNotes = mutableListOf(
        com.sangrechy.devwidgets.data.model.CustomNote(
            id = "note-1",
            title = "Algorithm Patterns",
            content = "# Algorithm Patterns",
            category = "General",
            lastModifiedMillis = 1000L
        )
    )

    override suspend fun getProblems(): List<com.sangrechy.devwidgets.data.model.OfflineProblem> = problems
    override suspend fun getProblemDetail(folderName: String): com.sangrechy.devwidgets.data.model.OfflineProblem? =
        problems.find { it.folderName == folderName }
    override suspend fun downloadProblemOffline(folderName: String, titleSlug: String): Result<com.sangrechy.devwidgets.data.model.OfflineProblem> {
        val found = problems.find { it.folderName == folderName }
            ?: com.sangrechy.devwidgets.data.model.OfflineProblem(
                folderName = folderName,
                frontendId = "1",
                title = "Title",
                titleSlug = titleSlug,
                difficulty = "Easy"
            ).also { problems.add(it) }
        val updated = found.copy(isDownloadedOffline = true)
        return Result.success(updated)
    }
    override suspend fun deleteProblemOffline(folderName: String): Boolean =
        problems.removeIf { it.folderName == folderName }
    override suspend fun saveProblemNote(folderName: String, filename: String, content: String): com.sangrechy.devwidgets.data.model.ProblemNote {
        val note = com.sangrechy.devwidgets.data.model.ProblemNote(filename, filename, content, 2000L)
        val p = problems.find { it.folderName == folderName }
        if (p != null) {
            val updatedNotes = p.notes.filterNot { it.filename == filename } + note
            val idx = problems.indexOf(p)
            problems[idx] = p.copy(notes = updatedNotes)
        }
        return note
    }
    override suspend fun deleteProblemNote(folderName: String, filename: String): Boolean {
        val p = problems.find { it.folderName == folderName } ?: return false
        val idx = problems.indexOf(p)
        problems[idx] = p.copy(notes = p.notes.filterNot { it.filename == filename })
        return true
    }
    override suspend fun syncFromGitHubBackupRepo(owner: String, repo: String, token: String): Result<Int> = Result.success(5)
    override suspend fun getGeneralNotes(): List<com.sangrechy.devwidgets.data.model.CustomNote> = generalNotes
    override suspend fun saveGeneralNote(note: com.sangrechy.devwidgets.data.model.CustomNote): com.sangrechy.devwidgets.data.model.CustomNote {
        generalNotes.removeIf { it.id == note.id }
        generalNotes.add(note)
        return note
    }
    override suspend fun deleteGeneralNote(id: String): Boolean = generalNotes.removeIf { it.id == id }
}

private class FakeDataRepository : DataRepository {
    override val dashboardData: Flow<WidgetDashboardData> = flowOf(WidgetDashboardData())
    var interval = "1h"
    var orientation = "horizontal"
    var ignoredVersion = ""
    var isLoggedOut = false
    var backupRepo = "project_git_the_leet"

    override val problemRepository: com.sangrechy.devwidgets.data.ProblemRepository = FakeProblemRepository()

    override fun getGitHubBackupRepo(): String = backupRepo

    override fun setGitHubBackupRepo(repo: String) {
        backupRepo = repo
    }

    override fun getSavedUsernames(): Triple<String, String, String> {
        return Triple("", "", "")
    }

    override suspend fun saveUsernames(leetCode: String, gitHub: String, token: String) {}

    var lastVerifiedLeetCode: String? = null
    var lastVerifiedGitHub: String? = null

    override suspend fun verifyLeetCode(username: String): LeetCodeStats {
        lastVerifiedLeetCode = username
        return LeetCodeStats(username = username)
    }

    override suspend fun verifyGitHub(username: String, token: String): GitHubStats {
        lastVerifiedGitHub = username
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

    override fun getHeatmapOrientation(): String = orientation

    override fun setHeatmapOrientation(orientation: String) {
        this.orientation = orientation
    }

    override suspend fun logoutAll() {
        isLoggedOut = true
    }
}
