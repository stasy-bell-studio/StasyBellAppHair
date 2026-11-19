package com.stasybell.os.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stasybell.os.domain.AppointmentKind
import com.stasybell.os.domain.CalendarEntry
import com.stasybell.os.domain.DataMode
import com.stasybell.os.domain.StudioRules
import com.stasybell.os.domain.WorkingWindow
import com.stasybell.os.ui.components.AppointmentStatusBadge
import com.stasybell.os.ui.components.DemoModePill
import com.stasybell.os.ui.components.EmptyStateCard
import com.stasybell.os.ui.components.OperationalCard
import com.stasybell.os.ui.theme.ForestPalette
import com.stasybell.os.ui.viewmodel.AppUiState
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

private enum class CalendarZoom(val label: String) {
    DAY("День"),
    WEEK("Неделя"),
    MONTH("Месяц")
}

/**
 * Screens 03 & 04: Calendar (Day vertical timeline + Week + Month views).
 * Opaque light surface for maximum readability; supports creating appointments, calendar blocks, and opening schedule.
 */
@Composable
fun CalendarScreen(
    state: AppUiState,
    onSelectDate: (LocalDate) -> Unit,
    onNewAppointment: (LocalDate) -> Unit,
    onCreateBlock: (Instant, Int, String) -> Unit,
    onOpenEntryDetail: (String) -> Unit,
    onOpenSchedule: () -> Unit
) {
    var zoom by rememberSaveable { mutableStateOf(CalendarZoom.DAY) }
    var showBlockDialog by rememberSaveable { mutableStateOf(false) }

    val selectedDate = state.selectedDate
    val dayWindows = remember(state.workingWindows, selectedDate) {
        state.workingWindows.filter { it.date == selectedDate }.sortedBy { it.startMinuteOfDay }
    }
    val dayEntries = remember(state.calendarEntries, selectedDate) {
        state.calendarEntries
            .filter { it.deletedAt == null && StudioRules.toLocalDate(it.startsAt) == selectedDate }
            .sortedBy { it.startsAt }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ForestPalette.Canvas)
            .statusBarsPadding()
            .testTag("calendar_screen")
    ) {
        // Crisp light top bar (no opaque heavy green overlay on schedule)
        Surface(
            color = ForestPalette.Surface,
            border = BorderStroke(1.dp, ForestPalette.Border)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Календарь", style = MaterialTheme.typography.headlineSmall, color = ForestPalette.Ink)
                        if (state.dataMode == DataMode.DEMO_ROOM) DemoModePill()
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onOpenSchedule, modifier = Modifier.testTag("calendar_schedule_button")) {
                            Icon(Icons.Outlined.EditCalendar, contentDescription = "Рабочий график", tint = ForestPalette.Primary)
                        }
                        IconButton(onClick = { showBlockDialog = true }, modifier = Modifier.testTag("calendar_add_block_button")) {
                            Icon(Icons.Outlined.Block, contentDescription = "Заблокировать время", tint = ForestPalette.Info)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CalendarZoom.entries.forEach { mode ->
                            FilterChip(
                                selected = zoom == mode,
                                onClick = { zoom = mode },
                                label = { Text(mode.label) },
                                modifier = Modifier.testTag("calendar_zoom_${mode.name.lowercase()}")
                            )
                        }
                    }
                    Button(
                        onClick = { onNewAppointment(selectedDate) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestPalette.Primary),
                        modifier = Modifier.testTag("calendar_new_appointment_button")
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Запись")
                    }
                }

                Spacer(Modifier.height(6.dp))
                DateNavigatorBar(
                    selectedDate = selectedDate,
                    zoom = zoom,
                    onSelectDate = onSelectDate
                )
            }
        }

        when (zoom) {
            CalendarZoom.DAY -> DayTimelineContent(
                date = selectedDate,
                windows = dayWindows,
                entries = dayEntries,
                onOpenEntry = onOpenEntryDetail,
                onOpenSchedule = onOpenSchedule
            )
            CalendarZoom.WEEK -> WeekGridContent(
                selectedDate = selectedDate,
                allEntries = state.calendarEntries.filter { it.deletedAt == null },
                allWindows = state.workingWindows,
                onPickDay = {
                    onSelectDate(it)
                    zoom = CalendarZoom.DAY
                }
            )
            CalendarZoom.MONTH -> MonthGridContent(
                selectedDate = selectedDate,
                allEntries = state.calendarEntries.filter { it.deletedAt == null },
                allWindows = state.workingWindows,
                onPickDay = {
                    onSelectDate(it)
                    zoom = CalendarZoom.DAY
                }
            )
        }
    }

    if (showBlockDialog) {
        CreateBlockDialog(
            date = selectedDate,
            onDismiss = { showBlockDialog = false },
            onConfirm = { startMin, duration, note ->
                onCreateBlock(StudioRules.toInstant(selectedDate, startMin), duration, note)
                showBlockDialog = false
            }
        )
    }
}

