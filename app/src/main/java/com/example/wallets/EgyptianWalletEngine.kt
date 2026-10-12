package com.example.wallets

import com.example.model.EgyptianWalletAccount
import com.example.model.FeaturePermission
import com.example.model.TransactionRecord
import com.example.model.TransactionStatus
import com.example.model.TransactionType
import com.example.model.WalletProvider
import com.example.security.AccessControlManager
import com.example.security.CryptoTokenManager
import com.example.security.PermissionCheckResult
import com.example.storage.CloudSyncManager
import com.example.storage.NilePayStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

sealed class WalletOperationResult {
    data class Success(val transaction: TransactionRecord, val extraInfo: String? = null) : WalletOperationResult()
    data class Error(val message: String) : WalletOperationResult()
}

object EgyptianWalletEngine {

    private val _wallets = MutableStateFlow<List<EgyptianWalletAccount>>(emptyList())
    val wallets: StateFlow<List<EgyptianWalletAccount>> = _wallets.asStateFlow()

    private val _recentTransactions = MutableStateFlow<List<TransactionRecord>>(emptyList())
    val recentTransactions: StateFlow<List<TransactionRecord>> = _recentTransactions.asStateFlow()

    fun loadFromStorageOrSeed() {
        val loadedWallets = NilePayStorageManager.loadWallets()
        val loadedTransactions = NilePayStorageManager.loadTransactions()

        if (!loadedWallets.isNullOrEmpty()) {
            _wallets.value = loadedWallets
            _recentTransactions.value = loadedTransactions ?: emptyList()
        } else {
            // Seed realistic Egyptian digital mobile wallets and InstaPay accounts
            val seedWallets = listOf(
                EgyptianWalletAccount(
                    id = "w-vf-01",
                    provider = WalletProvider.VODAFONE_CASH,
                    accountTitle = "George Youssef (Vodafone Cash)",
                    identifier = "010 9876 5432",
                    nationalIdMasked = "294•••••••••18",
                    balanceEGP = 4850.0,
                    isLinked = true,
                    isVerifiedByCbe = true
                ),
                EgyptianWalletAccount(
                    id = "w-ipn-02",
                    provider = WalletProvider.INSTAPAY_MEEZA,
                    accountTitle = "George Youssef (InstaPay IPN)",
                    identifier = "george@instapay",
                    nationalIdMasked = "294•••••••••18",
                    balanceEGP = 24500.0,
                    isLinked = true,
                    isVerifiedByCbe = true
                ),
                EgyptianWalletAccount(
                    id = "w-og-03",
                    provider = WalletProvider.ORANGE_MONEY,
                    accountTitle = "George Youssef (Orange Cash)",
                    identifier = "012 3456 7890",
                    nationalIdMasked = "294•••••••••18",
                    balanceEGP = 1750.0,
                    isLinked = true,
                    isVerifiedByCbe = true
                )
            )

            val seedTransactions = listOf(
                TransactionRecord(
                    id = "tx-seed-1",
                    title = "Starbucks Zamalek (NFC Tap)",
                    type = TransactionType.NFC_TAP_PAY,
                    amountEGP = 145.0,
                    recipient = "Starbucks Zamalek (POS-4921)",
                    source = "Visa •••• 4829 (CIB Egypt)",
                    timestamp = System.currentTimeMillis() - 3600000 * 2,
                    status = TransactionStatus.SUCCESS,
                    cbeReference = "CBE-NFC-8492019",
                    authMethod = "HCE_EMV_CRYPTOGRAM_A8F921",
                    terminalLocation = "Zamalek, Cairo"
                ),
                TransactionRecord(
                    id = "tx-seed-2",
                    title = "InstaPay Transfer to Kareem Maher",
                    type = TransactionType.INSTAPAY_P2P,
                    amountEGP = 850.0,
                    recipient = "kareem@instapay",
                    source = "InstaPay (george@instapay)",
                    timestamp = System.currentTimeMillis() - 3600000 * 8,
                    status = TransactionStatus.SUCCESS,
                    cbeReference = "CBE-IPN-3928174",
                    authMethod = "MEEZA_IPN_SECURE_TOKEN",
                    notes = "Dinner settlement."
                ),
                TransactionRecord(
                    id = "tx-seed-3",
                    title = "Vodafone Cash Bill Pay",
                    type = TransactionType.VODAFONE_CASH_TRANSFER,
                    amountEGP = 220.0,
                    recipient = "010 1234 5678",
                    source = "Vodafone Cash (010 9876 5432)",
                    timestamp = System.currentTimeMillis() - 86400000,
                    status = TransactionStatus.SUCCESS,
                    cbeReference = "CBE-VF-7193021",
                    authMethod = "WALLET_PIN_SDF"
                ),
                TransactionRecord(
                    id = "tx-seed-4",
                    title = "Carrefour Maadi City Center (NFC Tap)",
                    type = TransactionType.NFC_TAP_PAY,
                    amountEGP = 1420.0,
                    recipient = "Carrefour (POS-8820 - Hypermarket)",
                    source = "Mastercard •••• 9142 (NBE)",
                    timestamp = System.currentTimeMillis() - 86400000 * 2,
                    status = TransactionStatus.SUCCESS,
                    cbeReference = "CBE-NFC-4491022",
                    authMethod = "HCE_EMV_CRYPTOGRAM_C41890",
                    terminalLocation = "Maadi, Cairo"
                )
            )

            _wallets.value = seedWallets
            _recentTransactions.value = seedTransactions
            persistAndSync()
        }
    }

