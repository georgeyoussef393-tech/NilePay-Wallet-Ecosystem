package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Atm
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cards.CardRepository
import com.example.model.CardToken
import com.example.model.CardType
import com.example.model.FeaturePermission
import com.example.model.TransactionRecord
import com.example.nfc.EmvScannedCard
import com.example.nfc.NfcScanManager
import com.example.security.AccessControlManager
import com.example.ui.components.CloudSyncDialog
import com.example.ui.components.NfcCardScanDialog
import com.example.ui.components.NileTopHeader
import com.example.ui.components.SecurityVaultDialog
import com.example.ui.components.TransactionItemRow
import com.example.ui.theme.InstaPayPurple
import com.example.ui.theme.MeezaGreen
import com.example.ui.theme.NileCyan
import com.example.ui.theme.NileDarkSurface
import com.example.ui.theme.NileError
import com.example.ui.theme.NileGold
import com.example.ui.theme.NilePrimary
import com.example.ui.theme.NileSuccess
import com.example.ui.theme.VodafoneRed
import com.example.wallets.EgyptianWalletEngine
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToNfc: () -> Unit,
    onNavigateToWallets: () -> Unit,
    onNavigateToAdmin: () -> Unit
) {
    val cards by CardRepository.cards.collectAsState()
    val defaultCard by CardRepository.defaultNfcCard.collectAsState()
    val wallets by EgyptianWalletEngine.wallets.collectAsState()
    val transactions by EgyptianWalletEngine.recentTransactions.collectAsState()
    val activeCompany by AccessControlManager.activeCompany.collectAsState()
    val companies by AccessControlManager.companies.collectAsState()

    val scannedCard by NfcScanManager.scannedCard.collectAsState()
    val isScanning by NfcScanManager.isScanning.collectAsState()
    val scanStatus by NfcScanManager.scanStatus.collectAsState()
    val scanError by NfcScanManager.scanError.collectAsState()

    var showSwitchPersonaDialog by remember { mutableStateOf(false) }
    var showAddCardDialog by remember { mutableStateOf(false) }
    var showNfcScanDialog by remember { mutableStateOf(false) }
    var showCloudSyncDialog by remember { mutableStateOf(false) }
    var showSecurityVaultDialog by remember { mutableStateOf(false) }
    var selectedTxDetail by remember { mutableStateOf<TransactionRecord?>(null) }

    // Total aggregated balance across cards & Egyptian mobile wallets
    val totalWalletBalance = wallets.filter { it.isLinked }.sumOf { it.balanceEGP }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Sticky/Top Header with NilePay identity & testing company chip
        item {
            NileTopHeader(
                activeCompany = activeCompany,
                onOpenAdmin = onNavigateToAdmin,
                onSwitchCompany = { showSwitchPersonaDialog = true },
                onOpenCloudSync = { showCloudSyncDialog = true },
                onOpenSecurityCenter = { showSecurityVaultDialog = true }
            )
        }

        // Hero Balance Card
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hero_balance_card"),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300C2CB))
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF0F2B48),
                                        Color(0xFF071424)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = NileCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "AGGREGATED EGYPTIAN BALANCE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NileCyan,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NileGold.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "CBE IPN",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NileGold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "${String.format(Locale.US, "%,.2f", totalWalletBalance)} EGP",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column {
                                    Text(text = "Cards Registered", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "${cards.size} Cards (Visa / MC / Meeza)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                }
                                Column {
                                    Text(text = "Egyptian Wallets", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "${wallets.count { it.isLinked }} Active (Vodafone, InstaPay)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Actions 4-Way Grid
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "CORE PAYMENT CAPABILITIES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NileCyan,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        title = "NFC Tap & Pay",
                        subtitle = "Visa / Mastercard POS",
                        icon = Icons.Default.Nfc,
                        accentColor = NileCyan,
                        testTag = "quick_action_nfc",
                        onClick = onNavigateToNfc
                    )
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        title = "Vodafone Cash",
                        subtitle = "Transfer & Cash-Out",
                        icon = Icons.Default.PhoneAndroid,
                        accentColor = VodafoneRed,
                        testTag = "quick_action_vodafone",
                        onClick = onNavigateToWallets
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        title = "InstaPay IPN",
                        subtitle = "Instant CBE Transfer",
                        icon = Icons.Default.Send,
                        accentColor = InstaPayPurple,
                        testTag = "quick_action_instapay",
                        onClick = onNavigateToWallets
                    )
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        title = "Admin Console",
                        subtitle = "Dynamic Access Control",
                        icon = Icons.Default.AdminPanelSettings,
                        accentColor = NileGold,
                        testTag = "quick_action_admin",
                        onClick = onNavigateToAdmin
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))

                // Cloud & Local Data Protection Banner
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCloudSyncDialog = true }
                        .testTag("cloud_persistence_banner"),
                    shape = RoundedCornerShape(16.dp),
                    color = NileDarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NileSuccess.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(NileSuccess.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Cloud & Local Sync",
                                    tint = NileSuccess,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "الحفظ السحابي والمحلي نشط",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NileSuccess
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(NileCyan.copy(alpha = 0.2f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "حماية كاملة",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NileCyan
                                        )
                                    }
                                }
                                Text(
                                    text = "${cards.size} بطاقات • ${wallets.size} محافظ • بياناتك لا تضيع عند أي تعديل",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NileSuccess.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "إدارة",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NileSuccess,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Card Security & NFC Protection Banner
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSecurityVaultDialog = true }
                        .testTag("security_protection_banner"),
                    shape = RoundedCornerShape(16.dp),
                    color = NileDarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NileCyan.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(NileCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Card & NFC Protection",
                                    tint = NileCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "أمان البطاقات و NFC مفعل",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NileCyan
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(NileGold.copy(alpha = 0.2f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "Anti-Relay & AES-256",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NileGold
                                        )
                                    }
                                }
                                Text(
                                    text = "تشفير التلامس • حظر استنساخ الكروت • فحص زمني ذكي لمنع الاختراق",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NileCyan.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "فحص",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NileCyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // Cards Carousel
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOKENIZED PAYMENT CARDS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NileCyan,
                        letterSpacing = 1.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = {
                                NfcScanManager.startScanning()
                                showNfcScanDialog = true
                            },
                            modifier = Modifier.testTag("scan_nfc_card_button")
                        ) {
                            Icon(imageVector = Icons.Default.Nfc, contentDescription = null, tint = NileCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Scan NFC", color = NileCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = { showAddCardDialog = true },
                            modifier = Modifier.testTag("add_card_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = NileCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Manual", color = NileCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (cards.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("empty_cards_banner"),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Contactless,
                                contentDescription = null,
                                tint = NileCyan,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No Payment Cards Registered",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap your physical Visa / Mastercard against the phone to scan via NFC, or enter details manually.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        NfcScanManager.startScanning()
                                        showNfcScanDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NileCyan, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("empty_banner_scan_nfc")
                                ) {
                                    Icon(imageVector = Icons.Default.Nfc, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Scan Card (NFC)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { showAddCardDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                                    modifier = Modifier.testTag("empty_banner_manual")
                                ) {
                                    Text(text = "Manual Entry", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(cards) { card ->
                            CardItem(
                                card = card,
                                onMakeDefault = { CardRepository.setDefaultCard(card.id) },
                                onTapToPay = onNavigateToNfc
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Recent Activity
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT TRANSACTIONS & AUDIT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NileCyan,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${transactions.size} records",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (transactions.isEmpty()) {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("empty_transactions_banner"),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Transactions Recorded Yet",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Perform a contactless NFC payment or mobile wallet transfer to view real-time audit ledger.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else {
            items(transactions.take(5)) { tx ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    TransactionItemRow(tx = tx, onClick = { selectedTxDetail = tx })
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Modal Sheet: Persona / Company Testing Switcher
    if (showSwitchPersonaDialog) {
        AlertDialog(
            onDismissRequest = { showSwitchPersonaDialog = false },
            title = {
                Text(
                    text = "Switch Testing Company Persona",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        Text(
                            text = "Select an authorized testing partner to verify dynamic access control and permissions live in the app:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    items(companies) { company ->
                        val isSelected = company.companyId == activeCompany.companyId
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    AccessControlManager.switchActiveCompany(company.companyId)
                                    showSwitchPersonaDialog = false
                                }
                                .testTag("select_persona_${company.companyId}"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) NileCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (!company.isActive) NileError else if (isSelected) NileCyan else Color(0x22FFFFFF)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = company.companyName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (company.isActive) "Authorized" else "REVOKED",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = if (company.isActive) NileSuccess else NileError
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = company.notes,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSwitchPersonaDialog = false }) {
                    Text(text = "Close")
                }
            }
        )
    }

    // Modal Sheet: Register / Add Visa/Mastercard
    if (showAddCardDialog) {
        var cardNum by remember { mutableStateOf("") }
        var holder by remember { mutableStateOf("") }
        var expMonth by remember { mutableStateOf("") }
        var expYear by remember { mutableStateOf("") }
        var bankName by remember { mutableStateOf("") }
        var cardType by remember { mutableStateOf(CardType.VISA) }
        var validationError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddCardDialog = false },
            title = { Text(text = "Register Visa / Mastercard", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Encrypted via Android Keystore & Visa/Mastercard Token Service.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (validationError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = validationError!!, color = NileError, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = cardNum,
                        onValueChange = { cardNum = it },
                        label = { Text("16-digit Card Number") },
                        placeholder = { Text("Enter Card Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = holder,
                        onValueChange = { holder = it },
                        label = { Text("Cardholder Name") },
                        placeholder = { Text("Full Name on Card") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = expMonth,
                            onValueChange = { expMonth = it },
                            label = { Text("MM") },
                            placeholder = { Text("MM") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = expYear,
                            onValueChange = { expYear = it },
                            label = { Text("YY") },
                            placeholder = { Text("YY") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("Egyptian Issuing Bank") },
                        placeholder = { Text("e.g. CIB, NBE, Banque Misr, QNB") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanNum = cardNum.replace(" ", "").replace("-", "")
                        if (cleanNum.length < 12) {
                            validationError = "Please enter a valid card number (12-19 digits)."
                            return@Button
                        }
                        if (holder.isBlank()) {
                            validationError = "Please enter the cardholder name."
                            return@Button
                        }
                        if (expMonth.isBlank() || expYear.isBlank()) {
                            validationError = "Please enter expiration month and year."
                            return@Button
                        }
                        if (bankName.isBlank()) {
                            validationError = "Please enter the issuing bank."
                            return@Button
                        }

                        CardRepository.registerCard(
                            cardNumber = cleanNum,
                            holderName = holder,
                            issuerBank = bankName,
                            cardType = cardType,
                            expiryMonth = expMonth,
                            expiryYear = expYear
                        )
                        showAddCardDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NileCyan, contentColor = Color.Black),
                    modifier = Modifier.testTag("confirm_tokenize_card_button")
                ) {
                    Text(text = "Secure Tokenize", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCardDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    // Transaction Details Sheet
    if (selectedTxDetail != null) {
        val tx = selectedTxDetail!!
        AlertDialog(
            onDismissRequest = { selectedTxDetail = null },
            title = { Text(text = tx.title, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(text = "Amount: ${String.format(Locale.US, "%,.2f", tx.amountEGP)} EGP", fontWeight = FontWeight.Black, fontSize = 16.sp, color = NileCyan)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "CBE Reference: ${tx.cbeReference}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NileGold)
                    Text(text = "Auth: ${tx.authMethod}", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    if (tx.notes != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = tx.notes, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedTxDetail = null },
                    colors = ButtonDefaults.buttonColors(containerColor = NileCyan, contentColor = Color.Black)
                ) {
                    Text(text = "Done")
                }
            }
        )
    }

    // Modal Sheet: NFC Card Scan & EMV APDU Extraction
    NfcCardScanDialog(
        isOpen = showNfcScanDialog,
        onDismiss = { showNfcScanDialog = false }
    )

    // Cloud & Local Sync Dialog
    if (showCloudSyncDialog) {
        CloudSyncDialog(
            onDismiss = { showCloudSyncDialog = false }
        )
    }

    // Security Vault & NFC Protection Dialog
    if (showSecurityVaultDialog) {
        SecurityVaultDialog(
            onDismiss = { showSecurityVaultDialog = false }
        )
    }
}

@Composable
fun QuickActionCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CardItem(
    card: CardToken,
    onMakeDefault: () -> Unit,
    onTapToPay: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(260.dp)
            .height(155.dp)
            .clickable { onTapToPay() }
            .testTag("home_card_${card.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (card.cardType) {
                CardType.VISA -> Color(0xFF0F2B48)
                CardType.MASTERCARD -> Color(0xFF2E1A1A)
                CardType.MEEZA -> Color(0xFF0C2B1C)
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (card.isNfcDefault) 1.5.dp else 0.5.dp,
            color = if (card.isNfcDefault) NileCyan else Color(0x33FFFFFF)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = card.issuerBank,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (card.isNfcDefault) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NileCyan)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "NFC DEFAULT",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                }
            }

            Text(
                text = card.dpan,
                fontFamily = FontFamily.Monospace,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "HOLDER", fontSize = 8.sp, color = Color(0xFFAAAAAA))
                    Text(text = card.holderName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Text(
                    text = card.cardType.displayName,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = when (card.cardType) {
                        CardType.VISA -> NileCyan
                        CardType.MASTERCARD -> NileGold
                        CardType.MEEZA -> MeezaGreen
                    }
                )
            }
        }
    }
}
