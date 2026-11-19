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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.stasybell.os.domain.ServiceItem
import com.stasybell.os.domain.StudioRules
import com.stasybell.os.ui.components.OperationalCard
import com.stasybell.os.ui.components.StatusPill
import com.stasybell.os.ui.theme.ForestPalette
import com.stasybell.os.ui.viewmodel.AppUiState

/**
 * Screen 09: Service Catalog management (categories, variants, prices, duration, buffer, active).
 */
@Composable
fun ServicesScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onSaveService: (String?, String, String, String, Long, Int, Int, Boolean) -> Unit
) {
    var editingService by remember { mutableStateOf<ServiceItem?>(null) }
    var showCreate by rememberSaveable { mutableStateOf(false) }

    val grouped = remember(state.services) { state.services.groupBy { it.category } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ForestPalette.Canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("services_screen")
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
                    Text("Каталог услуг", style = MaterialTheme.typography.titleLarge, color = ForestPalette.Ink)
                }
                Button(
                    onClick = { showCreate = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestPalette.Primary),
                    modifier = Modifier.padding(end = 8.dp).testTag("services_add_button")
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Услуга")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Surface(shape = RoundedCornerShape(10.dp), color = ForestPalette.MintSoft, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Прайс предзаполнен шаблоном сайта (Демо) и полностью редактируется. Правка цены не меняет снимки уже созданных визитов.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ForestPalette.PrimaryDeep,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            grouped.forEach { (category, list) ->
                item {
                    Text(category, style = MaterialTheme.typography.titleMedium, color = ForestPalette.PrimaryDeep)
                }
                items(list, key = { it.id }) { srv ->
                    OperationalCard(onClick = { editingService = srv }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (srv.variant.isNotBlank()) "${srv.name} · ${srv.variant}" else srv.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ForestPalette.Ink
                                )
                                Text(
                                    text = "${srv.durationMinutes} мин + буфер ${srv.bufferMinutes} мин",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ForestPalette.Muted
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = StudioRules.formatRubles(srv.priceKopecks),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ForestPalette.Primary,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!srv.active) {
                                    StatusPill(label = "Скрыта", fg = ForestPalette.Muted, bg = ForestPalette.CanvasSoft)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate || editingService != null) {
        ServiceEditorDialog(
            initial = editingService,
            onDismiss = {
                showCreate = false
                editingService = null
            },
            onSave = { cat, name, variant, priceKopecks, dur, buf, active ->
                onSaveService(editingService?.id, cat, name, variant, priceKopecks, dur, buf, active)
                showCreate = false
                editingService = null
            }
        )
    }
}

@Composable
private fun ServiceEditorDialog(
    initial: ServiceItem?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Long, Int, Int, Boolean) -> Unit
) {
    var category by rememberSaveable { mutableStateOf(initial?.category ?: "Кератин / Ботокс") }
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var variant by rememberSaveable { mutableStateOf(initial?.variant ?: "") }
    var priceText by rememberSaveable { mutableStateOf(initial?.let { (it.priceKopecks / 100L).toString() } ?: "6500") }
    var durText by rememberSaveable { mutableStateOf(initial?.durationMinutes?.toString() ?: "150") }
    var bufText by rememberSaveable { mutableStateOf(initial?.bufferMinutes?.toString() ?: "15") }
    var active by rememberSaveable { mutableStateOf(initial?.active ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(18.dp),
        containerColor = ForestPalette.Surface,
        title = { Text(if (initial == null) "Новая услуга" else "Редактировать услугу") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Категория") }, singleLine = true)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название *") }, singleLine = true)
                OutlinedTextField(value = variant, onValueChange = { variant = it }, label = { Text("Вариант (длина / тип)") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Цена ₽") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = durText,
                        onValueChange = { durText = it.filter(Char::isDigit).take(3) },
                        label = { Text("Мин") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                    OutlinedTextField(
                        value = bufText,
                        onValueChange = { bufText = it.filter(Char::isDigit).take(2) },
                        label = { Text("Буфер") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.7f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Активна для записи", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val kopecks = StudioRules.parseRublesToKopecks(priceText) ?: return@TextButton
                val dur = durText.toIntOrNull() ?: return@TextButton
                val buf = bufText.toIntOrNull() ?: 0
                if (name.isNotBlank()) onSave(category, name, variant, kopecks, dur, buf, active)
            }) {
                Text("Сохранить", color = ForestPalette.Primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = ForestPalette.Muted) }
        }
    )
}
