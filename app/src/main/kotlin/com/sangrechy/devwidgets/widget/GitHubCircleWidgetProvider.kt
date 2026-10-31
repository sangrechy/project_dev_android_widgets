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
            WidgetWallpaperHelper.applyWallpaper(context, views, 240, 240, widgetKey = "GITHUB_CIRCLE")

            val prefs = WidgetPreferences(context)
            val offsetX = prefs.getWidgetElementOffsetX("GITHUB_CIRCLE")
            val offsetY = prefs.getWidgetElementOffsetY("GITHUB_CIRCLE")
            val density = context.resources.displayMetrics.density
            val dxPx = (offsetX * density).toInt()
            val dyPx = (offsetY * density).toInt()
            val padLeft = dxPx.coerceAtLeast(0)
            val padRight = (-dxPx).coerceAtLeast(0)
            val padTop = dyPx.coerceAtLeast(0)
            val padBottom = (-dyPx).coerceAtLeast(0)
            views.setViewPadding(R.id.section_circle_container, padLeft, padTop, padRight, padBottom)

            val ghCircle = WidgetBitmapRenderer.renderGitHubTypeMap(data.gitHub, sizePx = 360)
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
