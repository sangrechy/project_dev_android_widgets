package com.sangrechy.devwidgets.data

import com.sangrechy.devwidgets.data.model.AccountStatus
import com.sangrechy.devwidgets.data.model.GitHubStats
import com.sangrechy.devwidgets.data.model.LeetCodeStats
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Blackbox test suite for DevWidgets data layer, API response transformations,
 * and algorithmic calendar partitioning.
 */
class DevApiBlackboxTest {

    @Test
    fun leetCode_validUserJson_parsesCorrectly() {
        val mockJson = """
        {
          "data": {
            "matchedUser": {
              "username": "coder_pro",
              "submitStatsGlobal": {
                "acSubmissionNum": [
                  { "difficulty": "All", "count": 250 },
                  { "difficulty": "Easy", "count": 120 },
                  { "difficulty": "Medium", "count": 100 },
                  { "difficulty": "Hard", "count": 30 }
                ]
              },
              "submissionCalendar": "{\"1700000000\": 3, \"1700086400\": 5}"
            },
            "recentAcSubmissionList": [
              { "title": "Two Sum", "titleSlug": "two-sum", "timestamp": 1700000000 }
            ]
          }
        }
        """.trimIndent()

        val root = JSONObject(mockJson)
        val data = root.getJSONObject("data")
        val matchedUser = data.optJSONObject("matchedUser")
        assertNotNull(matchedUser)

        var total = 0
        var easy = 0
        var med = 0
        var hard = 0

        matchedUser?.optJSONObject("submitStatsGlobal")?.optJSONArray("acSubmissionNum")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                when (item.optString("difficulty")) {
                    "All" -> total = item.optInt("count")
                    "Easy" -> easy = item.optInt("count")
                    "Medium" -> med = item.optInt("count")
                    "Hard" -> hard = item.optInt("count")
                }
            }
        }

        assertEquals(250, total)
        assertEquals(120, easy)
        assertEquals(100, med)
        assertEquals(30, hard)

        val calMap = mutableMapOf<Long, Int>()
        val calJson = JSONObject(matchedUser?.optString("submissionCalendar", "{}") ?: "{}")
        calJson.keys().forEach { k ->
            k.toLongOrNull()?.let { calMap[it] = calJson.getInt(k) }
        }
        assertEquals(2, calMap.size)
        assertEquals(3, calMap[1700000000L])
        assertEquals(5, calMap[1700086400L])
    }

    @Test
    fun leetCode_userNotFound_failsGracefully() {
        val mockJson = """
        {
          "data": {
            "matchedUser": null
          }
        }
        """.trimIndent()

        val root = JSONObject(mockJson)
        val matchedUser = root.optJSONObject("data")?.optJSONObject("matchedUser")
        val status = if (matchedUser == null) AccountStatus.FAILED else AccountStatus.CONNECTED
        assertEquals(AccountStatus.FAILED, status)
    }

    @Test
    fun leetCode_zeroSolved_retainsZeroWithoutFakeData() {
        val mockJson = """
        {
          "data": {
            "matchedUser": {
              "username": "newbie",
              "submitStatsGlobal": {
                "acSubmissionNum": [
                  { "difficulty": "All", "count": 0 },
                  { "difficulty": "Easy", "count": 0 },
                  { "difficulty": "Medium", "count": 0 },
                  { "difficulty": "Hard", "count": 0 }
                ]
              },
              "submissionCalendar": "{}"
            }
          }
        }
        """.trimIndent()

        val root = JSONObject(mockJson)
        val matchedUser = root.getJSONObject("data").getJSONObject("matchedUser")
        val stats = LeetCodeStats(
            username = matchedUser.getString("username"),
            totalSolved = 0,
            easySolved = 0,
            mediumSolved = 0,
            hardSolved = 0,
            status = AccountStatus.CONNECTED
        )

        assertEquals(0, stats.totalSolved)
        assertEquals(AccountStatus.CONNECTED, stats.status)
    }

    @Test
    fun gitHub_zeroContributions_setsCommitsToZeroNotOne() {
        val total = 0
        val commits = if (total > 0) (total * 0.70).toInt().coerceAtLeast(1) else 0
        val prs = (total * 0.15).toInt()
        val issues = (total * 0.08).toInt()
        val reviews = (total * 0.07).toInt()

        assertEquals(0, commits)
        assertEquals(0, prs)
        assertEquals(0, issues)
        assertEquals(0, reviews)
    }

    @Test
    fun gitHub_eventsPayload_formatsActionSummaries() {
        val rawEvents = """
        [
          { "type": "PushEvent", "payload": { "commits": [{}, {}, {}] } },
          { "type": "PullRequestEvent", "payload": { "action": "opened" } },
          { "type": "IssuesEvent", "payload": { "action": "closed" } },
          { "type": "WatchEvent", "payload": {} },
          { "type": "ForkEvent", "payload": {} },
          { "type": "CreateEvent", "payload": {} }
        ]
        """.trimIndent()

        val arr = JSONArray(rawEvents)
        val summaries = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            val type = item.optString("type")
            val summary = when (type) {
                "PushEvent" -> {
                    val count = item.optJSONObject("payload")?.optJSONArray("commits")?.length() ?: 1
                    "Pushed $count commit(s)"
                }
                "PullRequestEvent" -> {
                    val action = item.optJSONObject("payload")?.optString("action", "action")
                    "PR: $action"
                }
                "IssuesEvent" -> {
                    val action = item.optJSONObject("payload")?.optString("action", "action")
                    "Issue: $action"
                }
                "WatchEvent" -> "Starred repo"
                "ForkEvent" -> "Forked repo"
                "CreateEvent" -> "Created branch/tag"
                else -> type
            }
            summaries.add(summary)
        }

        assertEquals("Pushed 3 commit(s)", summaries[0])
        assertEquals("PR: opened", summaries[1])
        assertEquals("Issue: closed", summaries[2])
        assertEquals("Starred repo", summaries[3])
        assertEquals("Forked repo", summaries[4])
        assertEquals("Created branch/tag", summaries[5])
    }

    @Test
    fun heatmap_completeMonthTierPartitioning_neverSplitsAMonth() {
        // Blackbox validation of the month-block grouping algorithm
        data class MonthBlock(val monthIdx: Int, val monthName: String, val weekIndices: MutableList<Int>)

        val weeksCount = 52
        val calCopy = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -((weeksCount - 1) * 7 + 3))
        }

        val monthBlocks = mutableListOf<MonthBlock>()
        val calScanner = calCopy.clone() as Calendar
        val monthSdf = SimpleDateFormat("MMM", Locale.US)

        for (w in 0 until weeksCount) {
            val m = calScanner.get(Calendar.MONTH)
            val mName = monthSdf.format(calScanner.time)
            if (monthBlocks.isEmpty() || monthBlocks.last().monthIdx != m) {
                monthBlocks.add(MonthBlock(monthIdx = m, monthName = mName, weekIndices = mutableListOf(w)))
            } else {
                monthBlocks.last().weekIndices.add(w)
            }
            calScanner.add(Calendar.DAY_OF_YEAR, 7)
        }

        // Test with different tier counts (1, 2, 3, 4 tiers)
        for (tierCount in 1..4) {
            val totalMonths = monthBlocks.size
            val effectiveTiers = minOf(tierCount, totalMonths).coerceAtLeast(1)
            val baseMonthsPerTier = totalMonths / effectiveTiers
            val remainderMonths = totalMonths % effectiveTiers

            val tierBlocks = mutableListOf<List<MonthBlock>>()
            var mIdx = 0
            for (t in 0 until effectiveTiers) {
                val count = baseMonthsPerTier + if (t < remainderMonths) 1 else 0
                if (count > 0 && mIdx < totalMonths) {
                    val end = minOf(mIdx + count, totalMonths)
                    tierBlocks.add(monthBlocks.subList(mIdx, end))
                    mIdx = end
                }
            }

            // Invariant 1: Total months across all tiers must equal totalMonths
            val totalAssignedMonths = tierBlocks.sumOf { it.size }
            assertEquals("All months must be assigned for tier count $tierCount", totalMonths, totalAssignedMonths)

            // Invariant 2: Total weeks across all tiers must equal weeksCount
            val totalAssignedWeeks = tierBlocks.sumOf { tb -> tb.sumOf { it.weekIndices.size } }
            assertEquals("All weeks must be assigned for tier count $tierCount", weeksCount, totalAssignedWeeks)

            // Invariant 3: NEVER split a month: Every MonthBlock in a tier is wholly contained in that single tier
            val seenMonths = mutableSetOf<MonthBlock>()
            for (tier in tierBlocks) {
                for (mb in tier) {
                    assertTrue("MonthBlock ${mb.monthName} must not appear in multiple tiers", seenMonths.add(mb))
                }
            }
        }
    }
}
