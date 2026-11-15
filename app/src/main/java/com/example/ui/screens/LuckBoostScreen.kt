package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.PackTier
import com.example.ui.MainUiState
import com.example.ui.components.LuckScoreArc
import com.example.ui.theme.CardSurface
import com.example.ui.theme.ChampGold
import com.example.ui.theme.ChampGoldContainer
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.PitchEmerald
import com.example.ui.theme.PitchEmeraldContainer
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LuckBoostScreen(
    uiState: MainUiState,
    onRunBoostCalibration: () -> Unit,
    onToggleRitualStep: (Int) -> Unit,
    onTogglePackTimer: () -> Unit,
    onResetPackTimer: () -> Unit,
    onLogPackPull: (String, PackTier, Int) -> Unit,
    onClearPackHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isArabic = uiState.settings.isArabic
    val luck = uiState.luckMetrics

    var packNameInput by remember { mutableStateOf("") }
    var selectedTier by remember { mutableStateOf(PackTier.EPIC_SHOWTIME) }
    var selectedCoins by remember { mutableStateOf(100) }
    var showLogForm by remember { mutableStateOf(false) }

    val ritualSteps = if (isArabic) {
        listOf(
            "تفعيل نفق VPN ألمانيا (فرانكفورت) تلقائياً",
            "فحص ثبات البنق وفقدان الحزم 0%",
            "الانتظار 25 ثانية لتهدئة الجلسة قبل التفتيح",
            "التركيز على البكج وتفتيح 100 كوينز بهدوء"
        )
    } else {
        listOf(
            "Activate Germany (Frankfurt) VPN route",
            "Verify ping stability & 0% packet loss",
            "Wait 25s session stabilization timer",
            "Open target pack cleanly in eFootball"
        )
    }

    val totalPulls = uiState.packHistory.size
    val epicPulls = uiState.packHistory.count { it.tier == PackTier.EPIC_SHOWTIME }
    val totalCoins = uiState.packHistory.sumOf { it.coinsSpent }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Crest Header Card + Luck Readiness Arc
        Surface(
            shape = MaterialTheme.shapes.large,
            color = CardSurface,
            border = BorderStroke(1.dp, ChampGold.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box {
                Image(
                    painter = painterResource(id = R.drawable.img_luck_pack_crest),
                    contentDescription = stringResource(id = R.string.cd_luck_crest),
                    contentScale = ContentScale.Crop,
                    alpha = 0.24f,
                    modifier = Modifier.matchParentSize()
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    ObsidianBg.copy(alpha = 0.35f),
                                    CardSurface.copy(alpha = 0.9f)
                                )
                            )
                        )
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isArabic) "مؤشر استقرار الحظ • ألمانيا" else "GERMANY LUCK & STABILITY INDEX",
                        style = MaterialTheme.typography.titleMedium,
                        color = ChampGold,
                        fontWeight = FontWeight.Bold
                    )

                    LuckScoreArc(
                        score = luck.overallScore,
                        label = if (isArabic) "جاهزية المسار" else "Route Readiness"
                    )

                    Text(
                        text = if (isArabic) luck.windowStatusAr else luck.windowStatusEn,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )

                    // Breakdown chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScorePill(
                            label = if (isArabic) "النفق" else "Tunnel",
                            value = "${luck.tunnelScore}/35",
                            modifier = Modifier.weight(1f)
                        )
                        ScorePill(
                            label = if (isArabic) "البنق" else "Ping",
                            value = "${luck.pingScore}/25",
                            modifier = Modifier.weight(1f)
                        )
                        ScorePill(
                            label = if (isArabic) "الثبات" else "Jitter",
                            value = "${luck.jitterScore}/20",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = onRunBoostCalibration,
                        enabled = !luck.isBoostRunning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("calibrate_luck_boost_button"),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PitchEmerald,
                            contentColor = ObsidianBg
                        )
                    ) {
                        if (luck.isBoostRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = ObsidianBg
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isArabic) "جارٍ تنشيط وفحص مسار ألمانيا..." else "Calibrating Germany Route...",
                                style = MaterialTheme.typography.labelLarge
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isArabic) "تفعيل البوست وفحص سيرفر ألمانيا" else "Activate Boost & Probe Germany",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    val summaryText = if (isArabic) luck.lastBurstSummaryAr else luck.lastBurstSummaryEn
                    AnimatedVisibility(visible = !summaryText.isNullOrBlank()) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = PitchEmeraldContainer,
                            border = BorderStroke(1.dp, PitchEmerald.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = summaryText.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = PitchEmerald,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // 25s Pack Focus Countdown Timer
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = CardSurface,
            border = BorderStroke(1.dp, OutlineSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = ChampGold
                        )
                        Text(
                            text = if (isArabic) "مؤقت التفتيح (25 ثانية)" else "Pack Timing Focus (25s)",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "${uiState.packTimerSecondsRemaining}s",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (uiState.packTimerSecondsRemaining == 0) PitchEmerald else ChampGold
                    )
                }

                LinearProgressIndicator(
                    progress = { (25 - uiState.packTimerSecondsRemaining) / 25f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                    color = ChampGold,
                    trackColor = ElevatedSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onTogglePackTimer,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("pack_timer_toggle_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isPackTimerRunning) ChampGoldContainer else ElevatedSurface,
                            contentColor = ChampGold
                        )
                    ) {
                        Icon(
                            imageVector = if (uiState.isPackTimerRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.isPackTimerRunning) {
                                if (isArabic) "إيقاف" else "Pause"
                            } else {
                                if (isArabic) "ابدأ العد التنازلي" else "Start 25s Timer"
                            }
                        )
                    }

                    OutlinedButton(
                        onClick = onResetPackTimer,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("pack_timer_reset_button"),
                        border = BorderStroke(1.dp, OutlineSubtle)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = if (isArabic) "إعادة المؤقت" else "Reset timer",
                            tint = TextSecondary
                        )
                    }
                }
            }
        }

        // Pack Ritual Checklist
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = CardSurface,
            border = BorderStroke(1.dp, OutlineSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isArabic) "خطوات تجهيز التفتيح قبل الدخول للبكج" else "Pre-Pack Stability Checklist",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                ritualSteps.forEachIndexed { idx, stepText ->
                    val checked = uiState.ritualStepsDone.contains(idx)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onToggleRitualStep(idx) }
                            .minimumInteractiveComponentSize()
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("ritual_step_$idx"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { onToggleRitualStep(idx) }
                        )
                        Text(
                            text = stepText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (checked) PitchEmerald else TextPrimary
                        )
                    }
                }
            }
        }

        // Pack Pull Tracker & Stats
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = CardSurface,
            border = BorderStroke(1.dp, OutlineSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EmojiEvents,
                            contentDescription = null,
                            tint = ChampGold
                        )
                        Text(
                            text = if (isArabic) "سجل التفتيح والحظ" else "Pack Pull Tracker",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    Row {
                        if (totalPulls > 0) {
                            IconButton(
                                onClick = onClearPackHistory,
                                modifier = Modifier.testTag("clear_pack_history_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteSweep,
                                    contentDescription = if (isArabic) "مسح السجل" else "Clear history",
                                    tint = TextSecondary
                                )
                            }
                        }
                        IconButton(
                            onClick = { showLogForm = !showLogForm },
                            modifier = Modifier.testTag("toggle_log_pack_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = if (isArabic) "تسجيل فتحة بكج" else "Log pack pull",
                                tint = PitchEmerald
                            )
                        }
                    }
                }

                // Summary Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScorePill(
                        label = if (isArabic) "إجمالي الفتحات" else "Pulls",
                        value = "$totalPulls",
                        modifier = Modifier.weight(1f)
                    )
                    ScorePill(
                        label = if (isArabic) "إيبك / شو تايم" else "Epic Hits",
                        value = "$epicPulls",
                        modifier = Modifier.weight(1f)
                    )
                    ScorePill(
                        label = if (isArabic) "الكوينز" else "Coins",
                        value = "$totalCoins",
                        modifier = Modifier.weight(1f)
                    )
                }

                AnimatedVisibility(visible = showLogForm) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(ElevatedSurface)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = packNameInput,
                            onValueChange = { packNameInput = it },
                            label = {
                                Text(if (isArabic) "اسم البكج (مثال: Epic Germany)" else "Pack Name (e.g. Epic Box)")
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pack_name_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PackTier.entries.forEach { tier ->
                                FilterChip(
                                    selected = selectedTier == tier,
                                    onClick = { selectedTier = tier },
                                    label = {
                                        Text(
                                            text = if (isArabic) tier.labelAr else tier.labelEn,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ChampGoldContainer,
                                        selectedLabelColor = ChampGold
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(100, 900).forEach { coins ->
                                FilterChip(
                                    selected = selectedCoins == coins,
                                    onClick = { selectedCoins = coins },
                                    label = { Text("$coins Coins") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PitchEmeraldContainer,
                                        selectedLabelColor = PitchEmerald
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                onClick = {
                                    onLogPackPull(packNameInput, selectedTier, selectedCoins)
                                    packNameInput = ""
                                    showLogForm = false
                                },
                                modifier = Modifier.testTag("save_pack_pull_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PitchEmerald,
                                    contentColor = ObsidianBg
                                )
                            ) {
                                Text(if (isArabic) "حفظ" else "Save")
                            }
                        }
                    }
                }

                if (uiState.packHistory.isEmpty()) {
                    Text(
                        text = if (isArabic) {
                            "لم تسجّل أي فتحة بكج بعد — اضغط (+) لحفظ نتائجك مع بنق ألمانيا."
                        } else {
                            "No pack pulls logged yet — tap (+) to track pulls alongside your Germany ping."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                } else {
                    uiState.packHistory.take(6).forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .background(ElevatedSurface.copy(alpha = 0.7f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (item.tier == PackTier.EPIC_SHOWTIME) {
                                        Icons.Filled.CheckCircle
                                    } else {
                                        Icons.Filled.RadioButtonUnchecked
                                    },
                                    contentDescription = null,
                                    tint = if (item.tier == PackTier.EPIC_SHOWTIME) ChampGold else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = item.packName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isArabic) item.tier.labelAr else item.tier.labelEn,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (item.tier == PackTier.EPIC_SHOWTIME) ChampGold else TextSecondary
                                    )
                                }
                            }
                            Text(
                                text = "${item.coinsSpent}c • ${item.pingAtPullMs?.let { "${it}ms" } ?: "--"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = PitchEmerald
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScorePill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(ElevatedSurface)
            .padding(vertical = 8.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
