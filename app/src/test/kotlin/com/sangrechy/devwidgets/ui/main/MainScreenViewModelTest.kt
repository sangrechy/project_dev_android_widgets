package com.sangrechy.devwidgets.ui.main

import com.sangrechy.devwidgets.data.DataRepository
import com.sangrechy.devwidgets.data.model.WidgetDashboardData
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MainScreenViewModelTest {

    @Test
    fun usernameInputs_canBeUpdated() = runTest {
        val fakeRepo = FakeDataRepository()
        val viewModel = MainScreenViewModel(fakeRepo)

        viewModel.onLeetCodeInputChanged("neetcode")
        assertEquals("neetcode", viewModel.leetCodeInput.value)

        viewModel.onGitHubInputChanged("torvalds")
        assertEquals("torvalds", viewModel.gitHubInput.value)
    }

    @Test
    fun sampleData_loadsSuccessfully() = runTest {
        val fakeRepo = FakeDataRepository()
        val viewModel = MainScreenViewModel(fakeRepo)

        viewModel.useSampleData()
        assertEquals("sangrechy", viewModel.leetCodeInput.value)
        assertEquals("sangrechy", viewModel.gitHubInput.value)
    }
}

private class FakeDataRepository : DataRepository {
    override val dashboardData: Flow<WidgetDashboardData> = flowOf(WidgetDashboardData())

    override fun getSavedUsernames(): Triple<String, String, String> {
        return Triple("", "", "")
    }

    override suspend fun saveUsernames(leetCode: String, gitHub: String, token: String) {}

    override suspend fun syncData(): Result<WidgetDashboardData> {
        return Result.success(WidgetDashboardData())
    }

    override fun loadSampleData() {}
}
