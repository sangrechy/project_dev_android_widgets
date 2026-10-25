package com.sangrechy.devwidgets.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.sangrechy.devwidgets.data.model.GitHubStats
import com.sangrechy.devwidgets.data.model.LeetCodeStats
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object WidgetBitmapRenderer {

    // 1. LeetCode Circle Counter & Breakdown
    fun renderLeetCodeCircle(stats: LeetCodeStats, sizePx: Int = 300): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val center = sizePx / 2f
        val strokeWidth = sizePx * 0.09f
        val radius = center - strokeWidth

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            color = Color.parseColor("#252A30")
        }
        canvas.drawCircle(center, center, radius, bgPaint)

        val total = stats.totalSolved.coerceAtLeast(1)
        val easyFraction = (stats.easySolved.toFloat() / total).coerceIn(0f, 1f)
        val medFraction = (stats.mediumSolved.toFloat() / total).coerceIn(0f, 1f)
        val hardFraction = (stats.hardSolved.toFloat() / total).coerceIn(0f, 1f)

        val oval = RectF(center - radius, center - radius, center + radius, center + radius)
        val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
        }

        var startAngle = -90f
        // Easy arc (Green)
        if (easyFraction > 0) {
            arcPaint.color = Color.parseColor("#00B8A3")
            val sweep = easyFraction * 360f
            canvas.drawArc(oval, startAngle, sweep, false, arcPaint)
            startAngle += sweep
        }
        // Medium arc (Yellow/Orange)
        if (medFraction > 0) {
            arcPaint.color = Color.parseColor("#FFC01E")
            val sweep = medFraction * 360f
            canvas.drawArc(oval, startAngle, sweep, false, arcPaint)
            startAngle += sweep
        }
        // Hard arc (Red)
        if (hardFraction > 0) {
            arcPaint.color = Color.parseColor("#EF4743")
            val sweep = hardFraction * 360f
            canvas.drawArc(oval, startAngle, sweep, false, arcPaint)
        }

        // Inner text: Total count
        val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = sizePx * 0.22f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val countY = center - ((countPaint.descent() + countPaint.ascent()) / 2f) - (sizePx * 0.05f)
        canvas.drawText(stats.totalSolved.toString(), center, countY, countPaint)

        // Subtitle: "SOLVED"
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8A939E")
            textSize = sizePx * 0.09f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            letterSpacing = 0.15f
        }
        canvas.drawText("SOLVED", center, countY + (sizePx * 0.16f), labelPaint)

        // Sub-counter at bottom: E: M: H:
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D0D7DE")
            textSize = sizePx * 0.075f
            textAlign = Paint.Align.CENTER
        }
        val breakdownText = "${stats.easySolved} / ${stats.mediumSolved} / ${stats.hardSolved}"
        canvas.drawText(breakdownText, center, countY + (sizePx * 0.28f), subPaint)

        return bitmap
    }

    // 2. Submission Activity Heatmap (7 rows x N weeks)
    fun renderHeatmap(
        countsByDateOrTimestamp: Map<*, Int>,
        isLeetCode: Boolean,
        widthPx: Int = 600,
        heightPx: Int = 300,
        weeksCount: Int = 18
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Title: "heatmap." (as in mockup)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9AA4B2")
            textSize = heightPx * 0.10f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("heatmap.", widthPx / 2f, heightPx * 0.12f, titlePaint)

        val gridTop = heightPx * 0.22f
        val gridBottom = heightPx * 0.94f
        val availableHeight = gridBottom - gridTop
        val cellRows = 7
        val cellCols = weeksCount

        val cellSize = (availableHeight / cellRows).coerceAtMost((widthPx * 0.92f) / cellCols)
        val cellGap = cellSize * 0.18f
        val actualCell = cellSize - cellGap

        val totalGridWidth = (actualCell + cellGap) * cellCols - cellGap
        val gridLeft = (widthPx - totalGridWidth) / 2f

        val emptyColor = if (isLeetCode) Color.parseColor("#262B32") else Color.parseColor("#161B22")
        val levelColors = if (isLeetCode) {
            intArrayOf(
                emptyColor,
                Color.parseColor("#1F6F3A"),
                Color.parseColor("#2DB55D"),
                Color.parseColor("#44D677"),
                Color.parseColor("#66FF99")
            )
        } else {
            intArrayOf(
                emptyColor,
                Color.parseColor("#0E4429"),
                Color.parseColor("#006D32"),
                Color.parseColor("#26A641"),
                Color.parseColor("#39D353")
            )
        }

        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        // Build 2D matrix backwards from current week
        val todayDow = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun .. 6=Sat
        val daysBackTotal = (weeksCount - 1) * 7 + todayDow

        val calCopy = Calendar.getInstance()
        calCopy.add(Calendar.DAY_OF_YEAR, -daysBackTotal)

        for (col in 0 until cellCols) {
            for (row in 0 until cellRows) {
                val dayMillis = calCopy.timeInMillis
                val epochSec = dayMillis / 1000L
                val dateStr = sdf.format(calCopy.time)

                val count = if (isLeetCode) {
                    // Try approximate epoch day matching
                    var matched = 0
                    for ((k, v) in countsByDateOrTimestamp) {
                        val ts = (k as? Long) ?: (k as? Number)?.toLong() ?: continue
                        if (Math.abs(ts - epochSec) < 43200) { // within 12 hours
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
                val rect = RectF(x, y, x + actualCell, y + actualCell)
                canvas.drawRoundRect(rect, actualCell * 0.25f, actualCell * 0.25f, cellPaint)

                calCopy.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        return bitmap
    }

    // 3. GitHub Contribution Type Map (Cross / 4-Quadrant Radar Chart)
    fun renderGitHubTypeMap(stats: GitHubStats, sizePx: Int = 300): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val center = sizePx / 2f
        val axisLength = sizePx * 0.36f

        // Draw cross axes (as drawn in mockup: a prominent cross)
        val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#444C56")
            strokeWidth = sizePx * 0.015f
            style = Paint.Style.STROKE
        }
        // Horizontal line
        canvas.drawLine(center - axisLength - (sizePx * 0.05f), center, center + axisLength + (sizePx * 0.05f), center, axisPaint)
        // Vertical line
        canvas.drawLine(center, center - axisLength - (sizePx * 0.05f), center, center + axisLength + (sizePx * 0.05f), axisPaint)

        // Normalization values for radar shape
        val commits = stats.commitsCount.coerceAtLeast(1).toFloat()
        val prs = stats.pullRequestsCount.coerceAtLeast(0).toFloat()
        val issues = stats.issuesCount.coerceAtLeast(0).toFloat()
        val reviews = stats.reviewsCount.coerceAtLeast(0).toFloat()

        val maxVal = maxOf(commits, prs * 3f, issues * 4f, reviews * 3f, 50f)
        val cNorm = (commits / maxVal).coerceIn(0.2f, 1f) * axisLength
        val prNorm = ((prs * 3f) / maxVal).coerceIn(0.15f, 1f) * axisLength
        val issueNorm = ((issues * 4f) / maxVal).coerceIn(0.15f, 1f) * axisLength
        val revNorm = ((reviews * 3f) / maxVal).coerceIn(0.15f, 1f) * axisLength

        // Polygon path
        val polyPath = Path().apply {
            moveTo(center, center - cNorm)          // Top: Commits
            lineTo(center + prNorm, center)         // Right: PRs
            lineTo(center, center + issueNorm)       // Bottom: Issues
            lineTo(center - revNorm, center)        // Left: Reviews
            close()
        }

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4026A641") // Translucent green
            style = Paint.Style.FILL
        }
        canvas.drawPath(polyPath, fillPaint)

        val polyStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#39D353")
            strokeWidth = sizePx * 0.022f
            style = Paint.Style.STROKE
        }
        canvas.drawPath(polyPath, polyStrokePaint)

        // Labels on 4 directions
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C9D1D9")
            textSize = sizePx * 0.072f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        // Top: Commits
        canvas.drawText("${stats.commitsCount} Commits", center, center - axisLength - (sizePx * 0.07f), labelPaint)
        // Bottom: Issues
        canvas.drawText("${stats.issuesCount} Issues", center, center + axisLength + (sizePx * 0.12f), labelPaint)
        // Right: PRs
        labelPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("${stats.pullRequestsCount} PRs", center + axisLength + (sizePx * 0.04f), center + (sizePx * 0.025f), labelPaint)
        // Left: Reviews
        labelPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("${stats.reviewsCount} Rev", center - axisLength - (sizePx * 0.04f), center + (sizePx * 0.025f), labelPaint)

        // Top title: "contrib type map"
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8B949E")
            textSize = sizePx * 0.065f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("type breakdown", center, sizePx * 0.10f, titlePaint)

        return bitmap
    }
}
