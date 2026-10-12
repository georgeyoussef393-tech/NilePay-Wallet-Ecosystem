package com.example.security

import com.example.model.CardToken
import com.example.model.CardType
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import java.util.UUID

object CryptoTokenManager {

    private val secureRandom = SecureRandom()

    /**
     * Simulates tokenization of a physical Funding PAN (FPAN) into a Device PAN (DPAN)
     * as required by Visa VTS (Visa Token Service) and Mastercard MDES.
     */
    fun tokenizeCard(
        rawCardNumber: String,
        holderName: String,
        issuerBank: String,
        cardType: CardType,
        expiryMonth: String,
        expiryYear: String
    ): CardToken {
        val cleanNumber = rawCardNumber.replace(" ", "").replace("-", "")
        val last4 = if (cleanNumber.length >= 4) cleanNumber.takeLast(4) else "8821"

        // Generate synthetic DPAN with BIN matching the network
        val binPrefix = when (cardType) {
            CardType.VISA -> "4916"
            CardType.MASTERCARD -> "5412"
            CardType.MEEZA -> "5078"
        }
        val randomMiddle = String.format(Locale.US, "%04d", secureRandom.nextInt(10000))
        val randomSuffix = String.format(Locale.US, "%04d", secureRandom.nextInt(10000))
        val dpanFormatted = "$binPrefix •••• $randomMiddle $randomSuffix"
        val fpanMasked = "${cleanNumber.take(4)} •••• •••• $last4"

        return CardToken(
            id = UUID.randomUUID().toString(),
            last4 = last4,
            holderName = holderName.uppercase(Locale.US),
            issuerBank = issuerBank,
            cardType = cardType,
            expiryMonth = expiryMonth,
            expiryYear = expiryYear,
            dpan = dpanFormatted,
            fpanMasked = fpanMasked,
            isNfcDefault = true,
            dailySpendLimitEGP = 20000.0,
            currentSpentTodayEGP = 0.0
        )
    }

    /**
     * Generates a dynamic EMV Application Cryptogram (AC) for a contactless NFC transaction.
     * Incorporates the transaction amount, unpredictable number (UN), and Terminal Country Code (818 = Egypt).
     */
    fun generateEmvCryptogram(
        dpan: String,
        amountEGP: Double,
        terminalUnpredictableNumber: String = "A7F419B2"
    ): EmvCryptogramResult {
        val amountMinorUnits = (amountEGP * 100).toLong()
        val atc = secureRandom.nextInt(65535) // Application Transaction Counter
        val rawInput = "$dpan:$amountMinorUnits:818:$terminalUnpredictableNumber:$atc"

        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(rawInput.toByteArray(Charsets.UTF_8))
        val cryptogramHex = hashBytes.take(8).joinToString("") { "%02X".format(it) }

        return EmvCryptogramResult(
            applicationCryptogramHex = cryptogramHex,
            atc = atc,
            unpredictableNumber = terminalUnpredictableNumber,
            currencyCode = "818", // Egyptian Pound ISO 4217 Numeric
            transactionType = "00", // Purchase
            cid = "80" // ARQC (Authorization Request Cryptogram)
        )
    }

    /**
     * Generate dynamic one-time ATM Cash-out OTP for Egyptian mobile wallets (e.g., Vodafone Cash)
     * Valid for 30 minutes at any CBE-certified ATM (National Bank of Egypt, Banque Misr, CIB).
     */
    fun generateAtmCashoutOtp(): String {
        val otp = secureRandom.nextInt(900000) + 100000
        return otp.toString()
    }

    /**
     * Generate CBE Meeza IPN reference code for instant transactions
     */
    fun generateCbeReference(): String {
        val prefix = "CBE-IPN-"
        val randomSegment = (10000000..99999999).random()
        return "$prefix$randomSegment"
    }
}

data class EmvCryptogramResult(
    val applicationCryptogramHex: String,
    val atc: Int,
    val unpredictableNumber: String,
    val currencyCode: String,
    val transactionType: String,
    val cid: String
)
