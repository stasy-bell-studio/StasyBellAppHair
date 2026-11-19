package com.stasybell.os.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.automirrored.outlined.Undo
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.stasybell.os.domain.FinanceCategory
import com.stasybell.os.domain.FinanceKind
import com.stasybell.os.domain.StudioRules
import com.stasybell.os.ui.components.ConfirmActionDialog
import com.stasybell.os.ui.components.EmptyStateCard
import com.stasybell.os.ui.components.ForestHeroHeader
import com.stasybell.os.ui.components.GlassKpiTile
import com.stasybell.os.ui.components.OperationalCard
import com.stasybell.os.ui.components.StatusPill
import com.stasybell.os.ui.components.WeeklyCashBarChart
import com.stasybell.os.ui.theme.ForestPalette
import com.stasybell.os.ui.viewmodel.AppUiState
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/**
 * Screens 10 & 11: Actual Cash Ledger Overview + Manual Transactions, Reversals (Сторно),
 * and Versioned Month Closing / Reopening.
 */
@Composable
fun FinanceScreen(
    state: AppUiState,
    prefillRequest: QuickTransactionPrefill? = null,
    onConsumePrefill: () -> Unit = {},
    onSelectMonth: (YearMonth) -> Unit,
    onAddTransaction: (FinanceKind, FinanceCategory, Long, Instant, String, String?, String) -> Unit,
    onReverseTransaction: (String) -> Unit,
    onCloseMonth: (YearMonth) -> Unit,
    onReopenMonth: (YearMonth, String) -> Unit
) {
    val ym = state.selectedMonth
    val summary = remember(state.financeTransactions, state.monthClosures, ym) {
        state.financeSummaryFor(ym)
    }
    val monthTxs = remember(state.financeTransactions, ym) {
        state.financeTransactions
            .filter { StudioRules.toYearMonth(it.occurredAt) == ym }
            .sortedByDescending { it.occurredAt }
    }
    val reversedIds = remember(monthTxs) {
        monthTxs.mapNotNull { it.correctionOfId }.toSet()
    }

    var dialogKind by remember { mutableStateOf<FinanceKind?>(null) }
    var prefillAmountKopecks by remember { mutableStateOf<Long?>(null) }
    var prefillNote by remember { mutableStateOf("") }
    var prefillEntryId by remember { mutableStateOf<String?>(null) }
    var filterKind by rememberSaveable { mutableStateOf<FinanceKind?>(null) }
    var showCloseConfirm by rememberSaveable { mutableStateOf(false) }
    var showReopenDialog by rememberSaveable { mutableStateOf(false) }
    var reverseTargetId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(prefillRequest) {
        if (prefillRequest != null && dialogKind == null) {
            dialogKind = prefillRequest.kind
            prefillAmountKopecks = prefillRequest.suggestedKopecks
            prefillNote = prefillRequest.note
            prefillEntryId = prefillRequest.entryId
            onConsumePrefill()
        }
    }

    val filteredTxs = if (filterKind == null) monthTxs else monthTxs.filter { it.kind == filterKind }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(ForestPalette.Canvas)
                .testTag("finance_screen"),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        item {
            ForestHeroHeader(
                title = "Фактическая касса",
                subtitle = "Только вручную подтверждённые поступления и траты",
                dataMode = state.dataMode,
                trailing = {
                    StatusPill(
                        label = if (summary.isClosed) "Закрыт v${summary.latestClosure?.version ?: 1}" else "Открыт",
                        fg = if (summary.isClosed) ForestPalette.Warning else ForestPalette.Mint,
                        bg = Color.White.copy(alpha = 0.14f)
                    )
                }
            ) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onSelectMonth(ym.minusMonths(1)) }) {
                        Icon(Icons.Outlined.ChevronLeft, contentDescription = "Предыдущий месяц", tint = Color.White)
                    }
                    Text(
                        text = StudioRules.formatMonthYear(ym),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = { onSelectMonth(ym.plusMonths(1)) }) {
                        Icon(Icons.Outlined.ChevronRight, contentDescription = "Следующий месяц", tint = Color.White)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassKpiTile(
                        label = "Доход (факт)",
                        value = StudioRules.formatRubles(summary.incomeKopecks),
                        modifier = Modifier.weight(1f),
                        testTag = "finance_kpi_income"
                    )
                    GlassKpiTile(
                        label = "Расход (факт)",
                        value = StudioRules.formatRubles(summary.expenseKopecks),
                        modifier = Modifier.weight(1f),
                        testTag = "finance_kpi_expense"
                    )
                }
                Spacer(Modifier.height(8.dp))
                GlassKpiTile(
                    label = "Остаток / разница (доход − расход)",
                    value = StudioRules.formatRubles(summary.balanceKopecks),
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "finance_kpi_balance"
                )
            }
        }

        // Action buttons or Closed Month Banner
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (summary.isClosed) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ForestPalette.WarningBg,
                        border = BorderStroke(1.dp, ForestPalette.Warning.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth().testTag("finance_month_closed_banner")
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.Lock, contentDescription = null, tint = ForestPalette.Warning, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Месяц закрыт (версия ${summary.latestClosure?.version ?: 1}) — новые операции запрещены",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ForestPalette.Warning
                                )
                            }
                            OutlinedButton(
                                onClick = { showReopenDialog = true },
                                modifier = Modifier.testTag("finance_reopen_month_button")
                            ) {
                                Icon(Icons.Outlined.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Переоткрыть с причиной")
                            }
                        }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                prefillAmountKopecks = null
                                prefillNote = ""
                                prefillEntryId = null
                                dialogKind = FinanceKind.INCOME
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ForestPalette.Primary),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("finance_add_income_button")
                        ) {
                            Icon(Icons.Outlined.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Доход")
                        }
                        OutlinedButton(
                            onClick = {
                                prefillAmountKopecks = null
                                prefillNote = ""
                                prefillEntryId = null
                                dialogKind = FinanceKind.EXPENSE
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ForestPalette.Warning),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("finance_add_expense_button")
                        ) {
                            Icon(Icons.Outlined.ArrowUpward, contentDescription = null, tint = ForestPalette.Warning, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Расход", color = ForestPalette.Warning)
                        }
                    }
                    OutlinedButton(
                        onClick = { showCloseConfirm = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp).testTag("finance_close_month_button")
                    ) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Закрыть месяц ${StudioRules.formatMonthYear(ym)}")
                    }
                }
            }
        }

        // Weekly chart
        item {
            OperationalCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Динамика кассы по неделям", style = MaterialTheme.typography.titleMedium, color = ForestPalette.Ink)
                Spacer(Modifier.height(10.dp))
                WeeklyCashBarChart(
                    weeklyIncome = summary.weeklyIncomeKopecks,
                    weeklyExpense = summary.weeklyExpenseKopecks
                )
            }
        }

        // Filter row
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Операции (${filteredTxs.size})", style = MaterialTheme.typography.titleMedium, color = ForestPalette.Ink)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = filterKind == null, onClick = { filterKind = null }, label = { Text("Все") })
                    FilterChip(selected = filterKind == FinanceKind.INCOME, onClick = { filterKind = FinanceKind.INCOME }, label = { Text("Доход") })
                    FilterChip(selected = filterKind == FinanceKind.EXPENSE, onClick = { filterKind = FinanceKind.EXPENSE }, label = { Text("Расход") })
                }
            }
        }

        if (filteredTxs.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Операций за этот период нет",
                    description = "Внесите фактический доход или расход вручную. Записи из календаря сюда автоматически не попадают.",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            items(filteredTxs, key = { it.id }) { tx ->
                val isReversal = tx.correctionOfId != null
                val alreadyReversed = tx.id in reversedIds
                val amountColor = when {
                    isReversal -> ForestPalette.Muted
                    tx.kind == FinanceKind.INCOME -> ForestPalette.Success
                    else -> ForestPalette.Warning
                }
                val prefix = if (tx.kind == FinanceKind.INCOME && !isReversal) "+" else if (tx.kind == FinanceKind.EXPENSE && !isReversal) "−" else ""

                OperationalCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(tx.category.labelRu, style = MaterialTheme.typography.titleSmall, color = ForestPalette.Ink)
                                if (isReversal) {
                                    StatusPill(label = "Сторно", fg = ForestPalette.Info, bg = ForestPalette.InfoBg)
                                } else if (alreadyReversed) {
                                    StatusPill(label = "Сторнировано", fg = ForestPalette.Muted, bg = ForestPalette.CanvasSoft)
                                }
                            }
                            if (tx.note.isNotBlank()) {
                                Text(tx.note, style = MaterialTheme.typography.bodySmall, color = ForestPalette.Body)
                            }
                            Text(StudioRules.formatDateTime(tx.occurredAt), style = MaterialTheme.typography.bodySmall, color = ForestPalette.Muted)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$prefix${StudioRules.formatRubles(tx.amountKopecks)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = amountColor,
                                fontWeight = FontWeight.Bold
                            )
                            if (!isReversal && !alreadyReversed && !summary.isClosed) {
                                TextButton(
                                    onClick = { reverseTargetId = tx.id },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("reverse_tx_${tx.id}")
                                ) {
                                    Icon(Icons.AutoMirrored.Outlined.Undo, contentDescription = "Сторно", modifier = Modifier.size(14.dp), tint = ForestPalette.Error)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Сторно", color = ForestPalette.Error, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (summary.closures.isNotEmpty()) {
            item {
                OperationalCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("История версий закрытия месяца", style = MaterialTheme.typography.titleSmall, color = ForestPalette.Ink)
                    Spacer(Modifier.height(6.dp))
                    summary.closures.forEach { cl ->
                        Text(
                            text = "v${cl.version} · ${if (cl.isClosed) "Закрыт" else "Переоткрыт"} · Доход ${StudioRules.formatRubles(cl.incomeKopecks)} / Расход ${StudioRules.formatRubles(cl.expenseKopecks)}" +
                                (cl.reopenReason?.let { " · Причина: $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = ForestPalette.Body
                        )
                    }
                }
            }
        }
    }

        dialogKind?.let { activeKind ->
            AddTransactionOverlaySheet(
                kind = activeKind,
                yearMonth = ym,
                initialAmountKopecks = prefillAmountKopecks,
                initialNote = prefillNote,
                linkedEntryId = prefillEntryId,
                onDismiss = { dialogKind = null },
                onSave = { cat, kopecks, occurredAt, note, entryId, key ->
                    onAddTransaction(activeKind, cat, kopecks, occurredAt, note, entryId, key)
                    dialogKind = null
                }
            )
        }
    }

    if (showCloseConfirm) {
        ConfirmActionDialog(
            title = "Закрыть месяц ${StudioRules.formatMonthYear(ym)}?",
            message = "Будет зафиксирован снимок фактической кассы (доход ${StudioRules.formatRubles(summary.incomeKopecks)}, расход ${StudioRules.formatRubles(summary.expenseKopecks)}). Добавление новых операций в этот месяц заблокируется.",
            confirmLabel = "Закрыть месяц",
            onConfirm = {
                showCloseConfirm = false
                onCloseMonth(ym)
            },
            onDismiss = { showCloseConfirm = false }
        )
    }

    if (showReopenDialog) {
        var reason by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReopenDialog = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = ForestPalette.Surface,
            title = { Text("Переоткрыть месяц") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Укажите причину переоткрытия. Старая версия закрытия сохранится в журнале.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Причина переоткрытия *") },
                        modifier = Modifier.fillMaxWidth().testTag("reopen_reason_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = reason.trim().length >= 3,
                    onClick = {
                        showReopenDialog = false
                        onReopenMonth(ym, reason)
                    },
                    modifier = Modifier.testTag("reopen_confirm_button")
                ) {
                    Text("Переоткрыть", color = ForestPalette.Primary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReopenDialog = false }) { Text("Отмена", color = ForestPalette.Muted) }
            }
        )
    }

    reverseTargetId?.let { tid ->
        ConfirmActionDialog(
            title = "Провести сторно операции?",
            message = "Создастся обратная корректирующая запись, оригинал останется в истории. Повторное сторно одной операции запрещено.",
            confirmLabel = "Провести сторно",
            isDestructive = true,
            onConfirm = {
                reverseTargetId = null
                onReverseTransaction(tid)
            },
            onDismiss = { reverseTargetId = null }
        )
    }
}

