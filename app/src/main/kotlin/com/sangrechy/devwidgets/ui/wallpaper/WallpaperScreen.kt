package com.sangrechy.devwidgets.ui.wallpaper

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sangrechy.devwidgets.data.WallpaperManager
import com.sangrechy.devwidgets.data.WallpaperPreset
import com.sangrechy.devwidgets.data.model.WidgetDashboardData
import com.sangrechy.devwidgets.ui.main.MainScreenViewModel
import com.sangrechy.devwidgets.ui.theme.*
import com.sangrechy.devwidgets.widget.WidgetBitmapRenderer

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WallpaperScreenContent(
    viewModel: MainScreenViewModel,
    dashboardData: WidgetDashboardData,
    heatmapOrientation: String
) {
    val context = LocalContext.current

    val appWallpaperType by viewModel.appWallpaperType.collectAsState()
    val appWallpaperPreset by viewModel.appWallpaperPreset.collectAsState()
    val appWallpaperDim by viewModel.appWallpaperDim.collectAsState()
    val appWallpaperCustomPath by viewModel.appWallpaperCustomPath.collectAsState()

    val widgetWallpaperType by viewModel.widgetWallpaperType.collectAsState()
    val widgetWallpaperPreset by viewModel.widgetWallpaperPreset.collectAsState()
    val widgetWallpaperDim by viewModel.widgetWallpaperDim.collectAsState()
    val widgetWallpaperCustomPath by viewModel.widgetWallpaperCustomPath.collectAsState()

    val appPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.setAppWallpaperCustomFromUri(uri)
        }
    }

    val widgetPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.setWidgetWallpaperCustomFromUri(uri)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

        // =========================================================================
        // 1. App Background
        // =========================================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(ThemeCardBg)
                .border(1.dp, ThemeCardBorder, RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "App Background",
                        color = ThemeTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val appBadge = when {
                        appWallpaperType == "CUSTOM" -> "Custom"
                        appWallpaperType == "PRESET" && appWallpaperPreset == WallpaperPreset.DEEP_COSMOS.id -> "Glassy"
                        appWallpaperType == "PRESET" -> WallpaperPreset.fromId(appWallpaperPreset).title
                        else -> "Obsidian"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ThemeInnerBoxBg)
                            .border(1.dp, ThemeInnerBoxBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = appBadge,
                            color = ThemeAccentBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Single unified options: Obsidian | Custom | Glassy | Cyber Matrix | Terminal | Carbon | Sunset
                val appThemes = listOf(
                    "OBSIDIAN" to "Obsidian",
                    "CUSTOM" to "Custom",
                    "GLASSY" to "Glassy",
                    WallpaperPreset.CYBER_MATRIX.id to "Cyber Matrix",
                    WallpaperPreset.TERMINAL_CODE.id to "Terminal Code",
                    WallpaperPreset.MINIMAL_CARBON.id to "Minimal Carbon",
                    WallpaperPreset.SUNSET_DEV.id to "Sunset Dev"
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    appThemes.forEach { (key, label) ->
                        val isSelected = when (key) {
                            "OBSIDIAN" -> appWallpaperType == "NONE"
                            "CUSTOM" -> appWallpaperType == "CUSTOM"
                            "GLASSY" -> appWallpaperType == "PRESET" && appWallpaperPreset == WallpaperPreset.DEEP_COSMOS.id
                            else -> appWallpaperType == "PRESET" && appWallpaperPreset == key
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ThemeAccentBlue.copy(alpha = 0.15f) else ThemeInnerBoxBg)
                                .border(1.dp, if (isSelected) ThemeAccentBlue else ThemeInnerBoxBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    when (key) {
                                        "OBSIDIAN" -> viewModel.setAppWallpaperDefault()
                                        "CUSTOM" -> appPhotoPicker.launch("image/*")
                                        "GLASSY" -> {
                                            viewModel.setAppWallpaperPreset(WallpaperPreset.DEEP_COSMOS.id)
                                            viewModel.setAppWallpaperDim(0.40f)
                                        }
                                        else -> viewModel.setAppWallpaperPreset(key)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) ThemeAccentBlue else ThemeTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // Dim Slider if active (non-default)
                if (appWallpaperType != "NONE") {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Dim", color = ThemeTextSecondary, fontSize = 12.sp)
                            Text("${(appWallpaperDim * 100).toInt()}%", color = ThemeAccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = appWallpaperDim,
                            onValueChange = viewModel::setAppWallpaperDim,
                            valueRange = 0.20f..0.90f,
                            colors = SliderDefaults.colors(
                                thumbColor = ThemeAccentBlue,
                                activeTrackColor = ThemeAccentBlue,
                                inactiveTrackColor = ThemeInnerBoxBorder
                            )
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 2. Homescreen Background
        // =========================================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(ThemeCardBg)
                .border(1.dp, ThemeCardBorder, RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Homescreen Background",
                        color = ThemeTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val widgetBadge = when (widgetWallpaperType) {
                        "CUSTOM" -> "Custom"
                        "TRANSLUCENT" -> "Glassy"
                        "TRANSPARENT" -> "Clear"
                        "MATCH_APP" -> "Match App"
                        else -> "Obsidian"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ThemeInnerBoxBg)
                            .border(1.dp, ThemeInnerBoxBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = widgetBadge,
                            color = ThemeAccentGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Modes Row: Obsidian | Glassy | Clear | Match App | Custom
                val widgetThemes = listOf(
                    "DEFAULT" to "Obsidian",
                    "TRANSLUCENT" to "Glassy",
                    "TRANSPARENT" to "Clear",
                    "MATCH_APP" to "Match App",
                    "CUSTOM" to "Custom"
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    widgetThemes.forEach { (typeKey, label) ->
                        val isSelected = (widgetWallpaperType == typeKey)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ThemeAccentBlue.copy(alpha = 0.15f) else ThemeInnerBoxBg)
                                .border(1.dp, if (isSelected) ThemeAccentBlue else ThemeInnerBoxBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    if (typeKey == "CUSTOM") {
                                        widgetPhotoPicker.launch("image/*")
                                    } else {
                                        viewModel.setWidgetWallpaperType(typeKey)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) ThemeAccentBlue else ThemeTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // Widget Dim Slider (if wallpaper active)
                if (widgetWallpaperType in listOf("MATCH_APP", "CUSTOM")) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Widget Dim", color = ThemeTextSecondary, fontSize = 12.sp)
                            Text("${(widgetWallpaperDim * 100).toInt()}%", color = ThemeAccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = widgetWallpaperDim,
                            onValueChange = viewModel::setWidgetWallpaperDim,
                            valueRange = 0.20f..0.90f,
                            colors = SliderDefaults.colors(
                                thumbColor = ThemeAccentBlue,
                                activeTrackColor = ThemeAccentBlue,
                                inactiveTrackColor = ThemeInnerBoxBorder
                            )
                        )
                    }
                }

                // Sync button
                Button(
                    onClick = { viewModel.applyWidgetWallpaperToAllWidgets() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThemeCardBgElevated,
                        contentColor = ThemeTextPrimary
                    ),
                    border = BorderStroke(1.dp, ThemeCardBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = ThemeAccentBlue
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sync to Launcher Widgets",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // =========================================================================
        // 3. Compact Dynamic Bottom Preview (Live Reacting to Both Settings)
        // =========================================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(ThemeCardBg)
                .border(1.dp, ThemeCardBorder, RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Preview",
                    color = ThemeTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                val appPreviewBitmap = remember(appWallpaperType, appWallpaperPreset, appWallpaperCustomPath) {
                    try {
                        WallpaperManager.getAppWallpaperBitmap(context, 720, 360)?.asImageBitmap()
                    } catch (_: Exception) { null }
                }

                val widgetCustomBitmap = remember(widgetWallpaperType, widgetWallpaperCustomPath) {
                    if (widgetWallpaperType == "CUSTOM") {
                        try {
                            WallpaperManager.getHighResCustomWallpaperForWidget(widgetWallpaperCustomPath, 340, 180)?.asImageBitmap()
                        } catch (_: Exception) { null }
                    } else null
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ThemeInnerBoxBg)
                        .border(1.dp, ThemeInnerBoxBorder, RoundedCornerShape(14.dp))
                ) {
                    // 1. App background canvas layer
                    if (appWallpaperType != "NONE" && appPreviewBitmap != null) {
                        Image(
                            bitmap = appPreviewBitmap,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = appWallpaperDim))
                        )
                    }

                    // 2. Widget Preview Cards reacting directly to widgetWallpaperType
                    val lcCircle = remember(dashboardData.leetCode) {
                        WidgetBitmapRenderer.renderLeetCodeCircle(dashboardData.leetCode, sizePx = 220).asImageBitmap()
                    }
                    val ghRadar = remember(dashboardData.gitHub) {
                        WidgetBitmapRenderer.renderGitHubTypeMap(dashboardData.gitHub, sizePx = 220).asImageBitmap()
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reusable lambda for widget card styling
                        @Composable
                        fun WidgetPreviewBox(title: String, imageBitmap: androidx.compose.ui.graphics.ImageBitmap) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(12.dp))
                                    .then(
                                        when (widgetWallpaperType) {
                                            "TRANSLUCENT" -> Modifier
                                                .background(Color(0x33262A36))
                                                .border(1.dp, Color(0x4DFFFFFF), RoundedCornerShape(12.dp))
                                            "TRANSPARENT" -> Modifier
                                                .background(Color(0x0DFFFFFF))
                                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                                            "MATCH_APP" -> Modifier
                                                .background(Color(0x26000000))
                                                .border(1.dp, ThemeCardBorder, RoundedCornerShape(12.dp))
                                            "CUSTOM" -> Modifier
                                                .background(Color(0x26000000))
                                                .border(1.dp, ThemeCardBorder, RoundedCornerShape(12.dp))
                                            else -> Modifier // DEFAULT (Obsidian)
                                                .background(ThemeCardBg.copy(alpha = 0.95f))
                                                .border(1.dp, ThemeCardBorder, RoundedCornerShape(12.dp))
                                        }
                                    )
                            ) {
                                // Background texture for MATCH_APP or CUSTOM
                                if (widgetWallpaperType == "MATCH_APP" && appPreviewBitmap != null) {
                                    Image(
                                        bitmap = appPreviewBitmap,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = widgetWallpaperDim))
                                    )
                                } else if (widgetWallpaperType == "CUSTOM" && widgetCustomBitmap != null) {
                                    Image(
                                        bitmap = widgetCustomBitmap,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = widgetWallpaperDim))
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = title,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Image(
                                        bitmap = imageBitmap,
                                        contentDescription = null,
                                        modifier = Modifier.size(72.dp)
                                    )
                                }
                            }
                        }

                        WidgetPreviewBox(title = "LeetCode", imageBitmap = lcCircle)
                        WidgetPreviewBox(title = "GitHub", imageBitmap = ghRadar)
                    }
                }
            }
        }
    }
}
