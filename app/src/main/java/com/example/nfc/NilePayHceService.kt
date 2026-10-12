package com.example.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import com.example.cards.CardRepository
import com.example.model.CardToken
import com.example.model.CardType
import com.example.model.FeaturePermission
import com.example.model.TransactionRecord
import com.example.model.TransactionStatus
import com.example.model.TransactionType
import com.example.security.AccessControlManager
import com.example.security.CryptoTokenManager
import com.example.wallets.EgyptianWalletEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class ApduLogEntry(
    val timestamp: Long,
    val commandName: String,
    val commandHex: String,
    val responseHex: String,
    val statusWord: String,
    val isSuccess: Boolean
)

object NfcApduMonitor {
    private val _logs = MutableStateFlow<List<ApduLogEntry>>(emptyList())
    val logs: StateFlow<List<ApduLogEntry>> = _logs.asStateFlow()

    private val _lastContactlessPaymentSuccess = MutableStateFlow<Boolean?>(null)
    val lastContactlessPaymentSuccess: StateFlow<Boolean?> = _lastContactlessPaymentSuccess.asStateFlow()

    fun logExchange(commandName: String, commandHex: String, responseHex: String, statusWord: String, isSuccess: Boolean) {
        val entry = ApduLogEntry(
            timestamp = System.currentTimeMillis(),
            commandName = commandName,
            commandHex = commandHex,
            responseHex = responseHex,
            statusWord = statusWord,
            isSuccess = isSuccess
        )
        _logs.value = (listOf(entry) + _logs.value).take(25)
        if (commandName.contains("GENERATE_AC") || commandName.contains("PAYMENT_COMPLETE")) {
            _lastContactlessPaymentSuccess.value = isSuccess
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
        _lastContactlessPaymentSuccess.value = null
    }
}

/**
 * Host Card Emulation (HCE) Service for NilePay
 * Handles incoming EMV contactless APDU exchanges from POS payment terminals (ISO 7816-4).
 */
class NilePayHceService : HostApduService() {

    companion object {
        // Status words (ISO 7816-4)
        private val STATUS_SUCCESS = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val STATUS_FAILED = byteArrayOf(0x6F.toByte(), 0x00.toByte())
        private val STATUS_FILE_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x82.toByte())
        private val STATUS_SECURITY_NOT_SATISFIED = byteArrayOf(0x69.toByte(), 0x82.toByte())
        private val STATUS_WRONG_LENGTH = byteArrayOf(0x67.toByte(), 0x00.toByte())

        // APDU command prefixes
        private const val CMD_SELECT = "00A40400"
        private const val CMD_GET_PROCESSING_OPTIONS = "80A8"
        private const val CMD_READ_RECORD = "00B2"
        private const val CMD_GENERATE_AC = "80AE"

