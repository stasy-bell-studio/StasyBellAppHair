package com.stasybell.os.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stasybell.os.domain.AppointmentKind
import com.stasybell.os.domain.AppointmentStatus
import com.stasybell.os.domain.CalendarEvent
import com.stasybell.os.domain.StudioRules
import com.stasybell.os.ui.components.AppointmentStatusBadge
import com.stasybell.os.ui.components.ConfirmActionDialog
import com.stasybell.os.ui.components.EmptyStateCard
import com.stasybell.os.ui.components.OperationalCard
import com.stasybell.os.ui.theme.ForestPalette
import com.stasybell.os.ui.viewmodel.AppUiState
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Screen 06: Appointment Detail Card.
 * Actions: Reschedule (with conflict validation), Complete, Cancel, No-Show, Protected Soft Delete,
 * Restore (conflict-guarded), Manual Payment shortcut (NEVER automatic), and Audit Events log.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppointmentDetailScreen(
    entryId: String,
    state: AppUiState,
    eventsFlow: Flow<List<CalendarEvent>>,
    onBack: () -> Unit,
    onUpdateStatus: (AppointmentStatus) -> Unit,
    onMove: (Instant) -> Unit,
    onSoftDelete: () -> Unit,
    onRestore: () -> Unit,
    onRecordManualPayment: (entryId: String, suggestedKopecks: Long, note: String) -> Unit
) {
    val entry = state.calendarEntries.firstOrNull { it.id == entryId }
    val events by eventsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showMoveDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ForestPalette.Canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("appointment_detail_screen")
    ) {
        Surface(color = ForestPalette.Surface, border = BorderStroke(1.dp, ForestPalette.Border)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад")
                }
                Text("Карточка записи", style = MaterialTheme.typography.titleLarge, color = ForestPalette.Ink)
            }
        }

        if (entry == null) {
            EmptyStateCard(
                title = "Запись не найдена",
                description = "Возможно, она была удалена или относится к другому режиму.",
                modifier = Modifier.padding(16.dp)
            )
            return
        }

        val linkedPayments = state.financeTransactions.filter { it.entryId == entry.id }
        val linkedSumKopecks = linkedPayments.sumOf { it.amountKopecks }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OperationalCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppointmentStatusBadge(status = entry.status)
                        if (entry.deletedAt != null) {
                            Text("Удалена", style = MaterialTheme.typography.labelLarge, color = ForestPalette.Error)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(entry.clientNameSnapshot, style = MaterialTheme.typography.headlineSmall, color = ForestPalette.Ink)
                    Spacer(Modifier.height(4.dp))
                    Text(entry.serviceNameSnapshot, style = MaterialTheme.typography.titleMedium, color = ForestPalette.Body)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${StudioRules.formatDate(StudioRules.toLocalDate(entry.startsAt))} · " +
                            "${StudioRules.formatTime(entry.startsAt)} – ${StudioRules.formatTime(entry.endsAt)} " +
                            "(${entry.durationMinutes} мин + буфер ${entry.bufferMinutes} мин)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ForestPalette.PrimaryDeep
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Источник: ${entry.source.labelRu}", style = MaterialTheme.typography.bodySmall, color = ForestPalette.Muted)
                    if (entry.kind == AppointmentKind.APPOINTMENT) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Снимок цены визита: ${StudioRules.formatRubles(entry.quotedPriceKopecks)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = ForestPalette.Ink
                        )
                        Text(
                            text = "Фактически внесено в кассу по визиту: ${StudioRules.formatRubles(linkedSumKopecks)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (linkedSumKopecks > 0) ForestPalette.Success else ForestPalette.Muted
                        )
                    }
                    if (entry.note.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text("Заметка: ${entry.note}", style = MaterialTheme.typography.bodySmall, color = ForestPalette.Body)
                    }
                }
            }

            if (entry.deletedAt == null) {
                item {
                    OperationalCard {
                        Text("Действия с визитом", style = MaterialTheme.typography.titleMedium, color = ForestPalette.Ink)
                        Spacer(Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (entry.kind == AppointmentKind.APPOINTMENT && entry.status != AppointmentStatus.COMPLETED) {
                                Button(
                                    onClick = { onUpdateStatus(AppointmentStatus.COMPLETED) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ForestPalette.Primary),
                                    modifier = Modifier.testTag("entry_complete_button")
                                ) {
                                    Text("Завершить визит")
                                }
                            }
                            OutlinedButton(
                                onClick = { showMoveDialog = true },
                                modifier = Modifier.testTag("entry_move_button")
                            ) {
                                Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                                Text("Перенести")
                            }
                            if (entry.status != AppointmentStatus.CANCELLED) {
                                OutlinedButton(
                                    onClick = { onUpdateStatus(AppointmentStatus.CANCELLED) },
                                    modifier = Modifier.testTag("entry_cancel_button")
                                ) {
                                    Text("Отменить", color = ForestPalette.Warning)
                                }
                            }
                            if (entry.kind == AppointmentKind.APPOINTMENT && entry.status != AppointmentStatus.NO_SHOW) {
                                OutlinedButton(
                                    onClick = { onUpdateStatus(AppointmentStatus.NO_SHOW) },
                                    modifier = Modifier.testTag("entry_noshow_button")
                                ) {
                                    Text("Неявка", color = ForestPalette.Error)
                                }
                            }
                        }

                        if (entry.kind == AppointmentKind.APPOINTMENT && entry.status == AppointmentStatus.COMPLETED) {
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ForestPalette.MintSoft,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Завершение визита не создаёт доход автоматически. Если деньги получены, внесите их вручную:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ForestPalette.PrimaryDeep
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            onRecordManualPayment(
                                                entry.id,
                                                entry.quotedPriceKopecks,
                                                "Оплата: ${entry.clientNameSnapshot} (${entry.serviceNameSnapshot})"
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ForestPalette.Primary),
                                        modifier = Modifier.testTag("entry_manual_payment_button")
                                    ) {
                                        Icon(Icons.Outlined.Payments, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                                        Text("Внести оплату вручную")
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        TextButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.testTag("entry_delete_button")
                        ) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = ForestPalette.Error)
                            Text("Удалить запись (с журналом)", color = ForestPalette.Error)
                        }
                    }
                }
            } else {
                item {
                    OperationalCard {
                        Text("Запись удалена", style = MaterialTheme.typography.titleMedium, color = ForestPalette.Error)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Восстановление возможно, если интервал не занят другой записью.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ForestPalette.Muted
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = onRestore,
                            colors = ButtonDefaults.buttonColors(containerColor = ForestPalette.Primary),
                            modifier = Modifier.testTag("entry_restore_button")
                        ) {
                            Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                            Text("Восстановить в календаре")
                        }
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.History, contentDescription = null, tint = ForestPalette.Muted)
                    Text("История действий", style = MaterialTheme.typography.titleMedium, color = ForestPalette.Ink)
                }
            }

            items(events, key = { it.id }) { ev ->
                OperationalCard {
                    Text(ev.summary, style = MaterialTheme.typography.bodyMedium, color = ForestPalette.Ink)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${ev.actor} · ${StudioRules.formatDateTime(ev.occurredAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ForestPalette.Muted
                    )
                }
            }
        }
    }

    if (showDeleteConfirm && entry != null) {
        val extraWarning = if (entry.status == AppointmentStatus.COMPLETED) {
            "Внимание: это завершённый визит! "
        } else {
            ""
        }
        ConfirmActionDialog(
            title = "Удалить запись?",
            message = "${extraWarning}Запись будет скрыта из календаря (мягкое удаление), а история действий сохранится.",
            confirmLabel = "Удалить",
            isDestructive = true,
            onConfirm = {
                showDeleteConfirm = false
                onSoftDelete()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    if (showMoveDialog && entry != null) {
        val baseDate = StudioRules.toLocalDate(entry.startsAt)
        var targetDate by remember { mutableStateOf(baseDate) }
        val slots = remember(targetDate, state.workingWindows, state.calendarEntries) {
            StudioRules.computeAvailableStartMinutes(
                date = targetDate,
                durationMinutes = entry.durationMinutes,
                bufferMinutes = entry.bufferMinutes,
                workingWindows = state.workingWindows,
                existingEntries = state.calendarEntries,
                excludeEntryId = entry.id
            )
        }
        var chosenSlot by remember(slots) { mutableStateOf(slots.firstOrNull()) }

        AlertDialog(
            onDismissRequest = { showMoveDialog = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = ForestPalette.Surface,
            title = { Text("Перенести запись") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { targetDate = targetDate.minusDays(1) }) { Text("←") }
                        Text(StudioRules.formatDate(targetDate), fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = { targetDate = targetDate.plusDays(1) }) { Text("→") }
                    }
                    if (slots.isEmpty()) {
                        Text("Свободных окон нет", color = ForestPalette.Error, style = MaterialTheme.typography.bodySmall)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            slots.take(12).chunked(3).forEach { rowSlots ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    rowSlots.forEach { s ->
                                        FilterChip(
                                            selected = chosenSlot == s,
                                            onClick = { chosenSlot = s },
                                            label = { Text(StudioRules.formatMinuteOfDay(s)) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = chosenSlot != null,
                    onClick = {
                        val m = chosenSlot ?: return@TextButton
                        showMoveDialog = false
                        onMove(StudioRules.toInstant(targetDate, m))
                    }
                ) {
                    Text("Перенести", color = ForestPalette.Primary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMoveDialog = false }) { Text("Отмена", color = ForestPalette.Muted) }
            }
        )
    }
}
