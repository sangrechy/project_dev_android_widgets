package com.sangrechy.devwidgets.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.random.Random

enum class WallpaperPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val primaryColorHex: String
) {
    OBSIDIAN_CORE(
        id = "OBSIDIAN_CORE",
        title = "Obsidian Core",
        subtitle = "Geometric dark tech obsidian with silver coordinate grid",
        primaryColorHex = "#FFFFFF"
    ),
    CYBER_MATRIX(
        id = "CYBER_MATRIX",
        title = "Mono Matrix",
        subtitle = "Matrix coordinate grid with clean monochrome lines",
        primaryColorHex = "#E0E0E0"
    ),
    DEEP_COSMOS(
        id = "DEEP_COSMOS",
        title = "Deep Void",
        subtitle = "Interstellar deep pitch black with monochrome stardust",
        primaryColorHex = "#CCCCCC"
    ),
    TERMINAL_CODE(
        id = "TERMINAL_CODE",
        title = "Terminal Mono",
        subtitle = "Developer code syntax & grayscale typography",
        primaryColorHex = "#AAAAAA"
    ),
    MINIMAL_CARBON(
        id = "MINIMAL_CARBON",
        title = "Carbon Weave",
        subtitle = "Sleek diagonal woven carbon-fiber texture",
        primaryColorHex = "#888888"
    ),
    SUNSET_DEV(
        id = "SUNSET_DEV",
        title = "Studio Dark",
        subtitle = "Dark studio vignette with subtle monochrome lighting",
        primaryColorHex = "#777777"
    );

    companion object {
        fun fromId(id: String): WallpaperPreset {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: OBSIDIAN_CORE
        }
    }
}

object WallpaperManager {

    private val presetCache = mutableMapOf<String, Bitmap>()

    fun saveCustomWallpaper(context: Context, inputStream: InputStream, forWidget: Boolean = false): String {
        val fileName = if (forWidget) "custom_widget_wallpaper.jpg" else "custom_app_wallpaper.jpg"
        val file = File(context.filesDir, fileName)
        FileOutputStream(file).use { output ->
            inputStream.copyTo(output)
        }
        return file.absolutePath
    }

    fun getCustomWallpaperBitmap(path: String, maxDim: Int = 1920): Bitmap? {
        if (path.isBlank()) return null
        val file = File(path)
        if (!file.exists()) return null

        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, boundsOptions)

            var sampleSize = 1
            while (boundsOptions.outWidth / sampleSize > maxDim || boundsOptions.outHeight / sampleSize > maxDim) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getPresetBitmap(preset: WallpaperPreset, width: Int = 720, height: Int = 1280): Bitmap {
        val cacheKey = "${preset.id}_${width}x$height"
        presetCache[cacheKey]?.let {
            if (!it.isRecycled) return it
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        when (preset) {
            WallpaperPreset.OBSIDIAN_CORE -> drawObsidianCore(canvas, width, height)
            WallpaperPreset.CYBER_MATRIX -> drawCyberMatrix(canvas, width, height)
            WallpaperPreset.DEEP_COSMOS -> drawDeepCosmos(canvas, width, height)
            WallpaperPreset.TERMINAL_CODE -> drawTerminalCode(canvas, width, height)
            WallpaperPreset.MINIMAL_CARBON -> drawMinimalCarbon(canvas, width, height)
            WallpaperPreset.SUNSET_DEV -> drawSunsetDev(canvas, width, height)
        }

        presetCache[cacheKey] = bitmap
        return bitmap
    }

    fun getAppWallpaperBitmap(context: Context, width: Int = 720, height: Int = 1280): Bitmap? {
        val prefs = WidgetPreferences(context)
        return when (prefs.appWallpaperType) {
            "PRESET" -> {
                val preset = WallpaperPreset.fromId(prefs.appWallpaperPreset)
                getPresetBitmap(preset, width, height)
            }
            "CUSTOM" -> {
                getCustomWallpaperBitmap(prefs.appWallpaperCustomPath, maxDim = maxOf(width, height))
            }
            else -> null // "NONE" -> uses default obsidian gradient
        }
    }

    fun getWidgetWallpaperBitmap(context: Context, width: Int = 400, height: Int = 260, widgetKey: String? = null): Bitmap? {
        val prefs = WidgetPreferences(context)
        val type = if (widgetKey != null) prefs.getWidgetWallpaperTypeFor(widgetKey) else prefs.widgetWallpaperType
        val presetId = if (widgetKey != null) prefs.getWidgetWallpaperPresetFor(widgetKey) else prefs.widgetWallpaperPreset
        val customPath = if (widgetKey != null) prefs.getWidgetWallpaperCustomPathFor(widgetKey) else prefs.widgetWallpaperCustomPath
        val safeW = width.coerceIn(240, 420)
        val safeH = height.coerceIn(160, 300)
        return when (type) {
            "MATCH_APP" -> {
                getAppWallpaperBitmap(context, safeW, safeH)
            }
            "CUSTOM" -> {
                getCustomWallpaperBitmap(customPath, maxDim = maxOf(safeW, safeH))
            }
            "PRESET" -> {
                val preset = WallpaperPreset.fromId(presetId)
                getPresetBitmap(preset, safeW, safeH)
            }
            else -> null
        }
    }

    // --- Procedural Preset Artists ---

    private fun drawObsidianCore(canvas: Canvas, w: Int, h: Int) {
        val basePaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                intArrayOf(Color.parseColor("#050505"), Color.parseColor("#101010"), Color.parseColor("#050505")),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), basePaint)

