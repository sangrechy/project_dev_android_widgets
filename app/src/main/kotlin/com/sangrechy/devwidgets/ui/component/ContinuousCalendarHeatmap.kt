package com.sangrechy.devwidgets.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.animateScrollBy
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
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
    val coroutineScope = rememberCoroutineScope()
    var selectedInfo by remember { mutableStateOf<String?>(null) }

    val density = LocalDensity.current
    var hasInitiallyScrolled by remember { mutableStateOf(false) }

    // Scroll to end (current month) once layout width is known
    LaunchedEffect(scrollState.maxValue) {
        if (!hasInitiallyScrolled && scrollState.maxValue > 0) {
            scrollState.scrollTo(scrollState.maxValue)
            hasInitiallyScrolled = true
        }
    }

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val displaySdf = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }
    val monthFmt = remember { SimpleDateFormat("MMM yyyy", Locale.US) }

    // Precompute 12 individual, separated Month Blocks with uniform 6-week x 7-day shapes
    val monthBlocks = remember(rawDaysMap) {
        val calIter = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        calIter.add(Calendar.MONTH, -11)

        val blocks = mutableListOf<MonthBlockData>()

        for (m in 0 until 12) {
            val y = calIter.get(Calendar.YEAR)
            val mIdx = calIter.get(Calendar.MONTH)
            val mName = monthFmt.format(calIter.time)
            val daysCount = calIter.getActualMaximum(Calendar.DAY_OF_MONTH)
            val calDow = calIter.get(Calendar.DAY_OF_WEEK)
            val firstDowMondayBased = (calDow + 5) % 7 // Mon=0, Tue=1, ..., Sun=6

            var monthTotal = 0
            val daysList = MutableList<HeatmapDayInfo?>(42) { null } // 6 weeks x 7 days = 42 slots

            for (dayNum in 1..daysCount) {
                val slot = firstDowMondayBased + (dayNum - 1)
                val col = slot / 7 // Week column (0..5)
                val row = slot % 7 // Day row (0..6)
                val slotIdx = col * 7 + row

                val dateCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, y)
                    set(Calendar.MONTH, mIdx)
                    set(Calendar.DAY_OF_MONTH, dayNum)
                }
                val dateStr = sdf.format(dateCal.time)
                val count = rawDaysMap[dateStr] ?: 0
                monthTotal += count

                if (slotIdx in 0 until 42) {
                    daysList[slotIdx] = HeatmapDayInfo(
                        dateStr = dateStr,
                        displayDate = displaySdf.format(dateCal.time),
                        count = count,
                        dayOfWeek = row,
                        weekIndex = col
                    )
                }
            }

            blocks.add(
                MonthBlockData(
                    year = y,
                    monthIdx = mIdx,
                    monthName = mName,
                    totalCount = monthTotal,
                    daysInMonth = daysCount,
                    firstDowMondayBased = firstDowMondayBased,
                    days = daysList
                )
            )

            calIter.add(Calendar.MONTH, 1)
        }
        blocks
    }

    val totalSubmissions = remember(rawDaysMap) {
        rawDaysMap.values.sum()
    }
    val activeDays = remember(rawDaysMap) {
        rawDaysMap.values.count { it > 0 }
    }

    val cellSize = 12.dp
    val cellSpacing = 3.dp
    val dayLabelWidth = 11.dp
    val uniformGridWidth = (6 * 12 + 5 * 3).dp // 87.dp
    val monthContentWidth = uniformGridWidth + dayLabelWidth + 4.dp // 102.dp
    // Card width (102.dp + 20.dp padding) + 10.dp inter-card spacing = 132.dp per step
    val monthStepPx = with(density) { (monthContentWidth + 30.dp).toPx() }

    Column(modifier = modifier.fillMaxWidth()) {
        // Summary Header Row in Glass Container
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x1EFFFFFF))
                    .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isLeetCode) "Submission Heatmap" else "Contribution Heatmap",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Scroll Left / Previous Month
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x1FFFFFFF))
                        .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(8.dp))
                        .clickable {
                            coroutineScope.launch {
                                scrollState.animateScrollBy(-monthStepPx)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Scroll Left",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Scroll Right / Next Month
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x1FFFFFFF))
                        .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(8.dp))
                        .clickable {
                            coroutineScope.launch {
                                scrollState.animateScrollBy(monthStepPx)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Scroll Right",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(14.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isLeetCode) Color(0x2EFFA116) else Color(0x2E22C55E))
                        .border(
                            1.dp,
                            if (isLeetCode) Color(0x66FFA116) else Color(0x6622C55E),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$totalSubmissions in 1 yr • $activeDays days",
                        color = if (isLeetCode) Color(0xFFFFA116) else Color(0xFF4ADE80),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Selected Cell Info Banner with Dismiss button
        selectedInfo?.let { info ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x263B82F6))
                    .border(1.dp, Color(0x5560A5FA), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = info,
                        color = Color(0xFF93C5FD),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color(0xFF93C5FD),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { selectedInfo = null }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Horizontally Scrollable Heatmap with Distinct Separated Month Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            monthBlocks.forEach { month ->
                // Individual Month Glass Div Box
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x241E293B))
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(Color(0x38FFFFFF), Color(0x14FFFFFF))
                            ),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Month Header
                        Row(
                            modifier = Modifier.width(monthContentWidth),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = month.monthName,
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )

                            if (month.totalCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isLeetCode) Color(0x33FFA116) else Color(0x3322C55E)
                                        )
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${month.totalCount}",
                                        color = if (isLeetCode) Color(0xFFFFA116) else Color(0xFF4ADE80),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Day Matrix Row: Day Labels Column + 6 Week Columns
                        Row(
                            modifier = Modifier.width(monthContentWidth),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mon, Wed, Fri Day labels on the left of this month
                            Column(verticalArrangement = Arrangement.spacedBy(cellSpacing)) {
                                val dowShort = listOf("M", "T", "W", "T", "F", "S", "S")
                                dowShort.forEachIndexed { idx, label ->
                                    Box(
                                        modifier = Modifier.size(width = dayLabelWidth, height = cellSize),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (idx == 0 || idx == 2 || idx == 4) {
                                            Text(
                                                text = label,
                                                color = Color(0xFF94A3B8),
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // 6 Columns (Weeks) x 7 Rows (Days) Matrix
                            Row(
                                modifier = Modifier.width(uniformGridWidth),
                                horizontalArrangement = Arrangement.spacedBy(cellSpacing)
                            ) {
                                for (col in 0 until 6) {
                                    Column(verticalArrangement = Arrangement.spacedBy(cellSpacing)) {
                                        for (row in 0 until 7) {
                                            val slotIdx = col * 7 + row
                                            val day = month.days.getOrNull(slotIdx)
                                            if (day != null) {
                                                val cellColor = getCellColor(day.count, isLeetCode)
                                                Box(
                                                    modifier = Modifier
                                                        .size(cellSize)
                                                        .clip(RoundedCornerShape(3.dp))
                                                        .background(cellColor)
                                                        .border(
                                                            0.5.dp,
                                                            if (day.count > 0) Color(0x40FFFFFF) else Color(0x18FFFFFF),
                                                            RoundedCornerShape(3.dp)
                                                        )
                                                        .pointerInput(day) {
                                                            detectTapGestures {
                                                                selectedInfo = "${day.displayDate}: ${day.count} ${if (isLeetCode) "submission(s)" else "contribution(s)"}"
                                                            }
                                                        }
                                                )
                                            } else {
                                                // Subtle placeholder dot to guarantee identical month shapes
                                                Box(
                                                    modifier = Modifier.size(cellSize),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(2.dp)
                                                            .clip(RoundedCornerShape(1.dp))
                                                            .background(Color(0x12FFFFFF))
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Legend and Swipe hint at bottom in Frosted Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Swipe horizontally for 12 months",
                color = Color(0xFF64748B),
                fontSize = 10.sp
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x1AFFFFFF))
                    .border(0.5.dp, Color(0x20FFFFFF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Less", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    Spacer(modifier = Modifier.width(5.dp))
                    val levels = listOf(0, 1, 3, 6, 10)
                    levels.forEach { lvl ->
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(getCellColor(lvl, isLeetCode))
                                .border(0.5.dp, Color(0x20FFFFFF), RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    Text(text = "More", color = Color(0xFF94A3B8), fontSize = 10.sp)
                }
            }
        }
    }
}

private data class MonthBlockData(
    val year: Int,
    val monthIdx: Int,
    val monthName: String,
    val totalCount: Int,
    val daysInMonth: Int,
    val firstDowMondayBased: Int,
    val days: List<HeatmapDayInfo?>
)

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
            count <= 0 -> Color(0x261E293B)
            count in 1..2 -> Color(0xFF7C2D12)
            count in 3..5 -> Color(0xFFC2410C)
            count in 6..9 -> Color(0xFFEA580C)
            else -> Color(0xFFFFA116)
        }
    } else {
        when {
            count <= 0 -> Color(0x261E293B)
            count in 1..2 -> Color(0xFF064E3B)
            count in 3..5 -> Color(0xFF047857)
            count in 6..9 -> Color(0xFF10B981)
            else -> Color(0xFF22C55E)
        }
    }
}
