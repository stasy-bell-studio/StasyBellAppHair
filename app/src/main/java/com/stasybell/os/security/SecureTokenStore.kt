package com.stasybell.os.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.stasybell.os.domain.DataMode
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypts refresh tokens & session credentials using Android Keystore AES-256-GCM.
 * Raw passwords are NEVER stored on device.
 */
class SecureTokenStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val fallbackKey: SecretKey by lazy {
        // Used only if AndroidKeyStore is unavailable (e.g. JVM Robolectric tests).
        val existing = prefs.getString(KEY_FALLBACK_WRAP, null)
        if (existing != null) {
            SecretKeySpec(Base64.decode(existing, Base64.NO_WRAP), "AES")
        } else {
            val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
            prefs.edit().putString(KEY_FALLBACK_WRAP, Base64.encodeToString(bytes, Base64.NO_WRAP)).apply()
            SecretKeySpec(bytes, "AES")
        }
    }

    private fun getOrCreateKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            if (existing != null) return existing

            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGen.init(spec)
            keyGen.generateKey()
        } catch (_: Throwable) {
            fallbackKey
        }
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(1 + iv.size + ciphertext.size)
        combined[0] = iv.size.toByte()
        System.arraycopy(iv, 0, combined, 1, iv.size)
        System.arraycopy(ciphertext, 0, combined, 1 + iv.size, ciphertext.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String? {
        return try {
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            val ivLen = combined[0].toInt() and 0xFF
            val iv = combined.copyOfRange(1, 1 + ivLen)
            val ciphertext = combined.copyOfRange(1 + ivLen, combined.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (_: Throwable) {
            null
        }
    }

    fun saveSessionTokens(sessionId: String, accessToken: String, refreshToken: String, expiresAtEpochMs: Long) {
        prefs.edit()
            .putString(KEY_SESSION_ID, encrypt(sessionId))
            .putString(KEY_ACCESS_TOKEN, encrypt(accessToken))
            .putString(KEY_REFRESH_TOKEN, encrypt(refreshToken))
            .putLong(KEY_EXPIRES_AT, expiresAtEpochMs)
            .apply()
    }

    fun getSessionId(): String? = prefs.getString(KEY_SESSION_ID, null)?.let { decrypt(it) }
    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)?.let { decrypt(it) }
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)?.let { decrypt(it) }
    fun getExpiresAtEpochMs(): Long = prefs.getLong(KEY_EXPIRES_AT, 0L)

    fun hasValidSession(nowEpochMs: Long = System.currentTimeMillis()): Boolean {
        val sid = getSessionId()
        val refresh = getRefreshToken()
        val exp = getExpiresAtEpochMs()
        return !sid.isNullOrBlank() && !refresh.isNullOrBlank() && exp > nowEpochMs
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_SESSION_ID)
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .apply()
    }

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC, false)
    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
    }

    fun getDataMode(): DataMode {
        val raw = prefs.getString(KEY_DATA_MODE, DataMode.DEMO_ROOM.name)
        return runCatching { DataMode.valueOf(raw ?: DataMode.DEMO_ROOM.name) }
            .getOrDefault(DataMode.DEMO_ROOM)
    }

    fun setDataMode(mode: DataMode) {
        prefs.edit().putString(KEY_DATA_MODE, mode.name).apply()
    }

    fun getServerBaseUrl(): String = prefs.getString(KEY_SERVER_URL, "") ?: ""
    fun setServerBaseUrl(url: String) {
        prefs.edit().putString(KEY_SERVER_URL, url).apply()
    }

    fun getAllowDebugHttp(): Boolean = prefs.getBoolean(KEY_ALLOW_DEBUG_HTTP, false)
    fun setAllowDebugHttp(allow: Boolean) {
        prefs.edit().putBoolean(KEY_ALLOW_DEBUG_HTTP, allow).apply()
    }

    companion object {
        private const val PREFS_NAME = "stasy_bell_secure_prefs"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "stasy_bell_os_aes_key_v2"
        private const val KEY_FALLBACK_WRAP = "fallback_wrap"
        private const val KEY_SESSION_ID = "enc_session_id"
        private const val KEY_ACCESS_TOKEN = "enc_access_token"
        private const val KEY_REFRESH_TOKEN = "enc_refresh_token"
        private const val KEY_EXPIRES_AT = "session_expires_at"
        private const val KEY_BIOMETRIC = "biometric_enabled"
        private const val KEY_DATA_MODE = "data_mode"
        private const val KEY_SERVER_URL = "server_base_url"
        private const val KEY_ALLOW_DEBUG_HTTP = "allow_debug_http"
    }
}

class LoginRateLimiter(
    private val maxFailures: Int = 5,
    private val cooldownMs: Long = 60_000L
) {
    private var failedCount = 0
    private var blockedUntilMs = 0L

    fun checkAllowed(nowMs: Long = System.currentTimeMillis()): Long? {
        return if (nowMs < blockedUntilMs) {
            ((blockedUntilMs - nowMs) / 1000L).coerceAtLeast(1L)
        } else {
            if (blockedUntilMs != 0L) {
                failedCount = 0
                blockedUntilMs = 0L
            }
            null
        }
    }

    fun recordFailure(nowMs: Long = System.currentTimeMillis()) {
        failedCount++
        if (failedCount >= maxFailures) {
            blockedUntilMs = nowMs + cooldownMs
        }
    }

    fun recordSuccess() {
        failedCount = 0
        blockedUntilMs = 0L
    }
}
