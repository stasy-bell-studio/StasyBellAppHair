package com.stasybell.os.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stasybell.os.domain.LegacyVisit
import com.stasybell.os.domain.StudioRules
import com.stasybell.os.ui.components.EmptyStateCard
import com.stasybell.os.ui.components.OperationalCard
import com.stasybell.os.ui.components.StatusPill
import com.stasybell.os.ui.theme.ForestPalette
import com.stasybell.os.ui.viewmodel.AppUiState
import java.time.YearMonth

private enum class ReportTab(val label: String) {
    CASH("Касса (факт)"),
    PRODUCTION("Визиты"),
    ARCHIVE("Архив Excel")
}

/**
 * Screen 12: Monthly Reports strictly separating Actual Cash, Production Visits, and Legacy Excel Archive
 * (including the 12 orphan visits reconciliation queue).
 */
@Composable
fun ReportsScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onSelectMonth: (YearMonth) -> Unit,
    onLinkLegacyVisit: (String, String) -> Unit
) {
    var activeTab by rememberSaveable { mutableStateOf(ReportTab.CASH) }
    var linkingVisit by remember { mutableStateOf<LegacyVisit?>(null) }

    val ym = state.selectedMonth
    val cash = remember(state.financeTransactions, state.monthClosures, ym) { state.financeSummaryFor(ym) }
    val prod = remember(state.calendarEntries, state.workingWindows, ym) { state.productionReportFor(ym) }
    val orphanVisits = remember(state.legacyVisits) { state.legacyVisits.filter { it.isUnmatched || it.clientId == null } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ForestPalette.Canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("reports_screen")
    ) {
        Surface(color = ForestPalette.Surface, border = BorderStroke(1.dp, ForestPalette.Border)) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("reports_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад")
                    }
                    Text("Отчёты и архив", style = MaterialTheme.typography.titleLarge, color = ForestPalette.Ink)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportTab.entries.forEach { tab ->
                        FilterChip(
                            selected = activeTab == tab,
                            onClick = { activeTab = tab },
                            label = { Text(tab.label) },
                            modifier = Modifier.testTag("report_tab_${tab.name.lowercase()}")
                        )
                    }
                }
                if (activeTab != ReportTab.ARCHIVE) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onSelectMonth(ym.minusMonths(1)) }) {
                            Icon(Icons.Outlined.ChevronLeft, contentDescription = "Предыдущий месяц")
                        }
                        Text(StudioRules.formatMonthYear(ym), style = MaterialTheme.typography.titleMedium, color = ForestPalette.PrimaryDeep)
                        IconButton(onClick = { onSelectMonth(ym.plusMonths(1)) }) {
                            Icon(Icons.Outlined.ChevronRight, contentDescription = "Следующий месяц")
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (activeTab) {
                ReportTab.CASH -> {
                    item {
                        OperationalCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Фактическая касса за месяц", style = MaterialTheme.typography.titleMedium, color = ForestPalette.Ink)
                                StatusPill(
                                    label = if (cash.isClosed) "Закрыт" else "Открыт",
                                    fg = if (cash.isClosed) ForestPalette.Warning else ForestPalette.Success,
                                    bg = if (cash.isClosed) ForestPalette.WarningBg else ForestPalette.SuccessBg
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            ReportMetricRow("Фактический доход", StudioRules.formatRubles(cash.incomeKopecks), ForestPalette.Success)
                            ReportMetricRow("Фактический расход", StudioRules.formatRubles(cash.expenseKopecks), ForestPalette.Warning)
                            ReportMetricRow("Разница (доход − расход)", StudioRules.formatRubles(cash.balanceKopecks), ForestPalette.PrimaryDeep)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Архивные суммы Excel и стоимость запланированных визитов в этот отчёт не включаются.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ForestPalette.Muted
                            )
                        }
                    }
                    item {
                        OperationalCard {
                            Text("Расходы по категориям", style = MaterialTheme.typography.titleMedium, color = ForestPalette.Ink)
                            Spacer(Modifier.height(8.dp))
                            cash.expenseByCategory.forEach { (cat, sum) ->
                                ReportMetricRow(cat.labelRu, StudioRules.formatRubles(sum), ForestPalette.Body)
                            }
                        }
                    }
                }

                ReportTab.PRODUCTION -> {
                    item {
                        OperationalCard {
                            Text("Производственная аналитика (отдельно от денег)", style = MaterialTheme.typography.titleMedium, color = ForestPalette.Ink)
                            Spacer(Modifier.height(10.dp))
                            ReportMetricRow("Всего визитов в месяце", "${prod.totalVisits}", ForestPalette.Ink)
                            ReportMetricRow("Завершены", "${prod.completedCount}", ForestPalette.Success)
                            ReportMetricRow("Подтверждены", "${prod.confirmedCount}", ForestPalette.Primary)
                            ReportMetricRow("Отменены", "${prod.cancelledCount}", ForestPalette.Warning)
                            ReportMetricRow("Неявки", "${prod.noShowCount}", ForestPalette.Error)
                            ReportMetricRow("Ожидают / Истекли", "${prod.pendingCount} / ${prod.expiredCount}", ForestPalette.Muted)
                            ReportMetricRow("Отработано минут (завершённые)", "${prod.workedMinutes} мин", ForestPalette.PrimaryDeep)
                            ReportMetricRow("Загрузка месяца", prod.utilizationPercent?.let { "$it%" } ?: "—", ForestPalette.PrimaryDeep)
                        }
                    }
                    item {
                        OperationalCard {
                            Text("Выполненные услуги", style = MaterialTheme.typography.titleMedium, color = ForestPalette.Ink)
                            Spacer(Modifier.height(8.dp))
                            if (prod.completedByService.isEmpty()) {
                                Text("Завершённых услуг за выбранный месяц нет.", style = MaterialTheme.typography.bodySmall, color = ForestPalette.Muted)
                            } else {
                                prod.completedByService.forEach { (name, count) ->
                                    ReportMetricRow(name, "$count шт.", ForestPalette.Ink)
                                }
                            }
                        }
                    }
                }

                ReportTab.ARCHIVE -> {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ForestPalette.WarningBg,
                            border = BorderStroke(1.dp, ForestPalette.Warning.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Архив Excel (неподтверждённые фактические деньги). Показатели ниже хранятся изолированно и никогда не суммируются с текущей кассой.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ForestPalette.Warning,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    item {
                        Text("Архивные сводки по месяцам (${state.legacyMonthlySummaries.size})", style = MaterialTheme.typography.titleMedium, color = ForestPalette.Ink)
                    }
                    items(state.legacyMonthlySummaries, key = { it.id }) { m ->
                        OperationalCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(m.yearMonthLabel, style = MaterialTheme.typography.titleSmall, color = ForestPalette.Ink)
                                StatusPill(label = "Архив", fg = ForestPalette.Warning, bg = ForestPalette.WarningBg)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Колонка paid (исходник): ${StudioRules.formatRubles(m.sourcePaidKopecks)} · Визитов: ${m.visitsCount} · Клиентов: ${m.uniqueClients}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ForestPalette.Body
                            )
                            Text(m.note, style = MaterialTheme.typography.bodySmall, color = ForestPalette.Muted)
                        }
                    }

                    item {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Очередь сверки непривязанных визитов (${orphanVisits.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = ForestPalette.Ink,
                            modifier = Modifier.testTag("archive_orphan_header")
                        )
                    }

                    if (orphanVisits.isEmpty()) {
                        item {
                            EmptyStateCard(title = "Очередь сверки пуста", description = "Все архивные визиты привязаны к карточкам клиентов.")
                        }
                    } else {
                        items(orphanVisits, key = { it.id }) { ov ->
                            OperationalCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${StudioRules.formatDate(ov.visitDate)} · ${ov.clientRawName}", style = MaterialTheme.typography.titleSmall, color = ForestPalette.Ink)
                                        Text(ov.serviceTitle, style = MaterialTheme.typography.bodySmall, color = ForestPalette.Body)
                                        Text(
                                            text = "Оценка исходника: ${StudioRules.formatRubles(ov.amountOriginKopecks)} · ${ov.sourceSheet}:${ov.sourceRow}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ForestPalette.Muted
                                        )
                                    }
                                    OutlinedButton(onClick = { linkingVisit = ov }) {
                                        Text("Привязать")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    linkingVisit?.let { lv ->
        AlertDialog(
            onDismissRequest = { linkingVisit = null },
            shape = RoundedCornerShape(18.dp),
            containerColor = ForestPalette.Surface,
            title = { Text("Привязать архивный визит") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${StudioRules.formatDate(lv.visitDate)} · ${lv.serviceTitle}", style = MaterialTheme.typography.bodySmall, color = ForestPalette.Muted)
                    state.clients.forEach { cl ->
                        OutlinedButton(
                            onClick = {
                                onLinkLegacyVisit(lv.id, cl.id)
                                linkingVisit = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(cl.name)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { linkingVisit = null }) { Text("Отмена", color = ForestPalette.Muted) }
            }
        )
    }
}

@Composable
private fun ReportMetricRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = ForestPalette.Body)
        Text(value, style = MaterialTheme.typography.titleSmall, color = valueColor, fontWeight = FontWeight.SemiBold)
    }
}