@Composable
private fun DateNavigatorBar(
    selectedDate: LocalDate,
    zoom: CalendarZoom,
    onSelectDate: (LocalDate) -> Unit
) {
    val label = when (zoom) {
        CalendarZoom.DAY -> StudioRules.formatDate(selectedDate)
        CalendarZoom.WEEK -> {
            val start = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val end = start.plusDays(6)
            "${StudioRules.formatDate(start)} – ${StudioRules.formatDate(end)}"
        }
        CalendarZoom.MONTH -> StudioRules.formatMonthYear(YearMonth.from(selectedDate))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                val prev = when (zoom) {
                    CalendarZoom.DAY -> selectedDate.minusDays(1)
                    CalendarZoom.WEEK -> selectedDate.minusWeeks(1)
                    CalendarZoom.MONTH -> selectedDate.minusMonths(1)
                }
                onSelectDate(prev)
            },
            modifier = Modifier.testTag("calendar_prev_button")
        ) {
            Icon(Icons.Outlined.ChevronLeft, contentDescription = "Назад")
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = ForestPalette.Ink)
            TextButton(onClick = { onSelectDate(LocalDate.now(StudioRules.STUDIO_ZONE)) }) {
                Text("Сегодня", color = ForestPalette.Primary)
            }
        }

        IconButton(
            onClick = {
                val next = when (zoom) {
                    CalendarZoom.DAY -> selectedDate.plusDays(1)
                    CalendarZoom.WEEK -> selectedDate.plusWeeks(1)
                    CalendarZoom.MONTH -> selectedDate.plusMonths(1)
                }
                onSelectDate(next)
            },
            modifier = Modifier.testTag("calendar_next_button")
        ) {
            Icon(Icons.Outlined.ChevronRight, contentDescription = "Вперёд")
        }
    }
}

