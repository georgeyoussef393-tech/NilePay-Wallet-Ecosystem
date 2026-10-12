package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cards.CardRepository
import com.example.cards.NfcPaymentResult
import com.example.model.CardToken
import com.example.model.CardType
import com.example.model.FeaturePermission
import com.example.nfc.NfcApduMonitor
import com.example.nfc.NfcScanManager
import com.example.security.AccessControlManager
import com.example.ui.components.NfcCardScanDialog
import com.example.ui.components.SecurityVaultDialog
import com.example.ui.theme.MeezaGreen
import com.example.ui.theme.NileCyan
import com.example.ui.theme.NileError
import com.example.ui.theme.NileGold
import com.example.ui.theme.NilePrimary
import com.example.ui.theme.NileSuccess
import java.util.Locale

@Composable
fun NfcPaymentScreen(
    onNavigateBack: () -> Unit
) {
    val cards by CardRepository.cards.collectAsState()
    val defaultCard by CardRepository.defaultNfcCard.collectAsState()
    val activeCompany by AccessControlManager.activeCompany.collectAsState()
    val apduLogs by NfcApduMonitor.logs.collectAsState()

    var selectedCardId by remember { mutableStateOf(defaultCard?.id ?: cards.firstOrNull()?.id.orEmpty()) }
    val selectedCard = cards.find { it.id == selectedCardId } ?: cards.firstOrNull()

    // POS Simulator state
    val merchants = listOf(
        Pair("Fawry Smart POS - Cairo Festival City", "POS-FWY-8821"),
        Pair("Carrefour Hypermarket - Maadi", "POS-CRF-4412"),
        Pair("Costa Coffee - Zamalek", "POS-CST-1099"),
        Pair("On The Run - New Cairo", "POS-OTR-7312")
    )
    var selectedMerchant by remember { mutableStateOf(merchants[0]) }
    var txAmountInput by remember { mutableStateOf("150.00") }
    var isSimulatingTap by remember { mutableStateOf(false) }
    var lastPaymentResult by remember { mutableStateOf<NfcPaymentResult?>(null) }
    var showApduDetails by remember { mutableStateOf(true) }
    var showScanCardDialog by remember { mutableStateOf(false) }
    var showSecurityVaultDialog by remember { mutableStateOf(false) }

    val hasNfcPermission = activeCompany.isActive && activeCompany.grantedPermissions.contains(FeaturePermission.NFC_CONTACTLESS_PAY)

    // Contactless pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "nfcPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        // Top Bar
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("nfc_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Contactless NFC Payment",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Host Card Emulation (HCE) ISO 7816-4",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Security Shield Action Button
                Surface(
                    onClick = { showSecurityVaultDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.testTag("nfc_open_security_vault_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "NFC Security",
                            tint = NileCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "الأمان",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NileCyan
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Access Policy Notice if NFC is blocked for current testing tenant
        if (!hasNfcPermission) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .testTag("nfc_policy_blocked_banner"),
                    colors = CardDefaults.cardColors(containerColor = NileError.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NileError)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = NileError,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "NFC Access Restricted by Owner",
                                fontWeight = FontWeight.Bold,
                                color = NileError,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Feature 'NFC_CONTACTLESS_PAY' is revoked for '${activeCompany.companyName}'. POS terminals will receive APDU 69 82.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Active Card Presentation
        item {
            Text(
                text = "SELECT PAYMENT CARD",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = NileCyan,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (cards.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("nfc_empty_cards_banner"),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = NileCyan,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Cards Available for NFC Emulation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Scan your contactless Visa or Mastercard via NFC, or add details to start paying at POS terminals.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                NfcScanManager.startScanning()
                                showScanCardDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NileCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("nfc_screen_scan_card_button")
                        ) {
                            Icon(imageVector = Icons.Default.Nfc, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Scan Contactless Card (NFC)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cards) { card ->
                        val isSelected = card.id == selectedCardId
                        Card(
                            modifier = Modifier
                                .width(280.dp)
                                .height(170.dp)
                                .clickable {
                                    selectedCardId = card.id
                                    CardRepository.setDefaultCard(card.id)
                                }
                                .testTag("card_token_${card.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when (card.cardType) {
                                    CardType.VISA -> Color(0xFF0F2B48)
                                    CardType.MASTERCARD -> Color(0xFF2E1A1A)
                                    CardType.MEEZA -> Color(0xFF0C2B1C)
                                }
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) NileCyan else Color(0x33FFFFFF)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = card.issuerBank,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Contactless,
                                            contentDescription = "Contactless",
                                            tint = NileCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = card.cardType.displayName,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            color = when (card.cardType) {
                                                CardType.VISA -> NileCyan
                                                CardType.MASTERCARD -> NileGold
                                                CardType.MEEZA -> MeezaGreen
                                            }
                                        )
                                    }
                                }

                                // Chip & DPAN
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp, 24.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFE0C070))
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = card.dpan,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "CARDHOLDER",
                                            fontSize = 9.sp,
                                            color = Color(0xFFAAAAAA)
                                        )
                                        Text(
                                            text = card.holderName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "EXPIRES",
                                            fontSize = 9.sp,
                                            color = Color(0xFFAAAAAA)
                                        )
                                        Text(
                                            text = "${card.expiryMonth}/${card.expiryYear}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Interactive POS Terminal Simulator
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pos_terminal_simulator_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, NileCyan.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = NileCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "POS Terminal Simulator",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NileCyan.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "CBE POS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NileCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Merchant selector chips
                    Text(
                        text = "SELECT MERCHANT TERMINAL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        merchants.forEach { m ->
                            val isChosen = m.second == selectedMerchant.second
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedMerchant = m }
                                    .testTag("select_merchant_${m.second}"),
                                shape = RoundedCornerShape(8.dp),
                                color = if (isChosen) NileCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isChosen) NileCyan else Color(0x22FFFFFF)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = m.first,
                                        fontSize = 12.sp,
                                        fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = m.second,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = NileCyan
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Amount input & Presets
                    Text(
                        text = "TRANSACTION AMOUNT (EGP)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = txAmountInput,
                        onValueChange = { txAmountInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pos_amount_input"),
                        singleLine = true,
                        trailingIcon = {
                            Text(
                                text = "EGP",
                                fontWeight = FontWeight.Bold,
                                color = NileCyan,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("50.00", "150.00", "500.00", "1500.00").forEach { preset ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { txAmountInput = preset },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "$preset", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Contactless Tap Action Button
                    Button(
                        onClick = {
                            if (selectedCard != null) {
                                val amt = txAmountInput.toDoubleOrNull() ?: 150.0
                                isSimulatingTap = true
                                val result = CardRepository.processNfcTapPayment(
                                    cardId = selectedCard.id,
                                    merchantName = selectedMerchant.first,
                                    merchantCategory = "Retail Merchant",
                                    terminalId = selectedMerchant.second,
                                    amountEGP = amt
                                )
                                lastPaymentResult = result
                                isSimulatingTap = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("tap_to_pay_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasNfcPermission) NileCyan else Color.Gray,
                            contentColor = Color.Black
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .scale(if (isSimulatingTap) pulseScale else 1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Nfc,
                                    contentDescription = "Tap to Pay",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "TAP PHONE TO POS TERMINAL",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Payment Result Card
        if (lastPaymentResult != null) {
            item {
                when (val result = lastPaymentResult) {
                    is NfcPaymentResult.Success -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("nfc_payment_success_card"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = NileSuccess.copy(alpha = 0.15f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NileSuccess)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = NileSuccess,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Payment Authorized & Settled",
                                        fontWeight = FontWeight.Bold,
                                        color = NileSuccess,
                                        fontSize = 15.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Amount: ${String.format(Locale.US, "%,.2f", result.transaction.amountEGP)} EGP",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Merchant: ${result.transaction.recipient}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "CBE Settlement Ref: ${result.transaction.cbeReference}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = NileCyan
                                )
                                Text(
                                    text = "EMV ARQC Cryptogram: ${result.cryptogramHex}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = NileGold
                                )
                            }
                        }
                    }
                    is NfcPaymentResult.Blocked -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("nfc_payment_blocked_card"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = NileError.copy(alpha = 0.2f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NileError)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = NileError,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Declined by Dynamic Access Control",
                                        fontWeight = FontWeight.Bold,
                                        color = NileError,
                                        fontSize = 15.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = result.message,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Status Word Sent to POS: 69 82 (Security Condition Not Satisfied)",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = NileError
                                )
                            }
                        }
                    }
                    is NfcPaymentResult.Failure -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = NileError.copy(alpha = 0.2f))
                        ) {
                            Text(text = result.error, modifier = Modifier.padding(14.dp), color = NileError)
                        }
                    }
                    null -> {}
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Live APDU Exchange Inspector
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EMV APDU TELEMETRY MONITOR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NileCyan,
                    letterSpacing = 1.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { NfcApduMonitor.clearLogs() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (apduLogs.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = "No APDU commands exchanged yet. Tap phone to POS to observe live ISO 7816-4 handshake.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    apduLogs.forEach { log ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF040A12),
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                if (log.isSuccess) Color(0x3300C2CB) else NileError.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = log.commandName,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (log.isSuccess) NileCyan else NileError
                                    )
                                    Text(
                                        text = log.statusWord,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = if (log.isSuccess) NileSuccess else NileError
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "CMD: ${log.commandHex}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = Color(0xFF90A4AE)
                                )
                                Text(
                                    text = "RSP: ${log.responseHex}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = Color(0xFFCFD8DC)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    NfcCardScanDialog(
        isOpen = showScanCardDialog,
        onDismiss = { showScanCardDialog = false },
        onCardTokenized = { tokenized ->
            selectedCardId = tokenized.id
        }
    )

    if (showSecurityVaultDialog) {
        SecurityVaultDialog(
            onDismiss = { showSecurityVaultDialog = false }
        )
    }
}