    fun applyRestoredWallets(restoredWallets: List<EgyptianWalletAccount>, restoredTx: List<TransactionRecord>) {
        _wallets.value = restoredWallets
        _recentTransactions.value = restoredTx
        NilePayStorageManager.saveWallets(_wallets.value)
        NilePayStorageManager.saveTransactions(_recentTransactions.value)
    }

    private fun persistAndSync() {
        NilePayStorageManager.saveWallets(_wallets.value)
        NilePayStorageManager.saveTransactions(_recentTransactions.value)
        CloudSyncManager.triggerAutoSyncIfEnabled()
    }

    fun recordTransaction(tx: TransactionRecord) {
        _recentTransactions.value = listOf(tx) + _recentTransactions.value
        persistAndSync()
    }

    /**
     * Link an Egyptian Mobile Wallet or InstaPay account
     */
    fun linkWallet(
        provider: WalletProvider,
        identifier: String,
        accountTitle: String,
        nationalId: String = "",
        initialBalance: Double = 0.0
    ): EgyptianWalletAccount {
        val newAccount = EgyptianWalletAccount(
            id = "w-${UUID.randomUUID().toString().take(8)}",
            provider = provider,
            accountTitle = accountTitle,
            identifier = identifier,
            nationalIdMasked = if (nationalId.length >= 4) "${nationalId.take(3)}•••••••${nationalId.takeLast(2)}" else "294•••••••••18",
            balanceEGP = initialBalance,
            isLinked = true,
            isVerifiedByCbe = true
        )
        _wallets.value = _wallets.value + newAccount
        persistAndSync()
        return newAccount
    }

    /**
     * Top-up or receive money into a linked Egyptian wallet
     */
    fun topupWallet(walletId: String, amountEGP: Double) {
        _wallets.value = _wallets.value.map {
            if (it.id == walletId) it.copy(balanceEGP = it.balanceEGP + amountEGP) else it
        }
        val wallet = _wallets.value.find { it.id == walletId }
        if (wallet != null) {
            val tx = TransactionRecord(
                id = UUID.randomUUID().toString(),
                title = "Fund Wallet (${wallet.provider.title})",
                type = TransactionType.MEEZA_QR_PAY,
                amountEGP = amountEGP,
                recipient = wallet.accountTitle,
                source = "Bank Account / Card Deposit",
                timestamp = System.currentTimeMillis(),
                status = TransactionStatus.SUCCESS,
                cbeReference = CryptoTokenManager.generateCbeReference(),
                authMethod = "CBE_DEPOSIT_CLEARED",
                notes = "Wallet balance topped up."
            )
            recordTransaction(tx)
        } else {
            persistAndSync()
        }
    }

