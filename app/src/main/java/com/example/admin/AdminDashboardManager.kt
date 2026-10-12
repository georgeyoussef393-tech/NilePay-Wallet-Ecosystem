package com.example.admin

import com.example.model.FeaturePermission
import com.example.model.TransactionRecord
import com.example.model.TransactionStatus
import com.example.security.AccessControlManager
import com.example.wallets.EgyptianWalletEngine
import kotlinx.coroutines.flow.map

data class SystemDashboardMetrics(
    val totalVolumeEGP: Double,
    val completedTransactionsCount: Int,
    val blockedByPolicyCount: Int,
    val activeTestingCompaniesCount: Int,
    val revokedCompaniesCount: Int,
    val cbeMeezaStatus: String,
    val vodafoneSdfStatus: String,
    val hceEmvVaultStatus: String
)

object AdminDashboardManager {

    val metricsFlow = EgyptianWalletEngine.recentTransactions.map { txList ->
        val total = txList.filter { it.status == TransactionStatus.SUCCESS }.sumOf { it.amountEGP }
        val successCount = txList.count { it.status == TransactionStatus.SUCCESS }
        val blockedCount = txList.count { it.status == TransactionStatus.BLOCKED_ACCESS_CONTROL }
        val companies = AccessControlManager.companies.value

        SystemDashboardMetrics(
            totalVolumeEGP = total,
            completedTransactionsCount = successCount,
            blockedByPolicyCount = blockedCount,
            activeTestingCompaniesCount = companies.count { it.isActive },
            revokedCompaniesCount = companies.count { !it.isActive },
            cbeMeezaStatus = "ONLINE (Latency 18ms)",
            vodafoneSdfStatus = "OPERATIONAL (SDF v2.4)",
            hceEmvVaultStatus = "PCI-DSS Level 1 Encrypted"
        )
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
