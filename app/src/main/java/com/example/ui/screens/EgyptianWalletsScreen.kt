package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Atm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EgyptianWalletAccount
import com.example.model.FeaturePermission
import com.example.model.WalletProvider
import com.example.security.AccessControlManager
import com.example.ui.theme.EtisalatGreen
import com.example.ui.theme.InstaPayPurple
import com.example.ui.theme.NileCyan
import com.example.ui.theme.NileError
import com.example.ui.theme.NileGold
import com.example.ui.theme.NileSuccess
import com.example.ui.theme.OrangeCash
import com.example.ui.theme.VodafoneRed
import com.example.ui.theme.WePayPurple
import com.example.wallets.EgyptianWalletEngine
import com.example.wallets.WalletOperationResult
import java.util.Locale

@Composable
fun EgyptianWalletsScreen(
    onNavigateBack: () -> Unit
) {
    val wallets by EgyptianWalletEngine.wallets.collectAsState()
    val activeCompany by AccessControlManager.activeCompany.collectAsState()

    var selectedWalletId by remember { mutableStateOf(wallets.firstOrNull()?.id.orEmpty()) }
    val selectedWallet = wallets.find { it.id == selectedWalletId } ?: wallets.firstOrNull()

    // Dialog state for Transfer
    var showTransferSheet by remember { mutableStateOf(false) }
    var transferTargetPhone by remember { mutableStateOf("") }
    var transferAmount by remember { mutableStateOf("") }
    var transferPin by remember { mutableStateOf("") }

    // Dialog state for ATM Cash-Out
    var showAtmDialog by remember { mutableStateOf(false) }
    var atmAmount by remember { mutableStateOf("") }
    var generatedAtmOtp by remember { mutableStateOf<String?>(null) }

    // Dialog state for Linking a New Wallet
    var showLinkWalletDialog by remember { mutableStateOf(false) }
    var linkProvider by remember { mutableStateOf(WalletProvider.VODAFONE_CASH) }
    var linkIdentifier by remember { mutableStateOf("") }
    var linkTitle by remember { mutableStateOf("") }
    var linkNationalId by remember { mutableStateOf("") }
    var linkInitialBalance by remember { mutableStateOf("") }

    // Dialog state for Top-up Wallet
    var showTopupDialog by remember { mutableStateOf(false) }
    var topupAmountInput by remember { mutableStateOf("") }

    // Feedback message
    var bannerMessage by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val hasWalletPerm = activeCompany.isActive && activeCompany.grantedPermissions.contains(FeaturePermission.EGYPTIAN_MOBILE_WALLETS)
    val hasCashoutPerm = activeCompany.isActive && activeCompany.grantedPermissions.contains(FeaturePermission.VODAFONE_CASH_CASHOUT)
    val hasInstapayPerm = activeCompany.isActive && activeCompany.grantedPermissions.contains(FeaturePermission.INSTAPAY_TRANSFERS)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        // Header
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("wallets_back_button")
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
                        text = "Egyptian Mobile Wallets",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Vodafone Cash • InstaPay IPN • Meeza Gateway",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Notification Banner
        if (bannerMessage != null) {
            item {
                val (isSuccess, text) = bannerMessage!!
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSuccess) NileSuccess.copy(alpha = 0.15f) else NileError.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSuccess) NileSuccess else NileError
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (isSuccess) NileSuccess else NileError,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = text,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                        IconButton(
                            onClick = { bannerMessage = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Policy Guard Warning if Egyptian Wallets are disabled
        if (!hasWalletPerm) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .testTag("wallets_policy_blocked_card"),
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
                                text = "Wallet Access Revoked for Testing Group",
                                fontWeight = FontWeight.Bold,
                                color = NileError,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Permission 'EGYPTIAN_MOBILE_WALLETS' is currently disabled for '${activeCompany.companyName}'.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Wallet Accounts List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LINKED EGYPTIAN WALLET ACCOUNTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NileCyan,
                    letterSpacing = 1.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (wallets.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                EgyptianWalletEngine.clearAllWalletsAndTransactions()
                                bannerMessage = Pair(true, "تم مسح وتفريغ كافّة المحافظ والمعاملات الافتراضية.")
                            },
                            modifier = Modifier.testTag("clear_all_wallets_button")
                        ) {
                            Text(text = "مسح المحافظ", color = NileError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Button(
                        onClick = { showLinkWalletDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NileCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("open_link_wallet_button")
                    ) {
                        Text(text = "+ Link Wallet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (wallets.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLinkWalletDialog = true }
                        .testTag("empty_wallets_banner"),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = NileCyan,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Egyptian Mobile Wallets Linked",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap here to link your Vodafone Cash, InstaPay (Meeza IPN), Orange, Etisalat, or WE Pay account.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(wallets) { wallet ->
                val isSelected = wallet.id == selectedWalletId
                val brandColor = Color(wallet.provider.colorHex)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clickable { selectedWalletId = wallet.id }
                        .testTag("wallet_account_${wallet.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        }
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 1.5.dp else 0.5.dp,
                        color = if (isSelected) brandColor else Color(0x22FFFFFF)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(brandColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = wallet.provider.title.take(2).uppercase(),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = brandColor
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = wallet.accountTitle,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${wallet.identifier} • ${wallet.provider.serviceCode}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Link Status toggle & Remove
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { EgyptianWalletEngine.toggleWalletLink(wallet.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (wallet.isLinked) Icons.Default.Link else Icons.Default.LinkOff,
                                        contentDescription = "Toggle Link",
                                        tint = if (wallet.isLinked) NileSuccess else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { EgyptianWalletEngine.removeWallet(wallet.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove Wallet",
                                        tint = NileError,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "AVAILABLE BALANCE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%,.2f", wallet.balanceEGP)} EGP",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (wallet.isLinked) Color.White else Color.Gray
                                )
                            }

                            Button(
                                onClick = {
                                    selectedWalletId = wallet.id
                                    showTopupDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00382B),
                                    contentColor = NileSuccess
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(text = "+ Top Up", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Actions for Selected Wallet
        if (selectedWallet != null) {
            item {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "WALLET QUICK ACTIONS (${selectedWallet.provider.title})",
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
                    // Send Money
                    Button(
                        onClick = { showTransferSheet = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("wallet_send_money_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(selectedWallet.provider.colorHex)
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Send Money", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    // ATM Cash-Out (Especially Vodafone Cash)
                    Button(
                        onClick = { showAtmDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("wallet_atm_cashout_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = NileCyan
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NileCyan)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Atm, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "ATM Cash-Out", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Inline Transfer Form
        if (showTransferSheet && selectedWallet != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .testTag("transfer_form_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NileCyan)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Send from ${selectedWallet.provider.title}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = { showTransferSheet = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (selectedWallet.provider == WalletProvider.INSTAPAY_MEEZA) {
                                "RECIPIENT INSTAPAY ADDRESS OR PHONE"
                            } else {
                                "RECIPIENT EGYPTIAN MOBILE NUMBER (010, 011, 012, 015)"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = transferTargetPhone,
                            onValueChange = { transferTargetPhone = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("transfer_recipient_input"),
                            singleLine = true,
                            placeholder = { Text("e.g. 01091234567 or user@instapay") }
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "AMOUNT (EGP)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = transferAmount,
                            onValueChange = { transferAmount = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("transfer_amount_input"),
                            singleLine = true,
                            trailingIcon = { Text("EGP", color = NileCyan, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp)) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "6-DIGIT WALLET SECURITY PIN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = transferPin,
                            onValueChange = { transferPin = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("transfer_pin_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val amt = transferAmount.toDoubleOrNull() ?: 100.0
                                val result = if (selectedWallet.provider == WalletProvider.INSTAPAY_MEEZA) {
                                    EgyptianWalletEngine.sendInstaPayTransfer(
                                        sourceWalletId = selectedWallet.id,
                                        targetIpaOrAccount = transferTargetPhone,
                                        amountEGP = amt,
                                        notes = "Instant P2P Transfer"
                                    )
                                } else {
                                    EgyptianWalletEngine.sendMobileWalletTransfer(
                                        walletId = selectedWallet.id,
                                        targetPhone = transferTargetPhone,
                                        amountEGP = amt,
                                        pin = transferPin
                                    )
                                }

                                when (result) {
                                    is WalletOperationResult.Success -> {
                                        bannerMessage = Pair(true, "Transfer of ${String.format(Locale.US, "%,.2f", amt)} EGP settled! Ref: ${result.transaction.cbeReference}")
                                        showTransferSheet = false
                                    }
                                    is WalletOperationResult.Error -> {
                                        bannerMessage = Pair(false, result.message)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("confirm_transfer_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = NileCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = "CONFIRM TRANSACTION", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        // Inline ATM Cash-Out Generator
        if (showAtmDialog && selectedWallet != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .testTag("atm_cashout_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E33)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NileGold)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Atm, contentDescription = null, tint = NileGold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ATM Cash-Out Code Generator",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                            }
                            IconButton(onClick = { showAtmDialog = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "WITHDRAWAL AMOUNT (EGP)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NileGold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = atmAmount,
                            onValueChange = { atmAmount = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = { Text("EGP", color = NileGold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp)) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val amt = atmAmount.toDoubleOrNull() ?: 500.0
                                val result = EgyptianWalletEngine.requestAtmCashout(
                                    walletId = selectedWallet.id,
                                    amountEGP = amt
                                )
                                when (result) {
                                    is WalletOperationResult.Success -> {
                                        generatedAtmOtp = result.extraInfo
                                        bannerMessage = Pair(true, "ATM OTP Generated successfully!")
                                    }
                                    is WalletOperationResult.Error -> {
                                        bannerMessage = Pair(false, result.message)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("generate_atm_otp_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = NileGold, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = "GENERATE 6-DIGIT ATM OTP", fontWeight = FontWeight.Bold)
                        }

                        if (generatedAtmOtp != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E3557)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "ATM WITHDRAWAL OTP CODE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NileGold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = generatedAtmOtp!!,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        letterSpacing = 4.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Valid for 30 minutes at any NBE, Banque Misr, or CIB ATM without a physical card. Select 'Cardless Services' > 'Smart Wallet'.",
                                        fontSize = 11.sp,
                                        color = Color(0xFFCFD8DC),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Modal Dialog: Link New Wallet Account
    if (showLinkWalletDialog) {
        var linkError by remember { mutableStateOf<String?>(null) }
        val providers = listOf(
            WalletProvider.VODAFONE_CASH,
            WalletProvider.INSTAPAY_MEEZA,
            WalletProvider.ORANGE_MONEY,
            WalletProvider.ETISALAT_CASH,
            WalletProvider.WE_PAY
        )

        AlertDialog(
            onDismissRequest = { showLinkWalletDialog = false },
            title = {
                Text(text = "Link Egyptian Mobile Wallet", fontWeight = FontWeight.Bold)
            },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        Text(
                            text = "SELECT PROVIDER",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NileCyan
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            providers.forEach { p ->
                                val isChosen = linkProvider == p
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { linkProvider = p },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isChosen) Color(p.colorHex).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isChosen) Color(p.colorHex) else Color(0x22FFFFFF)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = p.title, fontSize = 12.sp, fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal, color = Color.White)
                                        Text(text = p.serviceCode, fontSize = 10.sp, color = NileCyan)
                                    }
                                }
                            }
                        }

                        if (linkError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = linkError!!, color = NileError, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (linkProvider == WalletProvider.INSTAPAY_MEEZA) "INSTAPAY ADDRESS (e.g. name@instapay)" else "EGYPTIAN MOBILE (010, 011, 012, 015)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = linkIdentifier,
                            onValueChange = { linkIdentifier = it },
                            placeholder = { Text(if (linkProvider == WalletProvider.INSTAPAY_MEEZA) "yourname@instapay" else "010xxxxxxxx") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ACCOUNT TITLE / LABEL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = linkTitle,
                            onValueChange = { linkTitle = it },
                            placeholder = { Text("e.g. My Primary Vodafone Wallet") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "14-DIGIT NATIONAL ID",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = linkNationalId,
                            onValueChange = { linkNationalId = it },
                            placeholder = { Text("Egyptian National ID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "INITIAL DEPOSIT BALANCE (EGP)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = linkInitialBalance,
                            onValueChange = { linkInitialBalance = it },
                            placeholder = { Text("e.g. 1000.00") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanId = linkIdentifier.trim()
                        if (cleanId.isBlank()) {
                            linkError = "Please enter your mobile number or InstaPay address."
                            return@Button
                        }
                        val title = linkTitle.ifBlank { "${linkProvider.title} Account" }
                        val bal = linkInitialBalance.toDoubleOrNull() ?: 0.0

                        val newAcc = EgyptianWalletEngine.linkWallet(
                            provider = linkProvider,
                            identifier = cleanId,
                            accountTitle = title,
                            nationalId = linkNationalId,
                            initialBalance = bal
                        )
                        selectedWalletId = newAcc.id
                        bannerMessage = Pair(true, "Successfully linked ${linkProvider.title}!")
                        showLinkWalletDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NileCyan, contentColor = Color.Black),
                    modifier = Modifier.testTag("confirm_link_wallet_button")
                ) {
                    Text(text = "Link Account", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkWalletDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    // Modal Dialog: Top Up Balance
    if (showTopupDialog && selectedWallet != null) {
        AlertDialog(
            onDismissRequest = { showTopupDialog = false },
            title = {
                Text(text = "Top-Up ${selectedWallet.provider.title}", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Deposit funds into wallet from bank account or Fawry outlet:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = topupAmountInput,
                        onValueChange = { topupAmountInput = it },
                        label = { Text("Amount in EGP") },
                        placeholder = { Text("e.g. 500.00") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = { Text("EGP", color = NileCyan, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp)) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = topupAmountInput.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            EgyptianWalletEngine.topupWallet(selectedWallet.id, amt)
                            bannerMessage = Pair(true, "Deposited ${String.format(Locale.US, "%,.2f", amt)} EGP into ${selectedWallet.provider.title}!")
                            showTopupDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NileCyan, contentColor = Color.Black)
                ) {
                    Text(text = "Deposit EGP", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTopupDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }
}
