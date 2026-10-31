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

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // =========================================================================
        // 1. App Wallpaper Studio
        // =========================================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(ThemeCardBg)
                .border(1.dp, ThemeCardBorder, RoundedCornerShape(22.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "App Background Wallpaper",
                            color = ThemeTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Choose an aesthetic dark wallpaper rendered behind all app cards",
                            color = ThemeTextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    val badgeText = when (appWallpaperType) {
                        "CUSTOM" -> "Custom Photo"
                        "PRESET" -> WallpaperPreset.fromId(appWallpaperPreset).title
                        else -> "Default Obsidian"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ThemeAccentBlue.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = ThemeAccentBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                // Quick Mode Actions: Default Obsidian vs Custom Photo Picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isDefault = appWallpaperType == "NONE"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDefault) ThemeAccentBlue else ThemeInnerBoxBg)
                            .border(1.dp, if (isDefault) ThemeAccentBlue else ThemeInnerBoxBorder, RoundedCornerShape(12.dp))
                            .clickable { viewModel.setAppWallpaperDefault() }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatPaint,
                                contentDescription = null,
                                tint = if (isDefault) Color.White else ThemeTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Obsidian Default",
                                color = if (isDefault) Color.White else ThemeTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isDefault) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }

                    val isCustom = appWallpaperType == "CUSTOM"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCustom) ThemeAccentBlue else ThemeInnerBoxBg)
                            .border(1.dp, if (isCustom) ThemeAccentBlue else ThemeInnerBoxBorder, RoundedCornerShape(12.dp))
                            .clickable { appPhotoPicker.launch("image/*") }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = if (isCustom) Color.White else ThemeTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isCustom) "Photo Selected" else "Pick from Gallery",
                                color = if (isCustom) Color.White else ThemeTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // Curated Dark Developer Presets Grid
                Text(
                    text = "Curated Aesthetic Dark Presets",
                    color = ThemeTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 2
                ) {
                    WallpaperPreset.entries.forEach { preset ->
                        val isSelected = (appWallpaperType == "PRESET" && appWallpaperPreset == preset.id)
                        val thumbnailBitmap = remember(preset.id) {
                            WallpaperManager.getPresetBitmap(preset, width = 360, height = 200).asImageBitmap()
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(100.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    2.dp,
                                    if (isSelected) ThemeAccentBlue else ThemeInnerBoxBorder,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { viewModel.setAppWallpaperPreset(preset.id) }
                        ) {
                            // Thumbnail background
                            Image(
                                bitmap = thumbnailBitmap,
                                contentDescription = preset.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Scrim
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.40f))
                            )

                            // Selected indicator badge
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(ThemeAccentBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            // Preset Title and description at bottom
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .background(Color(0xD90D0D11))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = preset.title,
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = preset.subtitle,
                                    color = ThemeTextSecondary,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Wallpaper Dim & Contrast Slider
                if (appWallpaperType != "NONE") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Wallpaper Dim Scrim",
                                color = ThemeTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${(appWallpaperDim * 100).toInt()}% Darkness",
                                color = ThemeAccentBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
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

                        Text(
                            text = "Darkens the wallpaper background so that cards, text, and charts remain clear and readable.",
                            color = ThemeTextMuted,
                            fontSize = 10.5.sp
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 2. Home Screen Widget Wallpaper & Chassis Styles
        // =========================================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(ThemeCardBg)
                .border(1.dp, ThemeCardBorder, RoundedCornerShape(22.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Home Screen Widget Style",
                            color = ThemeTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Choose how widgets look on your launcher: solid obsidian, translucent frost, transparent, or custom wallpaper",
                            color = ThemeTextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ThemeAccentGreen.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when (widgetWallpaperType) {
                                "TRANSLUCENT" -> "Translucent Frost"
                                "TRANSPARENT" -> "Transparent Glass"
                                "MATCH_APP" -> "Match App"
                                "CUSTOM" -> "Custom Photo"
                                else -> "Obsidian Matte"
                            },
                            color = ThemeAccentGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                // 5 Widget Style Options
                val widgetStyles = listOf(
                    Triple("DEFAULT", "Obsidian Glass", "Solid dark luxury chassis"),
                    Triple("TRANSLUCENT", "Translucent Frost", "Bleeds home screen wallpaper"),
                    Triple("TRANSPARENT", "Transparent Glass", "Ultra-clear minimal border"),
                    Triple("MATCH_APP", "Match App Wallpaper", "Uses the app's chosen wallpaper"),
                    Triple("CUSTOM", "Custom Widget Photo", "Select photo for widgets only")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    widgetStyles.forEach { (typeKey, title, desc) ->
                        val isSelected = (widgetWallpaperType == typeKey)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) ThemeAccentBlue.copy(alpha = 0.12f) else ThemeInnerBoxBg)
                                .border(
                                    1.dp,
                                    if (isSelected) ThemeAccentBlue else ThemeInnerBoxBorder,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    if (typeKey == "CUSTOM") {
                                        widgetPhotoPicker.launch("image/*")
                                    } else {
                                        viewModel.setWidgetWallpaperType(typeKey)
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        color = if (isSelected) ThemeAccentBlue else ThemeTextPrimary,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = desc,
                                        color = ThemeTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(ThemeAccentBlue),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Active",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Widget Dim Slider (if wallpaper active)
                if (widgetWallpaperType == "MATCH_APP" || widgetWallpaperType == "CUSTOM") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Widget Wallpaper Dim",
                                color = ThemeTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${(widgetWallpaperDim * 100).toInt()}% Dim",
                                color = ThemeAccentBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
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

                // Force Sync Button for launcher widgets
                Button(
                    onClick = {
                        viewModel.applyWidgetWallpaperToAllWidgets()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeAccentBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sync Background to All Launcher Widgets",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // =========================================================================
        // 3. Live Card & Widget Preview On Wallpaper
        // =========================================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(ThemeCardBg)
                .border(1.dp, ThemeCardBorder, RoundedCornerShape(22.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Live Preview Over Current Wallpaper",
                    color = ThemeTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "How cards, gauges, and text appear on top of your selected wallpaper:",
                    color = ThemeTextSecondary,
                    fontSize = 11.sp
                )

                // Preview Box with actual wallpaper background
                val previewBitmap = remember(appWallpaperType, appWallpaperPreset, appWallpaperCustomPath) {
                    WallpaperManager.getAppWallpaperBitmap(context, 720, 360)?.asImageBitmap()
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ThemeInnerBoxBg)
                        .border(1.dp, ThemeInnerBoxBorder, RoundedCornerShape(16.dp))
                ) {
                    // Wallpaper inside preview
                    if (appWallpaperType != "NONE" && previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap,
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

                    // Floating Glass Card Inside Preview
                    val lcCircle = remember(dashboardData.leetCode) {
                        WidgetBitmapRenderer.renderLeetCodeCircle(dashboardData.leetCode, sizePx = 280).asImageBitmap()
                    }
                    val ghRadar = remember(dashboardData.gitHub) {
                        WidgetBitmapRenderer.renderGitHubTypeMap(dashboardData.gitHub, sizePx = 280).asImageBitmap()
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mini LeetCode Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(14.dp))
                                .background(ThemeCardBg)
                                .border(1.dp, ThemeCardBorder, RoundedCornerShape(14.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "LeetCode",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Image(
                                    bitmap = lcCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(90.dp)
                                )
                            }
                        }

                        // Mini GitHub Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(14.dp))
                                .background(ThemeCardBg)
                                .border(1.dp, ThemeCardBorder, RoundedCornerShape(14.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "GitHub Activity",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Image(
                                    bitmap = ghRadar,
                                    contentDescription = null,
                                    modifier = Modifier.size(90.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
