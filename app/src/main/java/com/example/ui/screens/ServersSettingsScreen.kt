package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ServerNode
import com.example.ui.MainUiState
import com.example.ui.components.GermanyFlagBadge
import com.example.ui.theme.CardSurface
import com.example.ui.theme.ChampGold
import com.example.ui.theme.ChampGoldContainer
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.GermanCrimson
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.PitchEmerald
import com.example.ui.theme.PitchEmeraldContainer
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ServersSettingsScreen(
    uiState: MainUiState,
    onSelectNode: (String) -> Unit,
    onProbeAllNodes: () -> Unit,
    onToggleAutoConnectGermany: (Boolean) -> Unit,
    onToggleBypassEfootballFromVpn: (Boolean) -> Unit,
    onToggleLuckStabilityMode: (Boolean) -> Unit,
    onToggleLanguage: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isArabic = uiState.settings.isArabic

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header + Ping All Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isArabic) "سيرفرات ألمانيا (DNS & Route)" else "Germany Route Nodes",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = if (isArabic) "اختر نود ألمانيا الأسرع لبكجات ومباريات PES" else "Pick fastest Germany node for PES stability",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onProbeAllNodes,
                enabled = !uiState.isProbingAll,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("probe_all_nodes_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElevatedSurface,
                    contentColor = PitchEmerald
                ),
                border = BorderStroke(1.dp, PitchEmerald.copy(alpha = 0.5f))
            ) {
                if (uiState.isProbingAll) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = PitchEmerald
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.NetworkCheck,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isArabic) "قياس البنق" else "Ping All",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // German Nodes List
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            uiState.nodes.forEach { node ->
                val isSelected = node.id == uiState.settings.selectedNodeId
                ServerNodeCard(
                    node = node,
                    isSelected = isSelected,
                    isArabic = isArabic,
                    onClick = { onSelectNode(node.id) }
                )
            }
        }

        // Automation & Routing Settings Section
        Text(
            text = if (isArabic) "إعدادات الأمان والتوجيه" else "Safety & Routing Settings",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )

        Surface(
            shape = MaterialTheme.shapes.medium,
            color = CardSurface,
            border = BorderStroke(1.dp, OutlineSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SettingToggleRow(
                    icon = Icons.Filled.SportsSoccer,
                    title = if (isArabic) "استثناء eFootball من الـ VPN (موصى به)" else "Bypass eFootball from VPN (Recommended)",
                    subtitle = if (isArabic) {
                        "يمنع ظهور خطأ الاتصال بخادم eFootball ويبقي اللعبة على اتصالها الأصلي"
                    } else {
                        "Prevents Konami server disconnect errors by keeping game traffic direct"
                    },
                    checked = uiState.settings.bypassEfootballFromVpn,
                    onCheckedChange = onToggleBypassEfootballFromVpn,
                    testTag = "switch_bypass_efootball"
                )

                SettingToggleRow(
                    icon = Icons.Filled.Dns,
                    title = if (isArabic) "تشغيل نفق ألمانيا تلقائياً عند الفتح" else "Auto-Connect VPN on Launch",
                    subtitle = if (isArabic) {
                        "يشغّل الـ VPN تلقائياً عند فتح التطبيق (مغلق افتراضياً لتجنب تقطيع اللعبة)"
                    } else {
                        "Automatically starts VPN tunnel on launch (off by default for game stability)"
                    },
                    checked = uiState.settings.autoConnectGermany,
                    onCheckedChange = onToggleAutoConnectGermany,
                    testTag = "switch_auto_connect_germany"
                )

                SettingToggleRow(
                    icon = Icons.Filled.AutoAwesome,
                    title = if (isArabic) "تفعيل وضع استقرار الحظ والبنق" else "Luck & Ping Stability Mode",
                    subtitle = if (isArabic) {
                        "تحديث فوري لحسابات مؤشر الاستقرار وتوقيت السيرفر الألماني"
                    } else {
                        "Keeps live Frankfurt timing & stability scoring prioritized"
                    },
                    checked = uiState.settings.luckStabilityMode,
                    onCheckedChange = onToggleLuckStabilityMode,
                    testTag = "switch_luck_stability"
                )

                SettingToggleRow(
                    icon = Icons.Filled.Language,
                    title = if (isArabic) "واجهة عربية (RTL)" else "Arabic Interface (RTL)",
                    subtitle = if (isArabic) "التبديل بين العربية والإنجليزية" else "Switch between Arabic & English",
                    checked = uiState.settings.isArabic,
                    onCheckedChange = onToggleLanguage,
                    testTag = "switch_language_arabic"
                )
            }
        }
    }
}

@Composable
private fun ServerNodeCard(
    node: ServerNode,
    isSelected: Boolean,
    isArabic: Boolean,
    onClick: () -> Unit
) {
    val pingColor = when (val ping = node.measuredPingMs) {
        null -> TextSecondary
        in 0..85 -> PitchEmerald
        in 86..140 -> ChampGold
        else -> GermanCrimson
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) ElevatedSurface else CardSurface,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) PitchEmerald else OutlineSubtle
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .testTag("server_node_${node.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) PitchEmerald else TextSecondary
                )
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isArabic) node.nameAr else node.nameEn,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (node.isDefaultGermany) {
                            GermanyFlagBadge(label = "DE")
                        }
                    }
                    Text(
                        text = "${if (isArabic) node.cityAr else node.cityEn} • ${node.primaryDns}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (node.measuredPingMs != null && node.measuredPingMs <= 85) {
                    PitchEmeraldContainer
                } else {
                    ChampGoldContainer.copy(alpha = 0.65f)
                }
            ) {
                Text(
                    text = when {
                        node.isProbing -> "..."
                        node.measuredPingMs != null -> "${node.measuredPingMs} ms"
                        else -> "-- ms"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = pingColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) PitchEmerald else TextSecondary
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianBg,
                checkedTrackColor = PitchEmerald
            )
        )
    }
}
