package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cards.CardRepository
import com.example.model.CardToken
import com.example.model.CardType
import com.example.nfc.NfcScanManager
import com.example.ui.theme.MeezaGreen
import com.example.ui.theme.NileCyan
import com.example.ui.theme.NileError
import com.example.ui.theme.NileGold
import com.example.ui.theme.NileSuccess

@Composable
fun NfcCardScanDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onCardTokenized: (CardToken) -> Unit = {}
) {
    if (!isOpen) return

    val scannedCard by NfcScanManager.scannedCard.collectAsState()
    val isScanning by NfcScanManager.isScanning.collectAsState()
    val scanStatus by NfcScanManager.scanStatus.collectAsState()
    val scanError by NfcScanManager.scanError.collectAsState()

    var showApduLogs by remember { mutableStateOf(false) }

    // Pulsing animation for NFC reader antenna
    val infiniteTransition = rememberInfiniteTransition(label = "nfcRadar")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    AlertDialog(
        onDismissRequest = {
            NfcScanManager.stopScanning()
            NfcScanManager.clearResult()
            onDismiss()
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Nfc,
                    contentDescription = null,
                    tint = NileCyan,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = if (scannedCard == null) "NFC Contactless Card Reader" else "Card Credentials Discovered",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (scannedCard == null) {
                    // Scanning State
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Radar outer circle
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(NileCyan.copy(alpha = 0.12f))
                                    .border(1.5.dp, NileCyan.copy(alpha = 0.4f), CircleShape)
                            )
                            // Inner core
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(NileCyan, Color(0xFF006874))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Nfc,
                                    contentDescription = "NFC Active",
                                    tint = Color.Black,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }
                    }

                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Ready to Read IsoDep ISO 14443-4",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = NileCyan
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = scanStatus,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (scanError != null) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = NileError.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NileError)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = NileError,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = scanError!!,
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Test Simulation Presets for environments without physical contactless cards
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Or Test With Contactless Card Presets:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    NfcScanManager.simulateCardTap(
                                        samplePan = "4211890123456789",
                                        sampleHolder = "MAHMOUD EL-SHARKAWY",
                                        sampleBank = "Commercial International Bank (CIB)",
                                        sampleType = CardType.VISA,
                                        sampleExpMonth = "10",
                                        sampleExpYear = "29"
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("simulate_tap_cib_visa"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "CIB Visa Platinum (4211 •••• 6789)", fontSize = 11.sp)
                                    Text(text = "VISA", color = NileCyan, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    NfcScanManager.simulateCardTap(
                                        samplePan = "5234901234561042",
                                        sampleHolder = "FATMA HASSAN ABDELLATIF",
                                        sampleBank = "Banque Misr",
                                        sampleType = CardType.MASTERCARD,
                                        sampleExpMonth = "08",
                                        sampleExpYear = "28"
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("simulate_tap_banque_misr_mc"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "Banque Misr World MC (5234 •••• 1042)", fontSize = 11.sp)
                                    Text(text = "MASTERCARD", color = NileGold, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    NfcScanManager.simulateCardTap(
                                        samplePan = "5078129087654321",
                                        sampleHolder = "OMAR KHALED IBRAHIM",
                                        sampleBank = "National Bank of Egypt (NBE)",
                                        sampleType = CardType.MEEZA,
                                        sampleExpMonth = "12",
                                        sampleExpYear = "30"
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("simulate_tap_meeza_nbe"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "NBE Meeza Card (5078 •••• 4321)", fontSize = 11.sp)
                                    Text(text = "MEEZA", color = MeezaGreen, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                } else {
                    // Card Successfully Read State
                    val card = scannedCard!!
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when (card.cardType) {
                                    CardType.VISA -> Color(0xFF0F2B48)
                                    CardType.MASTERCARD -> Color(0xFF2E1A1A)
                                    CardType.MEEZA -> Color(0xFF0C2B1C)
                                }
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, NileCyan)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = card.issuerBank,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
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

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "${card.pan.take(4)} •••• •••• ${card.last4}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 2.sp
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = "CARDHOLDER", fontSize = 8.sp, color = Color(0xFFAAAAAA))
                                        Text(text = card.holderName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = "EXPIRES", fontSize = 8.sp, color = Color(0xFFAAAAAA))
                                        Text(text = "${card.expiryMonth}/${card.expiryYear}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    // Security & EMV details
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = NileCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "EMV ISO 7816 Credentials Extracted",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = NileCyan
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "EMV AID: ${card.aid}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Tokenization Standard: PCI-DSS & EMVCo Token Service",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // APDU Exchange logs toggle
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showApduLogs = !showApduLogs },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "APDU Exchange Trace (${card.apduExchangeLogs.size} frames)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    imageVector = if (showApduLogs) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (showApduLogs) {
                        items(card.apduExchangeLogs) { log ->
                            Text(
                                text = log,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = if (log.contains("CMD")) NileCyan else if (log.contains("RSP")) NileGold else Color.White
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (scannedCard != null) {
                Button(
                    onClick = {
                        val card = scannedCard!!
                        val registered = CardRepository.registerCard(
                            cardNumber = card.pan,
                            holderName = card.holderName,
                            issuerBank = card.issuerBank,
                            cardType = card.cardType,
                            expiryMonth = card.expiryMonth,
                            expiryYear = card.expiryYear
                        )
                        NfcScanManager.stopScanning()
                        NfcScanManager.clearResult()
                        onCardTokenized(registered)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NileCyan,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.testTag("confirm_tokenize_scanned_card_button")
                ) {
                    Text(text = "Tokenize & Save to Vault", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (scannedCard != null) {
                TextButton(
                    onClick = {
                        NfcScanManager.clearResult()
                        NfcScanManager.startScanning()
                    }
                ) {
                    Text(text = "Scan Another")
                }
            } else {
                TextButton(
                    onClick = {
                        NfcScanManager.stopScanning()
                        NfcScanManager.clearResult()
                        onDismiss()
                    }
                ) {
                    Text(text = "Cancel")
                }
            }
        }
    )
}
