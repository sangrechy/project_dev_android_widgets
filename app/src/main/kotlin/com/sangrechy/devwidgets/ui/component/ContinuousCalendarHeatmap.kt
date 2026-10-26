package com.sangrechy.devwidgets.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ContinuousCalendarHeatmap(
    rawDaysMap: Map<String, Int>,
    isLeetCode: Boolean,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var selectedInfo by remember { mutableStateOf<String?>(null) }

    // Scroll to end (today's date) on initial display
    LaunchedEffect(Unit) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    val weeksCount = 52
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val displaySdf = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }
    val monthFmt = remember { SimpleDateFormat("MMM", Locale.US) }

    // Precompute 52 weeks x 7 days grid dates
    val gridDays = remember(rawDaysMap) {
        val daysList = mutableListOf<List<HeatmapDayInfo>>()
        val cal = Calendar.getInstance()
        val todayDow = cal.get(Calendar.DAY_OF_WEEK) // 1 (Sun) to 7 (Sat)
        
        // Align to the start of 52 weeks ago
        val totalDays = (weeksCount - 1) * 7 + todayDow
        cal.add(Calendar.DAY_OF_YEAR, -(totalDays - 1))

        var lastMonth = -1
        for (w in 0 until weeksCount) {
            val weekDays = mutableListOf<HeatmapDayInfo>()
            var monthLabel: String? = null

            for (d in 0 until 7) {
                val date = cal.time
                val dateStr = sdf.format(date)
                val count = rawDaysMap[dateStr] ?: 0
                val curMonth = cal.get(Calendar.MONTH)

                if (curMonth != lastMonth && d == 0) {
                    monthLabel = monthFmt.format(date)
                    lastMonth = curMonth
                }

                weekDays.add(
                    HeatmapDayInfo(
                        dateStr = dateStr,
                        displayDate = displaySdf.format(date),
                        count = count,
                        dayOfWeek = d,
                        weekIndex = w
                    )
                )
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            daysList.add(weekDays)
        }
        daysList
    }

    val totalSubmissions = remember(rawDaysMap) {
        rawDaysMap.values.sum()
    }
    val activeDays = remember(rawDaysMap) {
        rawDaysMap.values.count { it > 0 }
    }

    val cellSize = 13.dp
    val cellSpacing = 3.5.dp
    val headerHeight = 20.dp
    val leftLabelWidth = 26.dp

    Column(modifier = modifier.fillMaxWidth()) {
        // Summary Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isLeetCode) "Submission Heatmap" else "Contribution Heatmap",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$totalSubmissions ${if (isLeetCode) "submissions" else "contributions"} • $activeDays active days",
                color = if (isLeetCode) Color(0xFFFFA116) else Color(0xFF3FB950),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Selected Cell Info Banner
        selectedInfo?.let { info ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x333B82F6))
                    .border(1.dp, Color(0x6660A5FA), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = info,
                    color = Color(0xFF93C5FD),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Horizontally Scrollable Heatmap Body
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.Top
        ) {
            // Day-of-week labels column on left
            Column(
                modifier = Modifier
                    .width(leftLabelWidth)
                    .padding(top = headerHeight),
                verticalArrangement = Arrangement.spacedBy(cellSpacing)
            ) {
                val dowLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                dowLabels.forEachIndexed { idx, label ->
                    Box(
                        modifier = Modifier.size(width = leftLabelWidth, height = cellSize),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (idx % 2 != 0) { // Mon, Wed, Fri
                            Text(
                                text = label.take(1),
                                color = Color(0xFF64748B),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 52 Weeks Grid
            Row(horizontalArrangement = Arrangement.spacedBy(cellSpacing)) {
                gridDays.forEachIndexed { weekIdx, weekDays ->
                    Column(verticalArrangement = Arrangement.spacedBy(cellSpacing)) {
                        // Month label over first day of month or spacer
                        val firstDayDate = weekDays.firstOrNull()?.dateStr ?: ""
                        val isMonthStart = firstDayDate.endsWith("-01") ||
                                firstDayDate.endsWith("-02") ||
                                firstDayDate.endsWith("-03") ||
                                firstDayDate.endsWith("-04") ||
                                firstDayDate.endsWith("-05") ||
                                firstDayDate.endsWith("-06") ||
                                firstDayDate.endsWith("-07")

                        Box(
                            modifier = Modifier.size(width = cellSize, height = headerHeight),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            if (isMonthStart) {
                                val monthName = try {
                                    monthFmt.format(sdf.parse(firstDayDate) ?: Date())
                                } catch (_: Exception) { "" }
                                Text(
                                    text = monthName,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }

                        // 7 Days
                        weekDays.forEach { day ->
                            val cellColor = getCellColor(day.count, isLeetCode)
                            Box(
                                modifier = Modifier
                                    .size(cellSize)
                                    .clip(RoundedCornerShape(2.5.dp))
                                    .background(cellColor)
                                    .border(
                                        0.5.dp,
                                        if (day.count > 0) Color(0x33FFFFFF) else Color(0x15FFFFFF),
                                        RoundedCornerShape(2.5.dp)
                                    )
                                    .pointerInput(day) {
                                        detectTapGestures {
                                            selectedInfo = "${day.displayDate}: ${day.count} ${if (isLeetCode) "submission(s)" else "contribution(s)"}"
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Legend at bottom
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Less", color = Color(0xFF64748B), fontSize = 10.sp)
            Spacer(modifier = Modifier.width(4.dp))
            val levels = listOf(0, 1, 3, 6, 10)
            levels.forEach { lvl ->
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(getCellColor(lvl, isLeetCode))
                        .border(0.5.dp, Color(0x20FFFFFF), RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(text = "More", color = Color(0xFF64748B), fontSize = 10.sp)
        }
    }
}

private data class HeatmapDayInfo(
    val dateStr: String,
    val displayDate: String,
    val count: Int,
    val dayOfWeek: Int,
    val weekIndex: Int
)

private fun getCellColor(count: Int, isLeetCode: Boolean): Color {
    return if (isLeetCode) {
        when {
            count <= 0 -> Color(0xFF1E293B)
            count in 1..2 -> Color(0xFF5A320A)
            count in 3..5 -> Color(0xFF9A550F)
            count in 6..9 -> Color(0xFFD97706)
            else -> Color(0xFFFFA116)
        }
    } else {
        when {
            count <= 0 -> Color(0xFF161B22)
            count in 1..2 -> Color(0xFF0E4429)
            count in 3..5 -> Color(0xFF006D32)
            count in 6..9 -> Color(0xFF26A641)
            else -> Color(0xFF39D353)
        }
    }
}
