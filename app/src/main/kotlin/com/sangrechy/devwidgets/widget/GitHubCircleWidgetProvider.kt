package com.sangrechy.devwidgets.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import com.sangrechy.devwidgets.MainActivity
import com.sangrechy.devwidgets.R
import com.sangrechy.devwidgets.data.WallpaperManager
import com.sangrechy.devwidgets.data.WidgetPreferences
import com.sangrechy.devwidgets.data.model.WidgetDashboardData

class GitHubCircleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = WidgetPreferences(context)
        val data = prefs.getDashboardData()
        for (id in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, id, data)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val prefs = WidgetPreferences(context)
        updateAppWidget(context, appWidgetManager, appWidgetId, prefs.getDashboardData())
    }

    companion object {
        fun updateAllWidgets(context: Context, data: WidgetDashboardData) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, GitHubCircleWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (id in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, id, data)
            }
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, data: WidgetDashboardData) {
            val views = RemoteViews(context.packageName, R.layout.widget_github_circle)
            val prefs = WidgetPreferences(context)
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0).toFloat()
            val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0).toFloat()

            val selectedOuterSize = prefs.getWidgetOuterDivSize("GITHUB_CIRCLE")
            val defaultHeightDp = if (selectedOuterSize == "x*3") 210f else 140f
            val actualWidthDp = if (minWidthDp > 120f) minWidthDp else 160f
            val actualHeightDp = if (minHeightDp > 100f) minHeightDp else defaultHeightDp

            val showHeading = prefs.getWidgetShowHeading("GITHUB_CIRCLE")
            views.setViewVisibility(R.id.header_bar, if (showHeading) android.view.View.VISIBLE else android.view.View.GONE)

            WidgetWallpaperHelper.applyWallpaper(
                context, views,
                actualWidthDp.toInt().coerceAtLeast(160),
                actualHeightDp.toInt().coerceAtLeast(140),
                widgetKey = "GITHUB_CIRCLE"
            )

            val offsetX = prefs.getWidgetElementOffsetX("GITHUB_CIRCLE")
            val offsetY = prefs.getWidgetElementOffsetY("GITHUB_CIRCLE")
            val density = context.resources.displayMetrics.density

            val innerDivWidthDp = 86f
            val actualMaxOffsetX = ((actualWidthDp - innerDivWidthDp) / 2f).coerceAtLeast(0f)
            val effectiveDxDp = if (actualMaxOffsetX > 0f) offsetX.coerceIn(-actualMaxOffsetX, actualMaxOffsetX) else 0f
            val effectiveDyDp = offsetY.coerceIn(-24f, 24f)

            val maxShiftPx = (actualMaxOffsetX * 2f * density).toInt()
            val padLeft = if (effectiveDxDp > 0) (effectiveDxDp * 2f * density).toInt().coerceIn(0, maxShiftPx) else 0
            val padRight = if (effectiveDxDp < 0) ((-effectiveDxDp) * 2f * density).toInt().coerceIn(0, maxShiftPx) else 0
            val padTop = if (effectiveDyDp > 0) (effectiveDyDp * 2f * density).toInt().coerceAtLeast(0) else 0
            val padBottom = if (effectiveDyDp < 0) ((-effectiveDyDp) * 2f * density).toInt().coerceAtLeast(0) else 0

            views.setViewPadding(R.id.section_circle_container, padLeft, padTop, padRight, padBottom)

            val podColor = WallpaperManager.getEffectivePodColor(context, "GITHUB_CIRCLE")
            val podDrawable = if (podColor == "BLACK") R.drawable.widget_glass_circle_black else R.drawable.widget_glass_circle_white
            views.setInt(R.id.section_github, "setBackgroundResource", podDrawable)

            val ghCircle = WidgetBitmapRenderer.renderGitHubTypeMap(data.gitHub, sizePx = 240)
            views.setImageViewBitmap(R.id.iv_github_typemap, ghCircle)

            val tag = if (data.gitHub.username.isNotBlank()) "(@${data.gitHub.username})" else "(@not_linked)"
            views.setTextViewText(R.id.tv_gh_user_tag, tag)

            val appIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val appPendingIntent = PendingIntent.getActivity(
                context, 301, appIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.tv_gh_title, appPendingIntent)
            views.setOnClickPendingIntent(R.id.iv_gh_brand_logo, appPendingIntent)

            val refreshIntent = Intent(context, DevActivityWidgetProvider::class.java).apply {
                action = DevActivityWidgetProvider.ACTION_FORCE_REFRESH
                setPackage(context.packageName)
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context, 302, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent)
            views.setOnClickPendingIntent(R.id.header_right_wrap, refreshPendingIntent)

            val ghUser = data.gitHub.username
            if (ghUser.isNotBlank()) {
                val ghWebIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/$ghUser"))
                val ghPendingIntent = PendingIntent.getActivity(
                    context, 303, ghWebIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.section_github, ghPendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
