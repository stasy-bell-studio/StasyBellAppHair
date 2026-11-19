package com.stasybell.os.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stasybell.os.domain.AppointmentStatus
import com.stasybell.os.domain.DataMode
import com.stasybell.os.ui.theme.ForestPalette

/**
 * Compact hero gradient header (#092D1E -> #0B3825 -> #1F6342) with subtle botanical contour arcs
 * and limited glass KPI chips. Below this header all operational surfaces remain opaque and crisp.
 */
@Composable
fun ForestHeroHeader(
    title: String,
    subtitle: String,
    dataMode: DataMode,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val gradient = Brush.linearGradient(
        colors = listOf(ForestPalette.Forest, ForestPalette.PrimaryDeep, ForestPalette.Primary)
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp))
            .background(gradient)
    ) {
        // Subtle organic botanical arcs in corners
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = ForestPalette.Mint.copy(alpha = 0.08f),
                radius = size.width * 0.42f,
                center = Offset(size.width * 0.92f, size.height * 0.15f),
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = ForestPalette.GoldMicro.copy(alpha = 0.12f),
                radius = size.width * 0.24f,
                center = Offset(size.width * 0.95f, size.height * 0.22f),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (dataMode == DataMode.DEMO_ROOM) {
                            DemoModePill(onDark = true)
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = ForestPalette.Mint
                    )
                }
                if (trailing != null) {
                    Spacer(Modifier.width(8.dp))
                    trailing()
                }
            }
            content()
        }
    }
}

@Composable
fun GlassKpiTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    testTag: String? = null
) {
    Column(
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ForestPalette.Mint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (caption != null) {
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                color = ForestPalette.Mint.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DemoModePill(onDark: Boolean = false, modifier: Modifier = Modifier) {
    val bg = if (onDark) ForestPalette.GoldMicro.copy(alpha = 0.22f) else ForestPalette.WarningBg
    val fg = if (onDark) ForestPalette.GoldMicro else ForestPalette.Warning
    val border = if (onDark) ForestPalette.GoldMicro.copy(alpha = 0.45f) else ForestPalette.Warning.copy(alpha = 0.3f)
    Surface(
        modifier = modifier.testTag("demo_mode_badge"),
        shape = CircleShape,
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Text(
            text = "Демо",
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun StatusPill(
    label: String,
    fg: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(fg)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AppointmentStatusBadge(status: AppointmentStatus, modifier: Modifier = Modifier) {
    val (fg, bg) = when (status) {
        AppointmentStatus.CONFIRMED -> ForestPalette.Success to ForestPalette.SuccessBg
        AppointmentStatus.COMPLETED -> ForestPalette.PrimaryDeep to ForestPalette.Mint
        AppointmentStatus.PENDING_CHANNEL -> ForestPalette.Warning to ForestPalette.WarningBg
        AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW, AppointmentStatus.EXPIRED ->
            ForestPalette.Error to ForestPalette.ErrorBg
        AppointmentStatus.BLOCKED -> ForestPalette.Info to ForestPalette.InfoBg
    }
    StatusPill(label = status.labelRu, fg = fg, bg = bg, modifier = modifier)
}

@Composable
fun OperationalCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = ForestPalette.Surface,
            border = BorderStroke(1.dp, ForestPalette.Border)
        ) {
            Column(modifier = Modifier.padding(14.dp), content = content)
        }
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = ForestPalette.Surface,
            border = BorderStroke(1.dp, ForestPalette.Border)
        ) {
            Column(modifier = Modifier.padding(14.dp), content = content)
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    description: String,
    icon: ImageVector = Icons.Outlined.Info,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = ForestPalette.CanvasSoft,
        border = BorderStroke(1.dp, ForestPalette.Border)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ForestPalette.MintSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = ForestPalette.Primary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = ForestPalette.Ink)
                Spacer(Modifier.height(2.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = ForestPalette.Muted)
            }
        }
    }
}

@Composable
fun WeeklyCashBarChart(
    weeklyIncome: List<Long>,
    weeklyExpense: List<Long>,
    modifier: Modifier = Modifier
) {
    val maxVal = (weeklyIncome + weeklyExpense).maxOrNull()?.coerceAtLeast(1L) ?: 1L
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val count = 5
            val groupWidth = size.width / count
            val barWidth = (groupWidth * 0.28f).coerceAtLeast(8.dp.toPx())
            val gap = 4.dp.toPx()
            val bottomY = size.height - 6.dp.toPx()
            val usableH = (size.height - 16.dp.toPx()).coerceAtLeast(10f)

            drawLine(
                color = ForestPalette.Border,
                start = Offset(0f, bottomY),
                end = Offset(size.width, bottomY),
                strokeWidth = 1.dp.toPx()
            )

            for (i in 0 until count) {
                val inc = weeklyIncome.getOrElse(i) { 0L }.coerceAtLeast(0L)
                val exp = weeklyExpense.getOrElse(i) { 0L }.coerceAtLeast(0L)
                val incH = (inc.toFloat() / maxVal.toFloat()) * usableH
                val expH = (exp.toFloat() / maxVal.toFloat()) * usableH
                val centerX = groupWidth * i + groupWidth / 2f

                if (incH > 1f) {
                    drawRoundRect(
                        color = ForestPalette.Green,
                        topLeft = Offset(centerX - barWidth - gap / 2f, bottomY - incH),
                        size = Size(barWidth, incH),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
                if (expH > 1f) {
                    drawRoundRect(
                        color = ForestPalette.Warning.copy(alpha = 0.85f),
                        topLeft = Offset(centerX + gap / 2f, bottomY - expH),
                        size = Size(barWidth, expH),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            listOf("Нед 1", "Нед 2", "Нед 3", "Нед 4", "Нед 5").forEach { label ->
                Text(label, style = MaterialTheme.typography.labelSmall, color = ForestPalette.Muted)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(color = ForestPalette.Green, label = "Доход (факт)")
            LegendDot(color = ForestPalette.Warning, label = "Расход (факт)")
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = ForestPalette.Muted)
    }
}

@Composable
fun ConfirmActionDialog(
    title: String,
    message: String,
    confirmLabel: String,
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(18.dp),
        containerColor = ForestPalette.Surface,
        title = { Text(title, style = MaterialTheme.typography.titleMedium, color = ForestPalette.Ink) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium, color = ForestPalette.Body) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isDestructive) ForestPalette.Error else ForestPalette.Primary
                )
            ) {
                Text(confirmLabel, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = ForestPalette.Muted)
            }
        }
    )
}
