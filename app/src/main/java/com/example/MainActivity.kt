package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.cards.CardRepository
import com.example.nfc.NfcScanManager
import com.example.nfc.NfcService
import com.example.security.AccessControlManager
import com.example.storage.CloudSyncManager
import com.example.storage.NilePayStorageManager
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.EgyptianWalletsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NfcPaymentScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.wallets.EgyptianWalletEngine

enum class Screen {
    HOME,
    NFC_PAYMENT,
    EGYPTIAN_WALLETS,
    ADMIN_DASHBOARD
}

class MainActivity : ComponentActivity() {

    private lateinit var nfcService: NfcService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize persistent local storage and cloud sync systems
        NilePayStorageManager.init(this)
        CloudSyncManager.init(this)
        CardRepository.loadFromStorageOrSeed()
        EgyptianWalletEngine.loadFromStorageOrSeed()
        AccessControlManager.loadFromStorageOrSeed()

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
                NfcScanManager.onTagDiscovered(tag)
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
            NfcScanManager.onTagDiscovered(tag)
        }
    }
}

@Composable
fun NilePayApp() {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }

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
                        onNavigateToAdmin = { currentScreen = Screen.ADMIN_DASHBOARD }
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
                        onNavigateBack = { currentScreen = Screen.HOME }
                    )
                }

                Screen.ADMIN_DASHBOARD -> {
                    BackHandler { currentScreen = Screen.HOME }
                    AdminDashboardScreen(
                        onNavigateBack = { currentScreen = Screen.HOME }
                    )
                }
            }
        }
    }
}
