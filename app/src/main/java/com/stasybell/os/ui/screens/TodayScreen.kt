package com.stasybell.os.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stasybell.os.domain.AppointmentKind
import com.stasybell.os.domain.CalendarEntry
import com.stasybell.os.domain.StudioRules
import com.stasybell.os.ui.components.AppointmentStatusBadge
import com.stasybell.os.ui.components.EmptyStateCard
import com.stasybell.os.ui.components.ForestHeroHeader
import com.stasybell.os.ui.components.GlassKpiTile
import com.stasybell.os.ui.components.OperationalCard
import com.stasybell.os.ui.theme.ForestPalette
import com.stasybell.os.ui.viewmodel.AppUiState

/**
 * Screen 02: Today Dashboard ("Главная / Сегодня").
 * Strict invariant: KPI "Получено сегодня" shows ONLY manual cash ledger receipts, NEVER scheduled prices.
 */
@Composable
fun TodayScreen(
    state: AppUiState,
    onNewAppointment: () -> Unit,
    onQuickIncome: () -> Unit,
    onQuickExpense: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenEntryDetail: (String) -> Unit
) {
    val dashboard = state.todayDashboard
    val utilizationText = dashboard.utilizationPercent?.let { "$it%" } ?: "—"
    val lowStockCount = state.materials.count { it.isLowStock }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("today_screen"),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ForestHeroHeader(
                title = "Добрый день, Настя",
                subtitle = "${StudioRules.formatDate(dashboard.date)} · Санкт-Петербург (МСК)",
                dataMode = state.dataMode
            ) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassKpiTile(
                        label = "Записей сегодня",
                        value = dashboard.appointmentsTodayCount.toString(),
                        modifier = Modifier.weight(1f),
                        testTag = "kpi_today_appointments"
                    )
                    GlassKpiTile(
                        label = "Получено сегодня",
                        value = StudioRules.formatRubles(dashboard.actualCashReceivedTodayKopecks),
                        caption = "факт кассы",
                        modifier = Modifier.weight(1.3f),
                        testTag = "kpi_today_cash"
                    )
                    GlassKpiTile(
                        label = "Загрузка",
                        value = utilizationText,
                        caption = if (dashboard.utilizationPercent == null) "выходной" else "дня",
                        modifier = Modifier.weight(0.95f),
                        testTag = "kpi_today_utilization"
                    )
                }
            }
        }

        // Quick 1-tap operational actions
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onNewAppointment,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestPalette.Primary),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("today_new_appointment_button")
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Запись", fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = onOpenCalendar,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ForestPalette.Border),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("today_open_calendar_button")
                    ) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = ForestPalette.Primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Календарь", color = ForestPalette.Ink)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        onClick = onQuickIncome,
                        shape = RoundedCornerShape(12.dp),
                        color = ForestPalette.MintSoft,
                        border = BorderStroke(1.dp, ForestPalette.Mint),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("today_quick_income_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Outlined.ArrowDownward, contentDescription = null, tint = ForestPalette.Success, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Доход (факт)", style = MaterialTheme.typography.labelLarge, color = ForestPalette.PrimaryDeep)
                        }
                    }
                    Surface(
                        onClick = onQuickExpense,
                        shape = RoundedCornerShape(12.dp),
                        color = ForestPalette.WarningBg,
                        border = BorderStroke(1.dp, ForestPalette.Warning.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("today_quick_expense_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Outlined.ArrowUpward, contentDescription = null, tint = ForestPalette.Warning, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Расход", style = MaterialTheme.typography.labelLarge, color = ForestPalette.Warning)
                        }
                    }
                }
            }
        }

        if (lowStockCount > 0) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ForestPalette.WarningBg,
                    border = BorderStroke(1.dp, ForestPalette.Warning.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Склад: $lowStockCount поз. на критическом остатке — проверьте раздел «Материалы»",
                        style = MaterialTheme.typography.bodySmall,
                        color = ForestPalette.Warning,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ближайшие записи",
                    style = MaterialTheme.typography.titleLarge,
                    color = ForestPalette.Ink
                )
                Text(
                    text = "${dashboard.upcomingEntries.size} шт.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ForestPalette.Muted
                )
            }
        }

        if (dashboard.upcomingEntries.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "На сегодня записей нет",
                    description = "Нажмите «+ Запись», чтобы внести визит вручную, или откройте график дня.",
                    icon = Icons.Outlined.EventAvailable,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            items(dashboard.upcomingEntries, key = { it.id }) { entry ->
                TodayAppointmentRow(
                    entry = entry,
                    onClick = { onOpenEntryDetail(entry.id) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun TodayAppointmentRow(
    entry: CalendarEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OperationalCard(
        onClick = onClick,
        modifier = modifier.testTag("today_entry_${entry.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = ForestPalette.Primary, modifier = Modifier.size(16.dp))
                Text(
                    text = "${StudioRules.formatTime(entry.startsAt)} – ${StudioRules.formatTime(entry.endsAt)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = ForestPalette.PrimaryDeep,
                    fontWeight = FontWeight.SemiBold
                )
            }
            AppointmentStatusBadge(status = entry.status)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = entry.clientNameSnapshot,
            style = MaterialTheme.typography.titleMedium,
            color = ForestPalette.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.serviceNameSnapshot,
                style = MaterialTheme.typography.bodyMedium,
                color = ForestPalette.Body,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (entry.kind == AppointmentKind.APPOINTMENT && entry.quotedPriceKopecks > 0) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Прайс: ${StudioRules.formatRubles(entry.quotedPriceKopecks)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = ForestPalette.Muted
                )
            }
        }
    }
}