    fun removeWallet(walletId: String) {
        _wallets.value = _wallets.value.filter { it.id != walletId }
        persistAndSync()
    }

    /**
     * Send money via Vodafone Cash or local Egyptian MNO wallet
     */
    fun sendMobileWalletTransfer(
        walletId: String,
        targetPhone: String,
        amountEGP: Double,
        pin: String
    ): WalletOperationResult {
        // 1. Dynamic Access Control Check
        val permCheck = AccessControlManager.checkPermission(
            FeaturePermission.EGYPTIAN_MOBILE_WALLETS,
            amountEGP
        )
        if (permCheck is PermissionCheckResult.Denied) {
            val blockedTx = TransactionRecord(
                id = UUID.randomUUID().toString(),
                title = "Mobile Wallet Transfer (Blocked)",
                type = TransactionType.VODAFONE_CASH_TRANSFER,
                amountEGP = amountEGP,
                recipient = targetPhone,
                source = "Mobile Wallet",
                timestamp = System.currentTimeMillis(),
                status = TransactionStatus.BLOCKED_ACCESS_CONTROL,
                cbeReference = "REJECTED_BY_POLICY",
                authMethod = "PERMISSION_CHECK_FAILED",
                notes = permCheck.message
            )
            recordTransaction(blockedTx)
            return WalletOperationResult.Error(permCheck.message)
        }

        // 2. Validate Egyptian phone format (010, 011, 012, 015)
        val cleanPhone = targetPhone.replace(" ", "").replace("+20", "0")
        if (!cleanPhone.matches(Regex("^01[0125][0-9]{8}$"))) {
            return WalletOperationResult.Error("Invalid Egyptian mobile number. Must be 11 digits starting with 010, 011, 012, or 015.")
        }

        val wallet = _wallets.value.find { it.id == walletId }
            ?: return WalletOperationResult.Error("Selected wallet account was not found.")

        if (wallet.balanceEGP < amountEGP) {
            return WalletOperationResult.Error("Insufficient wallet balance. Current: ${wallet.balanceEGP} EGP, Requested: ${amountEGP} EGP.")
        }

        if (pin.length != 6) {
            return WalletOperationResult.Error("Egyptian wallet security PIN must be exactly 6 digits.")
        }

        // Deduct balance
        _wallets.value = _wallets.value.map {
            if (it.id == walletId) it.copy(balanceEGP = it.balanceEGP - amountEGP) else it
        }

        val tx = TransactionRecord(
            id = UUID.randomUUID().toString(),
            title = "${wallet.provider.title} Transfer",
            type = TransactionType.VODAFONE_CASH_TRANSFER,
            amountEGP = amountEGP,
            recipient = cleanPhone,
            source = "${wallet.provider.title} (${wallet.identifier})",
            timestamp = System.currentTimeMillis(),
            status = TransactionStatus.SUCCESS,
            cbeReference = CryptoTokenManager.generateCbeReference(),
            authMethod = "WALLET_PIN_SDF",
            notes = "CBE Mobile Payment clearing confirmed."
        )
        recordTransaction(tx)
        return WalletOperationResult.Success(tx)
    }

