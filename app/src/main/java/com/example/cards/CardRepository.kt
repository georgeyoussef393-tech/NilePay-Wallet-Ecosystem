package com.example.cards

import com.example.model.CardToken
import com.example.model.CardType
import com.example.model.FeaturePermission
import com.example.model.TransactionRecord
import com.example.model.TransactionStatus
import com.example.model.TransactionType
import com.example.nfc.NfcApduMonitor
import com.example.security.AccessControlManager
import com.example.security.CryptoTokenManager
import com.example.security.NilePaySecurityEngine
import com.example.security.PermissionCheckResult
import com.example.security.SecurityAlertLevel
import com.example.security.SecurityCategory
import com.example.security.SecurityValidationResult
import com.example.storage.CloudSyncManager
import com.example.storage.NilePayStorageManager
import com.example.wallets.EgyptianWalletEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

sealed class NfcPaymentResult {
    data class Success(val transaction: TransactionRecord, val cryptogramHex: String) : NfcPaymentResult()
    data class Blocked(val message: String) : NfcPaymentResult()
    data class Failure(val error: String) : NfcPaymentResult()
}

object CardRepository {

    private val _cards = MutableStateFlow<List<CardToken>>(emptyList())
    val cards: StateFlow<List<CardToken>> = _cards.asStateFlow()

    private val _defaultNfcCard = MutableStateFlow<CardToken?>(null)
    val defaultNfcCard: StateFlow<CardToken?> = _defaultNfcCard.asStateFlow()

    fun loadFromStorageOrSeed() {
        val loaded = NilePayStorageManager.loadCards()
        if (!loaded.isNullOrEmpty()) {
            _cards.value = loaded
            _defaultNfcCard.value = loaded.find { it.isNfcDefault } ?: loaded.firstOrNull()
        } else {
            // Seed realistic Egyptian banking cards
            val seedCards = listOf(
                CryptoTokenManager.tokenizeCard(
                    rawCardNumber = "4159283749284829",
                    holderName = "GEORGE YOUSSEF",
                    issuerBank = "CIB Egypt (Commercial International Bank)",
                    cardType = CardType.VISA,
                    expiryMonth = "08",
                    expiryYear = "29"
                ).copy(isNfcDefault = true, dailySpendLimitEGP = 50000.0),
                CryptoTokenManager.tokenizeCard(
                    rawCardNumber = "5210948271039142",
                    holderName = "GEORGE YOUSSEF",
                    issuerBank = "National Bank of Egypt (NBE)",
                    cardType = CardType.MASTERCARD,
                    expiryMonth = "11",
                    expiryYear = "28"
                ).copy(dailySpendLimitEGP = 35000.0),
                CryptoTokenManager.tokenizeCard(
                    rawCardNumber = "5078119283743390",
                    holderName = "GEORGE YOUSSEF",
                    issuerBank = "Banque Misr",
                    cardType = CardType.MEEZA,
                    expiryMonth = "05",
                    expiryYear = "30"
                ).copy(dailySpendLimitEGP = 20000.0)
            )
            _cards.value = seedCards
            _defaultNfcCard.value = seedCards.first()
            persistAndSync()
        }
    }

    fun applyRestoredCards(restored: List<CardToken>) {
        _cards.value = restored
        _defaultNfcCard.value = restored.find { it.isNfcDefault } ?: restored.firstOrNull()
        NilePayStorageManager.saveCards(_cards.value)
    }

    private fun persistAndSync() {
        NilePayStorageManager.saveCards(_cards.value)
        CloudSyncManager.triggerAutoSyncIfEnabled()
    }

    fun removeCard(cardId: String) {
        _cards.value = _cards.value.filter { it.id != cardId }
        if (_defaultNfcCard.value?.id == cardId) {
            _defaultNfcCard.value = _cards.value.firstOrNull()
        }
        persistAndSync()
    }

    fun setDefaultCard(cardId: String) {
        _cards.value = _cards.value.map { card ->
            val isDef = card.id == cardId
            card.copy(isNfcDefault = isDef)
        }
        _defaultNfcCard.value = _cards.value.find { it.id == cardId }
        persistAndSync()
    }

    fun registerCard(
        cardNumber: String,
        holderName: String,
        issuerBank: String,
        cardType: CardType,
        expiryMonth: String,
        expiryYear: String
    ): CardToken {
        val tokenized = CryptoTokenManager.tokenizeCard(
            rawCardNumber = cardNumber,
            holderName = holderName,
            issuerBank = issuerBank,
            cardType = cardType,
            expiryMonth = expiryMonth,
            expiryYear = expiryYear
        )
        _cards.value = _cards.value + tokenized
        if (_defaultNfcCard.value == null) {
            setDefaultCard(tokenized.id)
        } else {
            persistAndSync()
        }
        return tokenized
    }

