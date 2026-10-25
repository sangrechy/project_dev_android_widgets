package com.sangrechy.devwidgets.widget

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sangrechy.devwidgets.data.WidgetPreferences
import com.sangrechy.devwidgets.data.model.AccountStatus
import com.sangrechy.devwidgets.data.model.GitHubStats
import com.sangrechy.devwidgets.data.model.LeetCodeStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented blackbox test suite validating bitmap rendering engines,
 * edge cases (zeros, large values, aspect ratios), and widget preferences.
 */
@RunWith(AndroidJUnit4::class)
class WidgetBitmapRendererTest {

    @Test
    fun renderLeetCodeCircle_zeroSolved_rendersValidBitmap() {
        val stats = LeetCodeStats(
            username = "test_user",
            totalSolved = 0,
            easySolved = 0,
            mediumSolved = 0,
            hardSolved = 0,
            status = AccountStatus.CONNECTED
        )
        val bitmap = WidgetBitmapRenderer.renderLeetCodeCircle(stats, sizePx = 360)
        assertNotNull(bitmap)
        assertEquals(360, bitmap.width)
        assertEquals(360, bitmap.height)
    }

    @Test
    fun renderLeetCodeCircle_extremeStats_rendersWithoutCrash() {
        val stats = LeetCodeStats(
            username = "grandmaster",
            totalSolved = 3450,
            easySolved = 800,
            mediumSolved = 1850,
            hardSolved = 800,
            status = AccountStatus.CONNECTED
        )
        val bitmap = WidgetBitmapRenderer.renderLeetCodeCircle(stats, sizePx = 200)
        assertNotNull(bitmap)
        assertEquals(200, bitmap.width)
        assertEquals(200, bitmap.height)
    }

    @Test
    fun renderGitHubTypeMap_allZeros_rendersValidBitmap() {
        val stats = GitHubStats(
            username = "ghost",
            totalContributions = 0,
            commitsCount = 0,
            pullRequestsCount = 0,
            issuesCount = 0,
            reviewsCount = 0,
            status = AccountStatus.CONNECTED
        )
        val bitmap = WidgetBitmapRenderer.renderGitHubTypeMap(stats, sizePx = 360)
        assertNotNull(bitmap)
        assertEquals(360, bitmap.width)
        assertEquals(360, bitmap.height)
    }

    @Test
    fun renderGitHubTypeMap_highActivity_rendersValidBitmap() {
        val stats = GitHubStats(
            username = "superdev",
            totalContributions = 4500,
            commitsCount = 3500,
            pullRequestsCount = 600,
            issuesCount = 250,
            reviewsCount = 150,
            status = AccountStatus.CONNECTED
        )
        val bitmap = WidgetBitmapRenderer.renderGitHubTypeMap(stats, sizePx = 300)
        assertNotNull(bitmap)
        assertEquals(300, bitmap.width)
        assertEquals(300, bitmap.height)
    }

    @Test
    fun renderHeatmap_emptyMap_rendersWithoutCrash() {
        val bitmap = WidgetBitmapRenderer.renderHeatmap(
            countsByDateOrTimestamp = emptyMap<String, Int>(),
            isLeetCode = true,
            widthPx = 360,
            heightPx = 410,
            weeksCount = 52
        )
        assertNotNull(bitmap)
        assertEquals(360, bitmap.width)
        assertEquals(410, bitmap.height)
    }

    @Test
    fun renderHeatmap_multipleAspectRatios_rendersSuccessfully() {
        val sampleData = mapOf(
            "2026-01-01" to 5,
            "2026-03-15" to 12,
            "2026-06-20" to 8,
            "2026-09-10" to 15
        )

        // 1-tier (very wide)
        val b1 = WidgetBitmapRenderer.renderHeatmap(sampleData, isLeetCode = false, widthPx = 700, heightPx = 150)
        assertNotNull(b1)
        assertEquals(700, b1.width)

        // 2-tier
        val b2 = WidgetBitmapRenderer.renderHeatmap(sampleData, isLeetCode = true, widthPx = 480, heightPx = 250)
        assertNotNull(b2)
        assertEquals(480, b2.width)

        // 3-tier (standard)
        val b3 = WidgetBitmapRenderer.renderHeatmap(sampleData, isLeetCode = false, widthPx = 360, heightPx = 410)
        assertNotNull(b3)
        assertEquals(360, b3.width)

        // 4-tier (tall)
        val b4 = WidgetBitmapRenderer.renderHeatmap(sampleData, isLeetCode = true, widthPx = 240, heightPx = 450)
        assertNotNull(b4)
        assertEquals(240, b4.width)
    }

    @Test
    fun widgetPreferences_persistenceAndRoundTrip_succeeds() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = WidgetPreferences(context)

        val sample = prefs.getSampleDashboardData()
        assertNotNull(sample)
        assertTrue(sample.leetCode.totalSolved > 0)
        assertTrue(sample.gitHub.totalContributions > 0)

        prefs.saveDashboardData(sample)
        val retrieved = prefs.getDashboardData()
        assertNotNull(retrieved)
        assertEquals(sample.leetCode.totalSolved, retrieved.leetCode.totalSolved)
        assertEquals(sample.gitHub.totalContributions, retrieved.gitHub.totalContributions)
    }
}
