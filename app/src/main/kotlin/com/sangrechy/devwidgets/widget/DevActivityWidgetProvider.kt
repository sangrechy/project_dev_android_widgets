package com.sangrechy.devwidgets.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.SizeF
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
            val pendingResult = goAsync()
            val appContext = context.applicationContext
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val prefs = WidgetPreferences(appContext)
                    val current = prefs.getDashboardData()
                    WidgetManagerHelper.updateAll(appContext, current.copy(isSyncing = true))

                    val repo = com.sangrechy.devwidgets.data.DefaultDataRepository(appContext)
                    repo.syncData()

                    val updated = prefs.getDashboardData()
                    WidgetManagerHelper.updateAll(appContext, updated.copy(isSyncing = false))
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
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
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 260)
            val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 280)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val compactViews = buildRemoteViews(context, data, widthDp = 240, heightDp = 180)
                val standardViews = buildRemoteViews(context, data, widthDp = 300, heightDp = 300)
                val tallViews = buildRemoteViews(context, data, widthDp = 340, heightDp = 500)
                val viewMapping = mapOf(
                    SizeF(150f, 120f) to compactViews,
                    SizeF(240f, 220f) to standardViews,
                    SizeF(280f, 380f) to tallViews
                )
                appWidgetManager.updateAppWidget(appWidgetId, RemoteViews(viewMapping))
            } else {
                val views = buildRemoteViews(context, data, widthDp = minWidthDp, heightDp = minHeightDp)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        private fun buildRemoteViews(
            context: Context,
            data: WidgetDashboardData,
            widthDp: Int,
            heightDp: Int
        ): RemoteViews {
            val layoutId = if (heightDp < 220) R.layout.widget_dev_activity_compact else R.layout.widget_dev_activity
            val views = RemoteViews(context.packageName, layoutId)
            WidgetWallpaperHelper.applyWallpaper(context, views, widthDp, heightDp, widgetKey = "DEV_ACTIVITY")

            val sectionHeightDp = ((heightDp - 56) / 2).coerceAtLeast(80)
            val visualRowHeightDp = (sectionHeightDp - 36).coerceAtLeast(60)

            val cardWidthDp = (widthDp - 24).coerceAtLeast(140)
            val heatmapWidthDp = (cardWidthDp * 0.58f).coerceAtLeast(100f)
            val circleSizeDp = minOf(cardWidthDp * 0.42f, visualRowHeightDp.toFloat()).coerceAtLeast(60f)

            val scale = 2.5f
            val hmWidthPx = (heatmapWidthDp * scale).toInt().coerceIn(240, 960)
            val hmHeightPx = (visualRowHeightDp * scale).toInt().coerceIn(160, 960)
            val circleSizePx = (circleSizeDp * scale).toInt().coerceIn(200, 600)
            val prefs = WidgetPreferences(context)
            val orientation = prefs.heatmapOrientation

            val lcCircle = WidgetBitmapRenderer.renderLeetCodeCircle(data.leetCode, sizePx = circleSizePx)
            val lcHeatmap = WidgetBitmapRenderer.renderHeatmap(
                data.leetCode.submissionCalendar,
                isLeetCode = true,
                widthPx = hmWidthPx,
                heightPx = hmHeightPx,
                weeksCount = 52,
                orientation = orientation
            )
            val ghTypeMap = WidgetBitmapRenderer.renderGitHubTypeMap(data.gitHub, sizePx = circleSizePx)
            val ghHeatmap = WidgetBitmapRenderer.renderHeatmap(
                data.gitHub.contributionDays,
                isLeetCode = false,
                widthPx = hmWidthPx,
                heightPx = hmHeightPx,
                weeksCount = 52,
                orientation = orientation
            )

            views.setImageViewBitmap(R.id.iv_leetcode_circle, lcCircle)
            views.setImageViewBitmap(R.id.iv_leetcode_heatmap, lcHeatmap)
            views.setImageViewBitmap(R.id.iv_github_typemap, ghTypeMap)
            views.setImageViewBitmap(R.id.iv_github_heatmap, ghHeatmap)

            // User tags in headers
            val lcTag = when {
                data.leetCode.status == com.sangrechy.devwidgets.data.model.AccountStatus.FAILED ->
                    "@${data.leetCode.username.ifBlank { "dev" }} (Offline)"
                data.leetCode.username.isNotBlank() -> "@${data.leetCode.username}"
                else -> "@dev"
            }
            val ghTag = when {
                data.gitHub.status == com.sangrechy.devwidgets.data.model.AccountStatus.FAILED ->
                    "@${data.gitHub.username.ifBlank { "dev" }} (Offline)"
                data.gitHub.username.isNotBlank() -> "@${data.gitHub.username}"
                else -> "@dev"
            }
            views.setTextViewText(R.id.tv_lc_user_tag, lcTag)
            views.setTextViewText(R.id.tv_gh_user_tag, ghTag)

            // Sync timestamp or syncing animation
            val syncText = if (data.isSyncing) {
                "Syncing..."
            } else {
                val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
                "Sync: ${timeFmt.format(Date(data.lastUpdatedMillis))}"
            }
            views.setTextViewText(R.id.tv_last_updated, syncText)

            // Pending Intents
            val appIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val appPendingIntent = PendingIntent.getActivity(
                context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.tv_widget_title, appPendingIntent)
            views.setOnClickPendingIntent(R.id.iv_widget_chibi_logo, appPendingIntent)

            val refreshIntent = Intent(context, DevActivityWidgetProvider::class.java).apply {
                action = ACTION_FORCE_REFRESH
                setPackage(context.packageName)
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context, 1, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent)
            views.setOnClickPendingIntent(R.id.header_right_wrap, refreshPendingIntent)

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

            return views
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
