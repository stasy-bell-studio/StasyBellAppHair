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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stasybell.os.BuildConfig
import com.stasybell.os.domain.DataMode
import com.stasybell.os.domain.StudioRules
import com.stasybell.os.ui.components.DemoModePill
import com.stasybell.os.ui.components.OperationalCard
import com.stasybell.os.ui.components.StatusPill
import com.stasybell.os.ui.theme.ForestPalette
import com.stasybell.os.ui.viewmodel.AppUiState

/**
 * Screen 16: More & Settings hub (links to Services, Schedule, Materials, Reports,
 * plus Owner Sessions, Biometrics, FastAPI HTTPS configuration, and honest integration statuses).
 */
@Composable
fun MoreAndSettingsScreen(
    state: AppUiState,
    onOpenServices: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenMaterials: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenWebsite: () -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onCheckHealth: (String, Boolean) -> Unit,
    onSwitchMode: (DataMode, String, Boolean) -> Unit,
    onRevokeSession: (String) -> Unit,
    onLogout: () -> Unit
) {
    var serverUrl by rememberSaveable(state.serverUrl) { mutableStateOf(state.serverUrl) }
    var allowDebugHttp by rememberSaveable(state.allowDebugHttp) { mutableStateOf(state.allowDebugHttp) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ForestPalette.Canvas)
            .statusBarsPadding()
            .testTag("more_screen")
    ) {
        Surface(color = ForestPalette.Surface, border = BorderStroke(1.dp, ForestPalette.Border)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ещё и настройки", style = MaterialTheme.typography.headlineSmall, color = ForestPalette.Ink)
                    if (state.dataMode == DataMode.DEMO_ROOM) DemoModePill()
                }
                TextButton(onClick = onLogout, modifier = Modifier.testTag("settings_logout_button")) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Выйти", tint = ForestPalette.Error, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Выйти", color = ForestPalette.Error)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Операционные разделы", style = MaterialTheme.typography.titleMedium, color = ForestPalette.PrimaryDeep)
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MenuNavTile(
                        title = "Каталог услуг",
                        subtitle = "${state.services.size} позиций · цены, длительность и буфер",
                        icon = Icons.Outlined.ContentCut,
                        tag = "more_services_tile",
                        onClick = onOpenServices
                    )
                    MenuNavTile(
                        title = "Рабочий график",
                        subtitle = "Рабочие окна, перерывы и выходные дни",
                        icon = Icons.Outlined.EditCalendar,
                        tag = "more_schedule_tile",
                        onClick = onOpenSchedule
                    )
                    MenuNavTile(
                        title = "Материалы и склад",
                        subtitle = "${state.materials.size} позиций · остатки г/мл/шт до 0,001",
                        icon = Icons.Outlined.Inventory2,
                        tag = "more_materials_tile",
                        onClick = onOpenMaterials
                    )
                    MenuNavTile(
                        title = "Отчёты и архив Excel",
                        subtitle = "Касса, производственная аналитика и 12 визитов на сверку",
                        icon = Icons.Outlined.Assessment,
                        tag = "more_reports_tile",
                        onClick = onOpenReports
                    )
                    MenuNavTile(
                        title = "Сайт студии (превью)",
                        subtitle = "Silk Ivory & Espresso · До/После, квиз, калькулятор и живые слоты",
                        icon = Icons.Outlined.Language,
                        tag = "more_website_tile",
                        onClick = onOpenWebsite
                    )
                }
            }

            // Server & Data Mode Configuration
            item {
                OperationalCard {
                    Text("Режим данных и сервер API", style = MaterialTheme.typography.titleMedium, color = ForestPalette.Ink)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.dataMode == DataMode.DEMO_ROOM,
                            onClick = { onSwitchMode(DataMode.DEMO_ROOM, serverUrl, allowDebugHttp) },
                            label = { Text("Демо (Room)") }
                        )
                        FilterChip(
                            selected = state.dataMode == DataMode.REMOTE_FASTAPI,
                            onClick = { onSwitchMode(DataMode.REMOTE_FASTAPI, serverUrl, allowDebugHttp) },
                            label = { Text("FastAPI HTTPS") }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("URL сервера (https://api.example.ru)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_server_url_input")
                    )
                    if (BuildConfig.DEBUG) {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Debug-режим: разрешить localhost HTTP", style = MaterialTheme.typography.bodySmall, color = ForestPalette.Muted)
                            Switch(checked = allowDebugHttp, onCheckedChange = { allowDebugHttp = it })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onCheckHealth(serverUrl, allowDebugHttp) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Проверить /health")
                        }
                        Button(
                            onClick = { onSwitchMode(DataMode.REMOTE_FASTAPI, serverUrl, allowDebugHttp) },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestPalette.Primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Подключить")
                        }
                    }
                    state.serverHealthMessage?.let { msg ->
                        Spacer(Modifier.height(6.dp))
                        Text(msg, style = MaterialTheme.typography.bodySmall, color = ForestPalette.PrimaryDeep)
                    }
                }
            }

            // Security & Biometrics
            item {
                OperationalCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Outlined.Fingerprint, contentDescription = null, tint = ForestPalette.Primary)
                            Column {
                                Text("Вход по биометрии", style = MaterialTheme.typography.titleSmall, color = ForestPalette.Ink)
                                Text("Опционально при запуске (не блокирует пароль)", style = MaterialTheme.typography.bodySmall, color = ForestPalette.Muted)
                            }
                        }
                        Switch(
                            checked = state.biometricEnabled,
                            onCheckedChange = onToggleBiometric,
                            modifier = Modifier.testTag("settings_biometric_switch")
                        )
                    }
                }
            }

            // Honest integration statuses (never fake connected bots or backups)
            item {
                OperationalCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Security, contentDescription = null, tint = ForestPalette.Primary)
                        Text("Статус внешних интеграций", style = MaterialTheme.typography.titleMedium, color = ForestPalette.Ink)
                    }
                    Spacer(Modifier.height(8.dp))
                    IntegrationStatusRow("Telegram-бот подтверждений", "Не настроено")
                    IntegrationStatusRow("Сообщество VK (callback)", "Не настроено")
                    IntegrationStatusRow("Off-host шифрованный backup БД", "Не настроено")
                    IntegrationStatusRow("Онлайн-оплаты / эквайринг", "Отключено по ТЗ")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Адрес студии: Санкт-Петербург, ул. Ольги Берггольц, д. 9, корп. 2",
                        style = MaterialTheme.typography.bodySmall,
                        color = ForestPalette.Muted
                    )
                }
            }

            // Active Owner Sessions
            item {
                Text("Активные сессии владельца (${state.sessions.size})", style = MaterialTheme.typography.titleMedium, color = ForestPalette.Ink)
            }
            items(state.sessions, key = { it.id }) { sess ->
                OperationalCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sess.deviceName + if (sess.isCurrent) " (текущее)" else "",
                                style = MaterialTheme.typography.titleSmall,
                                color = ForestPalette.Ink
                            )
                            Text(
                                text = "До ${StudioRules.formatDate(StudioRules.toLocalDate(sess.expiresAt))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ForestPalette.Muted
                            )
                        }
                        if (sess.revoked) {
                            StatusPill(label = "Отозвана", fg = ForestPalette.Muted, bg = ForestPalette.CanvasSoft)
                        } else {
                            TextButton(onClick = { onRevokeSession(sess.id) }) {
                                Text("Отозвать", color = ForestPalette.Error, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuNavTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    OperationalCard(onClick = onClick, modifier = Modifier.testTag(tag)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(icon, contentDescription = null, tint = ForestPalette.Primary)
                Column {
                    Text(title, style = MaterialTheme.typography.titleSmall, color = ForestPalette.Ink)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ForestPalette.Muted)
                }
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = ForestPalette.Muted)
        }
    }
}

@Composable
private fun IntegrationStatusRow(label: String, status: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = ForestPalette.Body)
        StatusPill(label = status, fg = ForestPalette.Muted, bg = ForestPalette.CanvasSoft)
    }
}
