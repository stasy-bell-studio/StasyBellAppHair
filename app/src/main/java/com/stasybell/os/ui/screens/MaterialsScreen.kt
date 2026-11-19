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
import androidx.compose.material.icons.outlined.Remove
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.stasybell.os.domain.MaterialItem
import com.stasybell.os.domain.MaterialUnit
import com.stasybell.os.domain.StudioRules
import com.stasybell.os.ui.components.OperationalCard
import com.stasybell.os.ui.components.StatusPill
import com.stasybell.os.ui.theme.ForestPalette
import com.stasybell.os.ui.viewmodel.AppUiState

/**
 * Screens 13 & 14: Materials Inventory (precision 0.001 g/ml/pcs, low-stock alerts)
 * + Stock Movements (Receipt / Deduction with reason, non-negative guard, independent from cash ledger).
 */
@Composable
fun MaterialsScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onSaveMaterial: (String?, String, MaterialUnit, Long) -> Unit,
    onRecordMovement: (String, Long, String) -> Unit
) {
    var showCreateMaterial by rememberSaveable { mutableStateOf(false) }
    var movementTarget by remember { mutableStateOf<Pair<MaterialItem, Boolean>?>(null) } // item to isReceipt

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ForestPalette.Canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("materials_screen")
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
                    Text("Материалы и склад", style = MaterialTheme.typography.titleLarge, color = ForestPalette.Ink)
                }
                Button(
                    onClick = { showCreateMaterial = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestPalette.Primary),
                    modifier = Modifier.padding(end = 8.dp).testTag("materials_add_button")
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Позиция")
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
                        text = "Точность учёта: 0,001 (г/мл/шт). Остаток не может уйти в минус. Приход и списание не дублируют денежные расходы в кассе.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ForestPalette.PrimaryDeep,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            items(state.materials, key = { it.id }) { mat ->
                OperationalCard(modifier = Modifier.testTag("material_card_${mat.id}")) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mat.name, style = MaterialTheme.typography.titleMedium, color = ForestPalette.Ink)
                            Text(
                                text = "Порог предупреждения: ${StudioRules.formatQuantity(mat.lowStockThresholdMilliunits, mat.unit)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ForestPalette.Muted
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = StudioRules.formatQuantity(mat.stockMilliunits, mat.unit),
                                style = MaterialTheme.typography.titleLarge,
                                color = if (mat.isLowStock) ForestPalette.Error else ForestPalette.PrimaryDeep,
                                fontWeight = FontWeight.Bold
                            )
                            if (mat.isLowStock) {
                                StatusPill(label = "Мало", fg = ForestPalette.Error, bg = ForestPalette.ErrorBg)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { movementTarget = mat to true },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestPalette.Primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("material_in_${mat.id}")
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Приход")
                        }
                        OutlinedButton(
                            onClick = { movementTarget = mat to false },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("material_out_${mat.id}")
                        ) {
                            Icon(Icons.Outlined.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Списание")
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(6.dp))
                Text("Журнал движений (${state.materialMovements.size})", style = MaterialTheme.typography.titleMedium, color = ForestPalette.Ink)
            }

            items(state.materialMovements.take(25), key = { it.id }) { mov ->
                val mat = state.materials.firstOrNull { it.id == mov.materialId }
                val unit = mat?.unit ?: MaterialUnit.GRAM
                val isIn = mov.deltaMilliunits > 0
                OperationalCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mat?.name ?: "Материал", style = MaterialTheme.typography.titleSmall, color = ForestPalette.Ink)
                            Text(mov.reason, style = MaterialTheme.typography.bodySmall, color = ForestPalette.Body)
                            Text(StudioRules.formatDateTime(mov.occurredAt), style = MaterialTheme.typography.bodySmall, color = ForestPalette.Muted)
                        }
                        Text(
                            text = (if (isIn) "+" else "") + StudioRules.formatQuantity(mov.deltaMilliunits, unit),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isIn) ForestPalette.Success else ForestPalette.Warning,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showCreateMaterial) {
        var name by rememberSaveable { mutableStateOf("") }
        var unit by rememberSaveable { mutableStateOf(MaterialUnit.GRAM) }
        var threshold by rememberSaveable { mutableStateOf("50") }

        AlertDialog(
            onDismissRequest = { showCreateMaterial = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = ForestPalette.Surface,
            title = { Text("Новый материал") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название *") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MaterialUnit.entries.forEach { u ->
                            FilterChip(selected = unit == u, onClick = { unit = u }, label = { Text(u.labelRu) })
                        }
                    }
                    OutlinedTextField(
                        value = threshold,
                        onValueChange = { threshold = it },
                        label = { Text("Критический остаток (${unit.labelRu})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val thr = StudioRules.parseQuantityToMilliunits(threshold) ?: 0L
                    if (name.isNotBlank()) {
                        onSaveMaterial(null, name, unit, thr)
                        showCreateMaterial = false
                    }
                }) {
                    Text("Добавить", color = ForestPalette.Primary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateMaterial = false }) { Text("Отмена", color = ForestPalette.Muted) }
            }
        )
    }

    movementTarget?.let { (mat, isReceipt) ->
        var qtyText by rememberSaveable { mutableStateOf("") }
        var reason by rememberSaveable { mutableStateOf(if (isReceipt) "Приход партии" else "Списание на процедуру") }

        AlertDialog(
            onDismissRequest = { movementTarget = null },
            shape = RoundedCornerShape(18.dp),
            containerColor = ForestPalette.Surface,
            title = { Text(if (isReceipt) "Приход: ${mat.name}" else "Списание: ${mat.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Текущий остаток: ${StudioRules.formatQuantity(mat.stockMilliunits, mat.unit)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ForestPalette.Muted
                    )
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it },
                        label = { Text("Количество (${mat.unit.labelRu}, шаг до 0,001) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("movement_qty_input")
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Причина *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("movement_reason_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = StudioRules.parseQuantityToMilliunits(qtyText) != null && reason.isNotBlank(),
                    onClick = {
                        val milli = StudioRules.parseQuantityToMilliunits(qtyText) ?: return@TextButton
                        val delta = if (isReceipt) milli else -milli
                        onRecordMovement(mat.id, delta, reason)
                        movementTarget = null
                    },
                    modifier = Modifier.testTag("movement_confirm_button")
                ) {
                    Text("Провести", color = ForestPalette.Primary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { movementTarget = null }) { Text("Отмена", color = ForestPalette.Muted) }
            }
        )
    }
}
