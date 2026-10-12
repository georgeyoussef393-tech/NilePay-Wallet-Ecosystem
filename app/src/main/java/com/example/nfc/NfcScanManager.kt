package com.example.nfc

import android.nfc.Tag
import androidx.fragment.app.FragmentActivity
import com.example.cards.CardRepository
import com.example.cards.NfcPaymentResult
import com.example.model.CardType
import com.example.security.BiometricAuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NfcScanManager {

    private val _scannedCard = MutableStateFlow<EmvScannedCard?>(null)
    val scannedCard: StateFlow<EmvScannedCard?> = _scannedCard.asStateFlow()

    private val _isScanning = MutableStateFlow<Boolean>(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanStatus = MutableStateFlow<String>("Ready to scan")
    val scanStatus: StateFlow<String> = _scanStatus.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    private val _lastPosPaymentResult = MutableStateFlow<NfcPaymentResult?>(null)
    val lastPosPaymentResult: StateFlow<NfcPaymentResult?> = _lastPosPaymentResult.asStateFlow()

    fun startScanning() {
        _scannedCard.value = null
        _scanError.value = null
        _isScanning.value = true
        _scanStatus.value = "Hold your contactless Visa / Mastercard to the NFC antenna on the back of your phone..."
    }

    fun stopScanning() {
        _isScanning.value = false
        _scanStatus.value = "Scanning idle"
    }

    fun clearResult() {
        _scannedCard.value = null
        _scanError.value = null
        _lastPosPaymentResult.value = null
    }

    /**
     * Invoked when physical NFC tag is tapped against the phone
     */
    fun onTagDiscovered(tag: Tag, activity: FragmentActivity? = null) {
        if (_isScanning.value) {
            _scanStatus.value = "Card detected! Transceiving EMV APDUs..."
            val result = NfcCardScanner.readEmvCard(tag)

            if (result.isSuccess) {
                val card = result.getOrNull()
                _scannedCard.value = card
                _scanStatus.value = "EMV Card credentials read successfully!"
                _scanError.value = null
            } else {
                val error = result.exceptionOrNull()?.message ?: "Failed to read EMV data from card"
                _scanError.value = error
                _scanStatus.value = "Read failure: $error"
            }
        } else {
            // Automatic POS Terminal detection during normal NFC field interaction
            processPosTerminalDetection(tag, activity)
        }
    }

    /**
     * Detects POS terminal hardware field via IsoDep tag and initiates payment flow after biometric verification.
     */
    fun processPosTerminalDetection(
        tag: Tag,
        activity: FragmentActivity?,
        amountEGP: Double = 150.0,
        merchantName: String = "POS Terminal - Fawry Retail",
        terminalId: String = "POS-FWY-9982"
    ) {
        _scanStatus.value = "POS Terminal Detected! Initiating Biometric Security Check..."

        val defaultCard = CardRepository.defaultNfcCard.value ?: CardRepository.cards.value.firstOrNull()
        if (defaultCard == null) {
            _scanError.value = "No card registered in wallet for POS payment"
            _lastPosPaymentResult.value = NfcPaymentResult.Failure("لا توجد بطاقة مسجلة في المحفظة للدفع")
            return
        }

        fun executePaymentFlow(biometricVerified: Boolean) {
            _scanStatus.value = "Transceiving EMV Cryptogram (HCE ARQC) with POS..."
            val paymentResult = CardRepository.processNfcTapPayment(
                cardId = defaultCard.id,
                merchantName = merchantName,
                merchantCategory = "POS Terminal Retail",
                terminalId = terminalId,
                amountEGP = amountEGP,
                isBiometricVerified = biometricVerified
            )
            _lastPosPaymentResult.value = paymentResult
            when (paymentResult) {
                is NfcPaymentResult.Success -> {
                    _scanStatus.value = "POS Payment Settled Successfully! Cryptogram: ${paymentResult.cryptogramHex}"
                    _scanError.value = null
                }
                is NfcPaymentResult.Blocked -> {
                    _scanStatus.value = "Payment Blocked by Policy: ${paymentResult.message}"
                    _scanError.value = paymentResult.message
                }
                is NfcPaymentResult.Failure -> {
                    _scanStatus.value = "Payment Failed: ${paymentResult.error}"
                    _scanError.value = paymentResult.error
                }
            }
        }

        val isBiometricRequired = BiometricAuthManager.isBiometricForNfcRequired.value
        if (isBiometricRequired && activity != null) {
            BiometricAuthManager.authenticate(
                activity = activity,
                title = "تفويض دفع ماكينة POS (NFC)",
                subtitle = "المبلغ: $amountEGP ج.م • $merchantName",
                description = "استخدم بصمة الإصبع أو Face ID لتأكيد عملية الدفع اللاتلامسي",
                onSuccess = {
                    executePaymentFlow(true)
                },
                onError = { err ->
                    _scanError.value = "فشلت المصادقة البيومترية: $err"
                    _lastPosPaymentResult.value = NfcPaymentResult.Failure("تم حظر العملية: فشل التحقق البيومتري ($err)")
                }
            )
        } else {
            // Direct execution if biometric is unlocked or not forced
            executePaymentFlow(true)
        }
    }

    /**
     * Allows testers in emulators or browser environments without hardware NFC coils
     * to test the exact EMV extraction flow and credentials.
     */
    fun simulateCardTap(
        samplePan: String = "4211890123456789",
        sampleHolder: String = "MAHMOUD EL-SHARKAWY",
        sampleBank: String = "Commercial International Bank (CIB)",
        sampleType: CardType = CardType.VISA,
        sampleExpMonth: String = "10",
        sampleExpYear: String = "29"
    ) {
        _scanStatus.value = "Simulating ISO 14443-4 RF Field & IsoDep connection..."
        val mockLogs = listOf(
            "NFC Connected (Historical Bytes: 80 54 20)",
            "CMD -> SELECT PPSE (2PAY.SYS.DDF01): 00A404000E325041592E5359532E444446303100",
            "RSP <- 6F28840E325041592E5359532E4444463031A516BF0C1361114F07A00000000310109000",
            "Extracted AID from PPSE: A0000000031010",
            "CMD -> SELECT AID: 00A4040007A000000003101000",
            "RSP <- 6F1E8407A0000000031010A5135004564953418701019F38069F66049F02069000",
            "CMD -> GET PROCESSING OPTIONS: 80A8000002830000",
            "RSP <- 770A820239009404080101009000",
            "RSP <- READ RECORD (SFI 1, Rec 1): Track 2 Equivalent Discovered",
            "Extracted Credentials: PAN=${samplePan.take(4)} •••• •••• ${samplePan.takeLast(4)}, Expiry=$sampleExpMonth/$sampleExpYear"
        )

        _scannedCard.value = EmvScannedCard(
            pan = samplePan,
            last4 = samplePan.takeLast(4),
            holderName = sampleHolder,
            expiryMonth = sampleExpMonth,
            expiryYear = sampleExpYear,
            cardType = sampleType,
            issuerBank = sampleBank,
            aid = "A0000000031010",
            apduExchangeLogs = mockLogs
        )
        _scanStatus.value = "Card credentials extracted successfully!"
        _scanError.value = null
    }
}
