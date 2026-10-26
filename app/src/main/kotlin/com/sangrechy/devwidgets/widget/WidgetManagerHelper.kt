package com.sangrechy.devwidgets.widget

import android.content.Context
import com.sangrechy.devwidgets.data.model.WidgetDashboardData

object WidgetManagerHelper {

    fun updateAll(context: Context, data: WidgetDashboardData) {
        val appContext = context.applicationContext
        try {
            DevActivityWidgetProvider.updateAllWidgets(appContext, data)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            LeetCodeWidgetProvider.updateAllWidgets(appContext, data)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            GitHubWidgetProvider.updateAllWidgets(appContext, data)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            LeetCodeHeatmapWidgetProvider.updateAllWidgets(appContext, data)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            GitHubHeatmapWidgetProvider.updateAllWidgets(appContext, data)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            CombinedHeatmapWidgetProvider.updateAllWidgets(appContext, data)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
