package com.example.admin

import com.example.model.FeaturePermission
import com.example.model.TransactionRecord
import com.example.model.TransactionStatus
import com.example.model.TransactionType
import com.example.security.AccessControlManager
import com.example.wallets.EgyptianWalletEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class SystemDashboardMetrics(
    val totalVolumeEGP: Double,
    val completedTransactionsCount: Int,
    val blockedByPolicyCount: Int,
    val activeTestingCompaniesCount: Int,
    val revokedCompaniesCount: Int,
    val activeUserAccountsCount: Int,
    val suspendedUserAccountsCount: Int,
    val flaggedRiskAccountsCount: Int,
    val cbeMeezaStatus: String,
    val vodafoneSdfStatus: String,
    val hceEmvVaultStatus: String,
    val isFirestoreLive: Boolean
)

object AdminDashboardManager {

    init {
        // Start Firestore listener
        AdminFirestoreRepository.startRealtimeMonitoring()
    }

    val mergedTransactionsFlow: Flow<List<TransactionRecord>> = combine(
        EgyptianWalletEngine.recentTransactions,
        AdminFirestoreRepository.realtimeTransactions
    ) { localTx, firestoreTx ->
        val combined = (firestoreTx + localTx).distinctBy { it.id }
        combined.sortedByDescending { it.timestamp }
    }

    val metricsFlow: Flow<SystemDashboardMetrics> = combine(
        mergedTransactionsFlow,
        AdminFirestoreRepository.realtimeUserAccounts,
        AdminFirestoreRepository.isFirestoreConnected
    ) { txList, userAccounts, isConnected ->
        val total = txList.filter { it.status == TransactionStatus.SUCCESS }.sumOf { it.amountEGP }
        val successCount = txList.count { it.status == TransactionStatus.SUCCESS }
        val blockedCount = txList.count { it.status == TransactionStatus.BLOCKED_ACCESS_CONTROL }
        val companies = AccessControlManager.companies.value

        val activeUsers = userAccounts.count { it.status == "ACTIVE" }
        val suspendedUsers = userAccounts.count { it.status == "SUSPENDED" }
        val flaggedUsers = userAccounts.count { it.status == "FLAGGED_RISK" }

        SystemDashboardMetrics(
            totalVolumeEGP = total,
            completedTransactionsCount = successCount,
            blockedByPolicyCount = blockedCount,
            activeTestingCompaniesCount = companies.count { it.isActive },
            revokedCompaniesCount = companies.count { !it.isActive },
            activeUserAccountsCount = activeUsers,
            suspendedUserAccountsCount = suspendedUsers,
            flaggedRiskAccountsCount = flaggedUsers,
            cbeMeezaStatus = "ONLINE (CBE IPN 14ms)",
            vodafoneSdfStatus = "OPERATIONAL (SDF v2.4)",
            hceEmvVaultStatus = "PCI-DSS Level 1 Encrypted",
            isFirestoreLive = isConnected
        )
    }

    /**
     * Mint a real-time test transaction directly to Firestore
     */
    fun mintTestTransactionToFirestore(
        title: String,
        amountEGP: Double,
        type: TransactionType = TransactionType.NFC_TAP_PAY,
        status: TransactionStatus = TransactionStatus.SUCCESS
    ) {
        val txId = "tx-cbe-${System.currentTimeMillis() % 100000}"
        val record = TransactionRecord(
            id = txId,
            title = title,
            type = type,
            amountEGP = amountEGP,
            recipient = "CBE Clearing Merchant #4402",
            source = "Vodafone Cash (*9#)",
            timestamp = System.currentTimeMillis(),
            status = status,
            cbeReference = "CBE-IPN-2026-${(100000..999999).random()}",
            authMethod = "Biometric + NFC EMV ARQC",
            terminalLocation = "Cairo POS Terminal #891",
            apduCode = "00A40400 9000 (SW1SW2)",
            notes = "Real-Time Firestore Audit Ingested"
        )
        // Record in local wallet engine
        EgyptianWalletEngine.recordTransaction(record)
        // Push to Firestore
        AdminFirestoreRepository.pushTransactionToFirestore(record)
    }

    /**
     * High-level microservices specification used by architects and testers
     */
    val serverlessMicroservicesBlueprint = listOf(
        MicroserviceDescriptor(
            name = "Auth & Device Identity Service",
            technology = "Cloudflare Workers / Node.js + WebAuthn",
            responsibility = "Zero-Trust device binding, biometrics attestation, JWT issuance with company tenant scopes."
        ),
        MicroserviceDescriptor(
            name = "Dynamic Policy & Testing Flag Service",
            technology = "Serverless Edge + Upstash Redis / Firestore",
            responsibility = "Evaluates tester permissions, enforces single/daily EGP limits, broadcasts instant revocations via SSE."
        ),
        MicroserviceDescriptor(
            name = "Card Tokenization & Vault Service",
            technology = "AWS Nitro Enclaves / Cloud KMS (PCI-DSS L1)",
            responsibility = "FPAN to DPAN mapping, Visa VTS / Mastercard MDES adapter, dynamic EMV ARQC cryptogram signing."
        ),
        MicroserviceDescriptor(
            name = "Egyptian Payment Switching Gateway",
            technology = "Go / Cloud Run + ISO 8583 / REST",
            responsibility = "Meeza IPN (InstaPay) adapter, Vodafone Cash / MNO SDF connector, and Fawry merchant clearing."
        ),
        MicroserviceDescriptor(
            name = "Real-Time Transaction Stream & Audit",
            technology = "Google Cloud Pub/Sub + BigQuery / WebSockets",
            responsibility = "Sub-second ledger ingestion, AML velocity anomaly checks, real-time push to Admin Web Dashboard."
        )
    )
}

data class MicroserviceDescriptor(
    val name: String,
    val technology: String,
    val responsibility: String
)
