package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.VpnConnectionState
import com.example.ui.theme.CardSurface
import com.example.ui.theme.ChampGold
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.GermanCrimson
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.PitchEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GermanyFlagBadge(
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = CardSurface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, ChampGold.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .size(width = 20.dp, height = 14.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF111111))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(GermanCrimson)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(ChampGold)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = ChampGold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun VpnPowerReactorButton(
    state: VpnConnectionState,
    isArabic: Boolean,
    elapsedFormatted: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = state == VpnConnectionState.CONNECTED
    val isConnecting = state == VpnConnectionState.CONNECTING

    val infiniteTransition = rememberInfiniteTransition(label = "vpn_reactor_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isConnected || isConnecting) 1.14f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isConnecting) 750 else 1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600),
            repeatMode = RepeatMode.Restart
        ),
        label = "reactor_sweep"
    )

    val coreColor by animateColorAsState(
        targetValue = when (state) {
            VpnConnectionState.CONNECTED -> PitchEmerald
            VpnConnectionState.CONNECTING -> ChampGold
            VpnConnectionState.PERMISSION_REQUIRED -> ChampGold
            VpnConnectionState.ERROR -> GermanCrimson
            VpnConnectionState.DISCONNECTED -> OutlineSubtle
        },
        animationSpec = spring(),
        label = "core_color"
    )

    val statusText = when (state) {
        VpnConnectionState.CONNECTED -> if (isArabic) "متصل بألمانيا" else "CONNECTED • DE"
        VpnConnectionState.CONNECTING -> if (isArabic) "جارٍ الربط بألمانيا..." else "CONNECTING..."
        VpnConnectionState.PERMISSION_REQUIRED -> if (isArabic) "اضغط لمنح إذن VPN" else "GRANT VPN ACCESS"
        VpnConnectionState.ERROR -> if (isArabic) "إعادة المحاولة" else "RETRY ROUTE"
        VpnConnectionState.DISCONNECTED -> if (isArabic) "اضغط للاتصال التلقائي" else "TAP TO CONNECT"
    }

    Box(
        modifier = modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing pulse halo
        Box(
            modifier = Modifier
                .size(204.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            coreColor.copy(alpha = if (isConnected) 0.28f else 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Custom ring arc
        Canvas(modifier = Modifier.size(188.dp)) {
            val stroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(
                color = OutlineSubtle.copy(alpha = 0.5f),
                style = stroke
            )
            if (isConnected || isConnecting) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            coreColor.copy(alpha = 0.35f),
                            coreColor,
                            ChampGold
                        )
                    ),
                    startAngle = sweepAngle,
                    sweepAngle = 210f,
                    useCenter = false,
                    style = stroke
                )
            }
        }

        // Tactile circular button
        Surface(
            modifier = Modifier
                .size(156.dp)
                .minimumInteractiveComponentSize()
                .clip(CircleShape)
                .clickable(onClick = onClick)
                .testTag("vpn_connect_button"),
            shape = CircleShape,
            color = ElevatedSurface,
            border = BorderStroke(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(coreColor, ChampGold.copy(alpha = if (isConnected) 0.85f else 0.3f))
                )
            ),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                ElevatedSurface,
                                ObsidianBg
                            )
                        )
                    )
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Filled.Shield else Icons.Filled.PowerSettingsNew,
                    contentDescription = stringResource(id = R.string.cd_connect_button),
                    tint = coreColor,
                    modifier = Modifier.size(42.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isConnected) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = elapsedFormatted,
                        style = MaterialTheme.typography.labelMedium,
                        color = PitchEmerald
                    )
                }
            }
        }
    }
}

@Composable
fun TelemetryStatTile(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = CardSurface,
        border = BorderStroke(1.dp, OutlineSubtle.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LuckScoreArc(
    score: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    val animatedSweep by animateFloatAsState(
        targetValue = (score.coerceIn(0, 100) / 100f) * 260f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "luck_arc_sweep"
    )
    val scoreColor = when {
        score >= 80 -> PitchEmerald
        score >= 55 -> ChampGold
        else -> GermanCrimson
    }

    Box(
        modifier = modifier.size(168.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            val strokeWidth = 12.dp.toPx()
            val arcSize = Size(size.width, size.height)
            drawArc(
                color = OutlineSubtle.copy(alpha = 0.55f),
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = Offset.Zero,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.horizontalGradient(
                    colors = listOf(ChampGold, PitchEmerald)
                ),
                startAngle = 140f,
                sweepAngle = animatedSweep,
                useCenter = false,
                topLeft = Offset.Zero,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = label,
                tint = ChampGold,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = "$score%",
                style = MaterialTheme.typography.displaySmall,
                color = scoreColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}
