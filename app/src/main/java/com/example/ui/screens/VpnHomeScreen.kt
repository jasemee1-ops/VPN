package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
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
import com.example.model.VpnConnectionState
import com.example.ui.MainUiState
import com.example.ui.components.GermanyFlagBadge
import com.example.ui.components.TelemetryStatTile
import com.example.ui.components.VpnPowerReactorButton
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
fun VpnHomeScreen(
    uiState: MainUiState,
    onToggleVpn: () -> Unit,
    onNavigateToLuckBoost: () -> Unit,
    onNavigateToServers: () -> Unit,
    onLaunchEfootball: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isArabic = uiState.settings.isArabic
    val telemetry = uiState.telemetry
    val selectedNode = uiState.nodes.firstOrNull { it.id == uiState.settings.selectedNodeId }
        ?: uiState.nodes.first()
    val isConnected = telemetry.state == VpnConnectionState.CONNECTED

    val pingDisplay = (telemetry.lastMeasuredLatencyMs ?: selectedNode.measuredPingMs)
        ?.let { "$it ms" } ?: "-- ms"
    val jitterDisplay = selectedNode.jitterMs?.let { "$it ms" } ?: "-- ms"
    val lossDisplay = "${selectedNode.packetLossPercent}%"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Frankfurt Stadium Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(156.dp)
                .clip(MaterialTheme.shapes.large)
                .background(CardSurface)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_frankfurt_stadium_banner),
                contentDescription = stringResource(id = R.string.cd_frankfurt_banner),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                ObsidianBg.copy(alpha = 0.25f),
                                ObsidianBg.copy(alpha = 0.88f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GermanyFlagBadge(
                        label = if (isArabic) {
                            if (uiState.settings.bypassEfootballFromVpn) "وضع الأمان • eFootball" else "ألمانيا • DE"
                        } else {
                            if (uiState.settings.bypassEfootballFromVpn) "Safe Mode • eFootball" else "Germany • DE"
                        }
                    )

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isConnected) PitchEmeraldContainer else ElevatedSurface,
                        border = BorderStroke(
                            1.dp,
                            if (isConnected) PitchEmerald.copy(alpha = 0.6f) else OutlineSubtle
                        )
                    ) {
                        Text(
                            text = if (isArabic) {
                                if (isConnected) "VPN نشط" else "بدون DNS VPN (آمن)"
                            } else {
                                if (isConnected) "VPN LIVE" else "DIRECT (SAFE)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isConnected) PitchEmerald else TextSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = if (isArabic) selectedNode.nameAr else selectedNode.nameEn,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isArabic) {
                            "${selectedNode.cityAr} • DNS ${selectedNode.primaryDns}"
                        } else {
                            "${selectedNode.cityEn} • DNS ${selectedNode.primaryDns}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChampGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Warning if ANY system VPN is currently active (eFootball often rejects VPN transport)
        AnimatedVisibility(visible = uiState.isAnySystemVpnActive) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = ChampGoldContainer,
                border = BorderStroke(1.dp, ChampGold.copy(alpha = 0.65f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = ChampGold
                    )
                    Text(
                        text = if (isArabic) {
                            "يوجد VPN نشط على الجهاز حالياً: إذا ظهرت لك رسالة خطأ في خادم eFootball، أوقف الـ VPN ثم افتح اللعبة على الاتصال المباشر."
                        } else {
                            "A VPN is currently active on this device: if eFootball shows a server error, turn off VPN and launch the game directly."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }
        }

        // Permission Notice if needed
        AnimatedVisibility(visible = telemetry.state == VpnConnectionState.PERMISSION_REQUIRED) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = ChampGoldContainer,
                border = BorderStroke(1.dp, ChampGold.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = ChampGold
                    )
                    Text(
                        text = if (isArabic) {
                            "اضغط على زر الاتصال ووافق على إذن أندرويد لتشغيل نفق VPN الآمن."
                        } else {
                            "Tap Connect and approve the Android VPN prompt to enable the safe VPN tunnel."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }
        }

        // Central Power Reactor Button
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            VpnPowerReactorButton(
                state = telemetry.state,
                isArabic = isArabic,
                elapsedFormatted = formatDuration(uiState.elapsedSeconds),
                onClick = onToggleVpn
            )
        }

        // Safe eFootball status indicator
        Surface(
            shape = MaterialTheme.shapes.small,
            color = PitchEmeraldContainer.copy(alpha = 0.7f),
            border = BorderStroke(1.dp, PitchEmerald.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = PitchEmerald,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (isArabic) {
                        if (uiState.settings.bypassEfootballFromVpn) {
                            "اللعبة مستثناة من تقطيع DNS — اتصال خادم eFootball محمي ومستقر"
                        } else {
                            "وضع DNS المباشر غير الحاجز مفعّل (مع احتياطي 1.1.1.1)"
                        }
                    } else {
                        if (uiState.settings.bypassEfootballFromVpn) {
                            "eFootball DNS bypass active — prevents Konami server disconnect errors"
                        } else {
                            "Non-blocking DNS route active (with 1.1.1.1 fast fallback)"
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = PitchEmerald
                )
            }
        }

        // Live Real-Time Telemetry Grid (2x2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TelemetryStatTile(
                title = if (isArabic) "بنق ألمانيا" else "DE Ping",
                value = pingDisplay,
                icon = Icons.Filled.NetworkPing,
                accentColor = PitchEmerald,
                modifier = Modifier.weight(1f)
            )
            TelemetryStatTile(
                title = if (isArabic) "استقرار التذبذب" else "Jitter",
                value = jitterDisplay,
                icon = Icons.Filled.Speed,
                accentColor = ChampGold,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TelemetryStatTile(
                title = if (isArabic) "فقدان الحزم" else "Packet Loss",
                value = lossDisplay,
                icon = Icons.Filled.Dns,
                accentColor = PitchEmerald,
                modifier = Modifier.weight(1f)
            )
            TelemetryStatTile(
                title = if (isArabic) "توقيت فرانكفورت" else "Frankfurt Time",
                value = uiState.luckMetrics.germanyTimeFormatted,
                icon = Icons.Filled.Schedule,
                accentColor = ChampGold,
                modifier = Modifier.weight(1f)
            )
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onNavigateToLuckBoost,
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("open_luck_boost_button"),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChampGold,
                    contentColor = ObsidianBg
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isArabic) "بوست الحظ (${uiState.luckMetrics.overallScore}%)" else "Luck Boost (${uiState.luckMetrics.overallScore}%)",
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (uiState.isEfootballInstalled) {
                OutlinedButton(
                    onClick = onLaunchEfootball,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("launch_efootball_button"),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, PitchEmerald)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SportsSoccer,
                        contentDescription = null,
                        tint = PitchEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "تشغيل eFootball" else "Open eFootball",
                        style = MaterialTheme.typography.labelLarge,
                        color = PitchEmerald,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onNavigateToServers,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("switch_node_button"),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, OutlineSubtle)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Dns,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "سيرفرات ألمانيا" else "DE Servers",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Honest technical note card
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = CardSurface.copy(alpha = 0.75f),
            border = BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (isArabic) {
                        "للعب eFootball بدون أخطاء اتصال: الأفضل إبقاء خيار استثناء اللعبة مفعّلاً (أو اللعب بدون تشغيل VPN على DNS اللعبة)، لأن خوادم Konami تقطع الاتصال عند تأخر DNS على شبكات الهاتف."
                    } else {
                        "For stable eFootball connections: keeping eFootball VPN bypass enabled (or playing on direct connection) prevents Konami server timeout errors on mobile networks."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

private fun formatDuration(totalSeconds: Long): String {
    val hrs = totalSeconds / 3600
    val mins = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return if (hrs > 0) {
        String.format("%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}
