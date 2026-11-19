package com.stasybell.os.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.stasybell.os.ui.components.StatusPill
import com.stasybell.os.ui.theme.ForestPalette
import com.stasybell.os.ui.viewmodel.AppUiState

private const val LOCAL_VIRTUAL_HOST = "stasybell.local"
private const val LOCAL_START_URL = "https://$LOCAL_VIRTUAL_HOST/index.html"

/**
 * In-app live preview of the redesigned Dark Emerald Luxury Stasy Bell website.
 * Loads bundled single-source website assets via virtual HTTPS interception and
 * injects live services & working windows from STASY BELL OS 2.0.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebsitePreviewScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onSubmitWebsiteTestBooking: (WebIncomingBookingDto) -> Unit
) {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pageLoaded by remember { mutableStateOf(false) }

    val syncJson = remember(state.services, state.workingWindows, state.calendarEntries) {
        WebsitePreviewBridge.buildSyncPayloadJson(state)
    }

    LaunchedEffect(syncJson, pageLoaded) {
        val wv = webViewRef ?: return@LaunchedEffect
        if (pageLoaded) {
            val escaped = org.json.JSONObject.quote(syncJson)
            wv.evaluateJavascript("window.applyStasyBellAndroidSync && window.applyStasyBellAndroidSync($escaped);", null)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF151110))
            .statusBarsPadding()
            .testTag("website_preview_screen")
    ) {
        Surface(
            color = Color(0xFF151110),
            border = BorderStroke(1.dp, Color(0x40E0C49A))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("website_preview_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color(0xFFF6F3EC)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Сайт «Стэйси Белл»",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFF6F3EC),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Silk Ivory & Espresso · GitHub Pages + Live Sync",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE0C49A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusPill(
                        label = "${state.services.count { it.active }} услуг",
                        fg = Color(0xFFE0C49A),
                        bg = Color(0x26E0C49A)
                    )
                    IconButton(
                        onClick = { webViewRef?.reload() },
                        modifier = Modifier.testTag("website_preview_reload_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Обновить превью",
                            tint = Color(0xFFE0C49A)
                        )
                    }
                }
            }
        }

        Surface(
            color = Color(0xFF211B18),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Sync,
                    contentDescription = null,
                    tint = Color(0xFFE0C49A),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Прайс и свободные окна синхронизированы с STASY BELL OS. Тестовая заявка на сайте сразу попадёт в ваш календарь.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFBFB3A6)
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val isRobolectric = remember {
                android.os.Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
            }

            if (isRobolectric) {
                // Clean deterministic placeholder in JVM Robolectric tests where Chromium WebView is absent
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF211B18),
                    border = BorderStroke(1.dp, Color(0x44E0C49A)),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                        .testTag("website_preview_test_placeholder")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Language,
                            contentDescription = null,
                            tint = Color(0xFFE0C49A),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Превью сайта «Стэйси Белл» готово",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFF9F4EC)
                        )
                        Text(
                            text = "Синхронизировано услуг: ${state.services.count { it.active }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFBFB3A6)
                        )
                    }
                }
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize().testTag("website_preview_webview"),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(android.graphics.Color.parseColor("#FAF6F0"))
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true

                            addJavascriptInterface(
                                object {
                                    @JavascriptInterface
                                    fun submitDemoBooking(rawPayload: String): String {
                                        val parsed = WebsitePreviewBridge.parseIncomingBooking(rawPayload)
                                            ?: return """{"ok":false,"error":"Некорректный формат заявки"}"""
                                        mainHandler.post {
                                            onSubmitWebsiteTestBooking(parsed)
                                        }
                                        val shortId = parsed.idempotencyKey.take(6).ifEmpty { "WEB" }
                                        return """{"ok":true,"entryId":"$shortId"}"""
                                    }
                                },
                                "StasyBellAndroidBridge"
                            )

                            webViewClient = object : WebViewClient() {
                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): WebResourceResponse? {
                                    val uri = request?.url ?: return null
                                    if (uri.host != LOCAL_VIRTUAL_HOST) return null
                                    val assetPath = (uri.path ?: "/index.html").trimStart('/').ifEmpty { "index.html" }
                                    return try {
                                        val stream = ctx.assets.open(assetPath)
                                        val mimeFull = WebsitePreviewBridge.mimeTypeForPath(assetPath)
                                        val mime = mimeFull.substringBefore(';')
                                        val encoding = if (mimeFull.contains("utf-8")) "UTF-8" else null
                                        WebResourceResponse(mime, encoding, stream)
                                    } catch (_: Exception) {
                                        null
                                    }
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val uri = request?.url ?: return false
                                    if (uri.host == LOCAL_VIRTUAL_HOST) return false
                                    val scheme = uri.scheme?.lowercase() ?: return true
                                    if (scheme == "tel" || scheme == "https" || scheme == "http") {
                                        runCatching {
                                            val action = if (scheme == "tel") Intent.ACTION_DIAL else Intent.ACTION_VIEW
                                            context.startActivity(Intent(action, Uri.parse(uri.toString())))
                                        }
                                    }
                                    return true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    pageLoaded = true
                                    val escaped = org.json.JSONObject.quote(syncJson)
                                    view?.evaluateJavascript(
                                        "window.applyStasyBellAndroidSync && window.applyStasyBellAndroidSync($escaped);",
                                        null
                                    )
                                }
                            }

                            loadUrl(LOCAL_START_URL)
                            webViewRef = this
                        }
                    }
                )
            }
        }
    }
}
