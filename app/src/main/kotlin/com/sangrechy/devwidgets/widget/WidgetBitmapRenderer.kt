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
        val safeSize = sizePx.coerceAtLeast(60)
        val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val center = safeSize / 2f
        val gaugeCenterY = safeSize * 0.42f
        val outerRadius = safeSize * 0.36f
        val strokeWidth = safeSize * 0.082f

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
            textSize = safeSize * 0.22f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val countY = gaugeCenterY - ((countPaint.descent() + countPaint.ascent()) / 2f) - (safeSize * 0.03f)
        canvas.drawText(stats.totalSolved.toString(), center, countY, countPaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = safeSize * 0.065f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.16f
        }
        canvas.drawText("SOLVED", center, countY + (safeSize * 0.10f), labelPaint)

        // 4. Open Bottom Area: Difficulty Breakdown Columns (Easy, Medium, Hard)
        val bottomSectionY = safeSize * 0.81f
        val dotRadius = safeSize * 0.016f

        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_WHITE
            textSize = safeSize * 0.082f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = safeSize * 0.056f
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
            val gap = safeSize * 0.016f
            val itemWidth = (dotRadius * 2f) + gap + labelWidth
            val startX = colCenterX - (itemWidth / 2f)

            val tagY = bottomSectionY + (safeSize * 0.095f)
            val dotCenterY = tagY - (safeSize * 0.018f)

            canvas.drawCircle(startX + dotRadius, dotCenterY, dotRadius, dotPaint)
            canvas.drawText(label, startX + (dotRadius * 2f) + gap, tagY, tagPaint)
        }

        drawDifficultyCol(safeSize * 0.20f, stats.easySolved, "Easy", "#00B8A3")
        drawDifficultyCol(safeSize * 0.50f, stats.mediumSolved, "Med", "#FFA116")
        drawDifficultyCol(safeSize * 0.80f, stats.hardSolved, "Hard", "#EF4743")

        return bitmap
    }

    // 2. Frosted Glass Heatmap Matrix with Gaps Between Months & Dynamic Tiers (Horizontal or Vertical 90°)
    fun renderHeatmap(
        countsByDateOrTimestamp: Map<*, Int>,
        isLeetCode: Boolean,
        widthPx: Int = 360,
        heightPx: Int = 410,
        weeksCount: Int = 52,
        orientation: String = "horizontal"
    ): Bitmap {
        val safeW = widthPx.coerceAtLeast(80)
        val safeH = heightPx.coerceAtLeast(80)
        val safeWeeks = weeksCount.coerceIn(4, 53)
        val bitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val isVertical = (orientation == "vertical")

        // 1. Determine optimal tier count based on aspect ratio and orientation
        val ratio = safeW.toFloat() / safeH.toFloat()
        val tiers = if (isVertical) {
            when {
                ratio >= 3.6f -> 1 // 1 line of 12 (wide strip)
                ratio >= 1.8f -> 2 // 2 lines of 6 6
                ratio >= 1.0f -> 3 // 3 lines of 4 4 4
                else -> 4 // 4 lines of 3 3 3 3 for taller widgets
            }
        } else {
            when {
                ratio >= 4.0f -> 1 // 1 line (12 months)
                ratio >= 2.1f -> 2 // 2 lines (6 6)
                else -> 3 // 3 lines (4 4 4)
            }
        }

        // 2. Normalize submission / contribution data
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val monthSdf = SimpleDateFormat("MMM", Locale.US)

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

        // 3. Generate 12 sequential months ending at current month
        data class MonthCalendarBlock(
            val year: Int,
            val monthIdx: Int,
            val monthName: String,
            val daysInMonth: Int,
            val firstDowMondayBased: Int
        )

        val months = mutableListOf<MonthCalendarBlock>()
        val calIter = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        calIter.add(Calendar.MONTH, -11)

        for (m in 0 until 12) {
            val y = calIter.get(Calendar.YEAR)
            val mIdx = calIter.get(Calendar.MONTH)
            val mName = monthSdf.format(calIter.time)
            val daysCount = calIter.getActualMaximum(Calendar.DAY_OF_MONTH)
            val calDow = calIter.get(Calendar.DAY_OF_WEEK)
            val firstDow = (calDow + 5) % 7 // Mon=0, Tue=1, ..., Sun=6
            months.add(MonthCalendarBlock(y, mIdx, mName, daysCount, firstDow))
            calIter.add(Calendar.MONTH, 1)
        }

        // 4. Partition 12 months among tiers
        val monthsPerRow = when (tiers) {
            1 -> 12
            2 -> 6
            3 -> 4
            else -> 3
        }
        val effectiveTiers = (12 + monthsPerRow - 1) / monthsPerRow

        val marginX = 12f
        val availW = (safeW - 2 * marginX).coerceAtLeast(80f)

        // Inside each month:
        // Horizontal: 7 columns (days of week: Mon..Sun), 6 rows (weeks)
        // Vertical (90° rotated as in LeetCode): 6 columns (weeks), 7 rows (days: Mon..Sun)
        val monthCols = if (isVertical) 6 else 7
        val monthRows = if (isVertical) 7 else 6

        // Layout sizing
        val monthGap = (availW / (monthsPerRow * 10f)).coerceIn(4f, 14f)
        val cellGapRatio = 0.16f

        val totalMonthGaps = (monthsPerRow - 1) * monthGap
        val availWForMonths = (availW - totalMonthGaps).coerceAtLeast(60f)
        val maxMonthW = availWForMonths / monthsPerRow

        val cellFromW = maxMonthW / (monthCols + (monthCols - 1) * cellGapRatio)

        val fixedH = 22f + effectiveTiers * 14f + (effectiveTiers - 1) * 8f
        val availHForMonths = (safeH - fixedH).coerceAtLeast(60f)
        val maxMonthH = availHForMonths / effectiveTiers
        val cellFromH = maxMonthH / (monthRows + (monthRows - 1) * cellGapRatio)

        val actualCell = minOf(cellFromW, cellFromH).coerceIn(4.5f, 22f)
        val cellGap = (actualCell * cellGapRatio).coerceAtLeast(1.0f)
        val uniformMonthW = monthCols * actualCell + (monthCols - 1) * cellGap
        val uniformMonthH = monthRows * actualCell + (monthRows - 1) * cellGap

        val totalGridW = monthsPerRow * uniformMonthW + (monthsPerRow - 1) * monthGap
        val gridLeft = maxOf(marginX, (safeW - totalGridW) / 2f)

        // 5. Header Title & Legend
        val headerTitle = if (isLeetCode) {
            if (totalInPeriod > 0) "$totalInPeriod submissions in 1 year" else "0 submissions in 1 year"
        } else {
            if (totalInPeriod > 0) "$totalInPeriod contributions in 1 year" else "0 contributions in 1 year"
        }

        val headerY = (actualCell * 1.5f).coerceIn(14f, 22f)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SLATE
            textSize = (actualCell * 1.05f).coerceIn(10f, 14.5f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText(headerTitle, gridLeft, headerY, titlePaint)

        // Legend: Less [■][■][■][■][■] More
        val legendTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = (actualCell * 0.85f).coerceIn(8f, 11.5f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        val sqSize = (actualCell * 0.78f).coerceIn(5.5f, 10.5f)
        val moreWidth = legendTextPaint.measureText("More")
        val legendRight = (gridLeft + totalGridW).coerceAtMost(safeW - 4f)
        canvas.drawText("More", legendRight, headerY, legendTextPaint)

        var sqRight = legendRight - moreWidth - 3f
        for (lvl in 4 downTo 0) {
            val sqPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = levelColors[lvl]
            }
            val sqLeft = sqRight - sqSize
            val sqTop = headerY - sqSize + 1f
            canvas.drawRoundRect(RectF(sqLeft, sqTop, sqRight, sqTop + sqSize), 1.5f, 1.5f, sqPaint)
            sqRight -= (sqSize + cellGap)
        }
        canvas.drawText("Less", sqRight - 3f, headerY, legendTextPaint)

        // 6. Draw Tiers with Uniform Month Shapes
        val monthTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = (actualCell * 0.92f).coerceIn(8.5f, 12f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        val faintDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(12, 255, 255, 255)
        }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(30, 255, 255, 255)
            strokeWidth = 1f
        }

        var currentTierTop = headerY + actualCell * 1.35f

        for (tierIndex in 0 until effectiveTiers) {
            val startM = tierIndex * monthsPerRow
            val endM = minOf(startM + monthsPerRow, 12)
            val monthY = currentTierTop
            val gridTop = monthY + actualCell * 0.6f

            for (mIdx in startM until endM) {
                val mb = months[mIdx]
                val colInTier = mIdx - startM
                val monthLeft = gridLeft + colInTier * (uniformMonthW + monthGap)

                // Centered month name
                val monthCenterX = monthLeft + uniformMonthW / 2f
                canvas.drawText(mb.monthName, monthCenterX, monthY, monthTextPaint)

                // Draw month cells
                for (dayNum in 1..mb.daysInMonth) {
                    val slot = mb.firstDowMondayBased + (dayNum - 1)
                    val c = if (isVertical) (slot / 7) else (slot % 7)
                    val r = if (isVertical) (slot % 7) else (slot / 7)
                    if (c < monthCols && r < monthRows) {
                        val dateStr = String.format(Locale.US, "%04d-%02d-%02d", mb.year, mb.monthIdx + 1, dayNum)
                        val count = dayCountMap[dateStr] ?: 0
                        val level = when {
                            count <= 0 -> 0
                            count == 1 -> 1
                            count in 2..3 -> 2
                            count in 4..6 -> 3
                            else -> 4
                        }
                        cellPaint.color = levelColors[level]
                        val x = monthLeft + c * (actualCell + cellGap)
                        val y = gridTop + r * (actualCell + cellGap)
                        canvas.drawRoundRect(RectF(x, y, x + actualCell, y + actualCell), actualCell * 0.22f, actualCell * 0.22f, cellPaint)
                    }
                }

                // Draw faint boundary dots for unused cells to preserve identical month geometry
                for (slot in 0 until (monthCols * monthRows)) {
                    val c = if (isVertical) (slot / 7) else (slot % 7)
                    val r = if (isVertical) (slot % 7) else (slot / 7)
                    val slotIdx = if (isVertical) (c * 7 + r) else slot
                    val dayNum = slotIdx - mb.firstDowMondayBased + 1
                    if (dayNum !in 1..mb.daysInMonth) {
                        val x = monthLeft + c * (actualCell + cellGap)
                        val y = gridTop + r * (actualCell + cellGap)
                        canvas.drawCircle(x + actualCell / 2f, y + actualCell / 2f, 0.8f, faintDotPaint)
                    }
                }
            }

            val tierBottom = gridTop + uniformMonthH

            // Divider between tiers
            if (tierIndex < effectiveTiers - 1) {
                val dividerY = tierBottom + actualCell * 0.45f
                canvas.drawLine(gridLeft, dividerY, gridLeft + totalGridW, dividerY, dividerPaint)
                currentTierTop = dividerY + actualCell * 1.35f
            }
        }

        return bitmap
    }

    // 3. Authentic GitHub Activity Overview (Large 4-Quadrant Spider/Radar Chart with Total Hub & Clean Labels)
    fun renderGitHubTypeMap(stats: GitHubStats, sizePx: Int = 360): Bitmap {
        val safeSize = sizePx.coerceAtLeast(60)
        val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val center = safeSize / 2f
        val discRadius = safeSize * 0.485f

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

        val maxRadarRadius = safeSize * 0.36f // large radar polygon
        val minRadarRadius = safeSize * 0.09f
        val axisLength = safeSize * 0.38f     // cross length

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
        val hubRadius = safeSize * 0.095f
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
            textSize = safeSize * 0.065f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val hubTextY = center - ((hubValPaint.descent() + hubValPaint.ascent()) / 2f) - 4f
        canvas.drawText(stats.totalContributions.toString(), center, hubTextY, hubValPaint)

        val hubSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = safeSize * 0.026f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        canvas.drawText("CONTRIBS", center, hubTextY + (safeSize * 0.040f), hubSubPaint)

        // 5. Four Cardinal Labels with Brand Colors (Clean, non-colliding layout)
        val labelValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_WHITE
            textSize = safeSize * 0.046f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val labelTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = safeSize * 0.038f
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
        drawBadge(center, safeSize - 20f, "Issues", stats.issuesCount, "#3B82F6", Paint.Align.CENTER)
        // Right: PRs (Purple)
        drawBadge(safeSize - 16f, center + 4f, "PRs", stats.pullRequestsCount, "#A855F7", Paint.Align.RIGHT)
        // Left: Reviews (Amber)
        drawBadge(16f, center + 4f, "Reviews", stats.reviewsCount, "#F59E0B", Paint.Align.LEFT)

        return bitmap
    }
}
