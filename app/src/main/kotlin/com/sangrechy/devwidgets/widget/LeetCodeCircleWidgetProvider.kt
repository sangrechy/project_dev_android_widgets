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
            val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val optWidthDp = if (isLandscape) {
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0).toFloat()
            } else {
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0).toFloat()
            }
            val optHeightDp = if (isLandscape) {
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0).toFloat()
            } else {
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0).toFloat()
            }

            val actualWidthDp = if (optWidthDp > 86f) optWidthDp else 160f
            val actualHeightDp = if (optHeightDp > 86f) optHeightDp else 140f

            val showHeading = prefs.getWidgetShowHeading("LEETCODE_CIRCLE")
            views.setViewVisibility(R.id.header_bar, if (showHeading) android.view.View.VISIBLE else android.view.View.GONE)

            WidgetWallpaperHelper.applyWallpaper(
                context, views,
                actualWidthDp.toInt().coerceAtLeast(160),
                actualHeightDp.toInt().coerceAtLeast(140),
                widgetKey = "LEETCODE_CIRCLE"
            )

            val offsetX = prefs.getWidgetElementOffsetX("LEETCODE_CIRCLE")
            val offsetY = prefs.getWidgetElementOffsetY("LEETCODE_CIRCLE")
            val density = context.resources.displayMetrics.density

            // Absolute edge-to-edge calculation (0dp to max width/height)
            val availableWidthDp = actualWidthDp.coerceAtLeast(86f)
            val availableHeightDp = (actualHeightDp - (if (showHeading) 26f else 0f)).coerceAtLeast(86f)

            val maxShiftX = ((availableWidthDp - 86f) / 2f).coerceAtLeast(0f)
            val maxShiftY = ((availableHeightDp - 86f) / 2f).coerceAtLeast(0f)

            val ratioX = prefs.getWidgetElementRatioX("LEETCODE_CIRCLE", fallback = if (maxShiftX > 0f) offsetX / maxShiftX else 0f)
            val ratioY = prefs.getWidgetElementRatioY("LEETCODE_CIRCLE", fallback = if (maxShiftY > 0f) offsetY / maxShiftY else 0f)

            // When ratioX = -1f -> left is 0 (absolute left edge). When ratioX = 1f -> left is availableWidthDp - 86 (absolute right edge)
            val targetLeftDp = (maxShiftX + ratioX * maxShiftX).coerceIn(0f, (availableWidthDp - 86f).coerceAtLeast(0f))
            val targetTopDp = (maxShiftY + ratioY * maxShiftY).coerceIn(0f, (availableHeightDp - 86f).coerceAtLeast(0f))

            val padLeft = (targetLeftDp * density).toInt()
            val padTop = (targetTopDp * density).toInt()

            views.setViewPadding(R.id.section_circle_container, padLeft, padTop, 0, 0)

            val podColor = WallpaperManager.getEffectivePodColor(context, "LEETCODE_CIRCLE")
            val podDrawable = if (podColor == "BLACK") R.drawable.widget_glass_circle_black else R.drawable.widget_glass_circle_white
            views.setInt(R.id.section_leetcode, "setBackgroundResource", podDrawable)

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
