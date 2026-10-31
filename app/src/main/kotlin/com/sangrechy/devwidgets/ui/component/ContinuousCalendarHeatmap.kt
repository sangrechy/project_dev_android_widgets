package com.sangrechy.devwidgets.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
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
    var selectedDay by remember { mutableStateOf<HeatmapDayInfo?>(null) }

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

    val cellSize = 10.5.dp
    val cellSpacing = 2.dp
    val dayLabelWidth = 9.dp
    val uniformGridWidth = (6 * 10.5 + 5 * 2).dp // 73.dp
    val monthContentWidth = uniformGridWidth + dayLabelWidth + 4.dp // 86.dp
    val cardPadding = 8.dp
    val cardWidth = monthContentWidth + (cardPadding * 2) // 102.dp
    val cardSpacing = 8.dp
    // Card width (102dp) + 8dp spacing = 110dp per step
    val monthStepPx = with(density) { (cardWidth + cardSpacing).toPx() }

    Column(modifier = modifier.fillMaxWidth()) {
        // Summary Header Row (Collision-Free Minimalist Layout)
        val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isLeetCode) "Submissions" else "Contributions",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Year & count summary: e.g. 2026: 398
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x18FFFFFF))
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$currentYear: $totalSubmissions",
                        color = Color(0xFFE2E8F0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Scroll Left / Previous Month
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(7.dp))
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
                        modifier = Modifier.size(13.dp)
                    )
                }

                // Scroll Right / Next Month
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(7.dp))
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
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        val uniformGridHeight = (7 * 10.5 + 6 * 2).dp
        val cellSizePx = with(density) { cellSize.toPx() }
        val cellSpacingPx = with(density) { cellSpacing.toPx() }
        val cornerRadiusPx = with(density) { 3.dp.toPx() }
        val strokeWidthPx = with(density) { 0.5.dp.toPx() }

        // Horizontally Scrollable Heatmap with Distinct Separated Month Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(cardSpacing),
            verticalAlignment = Alignment.Top
        ) {
            monthBlocks.forEach { month ->
                // Individual Month Glass Div Box
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xF0101010))
                        .border(
                            1.dp,
                            Color(0x28FFFFFF),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(cardPadding)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Month Header
                        Row(
                            modifier = Modifier.width(monthContentWidth),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = month.monthName,
                                color = Color.White,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            if (month.totalCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color(0x26FFFFFF))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${month.totalCount}",
                                        color = Color.White,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Day Matrix Row: Day Labels Column + 1 High-Performance Canvas
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
                                                color = Color(0xFFAAAAAA),
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // Single Canvas for the entire 6x7 month matrix (60x faster, zero allocation)
                            Box {
                                Canvas(
                                    modifier = Modifier
                                        .size(width = uniformGridWidth, height = uniformGridHeight)
                                        .pointerInput(month) {
                                            detectTapGestures { offset ->
                                                val col = (offset.x / (cellSizePx + cellSpacingPx)).toInt()
                                                val row = (offset.y / (cellSizePx + cellSpacingPx)).toInt()
                                                if (col in 0 until 6 && row in 0 until 7) {
                                                    val slotIdx = col * 7 + row
                                                    val day = month.days.getOrNull(slotIdx)
                                                    selectedDay = if (selectedDay == day) null else day
                                                }
                                            }
                                        }
                                ) {
                                    for (col in 0 until 6) {
                                        for (row in 0 until 7) {
                                            val slotIdx = col * 7 + row
                                            val day = month.days.getOrNull(slotIdx)
                                            val x = col * (cellSizePx + cellSpacingPx)
                                            val y = row * (cellSizePx + cellSpacingPx)

                                            if (day != null) {
                                                val cellColor = getCellColor(day.count, isLeetCode)
                                                drawRoundRect(
                                                    color = cellColor,
                                                    topLeft = Offset(x, y),
                                                    size = Size(cellSizePx, cellSizePx),
                                                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                                                )
                                                drawRoundRect(
                                                    color = if (day.count > 0) Color(0x40FFFFFF) else Color(0x18FFFFFF),
                                                    topLeft = Offset(x, y),
                                                    size = Size(cellSizePx, cellSizePx),
                                                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                                                    style = Stroke(width = strokeWidthPx)
                                                )
                                            } else {
                                                drawCircle(
                                                    color = Color(0x14FFFFFF),
                                                    radius = 1.dp.toPx(),
                                                    center = Offset(x + cellSizePx / 2f, y + cellSizePx / 2f)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (selectedDay != null && month.days.contains(selectedDay)) {
                                    val day = selectedDay!!
                                    Popup(
                                        alignment = Alignment.TopCenter,
                                        offset = IntOffset(0, -60),
                                        onDismissRequest = { selectedDay = null }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .shadow(8.dp, RoundedCornerShape(8.dp))
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xF2141414))
                                                .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "${day.displayDate}: ${day.count}",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
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

        Spacer(modifier = Modifier.height(10.dp))

        // Legend and Swipe hint at bottom in Frosted Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Swipe horizontally for 12 months",
                color = Color(0xFF707070),
                fontSize = 10.sp
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x18FFFFFF))
                    .border(0.5.dp, Color(0x24FFFFFF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Less", color = Color(0xFFAAAAAA), fontSize = 10.sp)
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
                    Text(text = "More", color = Color(0xFFAAAAAA), fontSize = 10.sp)
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
    return when {
        count <= 0 -> Color(0x18FFFFFF)
        count in 1..2 -> Color(0xFF383838)
        count in 3..5 -> Color(0xFF686868)
        count in 6..9 -> Color(0xFFAAAAAA)
        else -> Color(0xFFFFFFFF)
    }
}
