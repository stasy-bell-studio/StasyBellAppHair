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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stasybell.os.domain.StudioRules
import com.stasybell.os.ui.components.OperationalCard
import com.stasybell.os.ui.theme.ForestPalette
import com.stasybell.os.ui.viewmodel.AppUiState
import java.time.LocalDate

/**
 * Screen 15: Working Schedule & Breaks editor.
 * Validates non-overlapping windows and prevents closing windows over existing active appointments.
 */
@Composable
fun ScheduleScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onSaveWindows: (LocalDate, List<Pair<Int, Int>>) -> Unit
) {
    var date by remember { mutableStateOf(state.selectedDate) }
    val currentWindows = remember(state.workingWindows, date) {
        state.workingWindows.filter { it.date == date }.sortedBy { it.startMinuteOfDay }
    }
    val activeAppointmentsCount = remember(state.calendarEntries, date) {
        state.calendarEntries.count {
            it.deletedAt == null && it.status.blocksSlot && StudioRules.toLocalDate(it.startsAt) == date
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ForestPalette.Canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("schedule_screen")
    ) {
        Surface(color = ForestPalette.Surface, border = BorderStroke(1.dp, ForestPalette.Border)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад")
                }
                Text("Рабочий график", style = MaterialTheme.typography.titleLarge, color = ForestPalette.Ink)
            }
        }

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
                        IconButton(onClick = { date = date.minusDays(1) }) {
                            Icon(Icons.Outlined.ChevronLeft, contentDescription = "Предыдущий день")
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(StudioRules.formatDate(date), style = MaterialTheme.typography.titleLarge, color = ForestPalette.Ink)
                            Text(
                                text = "Активных записей на дату: $activeAppointmentsCount",
                                style = MaterialTheme.typography.bodySmall,
                                color = ForestPalette.Muted
                            )
                        }
                        IconButton(onClick = { date = date.plusDays(1) }) {
                            Icon(Icons.Outlined.ChevronRight, contentDescription = "Следующий день")
                        }
                    }
                }
            }

            item {
                OperationalCard {
                    Text("Текущие рабочие окна", style = MaterialTheme.typography.titleMedium, color = ForestPalette.Ink)
                    Spacer(Modifier.height(6.dp))
                    if (currentWindows.isEmpty()) {
                        Text("День закрыт (выходной / отпуск)", style = MaterialTheme.typography.bodyMedium, color = ForestPalette.Warning)
                    } else {
                        currentWindows.forEach { w ->
                            Text(
                                text = "• ${StudioRules.formatMinuteOfDay(w.startMinuteOfDay)} – ${StudioRules.formatMinuteOfDay(w.endMinuteOfDay)}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = ForestPalette.PrimaryDeep,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Пробелы между окнами автоматически считаются перерывами. Закрыть окно поверх существующей активной записи нельзя.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ForestPalette.Muted
                    )
                }
            }

            item {
                OperationalCard {
                    Text("Быстрые шаблоны дня", style = MaterialTheme.typography.titleMedium, color = ForestPalette.Ink)
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onSaveWindows(date, listOf(10 * 60 to 14 * 60, 14 * 60 + 30 to 20 * 60)) },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestPalette.Primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("schedule_preset_standard")
                        ) {
                            Text("Стандарт: 10:00–14:00 и 14:30–20:00 (обед 30 мин)")
                        }
                        OutlinedButton(
                            onClick = { onSaveWindows(date, listOf(10 * 60 to 18 * 60)) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Короткий день: 10:00–18:00")
                        }
                        OutlinedButton(
                            onClick = { onSaveWindows(date, listOf(9 * 60 to 21 * 60)) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Расширенный день: 09:00–21:00")
                        }
                        OutlinedButton(
                            onClick = { onSaveWindows(date, emptyList()) },
                            border = BorderStroke(1.dp, ForestPalette.Error),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("schedule_preset_dayoff")
                        ) {
                            Text("Сделать выходным / закрыть день", color = ForestPalette.Error)
                        }
                    }
                }
            }
        }
    }
}
