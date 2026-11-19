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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PersonAdd
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.stasybell.os.domain.AppointmentSource
import com.stasybell.os.domain.Client
import com.stasybell.os.domain.PreferredChannel
import com.stasybell.os.domain.StudioRules
import com.stasybell.os.ui.components.OperationalCard
import com.stasybell.os.ui.theme.ForestPalette
import com.stasybell.os.ui.viewmodel.AppUiState
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Screen 05: Manual Appointment Creation (Master Nastya).
 * Phone is optional, service price/duration/buffer are captured as editable visit snapshots,
 * saved immediately as CONFIRMED with zero bot/SMS friction.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewAppointmentScreen(
    state: AppUiState,
    initialDate: LocalDate,
    initialClientId: String? = null,
    onBack: () -> Unit,
    onQuickCreateClient: (String, String?, (Client) -> Unit) -> Unit,
    onSubmit: (
        clientId: String,
        serviceId: String?,
        serviceName: String,
        source: AppointmentSource,
        startsAt: Instant,
        durationMinutes: Int,
        bufferMinutes: Int,
        quotedPriceKopecks: Long,
        note: String,
        idempotencyKey: String
    ) -> Unit
) {
    val activeServices = remember(state.services) { state.services.filter { it.active } }
    var selectedClientId by rememberSaveable { mutableStateOf(initialClientId ?: state.clients.firstOrNull()?.id ?: "") }
    var selectedServiceId by rememberSaveable { mutableStateOf(activeServices.firstOrNull()?.id ?: "") }
    var selectedDateIso by rememberSaveable { mutableStateOf(initialDate.toString()) }
    var selectedStartMinute by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedSource by rememberSaveable { mutableStateOf(AppointmentSource.PHONE) }

    val selectedService = activeServices.firstOrNull { it.id == selectedServiceId }
    var priceRublesText by rememberSaveable {
        mutableStateOf(((selectedService?.priceKopecks ?: 620_000L) / 100L).toString())
    }
    var durationText by rememberSaveable {
        mutableStateOf((selectedService?.durationMinutes ?: 150).toString())
    }
    var bufferText by rememberSaveable {
        mutableStateOf((selectedService?.bufferMinutes ?: 15).toString())
    }
    var note by rememberSaveable { mutableStateOf("") }
    var showNewClientDialog by rememberSaveable { mutableStateOf(false) }
    val submitKey = remember { UUID.randomUUID().toString() }

    val date = LocalDate.parse(selectedDateIso)
    val durationInt = durationText.toIntOrNull()?.coerceAtLeast(15) ?: 120
    val bufferInt = bufferText.toIntOrNull()?.coerceAtLeast(0) ?: 15

    val availableSlots = remember(date, durationInt, bufferInt, state.workingWindows, state.calendarEntries) {
        StudioRules.computeAvailableStartMinutes(
            date = date,
            durationMinutes = durationInt,
            bufferMinutes = bufferInt,
            workingWindows = state.workingWindows,
            existingEntries = state.calendarEntries
        )
    }

    LaunchedEffect(availableSlots) {
        if (selectedStartMinute == null || selectedStartMinute !in availableSlots) {
            selectedStartMinute = availableSlots.firstOrNull()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ForestPalette.Canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("new_appointment_screen")
    ) {
        Surface(color = ForestPalette.Surface, border = BorderStroke(1.dp, ForestPalette.Border)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("new_appointment_back")) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад")
                }
                Text("Новая запись (вручную)", style = MaterialTheme.typography.titleLarge, color = ForestPalette.Ink)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Client picker + quick create without phone
            item {
                OperationalCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("1. Клиент", style = MaterialTheme.typography.titleMedium, color = ForestPalette.Ink)
                        TextButton(
                            onClick = { showNewClientDialog = true },
                            modifier = Modifier.testTag("quick_new_client_button")
                        ) {
                            Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                            Text("Новый клиент")
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.clients.forEach { c ->
                            FilterChip(
                                selected = c.id == selectedClientId,
                                onClick = { selectedClientId = c.id },
                                label = { Text(c.name) },
                                modifier = Modifier.testTag("select_client_${c.id}")
                            )
                        }
                    }
                }
            }

            // 2. Service picker + editable snapshot fields
            item {
                OperationalCard {
                    Text("2. Услуга и условия визита", style = MaterialTheme.typography.titleMedium, color = ForestPalette.Ink)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        activeServices.forEach { s ->
                            val label = if (s.variant.isNotBlank()) "${s.name} · ${s.variant}" else s.name
                            FilterChip(
                                selected = s.id == selectedServiceId,
                                onClick = {
                                    selectedServiceId = s.id
                                    priceRublesText = (s.priceKopecks / 100L).toString()
                                    durationText = s.durationMinutes.toString()
                                    bufferText = s.bufferMinutes.toString()
                                },
                                label = { Text("$label (${StudioRules.formatRubles(s.priceKopecks)})") }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = priceRublesText,
                            onValueChange = { priceRublesText = it },
                            label = { Text("Цена (₽)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("booking_price_input")
                        )
                        OutlinedTextField(
                            value = durationText,
                            onValueChange = { durationText = it.filter(Char::isDigit).take(3) },
                            label = { Text("Мин.") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(0.75f)
                        )
                        OutlinedTextField(
                            value = bufferText,
                            onValueChange = { bufferText = it.filter(Char::isDigit).take(2) },
                            label = { Text("Буфер") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(0.75f)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Сохраняется снимок цены и длительности: будущие изменения прайса не перепишут этот визит.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ForestPalette.Muted
                    )
                }
            }

            // 3. Date & available 15-min slots
            item {
                OperationalCard {
                    Text("3. Дата и свободное время (шаг 15 мин)", style = MaterialTheme.typography.titleMedium, color = ForestPalette.Ink)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = { selectedDateIso = date.minusDays(1).toString() }) { Text("← День") }
                        Text(StudioRules.formatDate(date), style = MaterialTheme.typography.titleMedium, color = ForestPalette.PrimaryDeep)
                        OutlinedButton(onClick = { selectedDateIso = date.plusDays(1).toString() }) { Text("День →") }
                    }
                    Spacer(Modifier.height(10.dp))
                    if (availableSlots.isEmpty()) {
                        Text(
                            text = "Свободных окон на эту дату нет (выходной или время занято).",
                            style = MaterialTheme.typography.bodySmall,
                            color = ForestPalette.Error
                        )
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            availableSlots.take(24).forEach { min ->
                                FilterChip(
                                    selected = selectedStartMinute == min,
                                    onClick = { selectedStartMinute = min },
                                    label = { Text(StudioRules.formatMinuteOfDay(min)) },
                                    modifier = Modifier.testTag("slot_chip_$min")
                                )
                            }
                        }
                    }
                }
            }

            // 4. Source & note
            item {
                OperationalCard {
                    Text("4. Источник и примечание", style = MaterialTheme.typography.titleMedium, color = ForestPalette.Ink)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            AppointmentSource.PHONE,
                            AppointmentSource.TELEGRAM_MANUAL,
                            AppointmentSource.VK_MANUAL,
                            AppointmentSource.OTHER_MANUAL
                        ).forEach { src ->
                            FilterChip(
                                selected = selectedSource == src,
                                onClick = { selectedSource = src },
                                label = { Text(src.labelRu) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Заметка к визиту (необязательно)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Surface(color = ForestPalette.Surface, border = BorderStroke(1.dp, ForestPalette.Border)) {
            Button(
                onClick = {
                    val startMin = selectedStartMinute ?: return@Button
                    val kopecks = StudioRules.parseRublesToKopecks(priceRublesText) ?: 0L
                    val srvTitle = selectedService?.let {
                        if (it.variant.isNotBlank()) "${it.name} · ${it.variant}" else it.name
                    } ?: "Процедура"
                    onSubmit(
                        selectedClientId,
                        selectedService?.id,
                        srvTitle,
                        selectedSource,
                        StudioRules.toInstant(date, startMin),
                        durationInt,
                        bufferInt,
                        kopecks,
                        note,
                        submitKey
                    )
                },
                enabled = !state.isBusy && selectedClientId.isNotBlank() && selectedStartMinute != null,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForestPalette.Primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp)
                    .testTag("submit_manual_booking_button")
            ) {
                Text("Подтвердить и сохранить запись", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showNewClientDialog) {
        QuickClientDialog(
            onDismiss = { showNewClientDialog = false },
            onCreate = { name, phone ->
                onQuickCreateClient(name, phone) { created ->
                    selectedClientId = created.id
                    showNewClientDialog = false
                }
            }
        )
    }
}

@Composable
private fun QuickClientDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String?) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(18.dp),
        containerColor = ForestPalette.Surface,
        title = { Text("Новый клиент") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("quick_client_name_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Телефон (необязательно)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("quick_client_phone_input")
                )
                Text(
                    text = "Телефон не требуется для ручной записи.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ForestPalette.Muted
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name, phone.takeIf { it.isNotBlank() }) },
                modifier = Modifier.testTag("quick_client_save_button")
            ) {
                Text("Создать", color = ForestPalette.Primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = ForestPalette.Muted) }
        }
    )
}
