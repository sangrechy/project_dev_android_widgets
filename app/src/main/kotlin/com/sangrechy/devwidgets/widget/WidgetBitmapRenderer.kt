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
import java.util.Date
import java.util.Locale

object WidgetBitmapRenderer {

    // Frosted Glass Palette Tokens
    private const val COLOR_TEXT_WHITE = 0xFFFFFFFF.toInt()
    private const val COLOR_TEXT_SLATE = 0xFFCBD5E1.toInt()
    private const val COLOR_TEXT_MUTED = 0xFF94A3B8.toInt()

    // 1. Authentic LeetCode 3/4 Speedometer Gauge (270° with open bottom for free difficulty stats)
    fun renderLeetCodeCircle(stats: LeetCodeStats, sizePx: Int = 360): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val center = sizePx / 2f
        val gaugeCenterY = sizePx * 0.42f
        val outerRadius = sizePx * 0.36f
        val strokeWidth = sizePx * 0.082f

        val arcOval = RectF(
            center - outerRadius, gaugeCenterY - outerRadius,
            center + outerRadius, gaugeCenterY + outerRadius
        )

        // 1. Translucent Frosted Base Track (270° arc starting at 135° to 405°)
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            color = Color.argb(35, 255, 255, 255)
        }
        canvas.drawArc(arcOval, 135f, 270f, false, trackPaint)

        // 2. Proportional Arcs: Easy, Medium, Hard along the 270° track
        val total = stats.totalSolved.coerceAtLeast(1)
        val easyFraction = (stats.easySolved.toFloat() / total).coerceIn(0f, 1f)
        val medFraction = (stats.mediumSolved.toFloat() / total).coerceIn(0f, 1f)
        val hardFraction = (stats.hardSolved.toFloat() / total).coerceIn(0f, 1f)

        val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
        }

        var currentAngle = 135f
        val gapAngle = 4f

        // Easy Arc (Emerald Teal: #00B8A3)
        val easySweep = (easyFraction * 270f)
        if (easySweep > 6f) {
            arcPaint.color = Color.parseColor("#00B8A3")
            val sweep = (easySweep - gapAngle).coerceAtLeast(2f)
            canvas.drawArc(arcOval, currentAngle + 2f, sweep, false, arcPaint)
        }
        currentAngle += easySweep

        // Medium Arc (Warm Amber: #FFA116)
        val medSweep = (medFraction * 270f)
        if (medSweep > 6f) {
            arcPaint.color = Color.parseColor("#FFA116")
            val sweep = (medSweep - gapAngle).coerceAtLeast(2f)
            canvas.drawArc(arcOval, currentAngle + 2f, sweep, false, arcPaint)
        }
        currentAngle += medSweep

        // Hard Arc (Rose Coral: #EF4743)
        val hardSweep = (hardFraction * 270f)
        if (hardSweep > 6f) {
            arcPaint.color = Color.parseColor("#EF4743")
            val sweep = (hardSweep - gapAngle).coerceAtLeast(2f)
            canvas.drawArc(arcOval, currentAngle + 2f, sweep, false, arcPaint)
        }

        // 3. Center: Total Solved Number & "SOLVED"
        val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_WHITE
            textSize = sizePx * 0.22f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val countY = gaugeCenterY - ((countPaint.descent() + countPaint.ascent()) / 2f) - (sizePx * 0.03f)
        canvas.drawText(stats.totalSolved.toString(), center, countY, countPaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = sizePx * 0.065f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.16f
        }
        canvas.drawText("SOLVED", center, countY + (sizePx * 0.10f), labelPaint)

        // 4. Open Bottom Area: Free Difficulty Breakdown Columns (Easy, Medium, Hard)
        val bottomSectionY = sizePx * 0.81f
        val dotRadius = sizePx * 0.016f

        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_WHITE
            textSize = sizePx * 0.082f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sizePx * 0.056f
            textAlign = Paint.Align.LEFT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        fun drawDifficultyCol(colCenterX: Float, count: Int, label: String, colorHex: String) {
            val color = Color.parseColor(colorHex)
            // Number on top (centered)
            canvas.drawText(count.toString(), colCenterX, bottomSectionY, valPaint)

            // Dot + Label centered together below
            tagPaint.color = color
            dotPaint.color = color
            val labelWidth = tagPaint.measureText(label)
            val gap = sizePx * 0.016f
            val itemWidth = (dotRadius * 2f) + gap + labelWidth
            val startX = colCenterX - (itemWidth / 2f)

            val tagY = bottomSectionY + (sizePx * 0.095f)
            val dotCenterY = tagY - (sizePx * 0.018f)

            canvas.drawCircle(startX + dotRadius, dotCenterY, dotRadius, dotPaint)
            canvas.drawText(label, startX + (dotRadius * 2f) + gap, tagY, tagPaint)
        }

        drawDifficultyCol(sizePx * 0.20f, stats.easySolved, "Easy", "#00B8A3")
        drawDifficultyCol(sizePx * 0.50f, stats.mediumSolved, "Med", "#FFA116")
        drawDifficultyCol(sizePx * 0.80f, stats.hardSolved, "Hard", "#EF4743")

        return bitmap
    }

    // 2. Frosted Glass Heatmap Matrix with Gaps Between Months & Month Labels (As in LeetCode)
    fun renderHeatmap(
        countsByDateOrTimestamp: Map<*, Int>,
        isLeetCode: Boolean,
        widthPx: Int = 640,
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
            this.strokeWidth = 1f
            color = Color.argb(55, 255, 255, 255)
        }
        canvas.drawRoundRect(plateRect, cornerR, cornerR, rimPaint)

        // 2. Pre-calculate month transitions for realistic LeetCode month gaps
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val monthSdf = SimpleDateFormat("MMM", Locale.US)

        val todayDow = calendar.get(Calendar.DAY_OF_WEEK) - 1
        val daysBackTotal = (weeksCount - 1) * 7 + todayDow

        val calCopy = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        calCopy.add(Calendar.DAY_OF_YEAR, -daysBackTotal)

        // Pre-normalize all counts to "yyyy-MM-dd" for O(1) matching
        val dayCountMap = mutableMapOf<String, Int>()
        for ((k, v) in countsByDateOrTimestamp) {
            when (k) {
                is String -> {
                    val epoch = k.toLongOrNull()
                    if (epoch != null && epoch > 100000000) {
                        val dStr = sdf.format(Date(epoch * 1000L))
                        dayCountMap[dStr] = (dayCountMap[dStr] ?: 0) + v
                    } else {
                        dayCountMap[k] = (dayCountMap[k] ?: 0) + v
                    }
                }
                is Long -> {
                    val dStr = sdf.format(Date(k * 1000L))
                    dayCountMap[dStr] = (dayCountMap[dStr] ?: 0) + v
                }
                is Number -> {
                    val dStr = sdf.format(Date(k.toLong() * 1000L))
                    dayCountMap[dStr] = (dayCountMap[dStr] ?: 0) + v
                }
            }
        }

        // Calculate available cell sizing
        val cellRows = 7
        val gridTop = heightPx * 0.28f
        val gridBottom = heightPx * 0.90f
        val availableHeight = gridBottom - gridTop

        val rowUnit = availableHeight / cellRows
        val cellGap = rowUnit * 0.18f
        val actualCell = rowUnit - cellGap
        val monthGap = actualCell * 1.05f // Visible spacing between months as in LeetCode

        // Pre-scan to find week-to-month boundaries
        val weekMonthList = mutableListOf<Int>()
        val weekMonthNameList = mutableListOf<String>()
        val calScanner = calCopy.clone() as Calendar

        for (w in 0 until weeksCount) {
            val m = calScanner.get(Calendar.MONTH)
            val mName = monthSdf.format(calScanner.time)
            weekMonthList.add(m)
            weekMonthNameList.add(mName)
            calScanner.add(Calendar.DAY_OF_YEAR, 7)
        }

        var totalMonthTransitions = 0
        for (w in 1 until weeksCount) {
            if (weekMonthList[w] != weekMonthList[w - 1]) {
                totalMonthTransitions++
            }
        }

        val totalGridWidth = (weeksCount * (actualCell + cellGap) - cellGap) + (totalMonthTransitions * monthGap)
        val gridLeft = (widthPx - totalGridWidth) / 2f

        // Month Labels Painter
        val monthTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = heightPx * 0.082f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }

        // Header "heatmap." painter
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SLATE
            textSize = heightPx * 0.085f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
            letterSpacing = 0.08f
        }
        canvas.drawText("heatmap.", 16f, heightPx * 0.17f, titlePaint)

        // Glowing Levels
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

        // 3. Render Columns with Month Gaps & Labels
        var currentX = gridLeft
        var lastLabelMonth = -1

        for (col in 0 until weeksCount) {
            val colMonth = weekMonthList[col]
            if (col > 0 && colMonth != weekMonthList[col - 1]) {
                currentX += monthGap // Month Gap as in LeetCode
            }

            // Draw month label at start of each month block (skip orphan tail week 0)
            if (colMonth != lastLabelMonth) {
                val isNotCrowdedOrphan = (col > 0) || (col + 1 < weeksCount && weekMonthList[col + 1] == colMonth)
                if (isNotCrowdedOrphan) {
                    canvas.drawText(weekMonthNameList[col], currentX, heightPx * 0.22f, monthTextPaint)
                }
                lastLabelMonth = colMonth
            }

            for (row in 0 until cellRows) {
                val dateStr = sdf.format(calCopy.time)
                val count = dayCountMap[dateStr] ?: 0

                val level = when {
                    count <= 0 -> 0
                    count == 1 -> 1
                    count in 2..3 -> 2
                    count in 4..6 -> 3
                    else -> 4
                }

                cellPaint.color = levelColors[level]
                val y = gridTop + row * (actualCell + cellGap)
                val cellRect = RectF(currentX, y, currentX + actualCell, y + actualCell)
                canvas.drawRoundRect(cellRect, actualCell * 0.30f, actualCell * 0.30f, cellPaint)

                calCopy.add(Calendar.DAY_OF_YEAR, 1)
            }

            currentX += (actualCell + cellGap)
        }

        return bitmap
    }

    // 3. Frosted Glass 4-Quadrant Contribution Type Map
    fun renderGitHubTypeMap(stats: GitHubStats, sizePx: Int = 360): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val center = sizePx / 2f
        val discRadius = sizePx * 0.44f

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
            this.strokeWidth = 1.2f
            color = Color.argb(70, 255, 255, 255)
        }
        canvas.drawCircle(center, center, discRadius, bevelPaint)

        // 2. Concentric guide rings
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = 1f
            color = Color.argb(45, 255, 255, 255)
        }
        canvas.drawCircle(center, center, discRadius * 0.32f, ringPaint)
        canvas.drawCircle(center, center, discRadius * 0.65f, ringPaint)

        // 3. Cross Axes (White translucent coordinate lines)
        val axisLength = discRadius * 0.62f
        val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(90, 255, 255, 255)
            this.strokeWidth = 1.8f
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
            this.strokeWidth = 2.8f
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
            textSize = sizePx * 0.058f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        // Top: Commits
        canvas.drawText("${stats.commitsCount} Commits", center, center - axisLength - (sizePx * 0.035f), labelPaint)
        // Bottom: Issues
        canvas.drawText("${stats.issuesCount} Issues", center, center + axisLength + (sizePx * 0.065f), labelPaint)
        // Right: PRs
        labelPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("${stats.pullRequestsCount} PRs", center + axisLength + (sizePx * 0.025f), center + (sizePx * 0.02f), labelPaint)
        // Left: Reviews
        labelPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("${stats.reviewsCount} Rev", center - axisLength - (sizePx * 0.025f), center + (sizePx * 0.02f), labelPaint)

        // Top-Left Header "type map."
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SLATE
            textSize = sizePx * 0.055f
            textAlign = Paint.Align.LEFT
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        canvas.drawText("type map.", 14f, sizePx * 0.16f, headerPaint)

        return bitmap
    }
}
