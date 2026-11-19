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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stasybell.os.domain.Client
import com.stasybell.os.domain.DataMode
import com.stasybell.os.domain.PreferredChannel
import com.stasybell.os.domain.StudioRules
import com.stasybell.os.ui.components.AppointmentStatusBadge
import com.stasybell.os.ui.components.DemoModePill
import com.stasybell.os.ui.components.EmptyStateCard
import com.stasybell.os.ui.components.OperationalCard
import com.stasybell.os.ui.components.StatusPill
import com.stasybell.os.ui.theme.ForestPalette
import com.stasybell.os.ui.viewmodel.AppUiState

/**
 * Screen 07: Clients list with search, last visit indicator, quick new client, and direct booking.
 */
@Composable
fun ClientsListScreen(
    state: AppUiState,
    onOpenClient: (String) -> Unit,
    onSaveClient: (String?, String, String?, String, PreferredChannel) -> Unit,
    onBookClient: (String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }

    val filtered = remember(state.clients, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) state.clients
        else state.clients.filter {
            it.name.lowercase().contains(q) ||
                (it.phone?.lowercase()?.contains(q) == true) ||
                it.notes.lowercase().contains(q)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ForestPalette.Canvas)
            .statusBarsPadding()
            .testTag("clients_screen")
    ) {
        Surface(color = ForestPalette.Surface, border = BorderStroke(1.dp, ForestPalette.Border)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Клиенты", style = MaterialTheme.typography.headlineSmall, color = ForestPalette.Ink)
                        if (state.dataMode == DataMode.DEMO_ROOM) DemoModePill()
                    }
                    Button(
                        onClick = { showCreateDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestPalette.Primary),
                        modifier = Modifier.testTag("clients_add_button")
                    ) {
                        Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Клиент")
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Поиск по имени, телефону, заметке") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("clients_search_input")
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filtered.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "Клиенты не найдены",
                        description = "Добавьте нового клиента (телефон необязателен) или измените поисковый запрос."
                    )
                }
            } else {
                items(filtered, key = { it.id }) { client ->
                    val visits = state.calendarEntries.filter { it.clientId == client.id && it.deletedAt == null }
                    val lastVisit = visits.maxByOrNull { it.startsAt }
                    val archiveCount = state.legacyVisits.count { it.clientId == client.id }

                    OperationalCard(
                        onClick = { onOpenClient(client.id) },
                        modifier = Modifier.testTag("client_card_${client.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(client.name, style = MaterialTheme.typography.titleMedium, color = ForestPalette.Ink)
                                Text(
                                    text = client.phone ?: "Без телефона",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ForestPalette.Muted
                                )
                            }
                            OutlinedButton(
                                onClick = { onBookClient(client.id) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Запись")
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        val meta = buildString {
                            append("Визитов: ${visits.size}")
                            if (lastVisit != null) {
                                append(" · Последний: ${StudioRules.formatDate(StudioRules.toLocalDate(lastVisit.startsAt))}")
                            }
                            if (archiveCount > 0) {
                                append(" · Архив Excel: $archiveCount")
                            }
                        }
                        Text(meta, style = MaterialTheme.typography.bodySmall, color = ForestPalette.Body)
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        ClientEditorDialog(
            initial = null,
            onDismiss = { showCreateDialog = false },
            onSave = { name, phone, notes, channel ->
                onSaveClient(null, name, phone, notes, channel)
                showCreateDialog = false
            }
        )
    }
}

/**
 * Screen 08: Client Detail Card with editable contacts, active visit history, linked manual cash,
 * and strictly separated Legacy Excel Archive block.
 */
@Composable
fun ClientDetailScreen(
    clientId: String,
    state: AppUiState,
    onBack: () -> Unit,
    onSaveClient: (String?, String, String?, String, PreferredChannel) -> Unit,
    onBookClient: (String) -> Unit,
    onOpenEntry: (String) -> Unit
) {
    val client = state.clients.firstOrNull { it.id == clientId }
    var showEditDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ForestPalette.Canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("client_detail_screen")
    ) {
        Surface(color = ForestPalette.Surface, border = BorderStroke(1.dp, ForestPalette.Border)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад")
                    }
                    Text("Карточка клиента", style = MaterialTheme.typography.titleLarge, color = ForestPalette.Ink)
                }
                if (client != null) {
                    IconButton(onClick = { showEditDialog = true }, modifier = Modifier.testTag("client_edit_button")) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Редактировать клиента", tint = ForestPalette.Primary)
                    }
                }
            }
        }

        if (client == null) {
            EmptyStateCard(title = "Клиент не найден", description = "Карточка недоступна.", modifier = Modifier.padding(16.dp))
            return
        }

        val visits = state.calendarEntries
            .filter { it.clientId == client.id }
            .sortedByDescending { it.startsAt }
        val visitIds = visits.map { it.id }.toSet()
        val manualCashKopecks = state.financeTransactions
            .filter { it.entryId != null && it.entryId in visitIds }
            .sumOf { it.amountKopecks }
        val archiveVisits = state.legacyVisits
            .filter { it.clientId == client.id }
            .sortedByDescending { it.visitDate }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OperationalCard {
                    Text(client.name, style = MaterialTheme.typography.headlineSmall, color = ForestPalette.Ink)
                    Spacer(Modifier.height(4.dp))
                    Text(client.phone ?: "Телефон не указан", style = MaterialTheme.typography.bodyMedium, color = ForestPalette.Body)
                    Spacer(Modifier.height(4.dp))
                    Text("Предпочтительный канал: ${client.preferredChannel.labelRu}", style = MaterialTheme.typography.bodySmall, color = ForestPalette.Muted)
                    Text(
                        text = "Telegram ID: ${client.telegramChatId ?: "не привязан"} · VK ID: ${client.vkUserId ?: "не привязан"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ForestPalette.Muted
                    )
                    if (client.notes.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text("Заметки: ${client.notes}", style = MaterialTheme.typography.bodyMedium, color = ForestPalette.Ink)
                    }
                    Spacer(Modifier.height(10.dp))
                    Surface(shape = RoundedCornerShape(10.dp), color = ForestPalette.MintSoft, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Связанные ручные поступления в кассу: ${StudioRules.formatRubles(manualCashKopecks)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = ForestPalette.PrimaryDeep,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { onBookClient(client.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestPalette.Primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Новая запись для клиента")
                    }
                }
            }

            item {
                Text("История визитов (${visits.size})", style = MaterialTheme.typography.titleMedium, color = ForestPalette.Ink)
            }
            if (visits.isEmpty()) {
                item {
                    EmptyStateCard(title = "Актуальных визитов нет", description = "Создайте первую запись в новом календаре.")
                }
            } else {
                items(visits, key = { it.id }) { v ->
                    OperationalCard(onClick = { onOpenEntry(v.id) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(StudioRules.formatDateTime(v.startsAt), style = MaterialTheme.typography.titleSmall, color = ForestPalette.PrimaryDeep)
                            AppointmentStatusBadge(status = v.status)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(v.serviceNameSnapshot, style = MaterialTheme.typography.bodyMedium, color = ForestPalette.Ink)
                        Text(
                            text = "Снимок цены: ${StudioRules.formatRubles(v.quotedPriceKopecks)} · ${v.source.labelRu}" +
                                if (v.deletedAt != null) " · Удалена" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = ForestPalette.Muted
                        )
                    }
                }
            }

            // Strictly separated Legacy Excel Archive block
            item {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = ForestPalette.Warning)
                    Text("Архив из Excel (${archiveVisits.size})", style = MaterialTheme.typography.titleMedium, color = ForestPalette.Ink)
                }
                Text(
                    text = "Исторические даты без времени. Исходные суммы не являются подтверждённой кассой.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ForestPalette.Muted
                )
            }
            if (archiveVisits.isEmpty()) {
                item {
                    EmptyStateCard(title = "Архивных визитов нет", description = "Старые записи из Excel к этому клиенту не привязаны.")
                }
            } else {
                items(archiveVisits, key = { it.id }) { lv ->
                    OperationalCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(StudioRules.formatDate(lv.visitDate), style = MaterialTheme.typography.titleSmall, color = ForestPalette.Ink)
                            StatusPill(label = "Архив Excel", fg = ForestPalette.Warning, bg = ForestPalette.WarningBg)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(lv.serviceTitle, style = MaterialTheme.typography.bodyMedium, color = ForestPalette.Body)
                        Text(
                            text = "Исходная оценка: ${StudioRules.formatRubles(lv.amountOriginKopecks)} (не касса) · ${lv.sourceFile} [${lv.sourceSheet}:${lv.sourceRow}]",
                            style = MaterialTheme.typography.bodySmall,
                            color = ForestPalette.Muted
                        )
                    }
                }
            }
        }
    }

    if (showEditDialog && client != null) {
        ClientEditorDialog(
            initial = client,
            onDismiss = { showEditDialog = false },
            onSave = { name, phone, notes, channel ->
                onSaveClient(client.id, name, phone, notes, channel)
                showEditDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClientEditorDialog(
    initial: Client?,
    onDismiss: () -> Unit,
    onSave: (String, String?, String, PreferredChannel) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var phone by rememberSaveable { mutableStateOf(initial?.phone ?: "") }
    var notes by rememberSaveable { mutableStateOf(initial?.notes ?: "") }
    var channel by rememberSaveable { mutableStateOf(initial?.preferredChannel ?: PreferredChannel.NONE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(18.dp),
        containerColor = ForestPalette.Surface,
        title = { Text(if (initial == null) "Новый клиент" else "Редактировать клиента") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("client_dialog_name")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Телефон (необязательно)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("client_dialog_phone")
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Заметки (волосы, предпочтения)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Предпочтительный канал", style = MaterialTheme.typography.labelMedium, color = ForestPalette.Muted)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PreferredChannel.entries.chunked(2).forEach { rowCh ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowCh.forEach { ch ->
                                FilterChip(
                                    selected = channel == ch,
                                    onClick = { channel = ch },
                                    label = { Text(ch.labelRu, maxLines = 1) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onSave(name, phone.takeIf { it.isNotBlank() }, notes, channel) },
                modifier = Modifier.testTag("client_dialog_save")
            ) {
                Text("Сохранить", color = ForestPalette.Primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = ForestPalette.Muted) }
        }
    )
}
