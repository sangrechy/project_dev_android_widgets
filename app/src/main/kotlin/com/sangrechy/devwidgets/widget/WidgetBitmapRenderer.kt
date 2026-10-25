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

        // 4. Open Bottom Area: Difficulty Breakdown Columns (Easy, Medium, Hard)
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
            canvas.drawText(count.toString(), colCenterX, bottomSectionY, valPaint)

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

    // 2. Frosted Glass Heatmap Matrix with Gaps Between Months & Dynamic Tiers (1 line, 2 lines as 6 6, 3 lines as 4 4 4, or more)
    fun renderHeatmap(
        countsByDateOrTimestamp: Map<*, Int>,
        isLeetCode: Boolean,
        widthPx: Int = 360,
        heightPx: Int = 410,
        weeksCount: Int = 52
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Determine optimal tier count based on aspect ratio
        val ratio = widthPx.toFloat() / heightPx.toFloat()
        val tiers = when {
            ratio >= 3.5f -> 1 // 1 line (12 months)
            ratio >= 1.55f -> 2 // 2 lines (6 6)
            ratio >= 0.70f -> 3 // 3 lines (4 4 4)
            else -> 4 // 4 lines (3 3 3 3)
        }

        // 2. Pre-calculate month transitions & normalize data
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

        val dayCountMap = mutableMapOf<String, Int>()
        var totalInPeriod = 0
        for ((k, v) in countsByDateOrTimestamp) {
            val dStr = when (k) {
                is String -> {
                    val epoch = k.toLongOrNull()
                    if (epoch != null && epoch > 100000000) {
                        sdf.format(Date(epoch * 1000L))
                    } else {
                        k
                    }
                }
                is Long -> sdf.format(Date(k * 1000L))
                is Number -> sdf.format(Date(k.toLong() * 1000L))
                else -> null
            }
            if (dStr != null) {
                dayCountMap[dStr] = (dayCountMap[dStr] ?: 0) + v
                totalInPeriod += v
            }
        }

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

        // 3. Collect week months
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

        // 4. Calculate layout parameters for the chosen tier count
        val cellRows = 7
        val weeksPerTier = (weeksCount + tiers - 1) / tiers
        val monthsPerTier = maxOf(1, 12 / tiers)
        val maxMonthTransitions = maxOf(1, monthsPerTier - 1)

        val leftDayMargin = 28f
        val rightMargin = 8f
        val availW = (widthPx - leftDayMargin - rightMargin).coerceAtLeast(80f)

        val widthDivisor = weeksPerTier + (weeksPerTier - 1) * 0.18f + maxMonthTransitions * 1.25f
        val maxCellFromW = availW / widthDivisor

        val fixedVerticalH = 20f + tiers * 14f + (tiers - 1) * 12f
        val availCellH = (heightPx - fixedVerticalH).coerceAtLeast(60f)
        val maxCellFromH = availCellH / (tiers * 8.08f)

        val actualCell = minOf(maxCellFromW, maxCellFromH).coerceIn(6.5f, 26f)
        val cellGap = (actualCell * 0.18f).coerceAtLeast(1.5f)
        val monthGap = (actualCell * 1.25f).coerceAtLeast(8.0f)

        val maxTierCols = weeksPerTier
        val totalGridW = maxTierCols * actualCell + (maxTierCols - 1) * cellGap + maxMonthTransitions * monthGap
        val gridLeft = maxOf(leftDayMargin, (widthPx - totalGridW) / 2f)

        // 5. Header Label & Legend
        val headerTitle = if (isLeetCode) {
            if (totalInPeriod > 0) "$totalInPeriod submissions in 1 year" else "Submissions in 1 year"
        } else {
            if (totalInPeriod > 0) "$totalInPeriod contributions in 1 year" else "Contributions in 1 year"
        }

        val headerY = (actualCell * 1.4f).coerceIn(16f, 24f)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SLATE
            textSize = (actualCell * 1.0f).coerceIn(10.5f, 15f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText(headerTitle, gridLeft, headerY, titlePaint)

        // Legend: Less [■][■][■][■][■] More
        val legendTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = (actualCell * 0.82f).coerceIn(8.5f, 12f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        val sqSize = (actualCell * 0.75f).coerceIn(6.5f, 11f)
        val sqGap = cellGap
        val moreWidth = legendTextPaint.measureText("More")

        val legendRight = (gridLeft + totalGridW).coerceAtMost(widthPx - 6f)
        canvas.drawText("More", legendRight, headerY, legendTextPaint)

        var sqRight = legendRight - moreWidth - 4f
        for (lvl in 4 downTo 0) {
            val sqPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = levelColors[lvl]
            }
            val sqLeft = sqRight - sqSize
            val sqTop = headerY - sqSize + 1f
            canvas.drawRoundRect(RectF(sqLeft, sqTop, sqRight, sqTop + sqSize), 1.5f, 1.5f, sqPaint)
            sqRight -= (sqSize + sqGap)
        }
        canvas.drawText("Less", sqRight - 4f, headerY, legendTextPaint)

        // 6. Draw Tiers
        val dayTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = (actualCell * 0.72f).coerceIn(8f, 11f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        val monthTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = (actualCell * 0.82f).coerceIn(9f, 12.5f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(35, 255, 255, 255)
            strokeWidth = 1f
        }

        val dowRows = mapOf(1 to "Mon", 3 to "Wed", 5 to "Fri")

        var currentTierTop = headerY + actualCell * 1.35f

        for (tierIndex in 0 until tiers) {
            val startWeek = tierIndex * weeksPerTier
            val endWeek = minOf(startWeek + weeksPerTier, weeksCount)
            val numCols = endWeek - startWeek
            if (numCols <= 0) break

            val monthY = currentTierTop
            val gridTop = monthY + actualCell * 0.5f

            // Day-of-week labels
            for ((r, label) in dowRows) {
                val y = gridTop + r * (actualCell + cellGap) + actualCell * 0.82f
                canvas.drawText(label, gridLeft - 4f, y, dayTextPaint)
            }

            val calTier = calCopy.clone() as Calendar
            calTier.add(Calendar.DAY_OF_YEAR, startWeek * 7)

            var currentX = gridLeft
            var lastMonth = -1

            for (col in 0 until numCols) {
                val weekIdx = startWeek + col
                val colMonth = weekMonthList[weekIdx]

                if (col > 0 && colMonth != weekMonthList[weekIdx - 1]) {
                    currentX += monthGap
                }
                if (colMonth != lastMonth) {
                    canvas.drawText(weekMonthNameList[weekIdx], currentX, monthY, monthTextPaint)
                    lastMonth = colMonth
                }

                for (row in 0 until cellRows) {
                    val dateStr = sdf.format(calTier.time)
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
                    canvas.drawRoundRect(cellRect, actualCell * 0.22f, actualCell * 0.22f, cellPaint)

                    calTier.add(Calendar.DAY_OF_YEAR, 1)
                }
                currentX += (actualCell + cellGap)
            }

            val tierBottom = gridTop + 7 * actualCell + 6 * cellGap

            // Divider between tiers
            if (tierIndex < tiers - 1) {
                val dividerY = tierBottom + actualCell * 0.6f
                canvas.drawLine(gridLeft, dividerY, gridLeft + totalGridW, dividerY, dividerPaint)
                currentTierTop = dividerY + actualCell * 1.35f
            }
        }

        return bitmap
    }

    // 3. Authentic GitHub Activity Overview (Large 4-Quadrant Spider/Radar Chart with Total Hub & Clean Labels)
    fun renderGitHubTypeMap(stats: GitHubStats, sizePx: Int = 360): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val center = sizePx / 2f
        val discRadius = sizePx * 0.485f

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

        // 2. Proportions & Axis setup (Significantly enlarged "+" graph)
        val commits = stats.commitsCount.coerceAtLeast(0).toFloat()
        val prs = stats.pullRequestsCount.coerceAtLeast(0).toFloat()
        val issues = stats.issuesCount.coerceAtLeast(0).toFloat()
        val reviews = stats.reviewsCount.coerceAtLeast(0).toFloat()

        val maxRadarRadius = sizePx * 0.36f // 129.6px in 360px (large radar polygon)
        val minRadarRadius = sizePx * 0.09f // 32.4px in 360px
        val axisLength = sizePx * 0.38f     // 136.8px cross length

        // Concentric guide rings (50%, 100%)
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = 1f
            color = Color.argb(35, 255, 255, 255)
        }
        canvas.drawCircle(center, center, minRadarRadius + (maxRadarRadius - minRadarRadius) * 0.50f, ringPaint)
        canvas.drawCircle(center, center, maxRadarRadius, ringPaint)

        // Plus cross coordinate lines
        val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(75, 255, 255, 255)
            this.strokeWidth = 1.5f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(center, center - axisLength, center, center + axisLength, axisPaint)
        canvas.drawLine(center - axisLength, center, center + axisLength, center, axisPaint)

        // 3. Radar Polygon Shape
        val maxVal = maxOf(commits, prs * 2.8f, issues * 4.0f, reviews * 3.0f, 40f)
        val cNorm = minRadarRadius + ((commits / maxVal).coerceIn(0.20f, 1f) * (maxRadarRadius - minRadarRadius))
        val prNorm = minRadarRadius + (((prs * 2.8f) / maxVal).coerceIn(0.20f, 1f) * (maxRadarRadius - minRadarRadius))
        val issueNorm = minRadarRadius + (((issues * 4.0f) / maxVal).coerceIn(0.20f, 1f) * (maxRadarRadius - minRadarRadius))
        val revNorm = minRadarRadius + (((reviews * 3.0f) / maxVal).coerceIn(0.20f, 1f) * (maxRadarRadius - minRadarRadius))

        val polyPath = Path().apply {
            moveTo(center, center - cNorm)          // Top: Commits
            lineTo(center + prNorm, center)         // Right: PRs
            lineTo(center, center + issueNorm)       // Bottom: Issues
            lineTo(center - revNorm, center)        // Left: Reviews
            close()
        }

        // Glowing green-to-cyan gradient fill
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                center, center - cNorm, center, center + issueNorm,
                Color.argb(135, 34, 197, 94),
                Color.argb(60, 16, 185, 129),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(polyPath, fillPaint)

        // Glowing perimeter stroke
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#22C55E")
            this.strokeWidth = 2.6f
            style = Paint.Style.STROKE
        }
        canvas.drawPath(polyPath, strokePaint)

        // Directional Vertex Dots
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        dotPaint.color = Color.parseColor("#22C55E")
        canvas.drawCircle(center, center - cNorm, 4.5f, dotPaint)
        dotPaint.color = Color.parseColor("#A855F7")
        canvas.drawCircle(center + prNorm, center, 4.5f, dotPaint)
        dotPaint.color = Color.parseColor("#3B82F6")
        canvas.drawCircle(center, center + issueNorm, 4.5f, dotPaint)
        dotPaint.color = Color.parseColor("#F59E0B")
        canvas.drawCircle(center - revNorm, center, 4.5f, dotPaint)

        // 4. Center Glass Hub (Total Contributions Count)
        val hubRadius = sizePx * 0.095f
        val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(215, 15, 23, 42)
        }
        canvas.drawCircle(center, center, hubRadius, hubPaint)
        val hubStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = 1f
            color = Color.argb(65, 255, 255, 255)
        }
        canvas.drawCircle(center, center, hubRadius, hubStrokePaint)

        val hubValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_WHITE
            textSize = sizePx * 0.065f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val hubTextY = center - ((hubValPaint.descent() + hubValPaint.ascent()) / 2f) - 4f
        canvas.drawText(stats.totalContributions.toString(), center, hubTextY, hubValPaint)

        val hubSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = sizePx * 0.026f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        canvas.drawText("CONTRIBS", center, hubTextY + (sizePx * 0.040f), hubSubPaint)

        // 5. Four Cardinal Labels with Brand Colors (Clean, non-colliding layout)
        val labelValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_WHITE
            textSize = sizePx * 0.046f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val labelTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sizePx * 0.038f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        fun drawBadge(centerX: Float, centerY: Float, title: String, count: Int, colorHex: String, align: Paint.Align) {
            val color = Color.parseColor(colorHex)
            val countStr = count.toString()
            labelValPaint.textAlign = align
            labelTitlePaint.textAlign = align
            labelTitlePaint.color = color

            when (align) {
                Paint.Align.CENTER -> {
                    labelTitlePaint.color = color
                    canvas.drawText(title, centerX, centerY - 6f, labelTitlePaint)
                    canvas.drawText(countStr, centerX, centerY + 12f, labelValPaint)
                }
                Paint.Align.LEFT -> {
                    labelTitlePaint.color = color
                    canvas.drawText(title, centerX, centerY - 4f, labelTitlePaint)
                    canvas.drawText(countStr, centerX, centerY + 12f, labelValPaint)
                }
                Paint.Align.RIGHT -> {
                    labelTitlePaint.color = color
                    canvas.drawText(title, centerX, centerY - 4f, labelTitlePaint)
                    canvas.drawText(countStr, centerX, centerY + 12f, labelValPaint)
                }
            }
        }

        // Top: Commits (Emerald)
        drawBadge(center, 22f, "Commits", stats.commitsCount, "#22C55E", Paint.Align.CENTER)
        // Bottom: Issues (Blue)
        drawBadge(center, sizePx - 20f, "Issues", stats.issuesCount, "#3B82F6", Paint.Align.CENTER)
        // Right: PRs (Purple)
        drawBadge(sizePx - 16f, center + 4f, "PRs", stats.pullRequestsCount, "#A855F7", Paint.Align.RIGHT)
        // Left: Reviews (Amber)
        drawBadge(16f, center + 4f, "Reviews", stats.reviewsCount, "#F59E0B", Paint.Align.LEFT)

        return bitmap
    }
}
