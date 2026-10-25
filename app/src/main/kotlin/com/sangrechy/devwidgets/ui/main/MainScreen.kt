package com.sangrechy.devwidgets.ui.main

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.sangrechy.devwidgets.R
import com.sangrechy.devwidgets.data.DefaultDataRepository
import com.sangrechy.devwidgets.data.model.AccountStatus
import com.sangrechy.devwidgets.data.model.WidgetDashboardData
import com.sangrechy.devwidgets.widget.DevActivityWidgetProvider
import com.sangrechy.devwidgets.widget.WidgetBitmapRenderer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: MainScreenViewModel = viewModel {
        MainScreenViewModel(DefaultDataRepository(context.applicationContext))
    }

    val leetCodeInput by viewModel.leetCodeInput.collectAsStateWithLifecycle()
    val gitHubInput by viewModel.gitHubInput.collectAsStateWithLifecycle()
    val tokenInput by viewModel.gitHubTokenInput.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val isVerifyingLc by viewModel.isVerifyingLeetCode.collectAsStateWithLifecycle()
    val isVerifyingGh by viewModel.isVerifyingGitHub.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    val dashboardData by viewModel.dashboardData.collectAsStateWithLifecycle()

    val bgGradient = remember {
        Brush.verticalGradient(
            listOf(
                Color(0xFF090D16),
                Color(0xFF0F172A),
                Color(0xFF090D16)
            )
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Top Header with Chibi Jane Logo & App Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xD9101824))
                        .border(1.dp, Color(0x38FFFFFF), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_skill_jane),
                        contentDescription = "Chibi Jane Logo",
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color(0x55FFFFFF), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dev Life",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Developer Activity & Widget Dashboard",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                // Status Banner if any
                syncMessage?.let { msg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x2E3B82F6))
                            .border(1.dp, Color(0x5560A5FA), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = msg,
                            color = Color(0xFF93C5FD),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // SECTION 1: PROFILES CONFIGURATION & PER-ACCOUNT STATUS
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xD9101824))
                        .border(1.dp, Color(0x38FFFFFF), RoundedCornerShape(22.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "Developer Accounts & Linking",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // 1. LeetCode Profile Section
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x1FFFFFFF))
                                .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(R.drawable.ic_leetcode_logo),
                                            contentDescription = "LeetCode",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "LeetCode Profile",
                                            color = Color(0xFFFFA116),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    AccountStatusBadge(
                                        status = dashboardData.leetCode.status,
                                        isVerifying = isVerifyingLc,
                                        connectedText = "✓ Linked (${dashboardData.leetCode.totalSolved} solved)",
                                        failedText = "✗ Failed"
                                    )
                                }

                                if (dashboardData.leetCode.status == AccountStatus.FAILED && dashboardData.leetCode.statusMessage.isNotBlank()) {
                                    Text(
                                        text = dashboardData.leetCode.statusMessage,
                                        color = Color(0xFFF87171),
                                        fontSize = 11.sp
                                    )
                                }

                                OutlinedTextField(
                                    value = leetCodeInput,
                                    onValueChange = viewModel::onLeetCodeInputChanged,
                                    label = { Text("LeetCode Username", color = Color(0xFF94A3B8)) },
                                    placeholder = { Text("e.g. sangrechy", color = Color(0xFF64748B)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFFFFA116),
                                        unfocusedBorderColor = Color(0x40FFFFFF)
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                                )

                                Button(
                                    onClick = {
                                        viewModel.verifyLeetCode()
                                        DevActivityWidgetProvider.updateAllWidgets(context, dashboardData)
                                    },
                                    enabled = !isVerifyingLc && !isSyncing,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFFA116),
                                        contentColor = Color.Black
                                    )
                                ) {
                                    if (isVerifyingLc) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.Black
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Verifying LeetCode...", fontWeight = FontWeight.Bold)
                                    } else {
                                        Text("Link & Verify LeetCode", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // 2. GitHub Profile Section
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x1FFFFFFF))
                                .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(R.drawable.ic_github_logo),
                                            contentDescription = "GitHub",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "GitHub Profile",
                                            color = Color(0xFF3FB950),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    AccountStatusBadge(
                                        status = dashboardData.gitHub.status,
                                        isVerifying = isVerifyingGh,
                                        connectedText = "✓ Linked (${dashboardData.gitHub.totalContributions} contribs)",
                                        failedText = "✗ Failed"
                                    )
                                }

                                if (dashboardData.gitHub.status == AccountStatus.FAILED && dashboardData.gitHub.statusMessage.isNotBlank()) {
                                    Text(
                                        text = dashboardData.gitHub.statusMessage,
                                        color = Color(0xFFF87171),
                                        fontSize = 11.sp
                                    )
                                }

                                OutlinedTextField(
                                    value = gitHubInput,
                                    onValueChange = viewModel::onGitHubInputChanged,
                                    label = { Text("GitHub Username", color = Color(0xFF94A3B8)) },
                                    placeholder = { Text("e.g. sangrechy", color = Color(0xFF64748B)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF3FB950),
                                        unfocusedBorderColor = Color(0x40FFFFFF)
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                                )

                                OutlinedTextField(
                                    value = tokenInput,
                                    onValueChange = viewModel::onGitHubTokenChanged,
                                    label = { Text("GitHub Token (Optional)", color = Color(0xFF94A3B8)) },
                                    placeholder = { Text("ghp_... for private contributions", color = Color(0xFF64748B)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    visualTransformation = PasswordVisualTransformation(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF3FB950),
                                        unfocusedBorderColor = Color(0x40FFFFFF)
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    )
                                )

                                Button(
                                    onClick = {
                                        viewModel.verifyGitHub()
                                        DevActivityWidgetProvider.updateAllWidgets(context, dashboardData)
                                    },
                                    enabled = !isVerifyingGh && !isSyncing,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF22C55E),
                                        contentColor = Color.Black
                                    )
                                ) {
                                    if (isVerifyingGh) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.Black
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Verifying GitHub...", fontWeight = FontWeight.Bold)
                                    } else {
                                        Text("Link & Verify GitHub", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Global Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.saveAndSync()
                                    DevActivityWidgetProvider.enqueueImmediateSync(context)
                                },
                                enabled = !isSyncing,
                                modifier = Modifier.weight(1.2f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2563EB),
                                    contentColor = Color.White
                                )
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Syncing...", fontWeight = FontWeight.Bold)
                                } else {
                                    Text("Sync All & Update Widget", fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.useSampleData()
                                    DevActivityWidgetProvider.updateAllWidgets(context, dashboardData)
                                },
                                modifier = Modifier.weight(0.8f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFCBD5E1)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Brush.linearGradient(listOf(Color(0x60FFFFFF), Color(0x30FFFFFF)))
                                )
                            ) {
                                Text("Use Sample")
                            }
                        }
                    }
                }

                // SECTION 2: LIVE WIDGET PREVIEW
                Text(
                    text = "Live Home Screen Widget Preview",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                WidgetVisualPreview(data = dashboardData)

                // SECTION 3: PIN WIDGET SHORTCUT
                Button(
                    onClick = {
                        pinWidgetToHomeScreen(context)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF334155),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Pin Widget to Home Screen", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AccountStatusBadge(
    status: AccountStatus,
    isVerifying: Boolean,
    connectedText: String,
    failedText: String
) {
    val (bgColor, borderColor, textColor, text) = when {
        isVerifying -> Quadruple(Color(0x333B82F6), Color(0x663B82F6), Color(0xFF93C5FD), "Verifying...")
        status == AccountStatus.CONNECTED -> Quadruple(Color(0x3322C55E), Color(0x6622C55E), Color(0xFF4ADE80), connectedText)
        status == AccountStatus.FAILED -> Quadruple(Color(0x33EF4444), Color(0x66EF4444), Color(0xFFF87171), failedText)
        else -> Quadruple(Color(0x26FFFFFF), Color(0x38FFFFFF), Color(0xFF94A3B8), "○ Not Linked")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun WidgetVisualPreview(data: WidgetDashboardData) {
    val lcCircle = remember(data.leetCode) {
        WidgetBitmapRenderer.renderLeetCodeCircle(data.leetCode, sizePx = 360).asImageBitmap()
    }
    val lcHeatmap = remember(data.leetCode.submissionCalendar) {
        WidgetBitmapRenderer.renderHeatmap(
            data.leetCode.submissionCalendar,
            isLeetCode = true,
            widthPx = 450,
            heightPx = 480,
            weeksCount = 52
        ).asImageBitmap()
    }
    val ghTypeMap = remember(data.gitHub) {
        WidgetBitmapRenderer.renderGitHubTypeMap(data.gitHub, sizePx = 360).asImageBitmap()
    }
    val ghHeatmap = remember(data.gitHub.contributionDays) {
        WidgetBitmapRenderer.renderHeatmap(
            data.gitHub.contributionDays,
            isLeetCode = false,
            widthPx = 450,
            heightPx = 480,
            weeksCount = 52
        ).asImageBitmap()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xD9101824))
            .border(
                1.5.dp,
                Color(0x55FFFFFF),
                RoundedCornerShape(26.dp)
            )
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

            // Header with Chibi Jane logo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.ic_skill_jane),
                        contentDescription = "Chibi Jane Logo",
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 6.dp)
                    )
                    Text(
                        text = "Dev Life",
                        color = Color(0xFFFFFFFF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    text = "Live Glass Preview",
                    color = Color(0xFFCBD5E1),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Top Card: LeetCode Glass Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x26FFFFFF))
                    .border(
                        1.dp,
                        Color(0x38FFFFFF),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_leetcode_logo),
                                contentDescription = "LeetCode",
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "LeetCode",
                                color = Color(0xFFFFA116),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (data.leetCode.status == AccountStatus.FAILED)
                                "@${data.leetCode.username.ifBlank { "dev" }} (Offline)"
                            else
                                "@${data.leetCode.username.ifBlank { "dev" }}",
                            color = Color(0xFF94A3B8),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Side-by-side: 3/4 Circular Gauge (Left 42%) + Divider + Month-Aware Heatmap (Right 58%)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = lcCircle,
                            contentDescription = "LeetCode Count",
                            modifier = Modifier
                                .weight(0.42f)
                                .fillMaxHeight()
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(Color(0x26FFFFFF))
                        )
                        Image(
                            bitmap = lcHeatmap,
                            contentDescription = "LeetCode Heatmap",
                            modifier = Modifier
                                .weight(0.58f)
                                .fillMaxHeight()
                        )
                    }
                }
            }

            // Bottom Card: GitHub Glass Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x26FFFFFF))
                    .border(
                        1.dp,
                        Color(0x38FFFFFF),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_github_logo),
                                contentDescription = "GitHub",
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "GitHub",
                                color = Color(0xFF3FB950),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (data.gitHub.status == AccountStatus.FAILED)
                                "@${data.gitHub.username.ifBlank { "dev" }} (Offline)"
                            else
                                "@${data.gitHub.username.ifBlank { "dev" }}",
                            color = Color(0xFF94A3B8),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Side-by-side: Spider Radar Activity (Left 42%) + Divider + Month-Aware Heatmap (Right 58%)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = ghTypeMap,
                            contentDescription = "GitHub Type Map",
                            modifier = Modifier
                                .weight(0.42f)
                                .fillMaxHeight()
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(Color(0x26FFFFFF))
                        )
                        Image(
                            bitmap = ghHeatmap,
                            contentDescription = "GitHub Heatmap",
                            modifier = Modifier
                                .weight(0.58f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

private fun pinWidgetToHomeScreen(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val provider = ComponentName(context, DevActivityWidgetProvider::class.java)
        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            appWidgetManager.requestPinAppWidget(provider, null, null)
            Toast.makeText(context, "Pinning widget to home screen...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                context,
                "Add widget manually from your home screen widget menu",
                Toast.LENGTH_LONG
            ).show()
        }
    } else {
        Toast.makeText(
            context,
            "Long-press your home screen and select DevWidgets",
            Toast.LENGTH_LONG
        ).show()
    }
}
