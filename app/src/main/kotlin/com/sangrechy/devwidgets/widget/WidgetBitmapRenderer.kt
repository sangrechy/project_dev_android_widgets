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

    // 2. Frosted Glass Heatmap Matrix with Gaps Between Months & Authentic GitHub/LeetCode Labels
    fun renderHeatmap(
        countsByDateOrTimestamp: Map<*, Int>,
        isLeetCode: Boolean,
        widthPx: Int = 680,
        heightPx: Int = 200,
        weeksCount: Int = 26
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

        // 2. Pre-calculate month transitions for realistic LeetCode/GitHub month gaps
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

        // 3. Header Label & Legend (Authentic GitHub / LeetCode style)
        val headerTitle = if (isLeetCode) {
            if (totalInPeriod > 0) "$totalInPeriod submissions in 6 months" else "Submissions in 6 months"
        } else {
            if (totalInPeriod > 0) "$totalInPeriod contributions in 6 months" else "Contributions in 6 months"
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SLATE
            textSize = heightPx * 0.080f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText(headerTitle, 16f, heightPx * 0.17f, titlePaint)

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

        // Authentic Legend: Less [■][■][■][■][■] More
        val legendTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = heightPx * 0.062f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        val sqSize = heightPx * 0.052f
        val sqGap = 3.5f
        val moreWidth = legendTextPaint.measureText("More")

        val moreX = widthPx - 16f
        canvas.drawText("More", moreX, heightPx * 0.17f, legendTextPaint)

        var sqRight = moreX - moreWidth - 6f
        for (lvl in 4 downTo 0) {
            val sqPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = levelColors[lvl]
            }
            val sqLeft = sqRight - sqSize
            val sqTop = (heightPx * 0.17f) - sqSize + 1f
            canvas.drawRoundRect(RectF(sqLeft, sqTop, sqRight, sqTop + sqSize), 2f, 2f, sqPaint)
            sqRight -= (sqSize + sqGap)
        }
        canvas.drawText("Less", sqRight - 6f, heightPx * 0.17f, legendTextPaint)

        // 4. Calculate Grid Layout & Month Transitions
        val cellRows = 7
        val gridTop = heightPx * 0.35f
        val gridBottom = heightPx * 0.94f
        val availableHeight = gridBottom - gridTop

        val rowUnit = availableHeight / cellRows
        val cellGap = rowUnit * 0.18f
        val actualCell = rowUnit - cellGap
        val monthGap = actualCell * 0.85f

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
        val gridLeft = maxOf(40f, (widthPx - totalGridWidth) / 2f + 12f)

        // Day of week labels on left (Mon, Wed, Fri as on GitHub/LeetCode)
        val dayTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = heightPx * 0.060f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }
        val dowRows = mapOf(1 to "Mon", 3 to "Wed", 5 to "Fri")
        for ((r, label) in dowRows) {
            val y = gridTop + r * (actualCell + cellGap) + actualCell * 0.78f
            canvas.drawText(label, 14f, y, dayTextPaint)
        }

        // Month Labels Painter
        val monthTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = heightPx * 0.075f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }

        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        // 5. Render Columns with Month Gaps & Labels
        var currentX = gridLeft
        var lastLabelMonth = -1

        for (col in 0 until weeksCount) {
            val colMonth = weekMonthList[col]
            if (col > 0 && colMonth != weekMonthList[col - 1]) {
                currentX += monthGap // Month Gap as in LeetCode / GitHub
            }

            // Draw month label at start of each month block (skip orphan tail week 0)
            if (colMonth != lastLabelMonth) {
                val isNotCrowdedOrphan = (col > 0) || (col + 1 < weeksCount && weekMonthList[col + 1] == colMonth)
                if (isNotCrowdedOrphan) {
                    canvas.drawText(weekMonthNameList[col], currentX, heightPx * 0.28f, monthTextPaint)
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
                canvas.drawRoundRect(cellRect, actualCell * 0.26f, actualCell * 0.26f, cellPaint)

                calCopy.add(Calendar.DAY_OF_YEAR, 1)
            }

            currentX += (actualCell + cellGap)
        }

        return bitmap
    }

    // 3. Authentic GitHub Activity Overview (4-Quadrant Spider/Radar Chart with Total Hub & Clean Labels)
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

        // 2. Proportions & Axis setup
        val commits = stats.commitsCount.coerceAtLeast(0).toFloat()
        val prs = stats.pullRequestsCount.coerceAtLeast(0).toFloat()
        val issues = stats.issuesCount.coerceAtLeast(0).toFloat()
        val reviews = stats.reviewsCount.coerceAtLeast(0).toFloat()

        val maxRadarRadius = sizePx * 0.25f // 90px in 360px
        val minRadarRadius = sizePx * 0.08f // 28px in 360px

        // Concentric guide rings (33%, 66%, 100%)
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = 1f
            color = Color.argb(35, 255, 255, 255)
        }
        canvas.drawCircle(center, center, minRadarRadius + (maxRadarRadius - minRadarRadius) * 0.33f, ringPaint)
        canvas.drawCircle(center, center, minRadarRadius + (maxRadarRadius - minRadarRadius) * 0.66f, ringPaint)
        canvas.drawCircle(center, center, maxRadarRadius, ringPaint)

        // Cross coordinate lines
        val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(70, 255, 255, 255)
            this.strokeWidth = 1.4f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(center, center - maxRadarRadius, center, center + maxRadarRadius, axisPaint)
        canvas.drawLine(center - maxRadarRadius, center, center + maxRadarRadius, center, axisPaint)

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
                Color.argb(130, 34, 197, 94),
                Color.argb(55, 16, 185, 129),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(polyPath, fillPaint)

        // Glowing perimeter stroke
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#22C55E")
            this.strokeWidth = 2.4f
            style = Paint.Style.STROKE
        }
        canvas.drawPath(polyPath, strokePaint)

        // Directional Vertex Dots
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        dotPaint.color = Color.parseColor("#22C55E")
        canvas.drawCircle(center, center - cNorm, 4.2f, dotPaint)
        dotPaint.color = Color.parseColor("#A855F7")
        canvas.drawCircle(center + prNorm, center, 4.2f, dotPaint)
        dotPaint.color = Color.parseColor("#3B82F6")
        canvas.drawCircle(center, center + issueNorm, 4.2f, dotPaint)
        dotPaint.color = Color.parseColor("#F59E0B")
        canvas.drawCircle(center - revNorm, center, 4.2f, dotPaint)

        // 4. Center Glass Hub (Total Contributions Count)
        val hubRadius = sizePx * 0.088f
        val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(210, 15, 23, 42)
        }
        canvas.drawCircle(center, center, hubRadius, hubPaint)
        val hubStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = 1f
            color = Color.argb(60, 255, 255, 255)
        }
        canvas.drawCircle(center, center, hubRadius, hubStrokePaint)

        val hubValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_WHITE
            textSize = sizePx * 0.062f
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

        // 5. Four Cardinal Labels with Brand Dots (Clean, non-colliding layout)
        val labelValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_WHITE
            textSize = sizePx * 0.046f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val labelTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_MUTED
            textSize = sizePx * 0.038f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        // Helper to draw clean Pill / Badge
        fun drawBadge(centerX: Float, centerY: Float, title: String, count: Int, colorHex: String, align: Paint.Align) {
            val color = Color.parseColor(colorHex)
            val countStr = count.toString()
            labelValPaint.textAlign = align
            labelTitlePaint.textAlign = align

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
        drawBadge(center, center - maxRadarRadius - 16f, "Commits", stats.commitsCount, "#22C55E", Paint.Align.CENTER)
        // Bottom: Issues (Blue)
        drawBadge(center, center + maxRadarRadius + 22f, "Issues", stats.issuesCount, "#3B82F6", Paint.Align.CENTER)
        // Right: PRs (Purple)
        drawBadge(center + maxRadarRadius + 14f, center + 4f, "PRs", stats.pullRequestsCount, "#A855F7", Paint.Align.LEFT)
        // Left: Reviews (Amber)
        drawBadge(center - maxRadarRadius - 14f, center + 4f, "Reviews", stats.reviewsCount, "#F59E0B", Paint.Align.RIGHT)

        return bitmap
    }
}