@Composable
private fun DayTimelineContent(
    date: LocalDate,
    windows: List<WorkingWindow>,
    entries: List<CalendarEntry>,
    onOpenEntry: (String) -> Unit,
    onOpenSchedule: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (windows.isEmpty()) ForestPalette.CanvasSoft else ForestPalette.MintSoft,
                border = BorderStroke(1.dp, ForestPalette.Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (windows.isEmpty()) "Выходной / день закрыт" else "Рабочие окна",
                            style = MaterialTheme.typography.labelLarge,
                            color = ForestPalette.PrimaryDeep
                        )
                        val text = if (windows.isEmpty()) {
                            "Слоты недоступны для записи"
                        } else {
                            windows.joinToString(" · ") {
                                "${StudioRules.formatMinuteOfDay(it.startMinuteOfDay)}–${StudioRules.formatMinuteOfDay(it.endMinuteOfDay)}"
                            }
                        }
                        Text(text, style = MaterialTheme.typography.bodySmall, color = ForestPalette.Body)
                    }
                    TextButton(onClick = onOpenSchedule) {
                        Text("Изменить", color = ForestPalette.Primary)
                    }
                }
            }
        }

        if (entries.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Записей на ${StudioRules.formatDate(date)} нет",
                    description = "Вы можете добавить ручную запись или блокировку времени.",
                    icon = Icons.Outlined.EventBusy
                )
            }
        } else {
            items(entries, key = { it.id }) { entry ->
                OperationalCard(
                    onClick = { onOpenEntry(entry.id) },
                    modifier = Modifier.testTag("calendar_entry_${entry.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${StudioRules.formatTime(entry.startsAt)} – ${StudioRules.formatTime(entry.endsAt)}" +
                                if (entry.bufferMinutes > 0) " (+${entry.bufferMinutes} мин буфер)" else "",
                            style = MaterialTheme.typography.titleSmall,
                            color = ForestPalette.PrimaryDeep,
                            fontWeight = FontWeight.SemiBold
                        )
                        AppointmentStatusBadge(status = entry.status)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(entry.clientNameSnapshot, style = MaterialTheme.typography.titleMedium, color = ForestPalette.Ink)
                    Text(entry.serviceNameSnapshot, style = MaterialTheme.typography.bodyMedium, color = ForestPalette.Body)
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Источник: ${entry.source.labelRu}", style = MaterialTheme.typography.bodySmall, color = ForestPalette.Muted)
                        if (entry.kind == AppointmentKind.APPOINTMENT) {
                            Text(
                                text = StudioRules.formatRubles(entry.quotedPriceKopecks),
                                style = MaterialTheme.typography.labelLarge,
                                color = ForestPalette.Ink
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekGridContent(
    selectedDate: LocalDate,
    allEntries: List<CalendarEntry>,
    allWindows: List<WorkingWindow>,
    onPickDay: (LocalDate) -> Unit
) {
    val monday = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val days = (0..6).map { monday.plusDays(it.toLong()) }
    val dayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(days.indices.toList()) { idx ->
            val d = days[idx]
            val entries = allEntries.filter { StudioRules.toLocalDate(it.startsAt) == d && it.status.blocksSlot }
            val windows = allWindows.filter { it.date == d }
            val util = StudioRules.computeUtilizationPercent(windows, entries)

            OperationalCard(onClick = { onPickDay(d) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${dayNames[idx]} · ${StudioRules.formatDate(d)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (d == selectedDate) ForestPalette.Primary else ForestPalette.Ink
                        )
                        Text(
                            text = if (windows.isEmpty()) "Выходной" else "Записей: ${entries.size} · Загрузка: ${util ?: 0}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = ForestPalette.Muted
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = if (entries.isNotEmpty()) ForestPalette.MintSoft else ForestPalette.CanvasSoft
                    ) {
                        Text(
                            text = "${entries.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = ForestPalette.PrimaryDeep,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthGridContent(
    selectedDate: LocalDate,
    allEntries: List<CalendarEntry>,
    allWindows: List<WorkingWindow>,
    onPickDay: (LocalDate) -> Unit
) {
    val ym = YearMonth.from(selectedDate)
    val firstDayOfMonth = ym.atDay(1)
    val leadingBlanks = firstDayOfMonth.dayOfWeek.value - 1
    val daysInMonth = ym.lengthOfMonth()
    val cells = List(leadingBlanks) { null } + (1..daysInMonth).map { ym.atDay(it) }
    val weeks = cells.chunked(7)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { d ->
                    Text(
                        text = d,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = ForestPalette.Muted
                    )
                }
            }
        }
        items(weeks) { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 0 until 7) {
                    val date = week.getOrNull(i)
                    if (date == null) {
                        Spacer(modifier = Modifier.weight(1f).height(56.dp))
                    } else {
                        val count = allEntries.count { StudioRules.toLocalDate(it.startsAt) == date && it.status.blocksSlot }
                        val hasWork = allWindows.any { it.date == date }
                        val isSelected = date == selectedDate
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onPickDay(date) },
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                isSelected -> ForestPalette.Primary
                                count > 0 -> ForestPalette.MintSoft
                                hasWork -> ForestPalette.Surface
                                else -> ForestPalette.CanvasSoft
                            },
                            border = BorderStroke(1.dp, if (isSelected) ForestPalette.PrimaryDeep else ForestPalette.Border)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "${date.dayOfMonth}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) Color.White else ForestPalette.Ink
                                )
                                if (count > 0) {
                                    Text(
                                        text = "$count зап.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) ForestPalette.Mint else ForestPalette.Primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Clip
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateBlockDialog(
    date: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, String) -> Unit
) {
    var startHour by rememberSaveable { mutableStateOf("13") }
    var startMin by rememberSaveable { mutableStateOf("00") }
    var duration by rememberSaveable { mutableStateOf("60") }
    var note by rememberSaveable { mutableStateOf("Личный перерыв") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(18.dp),
        containerColor = ForestPalette.Surface,
        title = { Text("Блокировка времени · ${StudioRules.formatDate(date)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startHour,
                        onValueChange = { startHour = it.filter(Char::isDigit).take(2) },
                        label = { Text("Час (0-23)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = startMin,
                        onValueChange = { startMin = it.filter(Char::isDigit).take(2) },
                        label = { Text("Мин (0-59)") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it.filter(Char::isDigit).take(3) },
                    label = { Text("Длительность (мин)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Причина") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val h = (startHour.toIntOrNull() ?: 13).coerceIn(0, 23)
                val m = (startMin.toIntOrNull() ?: 0).coerceIn(0, 59)
                val d = (duration.toIntOrNull() ?: 60).coerceAtLeast(15)
                onConfirm(h * 60 + m, d, note)
            }) {
                Text("Заблокировать", color = ForestPalette.Primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = ForestPalette.Muted) }
        }
    )
}
