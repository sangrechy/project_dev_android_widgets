package com.sangrechy.devwidgets.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import androidx.work.ExistingWorkPolicy
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

        // Trigger background sync
        enqueueImmediateSync(context)
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

            // 1. Render Bitmaps
            val lcCircle = WidgetBitmapRenderer.renderLeetCodeCircle(data.leetCode, sizePx = 280)
            val lcHeatmap = WidgetBitmapRenderer.renderHeatmap(data.leetCode.submissionCalendar, isLeetCode = true, widthPx = 540, heightPx = 260)
            val ghTypeMap = WidgetBitmapRenderer.renderGitHubTypeMap(data.gitHub, sizePx = 280)
            val ghHeatmap = WidgetBitmapRenderer.renderHeatmap(data.gitHub.contributionDays, isLeetCode = false, widthPx = 540, heightPx = 260)

            views.setImageViewBitmap(R.id.iv_leetcode_circle, lcCircle)
            views.setImageViewBitmap(R.id.iv_leetcode_heatmap, lcHeatmap)
            views.setImageViewBitmap(R.id.iv_github_typemap, ghTypeMap)
            views.setImageViewBitmap(R.id.iv_github_heatmap, ghHeatmap)

            // 2. LeetCode Submissions list
            val lcSubIds = intArrayOf(
                R.id.tv_lc_sub_1,
                R.id.tv_lc_sub_2,
                R.id.tv_lc_sub_3,
                R.id.tv_lc_sub_4,
                R.id.tv_lc_sub_5
            )
            val subs = data.leetCode.recentSubmissions
            for (i in lcSubIds.indices) {
                if (i < subs.size) {
                    val sub = subs[i]
                    val shortStatus = if (sub.statusDisplay.contains("Accept", ignoreCase = true)) "AC" else "WA"
                    views.setTextViewText(lcSubIds[i], "${i + 1}. ${sub.title} • $shortStatus")
                } else {
                    views.setTextViewText(lcSubIds[i], "")
                }
            }

            // 3. GitHub Activities list
            val ghEvIds = intArrayOf(
                R.id.tv_gh_ev_1,
                R.id.tv_gh_ev_2,
                R.id.tv_gh_ev_3,
                R.id.tv_gh_ev_4,
                R.id.tv_gh_ev_5
            )
            val events = data.gitHub.recentEvents
            for (i in ghEvIds.indices) {
                if (i < events.size) {
                    val ev = events[i]
                    views.setTextViewText(ghEvIds[i], "${i + 1}. ${ev.repoName} • ${ev.actionSummary}")
                } else {
                    views.setTextViewText(ghEvIds[i], "")
                }
            }

            // 4. Last updated text
            val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
            views.setTextViewText(R.id.tv_last_updated, "Sync: ${timeFmt.format(Date(data.lastUpdatedMillis))}")

            // 5. Pending Intents
            // Main app intent
            val appIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val appPendingIntent = PendingIntent.getActivity(
                context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.tv_widget_title, appPendingIntent)

            // Refresh button intent
            val refreshIntent = Intent(context, DevActivityWidgetProvider::class.java).apply {
                action = ACTION_FORCE_REFRESH
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context, 1, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent)

            // LeetCode section intent (opens LeetCode profile)
            val lcUser = data.leetCode.username
            if (lcUser.isNotBlank()) {
                val lcWebIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://leetcode.com/u/$lcUser/"))
                val lcPendingIntent = PendingIntent.getActivity(
                    context, 2, lcWebIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.section_leetcode, lcPendingIntent)
            }

            // GitHub section intent (opens GitHub profile)
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

        fun enqueueImmediateSync(context: Context) {
            val syncRequest = OneTimeWorkRequestBuilder<WidgetSyncWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "dev_widget_immediate_sync",
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
        }
    }
}
