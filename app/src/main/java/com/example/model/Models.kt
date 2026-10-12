package com.example.model

enum class CardType(val displayName: String) {
    VISA("Visa"),
    MASTERCARD("Mastercard"),
    MEEZA("Meeza National Card")
}

enum class TokenStatus {
    ACTIVE,
    SUSPENDED,
    REVOKED
}

data class CardToken(
    val id: String,
    val last4: String,
    val holderName: String,
    val issuerBank: String,
    val cardType: CardType,
    val expiryMonth: String,
    val expiryYear: String,
    val dpan: String, // Device Primary Account Number (Tokenized)
    val fpanMasked: String, // Funding PAN masked
    val isNfcDefault: Boolean = false,
    val tokenStatus: TokenStatus = TokenStatus.ACTIVE,
    val cryptogramAlgorithm: String = "EMV_3DS_CVC3_CBE",
    val dailySpendLimitEGP: Double = 25000.0,
    val currentSpentTodayEGP: Double = 0.0
)

enum class WalletProvider(
    val title: String,
    val colorHex: Long,
    val serviceCode: String,
    val defaultIconName: String
) {
    VODAFONE_CASH("Vodafone Cash", 0xFFE60000, "*9#", "vodafone"),
    INSTAPAY_MEEZA("InstaPay (CBE IPN)", 0xFF6C1D8C, "IPN Meeza", "instapay"),
    ORANGE_MONEY("Orange Cash", 0xFFFF7900, "*115#", "orange"),
    ETISALAT_CASH("Etisalat Cash", 0xFF7AB800, "*777#", "etisalat"),
    WE_PAY("WE Pay", 0xFF5C2D91, "*322#", "we")
}

data class EgyptianWalletAccount(
    val id: String,
    val provider: WalletProvider,
    val accountTitle: String,
    val identifier: String, // Egyptian Phone (+20 1x xxx xxxx) or InstaPay IPA (name@instapay)
    val nationalIdMasked: String, // CBE 14-digit National ID masked
    val balanceEGP: Double,
    val isLinked: Boolean = true,
    val isVerifiedByCbe: Boolean = true,
    val lastSyncedTimestamp: Long = System.currentTimeMillis()
)

enum class FeaturePermission(
    val code: String,
    val title: String,
    val description: String,
    val category: String
) {
    NFC_CONTACTLESS_PAY(
        "NFC_PAY",
        "NFC Contactless Tap & Pay",
        "Enable HCE Host Card Emulation for POS contactless payments",
        "Payments"
    ),
    EGYPTIAN_MOBILE_WALLETS(
        "EG_WALLETS",
        "Egyptian Mobile Wallets",
        "Link and manage Vodafone Cash, Orange, Etisalat, and WE Pay accounts",
        "Wallets"
    ),
    VODAFONE_CASH_CASHOUT(
        "VF_CASHOUT",
        "Vodafone Cash ATM Cash-Out",
        "Generate one-time ATM withdrawal & Fawry agent verification codes",
        "Wallets"
    ),
    INSTAPAY_TRANSFERS(
        "INSTAPAY_IPN",
        "InstaPay Instant Transfers",
        "Execute instant peer-to-peer transfers over Central Bank of Egypt IPN",
        "Payments"
    ),
    HIGH_VALUE_TRANSACTIONS(
        "HIGH_LIMIT",
        "High-Value Transactions (> 10,000 EGP)",
        "Authorize transactions exceeding standard CBE retail limit thresholds",
        "Limits"
    ),
    CROSS_BORDER_REMITTANCE(
        "REMITTANCE",
        "Cross-Border Remittance",
        "Receive inbound transfers directly into Egyptian wallet IBAN",
        "Transfers"
    ),
    SECURITY_TELEMETRY_VIEW(
        "SECURITY_INSPECT",
        "APDU & Security Telemetry",
        "Inspect low-level EMV command exchanges and cryptogram verifications",
        "Testing"
    ),
    ADMIN_MANAGEMENT(
        "ADMIN_GOVERNANCE",
        "Admin Governance & Live Monitoring",
        "Full dashboard control to monitor live transactions and manage testing tenants",
        "Administration"
    )
}

data class CompanyTesterProfile(
    val companyId: String,
    val companyName: String,
    val contactPerson: String,
    val contactEmail: String,
    val isActive: Boolean = true,
    val grantedPermissions: Set<FeaturePermission>,
    val maxSingleTxLimitEGP: Double = 15000.0,
    val testEnvironment: String = "CBE Meeza Sandbox",
    val notes: String = "Authorized partner testing account"
)

enum class TransactionType(val label: String, val category: String) {
    NFC_TAP_PAY("NFC Tap to Pay", "Contactless POS"),
    VODAFONE_CASH_TRANSFER("Vodafone Cash Transfer", "P2P Mobile Wallet"),
    VODAFONE_CASH_ATM_CASHOUT("Vodafone Cash ATM Cash-Out", "ATM Withdrawal"),
    INSTAPAY_P2P("InstaPay Instant P2P", "Meeza IPN"),
    INSTAPAY_BANK_TRANSFER("InstaPay to Bank Account", "Meeza IPN"),
    MEEZA_QR_PAY("Meeza Merchant QR", "Merchant Payment"),
    FAWRY_BILL_PAY("Fawry Utility Payment", "Bills & Services")
}

enum class TransactionStatus {
    SUCCESS,
    PENDING_CBE,
    BLOCKED_ACCESS_CONTROL,
    FAILED
}

data class TransactionRecord(
    val id: String,
    val title: String,
    val type: TransactionType,
    val amountEGP: Double,
    val recipient: String,
    val source: String,
    val timestamp: Long,
    val status: TransactionStatus,
    val cbeReference: String,
    val authMethod: String,
    val terminalLocation: String? = null,
    val apduCode: String? = null,
    val notes: String? = null
)
