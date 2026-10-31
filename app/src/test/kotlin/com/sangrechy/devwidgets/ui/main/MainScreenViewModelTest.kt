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

    @Test
    fun wallpaperSettings_updateStatesAndRepository() = runTest {
        val fakeRepo = FakeDataRepository()
        val viewModel = MainScreenViewModel(fakeRepo)

        // App wallpaper preset
        viewModel.setAppWallpaperPreset("CYBER_MATRIX")
        assertEquals("PRESET", viewModel.appWallpaperType.value)
        assertEquals("CYBER_MATRIX", viewModel.appWallpaperPreset.value)
        assertEquals("PRESET", fakeRepo.getAppWallpaperType())
        assertEquals("CYBER_MATRIX", fakeRepo.getAppWallpaperPreset())

        // App wallpaper dim
        viewModel.setAppWallpaperDim(0.75f)
        assertEquals(0.75f, viewModel.appWallpaperDim.value)
        assertEquals(0.75f, fakeRepo.getAppWallpaperDim())

        // App wallpaper reset to obsidian theme
        viewModel.setAppWallpaperDefault()
        assertEquals("NONE", viewModel.appWallpaperType.value)
        assertEquals("NONE", fakeRepo.getAppWallpaperType())

        // Widget wallpaper type and preset
        val initialWidgetCalls = fakeRepo.updateWidgetsCallCount
        viewModel.setWidgetWallpaperType("TRANSLUCENT")
        assertEquals("TRANSLUCENT", viewModel.widgetWallpaperType.value)
        assertEquals("TRANSLUCENT", fakeRepo.getWidgetWallpaperType())
        assertTrue(fakeRepo.updateWidgetsCallCount > initialWidgetCalls)

        val presetWidgetCalls = fakeRepo.updateWidgetsCallCount
        viewModel.setWidgetWallpaperPreset("TERMINAL_CODE")
        assertEquals("TERMINAL_CODE", viewModel.widgetWallpaperPreset.value)
        assertEquals("TERMINAL_CODE", fakeRepo.getWidgetWallpaperPreset())
        assertTrue(fakeRepo.updateWidgetsCallCount > presetWidgetCalls)

        // Widget wallpaper dim
        viewModel.setWidgetWallpaperDim(0.4f)
        assertEquals(0.4f, viewModel.widgetWallpaperDim.value)
        assertEquals(0.4f, fakeRepo.getWidgetWallpaperDim())
    }

    @Test
    fun testSearchLeetCodeAndAddProblemFromLeetCode() = runTest {
        val fakeRepo = FakeDataRepository()
        val viewModel = MainScreenViewModel(fakeRepo)

        viewModel.searchLeetCode("two sum")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.leetCodeSearchResults.value.size)
        assertEquals("1", viewModel.leetCodeSearchResults.value.first().frontendId)
        assertEquals("two-sum", viewModel.leetCodeSearchResults.value.first().titleSlug)

        var completed = false
        viewModel.addProblemFromLeetCode("two-sum") { success: Boolean ->
            completed = success
        }
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(completed)
        assertEquals("1", viewModel.selectedProblemDetail.value?.frontendId)
        assertEquals("Two Sum", viewModel.selectedProblemDetail.value?.title)
        assertTrue(viewModel.leetCodeSearchResults.value.isEmpty())
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
    override suspend fun searchLeetCode(query: String): Result<List<com.sangrechy.devwidgets.data.model.LeetCodeSearchResult>> {
        val list = problems.map {
            com.sangrechy.devwidgets.data.model.LeetCodeSearchResult(
                frontendId = it.frontendId,
                title = it.title,
                titleSlug = it.titleSlug,
                difficulty = it.difficulty,
                topicTags = it.topicTags
            )
        }
        return Result.success(list)
    }
    override suspend fun addProblemFromLeetCode(input: String): Result<com.sangrechy.devwidgets.data.model.OfflineProblem> {
        val p = com.sangrechy.devwidgets.data.model.OfflineProblem(
            folderName = "0001-two-sum",
            frontendId = "1",
            title = "Two Sum",
            titleSlug = "two-sum",
            difficulty = "Easy",
            isDownloadedOffline = true
        )
        problems.add(p)
        return Result.success(p)
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

    private var appWallpaperTypeVal = "NONE"
    private var appWallpaperPresetVal = "OBSIDIAN_CORE"
    private var appWallpaperDimVal = 0.5f
    private var appWallpaperCustomPathVal = ""
    private var widgetWallpaperTypeVal = "DEFAULT"
    private var widgetWallpaperPresetVal = "OBSIDIAN_CORE"
    private var widgetWallpaperDimVal = 0.3f
    private var widgetWallpaperCustomPathVal = ""
    var updateWidgetsCallCount = 0

    override fun getAppWallpaperType(): String = appWallpaperTypeVal
    override fun setAppWallpaperType(type: String) { appWallpaperTypeVal = type }
    override fun getAppWallpaperPreset(): String = appWallpaperPresetVal
    override fun setAppWallpaperPreset(preset: String) { appWallpaperPresetVal = preset }
    override fun getAppWallpaperDim(): Float = appWallpaperDimVal
    override fun setAppWallpaperDim(dim: Float) { appWallpaperDimVal = dim }
    override fun getAppWallpaperCustomPath(): String = appWallpaperCustomPathVal
    override fun setAppWallpaperCustomPath(path: String) { appWallpaperCustomPathVal = path }

    private val widgetSpecificWallpapers = mutableMapOf<String, String>()
    override fun getWidgetWallpaperType(): String = widgetWallpaperTypeVal
    override fun setWidgetWallpaperType(type: String) { widgetWallpaperTypeVal = type }
    override fun getWidgetWallpaperTypeFor(widgetKey: String): String = widgetSpecificWallpapers[widgetKey] ?: widgetWallpaperTypeVal
    override fun setWidgetWallpaperTypeFor(widgetKey: String, type: String) { widgetSpecificWallpapers[widgetKey] = type }
    override fun getWidgetWallpaperPreset(): String = widgetWallpaperPresetVal
    override fun setWidgetWallpaperPreset(preset: String) { widgetWallpaperPresetVal = preset }
    override fun getWidgetWallpaperDim(): Float = widgetWallpaperDimVal
    override fun setWidgetWallpaperDim(dim: Float) { widgetWallpaperDimVal = dim }
    override fun getWidgetWallpaperCustomPath(): String = widgetWallpaperCustomPathVal
    override fun setWidgetWallpaperCustomPath(path: String) { widgetWallpaperCustomPathVal = path }

    override suspend fun saveCustomWallpaperFromUri(uri: android.net.Uri, forWidget: Boolean): String? = "/mock/path.jpg"
    override fun updateAllWidgets() {
        updateWidgetsCallCount++
    }
}