data class QuickTransactionPrefill(
    val kind: FinanceKind,
    val suggestedKopecks: Long? = null,
    val note: String = "",
    val entryId: String? = null
)

@Composable
private fun AddTransactionOverlaySheet(
    kind: FinanceKind,
    yearMonth: YearMonth,
    initialAmountKopecks: Long?,
    initialNote: String,
    linkedEntryId: String?,
    onDismiss: () -> Unit,
    onSave: (FinanceCategory, Long, Instant, String, String?, String) -> Unit
) {
    BackHandler(onBack = onDismiss)

    val categories = remember(kind) { FinanceCategory.forKind(kind) }
    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var amountText by rememberSaveable {
        mutableStateOf(initialAmountKopecks?.let { (it / 100L).toString() } ?: "")
    }
    var note by rememberSaveable { mutableStateOf(initialNote) }
    val idempotencyKey = remember { UUID.randomUUID().toString() }
    var submitted by remember { mutableStateOf(false) }

    val defaultDate = remember(yearMonth) {
        val today = LocalDate.now(StudioRules.STUDIO_ZONE)
        if (YearMonth.from(today) == yearMonth) today else yearMonth.atDay(15)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ForestPalette.Forest.copy(alpha = 0.56f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss
            )
            .imePadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .pointerInput(Unit) { detectTapGestures { } },
            shape = RoundedCornerShape(20.dp),
            color = ForestPalette.Surface,
            border = BorderStroke(1.dp, ForestPalette.Border),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (kind == FinanceKind.INCOME) "Внести фактический доход" else "Внести фактический расход",
                    style = MaterialTheme.typography.titleLarge,
                    color = ForestPalette.Ink
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Сумма в ₽ *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("tx_amount_input")
                )
                Text("Категория", style = MaterialTheme.typography.labelMedium, color = ForestPalette.Muted)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    categories.chunked(2).forEach { rowCats ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowCats.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat.labelRu, maxLines = 1) }
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Примечание") },
                    modifier = Modifier.fillMaxWidth().testTag("tx_note_input")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена", color = ForestPalette.Muted)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = !submitted && StudioRules.parseRublesToKopecks(amountText) != null,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestPalette.Primary),
                        onClick = {
                            val kopecks = StudioRules.parseRublesToKopecks(amountText) ?: return@Button
                            submitted = true
                            onSave(
                                selectedCategory,
                                kopecks,
                                StudioRules.toInstant(defaultDate, 12 * 60),
                                note,
                                linkedEntryId,
                                idempotencyKey
                            )
                        },
                        modifier = Modifier.testTag("tx_save_button")
                    ) {
                        Text("Сохранить", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
