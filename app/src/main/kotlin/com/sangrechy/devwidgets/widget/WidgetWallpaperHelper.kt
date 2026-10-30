package com.sangrechy.devwidgets.widget

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.sangrechy.devwidgets.R
import com.sangrechy.devwidgets.data.WallpaperManager
import com.sangrechy.devwidgets.data.WidgetPreferences

object WidgetWallpaperHelper {

    fun applyWallpaper(context: Context, views: RemoteViews, widthDp: Int = 300, heightDp: Int = 200, widgetKey: String? = null) {
        val prefs = WidgetPreferences(context)
        val type = if (widgetKey != null) prefs.getWidgetWallpaperTypeFor(widgetKey) else prefs.widgetWallpaperType
        val dim = (if (widgetKey != null) prefs.getWidgetWallpaperDimFor(widgetKey) else prefs.widgetWallpaperDim).coerceIn(0.1f, 0.95f)

        when (type) {
            "TRANSLUCENT" -> {
                views.setViewVisibility(R.id.iv_widget_wallpaper_bg, View.GONE)
                views.setViewVisibility(R.id.v_widget_wallpaper_scrim, View.GONE)
                views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_glass_translucent)
            }
            "TRANSPARENT" -> {
                views.setViewVisibility(R.id.iv_widget_wallpaper_bg, View.GONE)
                views.setViewVisibility(R.id.v_widget_wallpaper_scrim, View.GONE)
                views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_glass_transparent)
            }
            "MATCH_APP", "CUSTOM", "PRESET" -> {
                val wPx = widthDp.coerceIn(240, 420)
                val hPx = heightDp.coerceIn(160, 300)
                val bitmap = WallpaperManager.getWidgetWallpaperBitmap(context, wPx, hPx, widgetKey)

                if (bitmap != null) {
                    views.setViewVisibility(R.id.iv_widget_wallpaper_bg, View.VISIBLE)
                    views.setImageViewBitmap(R.id.iv_widget_wallpaper_bg, bitmap)
                    views.setViewVisibility(R.id.v_widget_wallpaper_scrim, View.VISIBLE)
                    val scrimColor = Color.argb((dim * 255).toInt(), 0, 0, 0)
                    views.setInt(R.id.v_widget_wallpaper_scrim, "setBackgroundColor", scrimColor)
                    views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_glass_overlay)
                } else {
                    views.setViewVisibility(R.id.iv_widget_wallpaper_bg, View.GONE)
                    views.setViewVisibility(R.id.v_widget_wallpaper_scrim, View.GONE)
                    views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_glass_chassis)
                }
            }
            else -> {
                // "DEFAULT" -> Obsidian Glass
                views.setViewVisibility(R.id.iv_widget_wallpaper_bg, View.GONE)
                views.setViewVisibility(R.id.v_widget_wallpaper_scrim, View.GONE)
                views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_glass_chassis)
            }
        }
    }
}