    /**
     * Generate an ATM Cash-Out OTP for Vodafone Cash
     */
    fun requestAtmCashout(
        walletId: String,
        amountEGP: Double
    ): WalletOperationResult {
        val permCheck = AccessControlManager.checkPermission(
            FeaturePermission.VODAFONE_CASH_CASHOUT,
            amountEGP
        )
        if (permCheck is PermissionCheckResult.Denied) {
            return WalletOperationResult.Error(permCheck.message)
        }

        val wallet = _wallets.value.find { it.id == walletId }
            ?: return WalletOperationResult.Error("Wallet not found.")

        if (wallet.balanceEGP < amountEGP) {
            return WalletOperationResult.Error("Insufficient balance in ${wallet.provider.title}.")
        }

        val otp = CryptoTokenManager.generateAtmCashoutOtp()
        val tx = TransactionRecord(
            id = UUID.randomUUID().toString(),
            title = "${wallet.provider.title} ATM Cash-Out Code",
            type = TransactionType.VODAFONE_CASH_ATM_CASHOUT,
            amountEGP = amountEGP,
            recipient = "ATM / Fawry Outlet",
            source = wallet.accountTitle,
            timestamp = System.currentTimeMillis(),
            status = TransactionStatus.SUCCESS,
            cbeReference = "OTP-$otp",
            authMethod = "CBE_ATM_OTP_DISPATCH",
            notes = "Withdrawal OTP: $otp (Valid for 30 min at NBE, Banque Misr, CIB ATMs)"
        )
        recordTransaction(tx)
        return WalletOperationResult.Success(tx, extraInfo = otp)
    }

    /**
     * Execute instant transfer over Central Bank of Egypt InstaPay (Meeza IPN)
     */
    fun sendInstaPayTransfer(
        sourceWalletId: String,
        targetIpaOrAccount: String,
        amountEGP: Double,
        notes: String
    ): WalletOperationResult {
        val permCheck = AccessControlManager.checkPermission(
            FeaturePermission.INSTAPAY_TRANSFERS,
            amountEGP
        )
        if (permCheck is PermissionCheckResult.Denied) {
            val blocked = TransactionRecord(
                id = UUID.randomUUID().toString(),
                title = "InstaPay Transfer (Blocked)",
                type = TransactionType.INSTAPAY_P2P,
                amountEGP = amountEGP,
                recipient = targetIpaOrAccount,
                source = "InstaPay IPN",
                timestamp = System.currentTimeMillis(),
                status = TransactionStatus.BLOCKED_ACCESS_CONTROL,
                cbeReference = "REJECTED_BY_POLICY",
                authMethod = "ACCESS_CONTROL_GUARD",
                notes = permCheck.message
            )
            recordTransaction(blocked)
            return WalletOperationResult.Error(permCheck.message)
        }

        val wallet = _wallets.value.find { it.id == sourceWalletId }
            ?: return WalletOperationResult.Error("Source account not found.")

        if (wallet.balanceEGP < amountEGP) {
            return WalletOperationResult.Error("Insufficient InstaPay balance.")
        }

        _wallets.value = _wallets.value.map {
            if (it.id == sourceWalletId) it.copy(balanceEGP = it.balanceEGP - amountEGP) else it
        }

        val tx = TransactionRecord(
            id = UUID.randomUUID().toString(),
            title = "InstaPay IPN Instant Transfer",
            type = TransactionType.INSTAPAY_P2P,
            amountEGP = amountEGP,
            recipient = targetIpaOrAccount,
            source = "InstaPay (${wallet.identifier})",
            timestamp = System.currentTimeMillis(),
            status = TransactionStatus.SUCCESS,
            cbeReference = CryptoTokenManager.generateCbeReference(),
            authMethod = "MEEZA_IPN_SECURE_TOKEN",
            notes = if (notes.isNotBlank()) notes else "Instant CBE interbank settlement."
        )
        recordTransaction(tx)
        return WalletOperationResult.Success(tx)
    }

    /**
     * Link/Unlink an Egyptian Mobile Wallet account
     */
    fun toggleWalletLink(walletId: String) {
        _wallets.value = _wallets.value.map {
            if (it.id == walletId) it.copy(isLinked = !it.isLinked) else it
        }
        persistAndSync()
    }
}
