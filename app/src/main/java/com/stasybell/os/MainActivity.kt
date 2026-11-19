package com.stasybell.os

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.stasybell.os.data.local.StasyBellDatabase
import com.stasybell.os.data.repository.DualModeRepository
import com.stasybell.os.data.repository.LocalDemoRepository
import com.stasybell.os.data.repository.RemoteFastApiRepository
import com.stasybell.os.security.SecureTokenStore
import com.stasybell.os.ui.navigation.StasyBellApp
import com.stasybell.os.ui.theme.StasyBellTheme
import com.stasybell.os.ui.viewmodel.MainViewModel

class MainActivity : FragmentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val tokenStore = SecureTokenStore(applicationContext)
        val database = StasyBellDatabase.getInstance(applicationContext)
        val localRepo = LocalDemoRepository(database, tokenStore)
        val remoteRepo = RemoteFastApiRepository(tokenStore)
        val repository = DualModeRepository(localRepo, remoteRepo, tokenStore)

        viewModel = ViewModelProvider(
            this,
            MainViewModel.Factory(repository, tokenStore)
        )[MainViewModel::class.java]

        setContent {
            StasyBellTheme {
                StasyBellApp(
                    viewModel = viewModel,
                    onRequestBiometricAuth = ::triggerBiometricPrompt
                )
            }
        }
    }

    private fun triggerBiometricPrompt() {
        val manager = BiometricManager.from(this)
        val canAuth = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            return
        }
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    viewModel.loginWithBiometricSuccess()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Вход в Stasy Bell OS 2.0")
            .setSubtitle("Подтвердите личность владельца")
            .setNegativeButtonText("Использовать пароль")
            .build()
        prompt.authenticate(info)
    }
}