        // Known AIDs
        private const val AID_PPSE = "325041592E5359532E4444463031" // 2PAY.SYS.DDF01
        private const val AID_VISA = "A0000000031010"
        private const val AID_MASTERCARD = "A0000000041010"
        private const val AID_MEEZA = "A0000007271010"
    }

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null) return STATUS_FAILED

        val cmdHex = commandApdu.joinToString("") { "%02X".format(it) }

        // 1. Dynamic Access Control Check
        if (!AccessControlManager.hasPermission(FeaturePermission.NFC_CONTACTLESS_PAY)) {
            NfcApduMonitor.logExchange(
                commandName = "DYNAMIC_ACCESS_GUARD",
                commandHex = cmdHex,
                responseHex = "6982",
                statusWord = "69 82 (Security Condition Not Satisfied - NFC Permission Revoked)",
                isSuccess = false
            )
            return STATUS_SECURITY_NOT_SATISFIED
        }

        // 2. Active Card Check
        val activeCard = CardRepository.defaultNfcCard.value ?: CardRepository.cards.value.firstOrNull()
        if (activeCard == null) {
            NfcApduMonitor.logExchange(
                commandName = "HCE_VAULT_EMPTY",
                commandHex = cmdHex,
                responseHex = "6A82",
                statusWord = "6A 82 (File Not Found - No Active Card in Vault)",
                isSuccess = false
            )
            return STATUS_FILE_NOT_FOUND
        }

        return when {
            cmdHex.startsWith(CMD_SELECT) -> handleSelect(cmdHex, activeCard)
            cmdHex.startsWith(CMD_GET_PROCESSING_OPTIONS) -> handleGetProcessingOptions(cmdHex, activeCard)
            cmdHex.startsWith(CMD_READ_RECORD) -> handleReadRecord(cmdHex, activeCard)
            cmdHex.startsWith(CMD_GENERATE_AC) -> handleGenerateAc(cmdHex, activeCard)
            else -> {
                NfcApduMonitor.logExchange("APDU_UNKNOWN", cmdHex, "9000", "90 00 (Acknowledged)", true)
                STATUS_SUCCESS
            }
        }
    }

    private fun handleSelect(cmdHex: String, activeCard: CardToken): ByteArray {
        val isPpse = cmdHex.contains(AID_PPSE, ignoreCase = true)

        if (isPpse) {
            // Return FCI Directory containing the active card's AID
            val targetAidHex = when (activeCard.cardType) {
                CardType.VISA -> AID_VISA
                CardType.MASTERCARD -> AID_MASTERCARD
                CardType.MEEZA -> AID_MEEZA
            }
            val ppseFciHex = "6F28840E$AID_PPSE" +
                    "A516BF0C1361114F07${targetAidHex}5006${encodeAsciiHex(activeCard.cardType.displayName.take(6))}"

            val responseBytes = hexStringToByteArray(ppseFciHex) + STATUS_SUCCESS
            NfcApduMonitor.logExchange(
                commandName = "SELECT PPSE (2PAY.SYS.DDF01)",
                commandHex = cmdHex,
                responseHex = ppseFciHex + "9000",
                statusWord = "90 00 (OK - FCI Directory with ${activeCard.cardType.displayName} AID)",
                isSuccess = true
            )
            return responseBytes
        }

        // SELECT AID
        val appLabelHex = encodeAsciiHex(activeCard.cardType.displayName)
        val fciHex = "6F228407" + when (activeCard.cardType) {
            CardType.VISA -> AID_VISA
            CardType.MASTERCARD -> AID_MASTERCARD
            CardType.MEEZA -> AID_MEEZA
        } + "A51750" + "%02X".format(appLabelHex.length / 2) + appLabelHex + "8701019F38069F66049F0206"

        val responseBytes = hexStringToByteArray(fciHex) + STATUS_SUCCESS
        NfcApduMonitor.logExchange(
            commandName = "SELECT AID (${activeCard.cardType.displayName})",
            commandHex = cmdHex,
            responseHex = fciHex + "9000",
            statusWord = "90 00 (OK - Selected ${activeCard.issuerBank})",
            isSuccess = true
        )
        return responseBytes
    }

    private fun handleGetProcessingOptions(cmdHex: String, activeCard: CardToken): ByteArray {
        // Return Application Interchange Profile (AIP 3900) & Application File Locator (AFL)
        val gpoHex = "770A82023900940408010100"
        val responseBytes = hexStringToByteArray(gpoHex) + STATUS_SUCCESS
        NfcApduMonitor.logExchange(
            commandName = "GET PROCESSING OPTIONS (GPO)",
            commandHex = cmdHex,
            responseHex = gpoHex + "9000",
            statusWord = "90 00 (OK - AIP 3900 & AFL 08010100)",
            isSuccess = true
        )
        return responseBytes
    }

    private fun handleReadRecord(cmdHex: String, activeCard: CardToken): ByteArray {
        // Format dynamic Track 2 Equivalent Data (Tag 57) using active card's tokenized DPAN
        val cleanDpanDigits = activeCard.dpan.replace(" ", "").replace("•", "8")
        val expYyMm = "${activeCard.expiryYear.takeLast(2)}${activeCard.expiryMonth.padStart(2, '0')}"
        val track2Content = "${cleanDpanDigits}D${expYyMm}20100000000000F"
        val track2Len = (track2Content.length + 1) / 2
        val track2Hex = "57" + "%02X".format(track2Len) + track2Content

        val recordHex = "70" + "%02X".format(track2Hex.length / 2) + track2Hex
        val responseBytes = hexStringToByteArray(recordHex) + STATUS_SUCCESS

        NfcApduMonitor.logExchange(
            commandName = "READ RECORD (Track 2 DPAN Data)",
            commandHex = cmdHex,
            responseHex = recordHex + "9000",
            statusWord = "90 00 (OK - DPAN ...${activeCard.last4} Streamed)",
            isSuccess = true
        )
        return responseBytes
    }

    private fun handleGenerateAc(cmdHex: String, activeCard: CardToken): ByteArray {
        // Extract unpredictable number and compute dynamic cryptogram
        val cryptogramResult = CryptoTokenManager.generateEmvCryptogram(
            dpan = activeCard.dpan,
            amountEGP = 150.0
        )

        val arqcPayload = "77129F2608${cryptogramResult.applicationCryptogramHex}9F2701809F3602${"%04X".format(cryptogramResult.atc)}"
        val responseBytes = hexStringToByteArray(arqcPayload) + STATUS_SUCCESS

        // Record contactless payment in real-time ledger
        val tx = TransactionRecord(
            id = UUID.randomUUID().toString(),
            title = "POS Contactless Terminal",
            type = TransactionType.NFC_TAP_PAY,
            amountEGP = 150.0,
            recipient = "POS Terminal (Contactless HCE)",
            source = "${activeCard.cardType.displayName} •••• ${activeCard.last4}",
            timestamp = System.currentTimeMillis(),
            status = TransactionStatus.SUCCESS,
            cbeReference = "CBE-HCE-${(1000000..9999999).random()}",
            authMethod = "HCE_ARQC_CRYPTOGRAM_${cryptogramResult.applicationCryptogramHex.take(6)}",
            terminalLocation = "Contactless POS Field",
            apduCode = "90 00 [EMV_HCE_SETTLED]",
            notes = "ATC: ${cryptogramResult.atc} | Cryptogram: ${cryptogramResult.applicationCryptogramHex}"
        )
        EgyptianWalletEngine.recordTransaction(tx)

        NfcApduMonitor.logExchange(
            commandName = "GENERATE_AC (ARQC Dynamic Cryptogram)",
            commandHex = cmdHex,
            responseHex = arqcPayload + "9000",
            statusWord = "90 00 (OK - Cryptogram ${cryptogramResult.applicationCryptogramHex} Settled)",
            isSuccess = true
        )
        return responseBytes
    }

    override fun onDeactivated(reason: Int) {
        val reasonStr = when (reason) {
            DEACTIVATION_LINK_LOSS -> "RF Field Disengaged"
            DEACTIVATION_DESELECTED -> "Application Deselected"
            else -> "Session Terminated"
        }
        NfcApduMonitor.logExchange(
            commandName = "HCE_DEACTIVATED",
            commandHex = "--",
            responseHex = "--",
            statusWord = reasonStr,
            isSuccess = true
        )
    }

    private fun encodeAsciiHex(str: String): String {
        return str.toByteArray(Charsets.US_ASCII).joinToString("") { "%02X".format(it) }
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
