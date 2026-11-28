package com.sangrechy.devwidgets.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import com.sangrechy.devwidgets.data.WidgetPreferences
import com.sangrechy.devwidgets.data.model.WidgetDashboardData

/**
 * Dedicated x*3 height-locked LeetCode Solved Circle widget (horizontal-only resize).
 * Shares rendering, wallpaper, and element offset logic with [LeetCodeCircleWidgetProvider].
 */
class LeetCodeCircleTallWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = WidgetPreferences(context)
        val data = prefs.getDashboardData()
        for (id in appWidgetIds) {
            LeetCodeCircleWidgetProvider.updateAppWidget(context, appWidgetManager, id, data)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val prefs = WidgetPreferences(context)
        LeetCodeCircleWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetId, prefs.getDashboardData())
    }

    companion object {
        fun updateAllWidgets(context: Context, data: WidgetDashboardData) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, LeetCodeCircleTallWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (id in appWidgetIds) {
                LeetCodeCircleWidgetProvider.updateAppWidget(context, appWidgetManager, id, data)
            }
        }
    }
}
