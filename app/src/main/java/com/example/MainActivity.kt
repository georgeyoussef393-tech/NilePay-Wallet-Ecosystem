package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.cards.CardRepository
import com.example.nfc.NfcScanManager
import com.example.nfc.NfcService
import com.example.security.AccessControlManager
import com.example.security.AuthManager
import com.example.security.BiometricAuthManager
import com.example.storage.CloudSyncManager
import com.example.storage.NilePayStorageManager
import com.example.ui.components.BiometricLockScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.EgyptianWalletsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LinkWalletScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.NfcPaymentScreen
import com.example.ui.screens.PrivacyPolicyScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.wallets.EgyptianWalletEngine

enum class Screen {
    LOGIN,
    HOME,
    NFC_PAYMENT,
    EGYPTIAN_WALLETS,
    LINK_WALLET,
    ADMIN_DASHBOARD,
    TRANSACTIONS,
    PRIVACY_POLICY
}

class MainActivity : FragmentActivity() {

    private lateinit var nfcService: NfcService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize persistent local storage, auth session, and cloud sync systems
        NilePayStorageManager.init(this)
        CloudSyncManager.init(this)
        AuthManager.init(this)
        CardRepository.loadFromStorageOrSeed()
        EgyptianWalletEngine.loadFromStorageOrSeed()
        AccessControlManager.loadFromStorageOrSeed()
        BiometricAuthManager.init(this)

        nfcService = NfcService.getInstance(this)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                NilePayApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcService.checkHardwareSupport()
        nfcService.enableReaderMode(this) { tag ->
            runOnUiThread {
                NfcScanManager.onTagDiscovered(tag, this)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        nfcService.disableReaderMode(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tag = nfcService.extractTagFromIntent(intent)
        if (tag != null) {
            NfcScanManager.onTagDiscovered(tag, this)
        }
    }
}

@Composable
fun NilePayApp() {
    val isLoggedIn by AuthManager.isLoggedIn.collectAsState()
    var currentScreen by remember { mutableStateOf(if (isLoggedIn) Screen.HOME else Screen.LOGIN) }
    val isWalletLocked by BiometricAuthManager.isWalletLocked.collectAsState()

    if (currentScreen == Screen.PRIVACY_POLICY) {
        PrivacyPolicyScreen(
            onNavigateBack = {
                currentScreen = if (isLoggedIn) Screen.HOME else Screen.LOGIN
            }
        )
    } else if (!isLoggedIn || currentScreen == Screen.LOGIN) {
        LoginScreen(
            onLoginSuccess = {
                currentScreen = Screen.HOME
            },
            onNavigateToPrivacy = {
                currentScreen = Screen.PRIVACY_POLICY
            }
        )
    } else if (isWalletLocked) {
        BiometricLockScreen(
            onUnlocked = {
                // Wallet is unlocked
            }
        )
    } else {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    Screen.HOME -> {
                        HomeScreen(
                            onNavigateToNfc = { currentScreen = Screen.NFC_PAYMENT },
                            onNavigateToWallets = { currentScreen = Screen.EGYPTIAN_WALLETS },
                            onNavigateToAdmin = { currentScreen = Screen.ADMIN_DASHBOARD },
                            onNavigateToTransactions = { currentScreen = Screen.TRANSACTIONS },
                            onNavigateToPrivacy = { currentScreen = Screen.PRIVACY_POLICY },
                            onNavigateToLogin = { currentScreen = Screen.LOGIN }
                        )
                    }

                    Screen.NFC_PAYMENT -> {
                        BackHandler { currentScreen = Screen.HOME }
                        NfcPaymentScreen(
                            onNavigateBack = { currentScreen = Screen.HOME }
                        )
                    }

                    Screen.EGYPTIAN_WALLETS -> {
                        BackHandler { currentScreen = Screen.HOME }
                        EgyptianWalletsScreen(
                            onNavigateBack = { currentScreen = Screen.HOME },
                            onNavigateToLinkWallet = { currentScreen = Screen.LINK_WALLET }
                        )
                    }

                    Screen.LINK_WALLET -> {
                        BackHandler { currentScreen = Screen.EGYPTIAN_WALLETS }
                        LinkWalletScreen(
                            onNavigateBack = { currentScreen = Screen.EGYPTIAN_WALLETS },
                            onWalletLinked = { currentScreen = Screen.EGYPTIAN_WALLETS }
                        )
                    }

                    Screen.ADMIN_DASHBOARD -> {
                        BackHandler { currentScreen = Screen.HOME }
                        AdminDashboardScreen(
                            onNavigateBack = { currentScreen = Screen.HOME }
                        )
                    }

                    Screen.TRANSACTIONS -> {
                        BackHandler { currentScreen = Screen.HOME }
                        TransactionsScreen(
                            onNavigateBack = { currentScreen = Screen.HOME }
                        )
                    }

                    Screen.PRIVACY_POLICY -> {
                        BackHandler { currentScreen = Screen.HOME }
                        PrivacyPolicyScreen(
                            onNavigateBack = { currentScreen = Screen.HOME }
                        )
                    }

                    Screen.LOGIN -> {
                        LoginScreen(
                            onLoginSuccess = { currentScreen = Screen.HOME },
                            onNavigateToPrivacy = { currentScreen = Screen.PRIVACY_POLICY }
                        )
                    }
                }
            }
        }
    }
}
