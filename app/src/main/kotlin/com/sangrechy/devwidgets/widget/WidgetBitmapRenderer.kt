package com.sangrechy.devwidgets.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.sangrechy.devwidgets.data.model.GitHubStats
import com.sangrechy.devwidgets.data.model.LeetCodeStats
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object WidgetBitmapRenderer {

    // Frosted Glass Palette Tokens
    private const val COLOR_TEXT_WHITE = 0xFFFFFFFF.toInt()
    private const val COLOR_TEXT_SLATE = 0xFFCBD5E1.toInt()
    private const val COLOR_TEXT_MUTED = 0xFF94A3B8.toInt()

    // 1. Frosted Glass Circular Gauge
    fun renderLeetCodeCircle(stats: LeetCodeStats, sizePx: Int = 360): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val center = sizePx / 2f
        val outerRadius = sizePx * 0.45f
        val strokeWidth = sizePx * 0.088f
        val innerRadius = outerRadius - strokeWidth

        // 1. Translucent Frosted Track Ring
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            color = Color.argb(35, 255, 255, 255)
        }
        canvas.drawCircle(center, center, outerRadius - (strokeWidth / 2f), trackPaint)

        // Track subtle specular rim
        val trackRim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = 1.2f
            color = Color.argb(55, 255, 255, 255)
        }
        canvas.drawCircle(center, center, outerRadius, trackRim)
        canvas.drawCircle(center, center, innerRadius, trackRim)

        // 2. Segmented Glowing Arcs (Easy, Medium, Hard)
        val total = stats.totalSolved.coerceAtLeast(1)
        val easyFraction = (stats.easySolved.toFloat() / total).coerceIn(0f, 1f)
        val medFraction = (stats.mediumSolved.toFloat() / total).coerceIn(0f, 1f)
        val hardFraction = (stats.hardSolved.toFloat() / total).coerceIn(0f, 1f)

        val arcRadius = outerRadius - (strokeWidth / 2f)
        val arcOval = RectF(center - arcRadius, center - arcRadius, center + arcRadius, center + arcRadius)

        val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth - 1.5f
            strokeCap = Paint.Cap.ROUND
        }

        var startAngle = -90f
        val gapAngle = 4f

        // Easy Arc (Emerald Teal)
        if (easyFraction > 0.03f) {
            arcPaint.color = Color.parseColor("#00B8A3")
            val sweep = (easyFraction * 360f) - gapAngle
            if (sweep > 0) canvas.drawArc(arcOval, startAngle, sweep, false, arcPaint)
            startAngle += (easyFraction * 360f)
        }

        // Medium Arc (Warm Amber)
        if (medFraction > 0.03f) {
            arcPaint.color = Color.parseColor("#FFA116")
            val sweep = (medFraction * 360f) - gapAngle
            if (sweep > 0) canvas.drawArc(arcOval, startAngle, sweep, false, arcPaint)
            startAngle += (medFraction * 360f)
        }

        // Hard Arc (Rose Coral)
        if (hardFraction > 0.03f) {
            arcPaint.color = Color.parseColor("#EF4743")
            val sweep = (hardFraction * 360f) - gapAngle
            if (sweep > 0) canvas.drawArc(arcOval, startAngle, sweep, false, arcPaint)
        }

        // 3. Center Disc (Frosted Translucent Glass Core)
        val coreRadius = innerRadius - 3f
        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                center - coreRadius, center - coreRadius,
                center + coreRadius, center + coreRadius,
                Color.argb(45, 255, 255, 255),
                Color.argb(20, 255, 255, 255),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(center, center, coreRadius, corePaint)

        // Crystalline rim on center core
        val coreRim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = 1.2f
            color = Color.argb(90, 255, 255, 255)
        }
        canvas.drawCircle(center, center, coreRadius, coreRim)

        // 4. Typography: Bold White Solved Count
        val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_WHITE
            textSize = sizePx * 0.22f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val countY = center - ((countPaint.descent() + countPaint.ascent()) / 2f) - (sizePx * 0.055f)
        canvas.drawText(stats.totalSolved.toString(), center, countY, countPaint)

        // Subtitle: "SOLVED"
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SLATE
            textSize = sizePx * 0.075f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.16f
        }
        canvas.drawText("SOLVED", center, countY + (sizePx * 0.12f), labelPaint)

        // 5. Tactile Glass Capsule for E / M / H breakdown
        val pillWidth = sizePx * 0.54f
        val pillHeight = sizePx * 0.11f
        val pillTop = countY + (sizePx * 0.17f)
        val pillRect = RectF(center - (pillWidth / 2f), pillTop, center + (pillWidth / 2f), pillTop + pillHeight)

        val pillBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(45, 255, 255, 255)
        }
        canvas.drawRoundRect(pillRect, pillHeight / 2f, pillHeight / 2f, pillBg)

        val pillRim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = 0.8f
            color = Color.argb(70, 255, 255, 255)
        }
        canvas.drawRoundRect(pillRect, pillHeight / 2f, pillHeight / 2f, pillRim)

        val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_WHITE
            textSize = sizePx * 0.062f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val breakdownText = "${stats.easySolved}  •  ${stats.mediumSolved}  •  ${stats.hardSolved}"
        val pillTextY = pillTop + (pillHeight / 2f) - ((subTextPaint.descent() + subTextPaint.ascent()) / 2f)
        canvas.drawText(breakdownText, center, pillTextY, subTextPaint)

        return bitmap
    }

    // 2. Frosted Glass Heatmap Matrix
    fun renderHeatmap(
        countsByDateOrTimestamp: Map<*, Int>,
        isLeetCode: Boolean,
        widthPx: Int = 600,
        heightPx: Int = 220,
        weeksCount: Int = 18
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cornerR = 14f

        // 1. Frosted Translucent Plate Base
        val plateRect = RectF(4f, 4f, widthPx - 4f, heightPx - 4f)
        val platePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                0f, 0f, 0f, heightPx.toFloat(),
                Color.argb(35, 255, 255, 255),
                Color.argb(16, 255, 255, 255),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(plateRect, cornerR, cornerR, platePaint)

        // Crystalline rim stroke
        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = Color.argb(55, 255, 255, 255)
        }
        canvas.drawRoundRect(plateRect, cornerR, cornerR, rimPaint)

        // 2. Header: "heatmap." in monospace
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SLATE
            textSize = heightPx * 0.095f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
            letterSpacing = 0.10f
        }
        canvas.drawText("heatmap.", 18f, heightPx * 0.17f, titlePaint)

        // 3. Grid Coordinates
        val gridTop = heightPx * 0.25f
        val gridBottom = heightPx * 0.90f
        val availableHeight = gridBottom - gridTop
        val cellRows = 7
        val cellCols = weeksCount

        val cellSize = (availableHeight / cellRows).coerceAtMost((widthPx * 0.92f) / cellCols)
        val cellGap = cellSize * 0.20f
        val actualCell = cellSize - cellGap

        val totalGridWidth = (actualCell + cellGap) * cellCols - cellGap
        val gridLeft = (widthPx - totalGridWidth) / 2f

        // Glowing Glass Levels
        val levelColors = if (isLeetCode) {
            intArrayOf(
                Color.argb(35, 255, 255, 255),   // Translucent empty cell
                Color.argb(150, 45, 212, 191),   // Level 1: Soft teal
                Color.argb(210, 20, 184, 166),   // Level 2: Vibrant teal
                Color.parseColor("#00B8A3"),     // Level 3: Deep teal
                Color.parseColor("#14B8A6")      // Level 4: Glowing teal
            )
        } else {
            intArrayOf(
                Color.argb(35, 255, 255, 255),   // Translucent empty cell
                Color.argb(150, 74, 222, 128),   // Level 1: Soft green
                Color.argb(210, 34, 197, 94),    // Level 2: Vibrant green
                Color.parseColor("#22C55E"),     // Level 3: Emerald
                Color.parseColor("#39D353")      // Level 4: Glowing lime
            )
        }

        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val todayDow = calendar.get(Calendar.DAY_OF_WEEK) - 1
        val daysBackTotal = (weeksCount - 1) * 7 + todayDow

        val calCopy = Calendar.getInstance()
        calCopy.add(Calendar.DAY_OF_YEAR, -daysBackTotal)

        for (col in 0 until cellCols) {
            for (row in 0 until cellRows) {
                val dayMillis = calCopy.timeInMillis
                val epochSec = dayMillis / 1000L
                val dateStr = sdf.format(calCopy.time)

                val count = if (isLeetCode) {
                    var matched = 0
                    for ((k, v) in countsByDateOrTimestamp) {
                        val ts = (k as? Long) ?: (k as? Number)?.toLong() ?: continue
                        if (Math.abs(ts - epochSec) < 43200) {
                            matched += v
                        }
                    }
                    matched
                } else {
                    countsByDateOrTimestamp[dateStr] ?: 0
                }

                val level = when {
                    count <= 0 -> 0
                    count == 1 -> 1
                    count in 2..3 -> 2
                    count in 4..6 -> 3
                    else -> 4
                }

                cellPaint.color = levelColors[level]
                val x = gridLeft + col * (actualCell + cellGap)
                val y = gridTop + row * (actualCell + cellGap)
                val cellRect = RectF(x, y, x + actualCell, y + actualCell)
                canvas.drawRoundRect(cellRect, actualCell * 0.32f, actualCell * 0.32f, cellPaint)

                calCopy.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        return bitmap
    }

    // 3. Frosted Glass 4-Quadrant Contribution Type Map
    fun renderGitHubTypeMap(stats: GitHubStats, sizePx: Int = 360): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val center = sizePx / 2f
        val discRadius = sizePx * 0.45f

        // 1. Frosted Translucent Disc Base
        val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                center - discRadius, center - discRadius,
                center + discRadius, center + discRadius,
                Color.argb(40, 255, 255, 255),
                Color.argb(18, 255, 255, 255),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(center, center, discRadius, basePaint)

        // Crystalline rim stroke
        val bevelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            color = Color.argb(70, 255, 255, 255)
        }
        canvas.drawCircle(center, center, discRadius, bevelPaint)

        // 2. Concentric guide rings
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = Color.argb(45, 255, 255, 255)
        }
        canvas.drawCircle(center, center, discRadius * 0.35f, ringPaint)
        canvas.drawCircle(center, center, discRadius * 0.70f, ringPaint)

        // 3. Cross Axes (White translucent coordinate lines)
        val axisLength = discRadius * 0.76f
        val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(90, 255, 255, 255)
            strokeWidth = 1.8f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(center - axisLength, center, center + axisLength, center, axisPaint)
        canvas.drawLine(center, center - axisLength, center, center + axisLength, axisPaint)

        // 4. Radar Polygon Shape
        val commits = stats.commitsCount.coerceAtLeast(1).toFloat()
        val prs = stats.pullRequestsCount.coerceAtLeast(0).toFloat()
        val issues = stats.issuesCount.coerceAtLeast(0).toFloat()
        val reviews = stats.reviewsCount.coerceAtLeast(0).toFloat()

        val maxVal = maxOf(commits, prs * 3.5f, issues * 4.5f, reviews * 3.5f, 60f)
        val cNorm = (commits / maxVal).coerceIn(0.25f, 1f) * axisLength
        val prNorm = ((prs * 3.5f) / maxVal).coerceIn(0.20f, 1f) * axisLength
        val issueNorm = ((issues * 4.5f) / maxVal).coerceIn(0.20f, 1f) * axisLength
        val revNorm = ((reviews * 3.5f) / maxVal).coerceIn(0.20f, 1f) * axisLength

        val polyPath = Path().apply {
            moveTo(center, center - cNorm)          // Top: Commits
            lineTo(center + prNorm, center)         // Right: PRs
            lineTo(center, center + issueNorm)       // Bottom: Issues
            lineTo(center - revNorm, center)        // Left: Reviews
            close()
        }

        // Glowing green gradient fill
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                center, center - cNorm, center, center + issueNorm,
                Color.argb(140, 34, 197, 94),
                Color.argb(60, 16, 185, 129),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(polyPath, fillPaint)

        // Glowing perimeter stroke
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#22C55E")
            strokeWidth = 2.8f
            style = Paint.Style.STROKE
        }
        canvas.drawPath(polyPath, strokePaint)

        // 5. Directional Vertex Dots
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#39D353")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center, center - cNorm, 4.5f, dotPaint)
        canvas.drawCircle(center + prNorm, center, 4.5f, dotPaint)
        canvas.drawCircle(center, center + issueNorm, 4.5f, dotPaint)
        canvas.drawCircle(center - revNorm, center, 4.5f, dotPaint)

        // 6. Bold White Labels on 4 Axes
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_WHITE
            textSize = sizePx * 0.065f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        // Top: Commits
        canvas.drawText("${stats.commitsCount} Commits", center, center - axisLength - 8f, labelPaint)
        // Bottom: Issues
        canvas.drawText("${stats.issuesCount} Issues", center, center + axisLength + 20f, labelPaint)
        // Right: PRs
        labelPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("${stats.pullRequestsCount} PRs", center + axisLength + 8f, center + 6f, labelPaint)
        // Left: Reviews
        labelPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("${stats.reviewsCount} Rev", center - axisLength - 8f, center + 6f, labelPaint)

        // Top Header
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SLATE
            textSize = sizePx * 0.060f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        canvas.drawText("type map.", center, sizePx * 0.12f, headerPaint)

        return bitmap
    }
}