        // Subtle monochromatic zenith glow
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.5f, h * 0.25f, w * 0.65f,
                intArrayOf(Color.argb(25, 255, 255, 255), Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), glowPaint)

        // Micro tech crosshair grid
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(18, 255, 255, 255)
            strokeWidth = 1f
        }
        val step = 48f
        var x = 0f
        while (x < w) {
            canvas.drawLine(x, 0f, x, h.toFloat(), gridPaint)
            x += step
        }
        var y = 0f
        while (y < h) {
            canvas.drawLine(0f, y, w.toFloat(), y, gridPaint)
            y += step
        }
    }

    private fun drawCyberMatrix(canvas: Canvas, w: Int, h: Int) {
        val basePaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                intArrayOf(Color.parseColor("#040404"), Color.parseColor("#0D0D0D"), Color.parseColor("#040404")),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), basePaint)

        // Matrix tech monochrome grid
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(20, 255, 255, 255)
            strokeWidth = 1.0f
        }
        val step = 56f
        var x = 0f
        while (x < w) {
            canvas.drawLine(x, 0f, x, h.toFloat(), linePaint)
            x += step
        }
        var y = 0f
        while (y < h) {
            canvas.drawLine(0f, y, w.toFloat(), y, linePaint)
            y += step
        }
    }

    private fun drawDeepCosmos(canvas: Canvas, w: Int, h: Int) {
        val basePaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                intArrayOf(Color.parseColor("#000000"), Color.parseColor("#080808"), Color.parseColor("#000000")),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), basePaint)

        // Starfield (Pure Monochrome)
        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }
        val rand = Random(42)
        for (i in 0..120) {
            val sx = rand.nextFloat() * w
            val sy = rand.nextFloat() * h
            val sr = rand.nextFloat() * 1.5f + 0.4f
            starPaint.alpha = rand.nextInt(30, 200)
            canvas.drawCircle(sx, sy, sr, starPaint)
        }
    }

    private fun drawTerminalCode(canvas: Canvas, w: Int, h: Int) {
        val basePaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                intArrayOf(Color.parseColor("#060606"), Color.parseColor("#0F0F0F"), Color.parseColor("#050505")),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), basePaint)

        val codePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(20, 255, 255, 255)
            textSize = 14f
            typeface = Typeface.MONOSPACE
        }

        val snippets = listOf(
            "const val DP_MASK = 0x7FFFFFFF",
            "fun solve(nums: IntArray): Int {",
            "    val memo = HashMap<Int, Int>()",
            "    val result = dp(nums, 0, memo)",
            "    return result % MOD",
            "}",
            "git commit -m \"feat: optimize widget renderer\"",
            "class Solution : LeetCodeSolver {",
            "    fun maxProfit(prices: IntArray): Int",
            "}"
        )

        var y = 40f
        var index = 0
        while (y < h) {
            val line = snippets[index % snippets.size]
            canvas.drawText(line, 24f, y, codePaint)
            y += 28f
            index++
        }
    }

    private fun drawMinimalCarbon(canvas: Canvas, w: Int, h: Int) {
        val basePaint = Paint().apply {
            color = Color.parseColor("#080808")
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), basePaint)

        val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(16, 255, 255, 255)
            strokeWidth = 2.5f
        }

        var offset = -h.toFloat()
        val spacing = 16f
        while (offset < w + h) {
            canvas.drawLine(offset, 0f, offset + h, h.toFloat(), stripePaint)
            offset += spacing
        }

        // Vignette
        val vignette = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.5f, h * 0.5f, maxOf(w, h) * 0.6f,
                intArrayOf(Color.TRANSPARENT, Color.argb(140, 0, 0, 0)),
                floatArrayOf(0.4f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), vignette)
    }

    private fun drawSunsetDev(canvas: Canvas, w: Int, h: Int) {
        val basePaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                intArrayOf(
                    Color.parseColor("#050505"),
                    Color.parseColor("#121212"),
                    Color.parseColor("#1A1A1A"),
                    Color.parseColor("#080808")
                ),
                floatArrayOf(0f, 0.4f, 0.7f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), basePaint)

        // Soft monochrome studio center vignette
        val studioGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.5f, h * 0.5f, w * 0.65f,
                intArrayOf(Color.argb(25, 255, 255, 255), Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), studioGlow)
    }
}
