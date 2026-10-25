package com.sangrechy.devwidgets.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.sangrechy.devwidgets.MainActivity
import com.sangrechy.devwidgets.R
import com.sangrechy.devwidgets.data.WidgetPreferences
import com.sangrechy.devwidgets.data.model.WidgetDashboardData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DevActivityWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = WidgetPreferences(context)
        val data = prefs.getDashboardData()

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, data)
        }

        enqueueImmediateSync(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val prefs = WidgetPreferences(context)
        updateAppWidget(context, appWidgetManager, appWidgetId, prefs.getDashboardData())
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_FORCE_REFRESH) {
            enqueueImmediateSync(context)
        }
    }

    companion object {
        const val ACTION_FORCE_REFRESH = "com.sangrechy.devwidgets.ACTION_FORCE_REFRESH"

        fun updateAllWidgets(context: Context, data: WidgetDashboardData) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, DevActivityWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, data)
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            data: WidgetDashboardData
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_dev_activity)

            // Dynamically detect widget height from options
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0

            // Rule 1: If user does not allow decent space (minHeight < 340dp), do NOT show recent submissions/commits at all.
            // When showRecentFeeds is true (height >= 340dp or unmeasured initial state 0), show 2 rows if height in 340..440dp, or 4 rows if >= 450dp.
            val showRecentFeeds = minHeight >= 340 || minHeight == 0
            val maxVisibleRows = when {
                !showRecentFeeds -> 0
                minHeight in 340..440 -> 2
                else -> 4
            }

            views.setViewVisibility(R.id.ll_leetcode_submissions, if (showRecentFeeds) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.ll_github_events, if (showRecentFeeds) View.VISIBLE else View.GONE)

            // 1. Render Glassmorphic Bitmaps
            val lcCircle = WidgetBitmapRenderer.renderLeetCodeCircle(data.leetCode, sizePx = 360)
            val lcHeatmap = WidgetBitmapRenderer.renderHeatmap(
                data.leetCode.submissionCalendar,
                isLeetCode = true,
                widthPx = 640,
                heightPx = 220,
                weeksCount = 18
            )
            val ghTypeMap = WidgetBitmapRenderer.renderGitHubTypeMap(data.gitHub, sizePx = 360)
            val ghHeatmap = WidgetBitmapRenderer.renderHeatmap(
                data.gitHub.contributionDays,
                isLeetCode = false,
                widthPx = 640,
                heightPx = 220,
                weeksCount = 18
            )

            views.setImageViewBitmap(R.id.iv_leetcode_circle, lcCircle)
            views.setImageViewBitmap(R.id.iv_leetcode_heatmap, lcHeatmap)
            views.setImageViewBitmap(R.id.iv_github_typemap, ghTypeMap)
            views.setImageViewBitmap(R.id.iv_github_heatmap, ghHeatmap)

            // 2. User Tags in Headers
            val lcHandle = data.leetCode.username.ifBlank { "dev" }
            val ghHandle = data.gitHub.username.ifBlank { "dev" }
            views.setTextViewText(R.id.tv_lc_user_tag, "@$lcHandle")
            views.setTextViewText(R.id.tv_gh_user_tag, "@$ghHandle")

            // 3. Last Submissions Full-Width Rows
            val lcRowIds = intArrayOf(R.id.row_lc_1, R.id.row_lc_2, R.id.row_lc_3, R.id.row_lc_4)
            val lcBadgeIds = intArrayOf(R.id.badge_lc_1, R.id.badge_lc_2, R.id.badge_lc_3, R.id.badge_lc_4)
            val lcTitleIds = intArrayOf(R.id.tv_lc_title_1, R.id.tv_lc_title_2, R.id.tv_lc_title_3, R.id.tv_lc_title_4)
            val lcSubIds = intArrayOf(R.id.tv_lc_sub_1, R.id.tv_lc_sub_2, R.id.tv_lc_sub_3, R.id.tv_lc_sub_4)

            val subs = data.leetCode.recentSubmissions
            for (i in 0..3) {
                if (i < subs.size && i < maxVisibleRows) {
                    val sub = subs[i]
                    val isAc = sub.statusDisplay.contains("Accept", ignoreCase = true)
                    views.setViewVisibility(lcRowIds[i], View.VISIBLE)
                    views.setTextViewText(lcTitleIds[i], sub.title)
                    views.setTextViewText(lcSubIds[i], "${sub.lang} • ${formatRelativeTime(sub.timestamp)}")
                    views.setTextViewText(lcBadgeIds[i], if (isAc) "AC" else "WA")
                    views.setInt(
                        lcBadgeIds[i],
                        "setBackgroundResource",
                        if (isAc) R.drawable.badge_pill_ac else R.drawable.badge_pill_wa
                    )
                    views.setTextColor(
                        lcBadgeIds[i],
                        if (isAc) Color.parseColor("#34D399") else Color.parseColor("#F87171")
                    )
                } else {
                    views.setViewVisibility(lcRowIds[i], View.GONE)
                }
            }

            // 4. Last Activities Full-Width Rows
            val ghRowIds = intArrayOf(R.id.row_gh_1, R.id.row_gh_2, R.id.row_gh_3, R.id.row_gh_4)
            val ghBadgeIds = intArrayOf(R.id.badge_gh_1, R.id.badge_gh_2, R.id.badge_gh_3, R.id.badge_gh_4)
            val ghTitleIds = intArrayOf(R.id.tv_gh_title_1, R.id.tv_gh_title_2, R.id.tv_gh_title_3, R.id.tv_gh_title_4)
            val ghSubIds = intArrayOf(R.id.tv_gh_sub_1, R.id.tv_gh_sub_2, R.id.tv_gh_sub_3, R.id.tv_gh_sub_4)

            val events = data.gitHub.recentEvents
            for (i in 0..3) {
                if (i < events.size && i < maxVisibleRows) {
                    val ev = events[i]
                    val badgeTag = when (ev.type) {
                        "PushEvent" -> "PUSH"
                        "PullRequestEvent" -> "PR"
                        "IssuesEvent" -> "ISSUE"
                        "PullRequestReviewEvent" -> "REV"
                        else -> "ACT"
                    }
                    views.setViewVisibility(ghRowIds[i], View.VISIBLE)
                    views.setTextViewText(ghBadgeIds[i], badgeTag)
                    views.setTextColor(ghBadgeIds[i], Color.parseColor("#A5B4FC"))
                    views.setTextViewText(ghTitleIds[i], ev.repoName)
                    views.setTextViewText(ghSubIds[i], "${ev.actionSummary} • ${ev.createdAt}")
                } else {
                    views.setViewVisibility(ghRowIds[i], View.GONE)
                }
            }

            // 5. Sync Timestamp
            val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
            views.setTextViewText(R.id.tv_last_updated, "Sync: ${timeFmt.format(Date(data.lastUpdatedMillis))}")

            // 6. Pending Intents
            val appIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val appPendingIntent = PendingIntent.getActivity(
                context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.tv_widget_title, appPendingIntent)

            val refreshIntent = Intent(context, DevActivityWidgetProvider::class.java).apply {
                action = ACTION_FORCE_REFRESH
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context, 1, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent)

            val lcUser = data.leetCode.username
            if (lcUser.isNotBlank()) {
                val lcWebIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://leetcode.com/u/$lcUser/"))
                val lcPendingIntent = PendingIntent.getActivity(
                    context, 2, lcWebIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.section_leetcode, lcPendingIntent)
            }

            val ghUser = data.gitHub.username
            if (ghUser.isNotBlank()) {
                val ghWebIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/$ghUser"))
                val ghPendingIntent = PendingIntent.getActivity(
                    context, 3, ghWebIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.section_github, ghPendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun formatRelativeTime(timestampSeconds: Long): String {
            val nowSec = System.currentTimeMillis() / 1000L
            val diffSec = (nowSec - timestampSeconds).coerceAtLeast(0)
            return when {
                diffSec < 60 -> "just now"
                diffSec < 3600 -> "${diffSec / 60}m ago"
                diffSec < 86400 -> "${diffSec / 3600}h ago"
                diffSec < 604800 -> "${diffSec / 86400}d ago"
                else -> "${diffSec / 604800}w ago"
            }
        }

        fun enqueueImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<WidgetSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "ImmediateDevWidgetSync",
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
        }
    }
}
