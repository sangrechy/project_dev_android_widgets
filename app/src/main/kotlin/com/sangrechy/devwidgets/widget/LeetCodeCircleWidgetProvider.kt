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
            WidgetWallpaperHelper.applyWallpaper(context, views, 240, 240, widgetKey = "LEETCODE_CIRCLE")

            val prefs = WidgetPreferences(context)
            val offsetX = prefs.getWidgetElementOffsetX("LEETCODE_CIRCLE")
            val offsetY = prefs.getWidgetElementOffsetY("LEETCODE_CIRCLE")
            val density = context.resources.displayMetrics.density
            val dxPx = (offsetX * density).toInt()
            val dyPx = (offsetY * density).toInt()
            val padLeft = dxPx.coerceAtLeast(0)
            val padRight = (-dxPx).coerceAtLeast(0)
            val padTop = dyPx.coerceAtLeast(0)
            val padBottom = (-dyPx).coerceAtLeast(0)
            views.setViewPadding(R.id.section_circle_container, padLeft, padTop, padRight, padBottom)

            val lcCircle = WidgetBitmapRenderer.renderLeetCodeCircle(data.leetCode, sizePx = 360)
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
