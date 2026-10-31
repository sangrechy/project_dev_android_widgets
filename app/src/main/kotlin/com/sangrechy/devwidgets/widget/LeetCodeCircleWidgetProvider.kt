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
import com.sangrechy.devwidgets.data.WidgetPreferences
import com.sangrechy.devwidgets.data.model.WidgetDashboardData

class LeetCodeCircleWidgetProvider : AppWidgetProvider() {

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
            val componentName = ComponentName(context, LeetCodeCircleWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (id in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, id, data)
            }
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, data: WidgetDashboardData) {
            val views = RemoteViews(context.packageName, R.layout.widget_leetcode_circle)
            val prefs = WidgetPreferences(context)
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0).toFloat()
            val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0).toFloat()

            val selectedOuterSize = prefs.getWidgetOuterDivSize("LEETCODE_CIRCLE")
            val refWidthDp = when (selectedOuterSize) {
                "3x2" -> 230f
                "4x2" -> 300f
                "5x2" -> 360f
                else -> 160f
            }
            val actualWidthDp = if (minWidthDp > 120f) minWidthDp else refWidthDp
            val actualHeightDp = if (minHeightDp > 100f) minHeightDp else 140f

            WidgetWallpaperHelper.applyWallpaper(
                context, views,
                actualWidthDp.toInt().coerceAtLeast(160),
                actualHeightDp.toInt().coerceAtLeast(140),
                widgetKey = "LEETCODE_CIRCLE"
            )

            val offsetX = prefs.getWidgetElementOffsetX("LEETCODE_CIRCLE")
            val offsetY = prefs.getWidgetElementOffsetY("LEETCODE_CIRCLE")
            val density = context.resources.displayMetrics.density

            val innerDivWidthDp = 86f
            val refMaxOffsetX = ((refWidthDp - innerDivWidthDp) / 2f).coerceAtLeast(10f)
            val fractionX = (offsetX / refMaxOffsetX).coerceIn(-1f, 1f)
            val actualMaxOffsetX = ((actualWidthDp - innerDivWidthDp) / 2f).coerceAtLeast(0f)
            val effectiveDxDp = fractionX * actualMaxOffsetX
            val effectiveDyDp = offsetY.coerceIn(-24f, 24f)

            val maxShiftPx = (actualMaxOffsetX * 2f * density).toInt()
            val padLeft = if (effectiveDxDp > 0) (effectiveDxDp * 2f * density).toInt().coerceIn(0, maxShiftPx) else 0
            val padRight = if (effectiveDxDp < 0) ((-effectiveDxDp) * 2f * density).toInt().coerceIn(0, maxShiftPx) else 0
            val padTop = if (effectiveDyDp > 0) (effectiveDyDp * 2f * density).toInt().coerceAtLeast(0) else 0
            val padBottom = if (effectiveDyDp < 0) ((-effectiveDyDp) * 2f * density).toInt().coerceAtLeast(0) else 0

            views.setViewPadding(R.id.section_circle_container, padLeft, padTop, padRight, padBottom)

            val lcCircle = WidgetBitmapRenderer.renderLeetCodeCircle(data.leetCode, sizePx = 240)
            views.setImageViewBitmap(R.id.iv_leetcode_circle, lcCircle)

            val tag = if (data.leetCode.username.isNotBlank()) "(@${data.leetCode.username})" else "(@not_linked)"
            views.setTextViewText(R.id.tv_lc_user_tag, tag)

            val appIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val appPendingIntent = PendingIntent.getActivity(
                context, 201, appIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.tv_lc_title, appPendingIntent)
            views.setOnClickPendingIntent(R.id.iv_lc_brand_logo, appPendingIntent)

            val refreshIntent = Intent(context, DevActivityWidgetProvider::class.java).apply {
                action = DevActivityWidgetProvider.ACTION_FORCE_REFRESH
                setPackage(context.packageName)
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context, 202, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent)
            views.setOnClickPendingIntent(R.id.header_right_wrap, refreshPendingIntent)

            val lcUser = data.leetCode.username
            if (lcUser.isNotBlank()) {
                val lcWebIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://leetcode.com/u/$lcUser/"))
                val lcPendingIntent = PendingIntent.getActivity(
                    context, 203, lcWebIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.section_leetcode, lcPendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