    /**
     * Executes contactless NFC tap payment against a POS terminal
     */
    fun processNfcTapPayment(
        cardId: String,
        merchantName: String,
        merchantCategory: String,
        terminalId: String,
        amountEGP: Double,
        location: String = "Cairo, Egypt"
    ): NfcPaymentResult {
        // Dynamic Access Control Guard: Check if NFC contactless payment is permitted for current testing tenant
        val permCheck = AccessControlManager.checkPermission(
            FeaturePermission.NFC_CONTACTLESS_PAY,
            amountEGP
        )
        if (permCheck is PermissionCheckResult.Denied) {
            val blockedTx = TransactionRecord(
                id = UUID.randomUUID().toString(),
                title = "NFC Tap @ $merchantName (Blocked)",
                type = TransactionType.NFC_TAP_PAY,
                amountEGP = amountEGP,
                recipient = "$merchantName ($terminalId)",
                source = "Tokenized Card",
                timestamp = System.currentTimeMillis(),
                status = TransactionStatus.BLOCKED_ACCESS_CONTROL,
                cbeReference = "POLICY_VIOLATION",
                authMethod = "NFC_GUARD_REJECT",
                terminalLocation = location,
                notes = permCheck.message
            )
            EgyptianWalletEngine.recordTransaction(blockedTx)
            NfcApduMonitor.logExchange(
                commandName = "DYNAMIC_ACCESS_CONTROL",
                commandHex = "NFC_TAP_ATTEMPT",
                responseHex = "6982",
                statusWord = "69 82 - REJECTED (Testing Policy: ${permCheck.message})",
                isSuccess = false
            )
            return NfcPaymentResult.Blocked(permCheck.message)
        }

        val card = _cards.value.find { it.id == cardId }
            ?: return NfcPaymentResult.Failure("Card not found in secure vault.")

        // Bank-Grade Security Engine Check: Fraud limit & biometric validation
        val secCheck = NilePaySecurityEngine.evaluateTransactionSafety(
            amountEGP = amountEGP,
            dailyCurrentSpent = card.currentSpentTodayEGP,
            dailyLimit = card.dailySpendLimitEGP,
            isBiometricVerified = true
        )
        if (secCheck is SecurityValidationResult.Blocked) {
            val blockedTx = TransactionRecord(
                id = UUID.randomUUID().toString(),
                title = "NFC Tap @ $merchantName (Fraud Blocked)",
                type = TransactionType.NFC_TAP_PAY,
                amountEGP = amountEGP,
                recipient = "$merchantName ($terminalId)",
                source = "${card.cardType.displayName} •••• ${card.last4}",
                timestamp = System.currentTimeMillis(),
                status = TransactionStatus.BLOCKED_ACCESS_CONTROL,
                cbeReference = "FRAUD_LIMIT_REJECT",
                authMethod = "NFC_SECURITY_ENGINE_BLOCK",
                terminalLocation = location,
                notes = secCheck.reason
            )
            EgyptianWalletEngine.recordTransaction(blockedTx)
            return NfcPaymentResult.Blocked(secCheck.reason)
        }

        // Anti-Relay Attack timing verification (EMV latency check)
        val simulatedExchangeDurationMs = (45..180).random().toLong()
        NilePaySecurityEngine.verifyNfcTimingGuard(simulatedExchangeDurationMs)

        // 1. Generate dynamic EMV Application Cryptogram (ARQC)
        val cryptogram = CryptoTokenManager.generateEmvCryptogram(
            dpan = card.dpan,
            amountEGP = amountEGP
        )

        // 2. Simulate APDU command flow
        NfcApduMonitor.logExchange(
            commandName = "SELECT_PPSE",
            commandHex = "00A404000E325041592E5359532E444446303100",
            responseHex = "6F28840E...9000",
            statusWord = "90 00 (PPSE Accepted)",
            isSuccess = true
        )
        NfcApduMonitor.logExchange(
            commandName = "GPO_PROCESSING",
            commandHex = "80A8000002830000",
            responseHex = "770A820239009404...9000",
            statusWord = "90 00 (AIP & AFL Loaded)",
            isSuccess = true
        )
        NfcApduMonitor.logExchange(
            commandName = "GENERATE_AC (EMV Cryptogram)",
            commandHex = "80AE8000...ATC=${cryptogram.atc}",
            responseHex = "77129F2608${cryptogram.applicationCryptogramHex}9000",
            statusWord = "90 00 (ARQC Cryptogram Approved)",
            isSuccess = true
        )

        // Update card daily spend
        _cards.value = _cards.value.map {
            if (it.id == cardId) it.copy(currentSpentTodayEGP = it.currentSpentTodayEGP + amountEGP) else it
        }
        persistAndSync()

        val tx = TransactionRecord(
            id = UUID.randomUUID().toString(),
            title = merchantName,
            type = TransactionType.NFC_TAP_PAY,
            amountEGP = amountEGP,
            recipient = "$merchantName ($terminalId - $merchantCategory)",
            source = "${card.cardType.displayName} •••• ${card.last4} (${card.issuerBank})",
            timestamp = System.currentTimeMillis(),
            status = TransactionStatus.SUCCESS,
            cbeReference = "CBE-NFC-${(1000000..9999999).random()}",
            authMethod = "HCE_EMV_CRYPTOGRAM_${cryptogram.applicationCryptogramHex.take(6)}",
            terminalLocation = location,
            apduCode = "90 00 [EMV_CONTACTLESS_SETTLED]",
            notes = "Terminal: $terminalId | Category: $merchantCategory"
        )
        EgyptianWalletEngine.recordTransaction(tx)

        return NfcPaymentResult.Success(tx, cryptogram.applicationCryptogramHex)
    }
}

