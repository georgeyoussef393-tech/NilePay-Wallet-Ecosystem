package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TransactionRecord
import com.example.model.TransactionStatus
import com.example.model.TransactionType
import com.example.storage.NilePayStorageManager
import com.example.storage.db.NilePayDatabase
import com.example.ui.components.TransactionItemRow
import com.example.ui.theme.InstaPayPurple
import com.example.ui.theme.NileCyan
import com.example.ui.theme.NileDarkSurface
import com.example.ui.theme.NileError
import com.example.ui.theme.NileGold
import com.example.ui.theme.NileSuccess
import com.example.ui.theme.VodafoneRed
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class TransactionFilterCategory {
    ALL,
    NFC_TAP,
    INSTAPAY,
    MOBILE_WALLET,
    POS_MERCHANT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Query directly from Room Database via NilePayDatabase / NilePayRoomRepository
    val roomDatabase = remember(context) { NilePayDatabase.getDatabase(context) }
    val roomRepo = remember(context) { NilePayStorageManager.getRoomRepository() }

    val transactionsState = if (roomRepo != null) {
        roomRepo.allTransactionsFlow.collectAsState(initial = emptyList())
    } else {
        remember { flowOf(emptyList<TransactionRecord>()) }.collectAsState(initial = emptyList())
    }

    val transactions = transactionsState.value

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(TransactionFilterCategory.ALL) }
    var selectedTxDetail by remember { mutableStateOf<TransactionRecord?>(null) }
    var showSimulatePosTapDialog by remember { mutableStateOf(false) }

    // Filter transactions based on search and category
    val filteredTransactions = transactions.filter { tx ->
        val matchesCategory = when (selectedCategory) {
            TransactionFilterCategory.ALL -> true
            TransactionFilterCategory.NFC_TAP -> tx.type == TransactionType.NFC_TAP_PAY
            TransactionFilterCategory.INSTAPAY -> tx.type == TransactionType.INSTAPAY_P2P || tx.type == TransactionType.INSTAPAY_BANK_TRANSFER
            TransactionFilterCategory.MOBILE_WALLET -> tx.type == TransactionType.VODAFONE_CASH_TRANSFER || tx.type == TransactionType.VODAFONE_CASH_ATM_CASHOUT
            TransactionFilterCategory.POS_MERCHANT -> tx.type == TransactionType.MEEZA_QR_PAY || tx.type == TransactionType.FAWRY_BILL_PAY
        }

        val matchesSearch = if (searchQuery.isBlank()) {
            true
        } else {
            val q = searchQuery.trim().lowercase()
            tx.title.lowercase().contains(q) ||
                    tx.recipient.lowercase().contains(q) ||
                    tx.source.lowercase().contains(q) ||
                    tx.cbeReference.lowercase().contains(q) ||
                    (tx.terminalLocation?.lowercase()?.contains(q) == true)
        }

        matchesCategory && matchesSearch
    }

    val totalSpentEGP = filteredTransactions
        .filter { it.status == TransactionStatus.SUCCESS }
        .sumOf { it.amountEGP }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "سجل المعاملات المشفر (Room DB)",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "مفحوصة وم decrypted مباشرة من Room Database 🛡️",
                            fontSize = 11.sp,
                            color = NileCyan
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("transactions_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSimulatePosTapDialog = true },
                        modifier = Modifier.testTag("add_test_pos_transaction_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "إضافة معاملة تجريبية",
                            tint = NileCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Room Encrypted DB Header Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("room_database_encrypted_banner"),
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(NileCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = NileCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "بيانات Room المخزنة مشفرة بـ AES-256",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(NileSuccess.copy(alpha = 0.2f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "Room SQLite",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NileSuccess
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "إجمالي الاستعلام الحالي: ${filteredTransactions.size} معاملة • ${String.format(Locale.US, "%,.2f", totalSpentEGP)} ج.م",
                                fontSize = 11.sp,
                                color = NileCyan
                            )
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("transactions_search_input"),
                placeholder = {
                    Text(
                        text = "ابحث بالتاجر، اسم التجهيزة، المرجع CBE، أو المصدر...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "بحث",
                        tint = NileCyan,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "مسح",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NileCyan,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )

            // Category Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == TransactionFilterCategory.ALL,
                        onClick = { selectedCategory = TransactionFilterCategory.ALL },
                        label = { Text("الكل (${transactions.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NileCyan,
                            selectedLabelColor = Color.Black
                        ),
                        modifier = Modifier.testTag("filter_chip_all")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedCategory == TransactionFilterCategory.NFC_TAP,
                        onClick = { selectedCategory = TransactionFilterCategory.NFC_TAP },
                        label = { Text("دفع NFC اللاتلامسي", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Nfc, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NileCyan,
                            selectedLabelColor = Color.Black
                        ),
                        modifier = Modifier.testTag("filter_chip_nfc")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedCategory == TransactionFilterCategory.INSTAPAY,
                        onClick = { selectedCategory = TransactionFilterCategory.INSTAPAY },
                        label = { Text("إنستا باي IPN", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = InstaPayPurple,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_chip_instapay")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedCategory == TransactionFilterCategory.MOBILE_WALLET,
                        onClick = { selectedCategory = TransactionFilterCategory.MOBILE_WALLET },
                        label = { Text("المحافظ الإلكترونية", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VodafoneRed,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_chip_wallet")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedCategory == TransactionFilterCategory.POS_MERCHANT,
                        onClick = { selectedCategory = TransactionFilterCategory.POS_MERCHANT },
                        label = { Text("ماكينات التجار POS", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Store, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NileGold,
                            selectedLabelColor = Color.Black
                        ),
                        modifier = Modifier.testTag("filter_chip_pos")
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Transactions List queried directly from Room
            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("empty_search_transactions_box"),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "لا توجد نتائج تطابق \"$searchQuery\"" else "لا توجد معاملات مسجلة لهذا التصنيف",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "تأكد من كتابة الكلمة بشكل صحيح أو اضغط على الزر بالعلوي لإنشاء معاملة POS تجريبية في Room.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showSimulatePosTapDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = NileCyan, contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إضافة معاملة POS تجريبية", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        TransactionItemRow(
                            tx = tx,
                            onClick = { selectedTxDetail = tx }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            }
        }
    }

    // Modal Dialog: Detailed Room Transaction View with Decrypted Metadata
    selectedTxDetail?.let { tx ->
        val dateFormatter = remember { SimpleDateFormat("EEEE, dd MMMM yyyy - hh:mm:ss a", Locale("ar")) }
        val formattedDate = dateFormatter.format(Date(tx.timestamp))

        AlertDialog(
            onDismissRequest = { selectedTxDetail = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (tx.type) {
                                TransactionType.NFC_TAP_PAY -> Icons.Default.Nfc
                                TransactionType.INSTAPAY_P2P, TransactionType.INSTAPAY_BANK_TRANSFER -> Icons.Default.Send
                                TransactionType.VODAFONE_CASH_TRANSFER -> Icons.Default.PhoneAndroid
                                TransactionType.VODAFONE_CASH_ATM_CASHOUT -> Icons.Default.CreditCard
                                else -> Icons.Default.Store
                            },
                            contentDescription = null,
                            tint = NileCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تفاصيل المعاملة (Room DB)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NileSuccess.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tx.status.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NileSuccess
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = tx.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Amount Large Badge
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = NileCyan.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NileCyan.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "المبلغ الإجمالي المدفوع",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${String.format(Locale.US, "%,.2f", tx.amountEGP)} ج.م",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = NileCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Room Decrypted Fields Metadata
                    DetailRow(label = "التاجر / المستلم:", value = tx.recipient)
                    DetailRow(label = "مصدر الدفع:", value = tx.source)
                    DetailRow(
                        label = "مرجع البنك المركزي CBE:",
                        value = tx.cbeReference,
                        isEncryptedRoomField = true
                    )
                    DetailRow(label = "طريقة المصادقة:", value = tx.authMethod)

                    tx.terminalLocation?.let {
                        DetailRow(label = "موقع ماكينة POS:", value = it)
                    }

                    tx.apduCode?.let {
                        DetailRow(label = "كود استجابة APDU:", value = it)
                    }

                    tx.notes?.let {
                        DetailRow(label = "ملاحظات وتفاصيل إضافية:", value = it, isEncryptedRoomField = true)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedTxDetail = null },
                    colors = ButtonDefaults.buttonColors(containerColor = NileCyan, contentColor = Color.Black),
                    modifier = Modifier.testTag("close_tx_detail_dialog")
                ) {
                    Text("إغلاق", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Modal Dialog: Add Simulated POS Tap Transaction to Room DB
    if (showSimulatePosTapDialog) {
        var merchantName by remember { mutableStateOf("Fawry Retail POS - Zamalek") }
        var amountText by remember { mutableStateOf("285") }
        var selectedSourceCard by remember { mutableStateOf("Visa •••• 4829 (CIB Egypt)") }

        AlertDialog(
            onDismissRequest = { showSimulatePosTapDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Nfc, contentDescription = null, tint = NileCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "محاكاة دفع NFC عبر ماكينة POS", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "سيتم إضافة المعاملة مباشرة وتشفير حقولها الحساسة بـ AES-256 وحفظها في قاعدة بيانات Room المحفوطة بالجهاز.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = merchantName,
                        onValueChange = { merchantName = it },
                        label = { Text("اسم التاجر / ماكينة POS", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("المبلغ بالجنية المصري (EGP)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedAmount = amountText.toDoubleOrNull() ?: 100.0
                        val newTx = TransactionRecord(
                            id = "tx-room-${UUID.randomUUID().toString().take(8)}",
                            title = "$merchantName (NFC Tap)",
                            type = TransactionType.NFC_TAP_PAY,
                            amountEGP = parsedAmount,
                            recipient = merchantName,
                            source = selectedSourceCard,
                            timestamp = System.currentTimeMillis(),
                            status = TransactionStatus.SUCCESS,
                            cbeReference = "CBE-ROOM-${(1000000..9999999).random()}",
                            authMethod = "HCE_EMV_CRYPTOGRAM_ROOM_AES256",
                            terminalLocation = "Cairo, Egypt",
                            apduCode = "0x9000_SW_OK",
                            notes = "معاملة Room محاكية ومحفوظة بنجاح."
                        )

                        scope.launch {
                            roomRepo?.saveTransaction(newTx)
                            // Also notify storage manager
                            NilePayStorageManager.saveTransactions(transactions + newTx)
                        }

                        showSimulatePosTapDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NileCyan, contentColor = Color.Black),
                    modifier = Modifier.testTag("submit_simulated_pos_transaction_button")
                ) {
                    Text("حفظ المعاملة في Room", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSimulatePosTapDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isEncryptedRoomField: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isEncryptedRoomField) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "AES-256 Room Encrypted Field",
                    tint = NileCyan,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
