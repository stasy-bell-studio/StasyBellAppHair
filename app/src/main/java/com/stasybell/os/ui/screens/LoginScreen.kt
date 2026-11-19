package com.stasybell.os.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.stasybell.os.data.repository.LocalDemoRepository
import com.stasybell.os.domain.DataMode
import com.stasybell.os.ui.components.DemoModePill
import com.stasybell.os.ui.theme.ForestPalette
import com.stasybell.os.ui.viewmodel.AppUiState

/**
 * Screen 01: Owner Login (Настя) with Android Keystore session restore,
 * optional BiometricPrompt unlock, and explicit Demo vs FastAPI HTTPS mode selector.
 */
@Composable
fun LoginScreen(
    state: AppUiState,
    onLogin: (String, String) -> Unit,
    onBiometricClick: () -> Unit,
    onSwitchMode: (DataMode, String, Boolean) -> Unit
) {
    var username by rememberSaveable { mutableStateOf(if (state.dataMode == DataMode.DEMO_ROOM) LocalDemoRepository.DEMO_USER else "") }
    var password by rememberSaveable { mutableStateOf(if (state.dataMode == DataMode.DEMO_ROOM) LocalDemoRepository.DEMO_PASS else "") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var serverUrl by rememberSaveable { mutableStateOf(state.serverUrl) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ForestPalette.Canvas)
    ) {
        // Top atmospheric deep forest header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .background(
                    Brush.linearGradient(
                        listOf(ForestPalette.Forest, ForestPalette.PrimaryDeep, ForestPalette.Primary)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Stasy Bell OS 2.0",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    if (state.dataMode == DataMode.DEMO_ROOM) {
                        DemoModePill(onDark = true)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Рабочее пространство мастера · Санкт-Петербург",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ForestPalette.Mint
                )
                Spacer(Modifier.height(24.dp))

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ForestPalette.Surface,
                    border = BorderStroke(1.dp, ForestPalette.Border),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Вход владельца",
                            style = MaterialTheme.typography.titleLarge,
                            color = ForestPalette.Ink
                        )

                        // Explicit environment switcher
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = state.dataMode == DataMode.DEMO_ROOM,
                                onClick = {
                                    onSwitchMode(DataMode.DEMO_ROOM, serverUrl, state.allowDebugHttp)
                                    username = LocalDemoRepository.DEMO_USER
                                    password = LocalDemoRepository.DEMO_PASS
                                },
                                label = { Text("Демо (Room)") },
                                modifier = Modifier.testTag("mode_demo_chip")
                            )
                            FilterChip(
                                selected = state.dataMode == DataMode.REMOTE_FASTAPI,
                                onClick = {
                                    if (serverUrl.isNotBlank()) {
                                        onSwitchMode(DataMode.REMOTE_FASTAPI, serverUrl, state.allowDebugHttp)
                                    }
                                },
                                label = { Text("FastAPI HTTPS") },
                                modifier = Modifier.testTag("mode_remote_chip")
                            )
                        }

                        if (state.dataMode == DataMode.DEMO_ROOM) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ForestPalette.MintSoft,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Синтетический стенд Room (данные локальные, не боевые).\nЛогин: ${LocalDemoRepository.DEMO_USER} · Пароль: ${LocalDemoRepository.DEMO_PASS}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ForestPalette.PrimaryDeep,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        } else {
                            OutlinedTextField(
                                value = serverUrl,
                                onValueChange = { serverUrl = it },
                                label = { Text("Адрес API (https://...)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_server_url_input")
                            )
                        }

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Логин") },
                            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_username_input")
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Пароль") },
                            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = "Показать пароль"
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input")
                        )

                        Button(
                            onClick = { onLogin(username, password) },
                            enabled = !state.isBusy && username.isNotBlank() && password.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ForestPalette.Primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("login_submit_button")
                        ) {
                            if (state.isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Войти", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (state.biometricEnabled) {
                            OutlinedButton(
                                onClick = onBiometricClick,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("login_biometric_button")
                            ) {
                                Icon(Icons.Outlined.Fingerprint, contentDescription = "Вход по биометрии")
                                Spacer(Modifier.width(8.dp))
                                Text("Войти по отпечатку")
                            }
                        }

                        Text(
                            text = "Только для владелицы студии. Пароль не сохраняется на устройстве; токены сессии шифруются в Android Keystore (30 дней).",
                            style = MaterialTheme.typography.bodySmall,
                            color = ForestPalette.Muted
                        )
                    }
                }
            }
        }
    }
}
