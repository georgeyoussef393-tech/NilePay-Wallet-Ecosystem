package com.example.admin

import android.util.Log
import com.example.model.CompanyTesterProfile
import com.example.model.FeaturePermission
import com.example.model.TransactionRecord
import com.example.model.TransactionStatus
import com.example.model.TransactionType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FirestoreUserAccountStatus(
    val userId: String,
    val name: String,
    val email: String,
    val status: String, // "ACTIVE", "SUSPENDED", "FLAGGED_RISK", "PENDING_VERIFICATION"
    val walletBalanceEGP: Double,
    val dailyLimitEGP: Double,
    val lastActiveTimestamp: Long,
    val linkedWalletsCount: Int
)

object AdminFirestoreRepository {
    private const val TAG = "AdminFirestoreRepo"
    private const val TRANSACTIONS_COLLECTION = "transactions"
    private const val COMPANIES_COLLECTION = "companies"
    private const val USER_ACCOUNTS_COLLECTION = "user_accounts"

    private val db by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize FirebaseFirestore", e)
            null
        }
    }

    private val _isFirestoreConnected = MutableStateFlow(false)
    val isFirestoreConnected: StateFlow<Boolean> = _isFirestoreConnected.asStateFlow()

    private val _realtimeTransactions = MutableStateFlow<List<TransactionRecord>>(emptyList())
    val realtimeTransactions: StateFlow<List<TransactionRecord>> = _realtimeTransactions.asStateFlow()

    private val _realtimeUserAccounts = MutableStateFlow<List<FirestoreUserAccountStatus>>(
        listOf(
            FirestoreUserAccountStatus(
                userId = "usr-eg-8921",
                name = "أحمد محمود (Ahmed Mahmoud)",
                email = "ahmed.m@nilepay.eg",
                status = "ACTIVE",
                walletBalanceEGP = 18450.00,
                dailyLimitEGP = 50000.00,
                lastActiveTimestamp = System.currentTimeMillis() - 120000,
                linkedWalletsCount = 3
            ),
            FirestoreUserAccountStatus(
                userId = "usr-eg-4432",
                name = "سارة حسن (Sara Hassan)",
                email = "sara.h@vodafone.eg",
                status = "ACTIVE",
                walletBalanceEGP = 9200.50,
                dailyLimitEGP = 25000.00,
                lastActiveTimestamp = System.currentTimeMillis() - 360000,
                linkedWalletsCount = 2
            ),
            FirestoreUserAccountStatus(
                userId = "usr-eg-9012",
                name = "محمد مصطفى (Mohamed Mostafa)",
                email = "m.mostafa@instapay.eg",
                status = "FLAGGED_RISK",
                walletBalanceEGP = 45000.00,
                dailyLimitEGP = 10000.00,
                lastActiveTimestamp = System.currentTimeMillis() - 850000,
                linkedWalletsCount = 4
            ),
            FirestoreUserAccountStatus(
                userId = "usr-eg-1108",
                name = "تامر عبد العزيز (Tamer Abdelaziz)",
                email = "tamer.a@cbe.gov.eg",
                status = "SUSPENDED",
                walletBalanceEGP = 0.00,
                dailyLimitEGP = 0.00,
                lastActiveTimestamp = System.currentTimeMillis() - 86400000,
                linkedWalletsCount = 1
            )
        )
    )
    val realtimeUserAccounts: StateFlow<List<FirestoreUserAccountStatus>> = _realtimeUserAccounts.asStateFlow()

    private var txListener: ListenerRegistration? = null
    private var companyListener: ListenerRegistration? = null
    private var userAccountsListener: ListenerRegistration? = null

    fun startRealtimeMonitoring() {
        val firestore = db
        if (firestore == null) {
            Log.w(TAG, "Firestore DB null. Offline monitoring fallback active.")
            _isFirestoreConnected.value = false
            return
        }

        // Listen to transactions real-time stream
        try {
            txListener?.remove()
            txListener = firestore.collection(TRANSACTIONS_COLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen failed for transactions", error)
                        _isFirestoreConnected.value = false
                        return@addSnapshotListener
                    }
                    _isFirestoreConnected.value = true
                    if (snapshot != null && !snapshot.isEmpty) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                val id = doc.id
                                val title = doc.getString("title") ?: "Transaction"
                                val typeStr = doc.getString("type") ?: TransactionType.NFC_TAP_PAY.name
                                val type = try { TransactionType.valueOf(typeStr) } catch (e: Exception) { TransactionType.NFC_TAP_PAY }
                                val amount = doc.getDouble("amountEGP") ?: 0.0
                                val recipient = doc.getString("recipient") ?: "Unknown"
                                val source = doc.getString("source") ?: "NilePay Wallet"
                                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                val statusStr = doc.getString("status") ?: TransactionStatus.SUCCESS.name
                                val status = try { TransactionStatus.valueOf(statusStr) } catch (e: Exception) { TransactionStatus.SUCCESS }
                                val cbeRef = doc.getString("cbeReference") ?: "CBE-REF"
                                val authMethod = doc.getString("authMethod") ?: "Biometric"
                                val location = doc.getString("terminalLocation")
                                val apdu = doc.getString("apduCode")
                                val notes = doc.getString("notes")

                                TransactionRecord(
                                    id = id,
                                    title = title,
                                    type = type,
                                    amountEGP = amount,
                                    recipient = recipient,
                                    source = source,
                                    timestamp = timestamp,
                                    status = status,
                                    cbeReference = cbeRef,
                                    authMethod = authMethod,
                                    terminalLocation = location,
                                    apduCode = apdu,
                                    notes = notes
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (list.isNotEmpty()) {
                            _realtimeTransactions.value = list
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error attaching txListener", e)
        }

        // Seed initial seed user accounts if missing, and attach listener
        try {
            userAccountsListener?.remove()
            userAccountsListener = firestore.collection(USER_ACCOUNTS_COLLECTION)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen failed for user accounts", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        val accounts = snapshot.documents.mapNotNull { doc ->
                            try {
                                FirestoreUserAccountStatus(
                                    userId = doc.id,
                                    name = doc.getString("name") ?: "Egyptian User",
                                    email = doc.getString("email") ?: "user@nilepay.eg",
                                    status = doc.getString("status") ?: "ACTIVE",
                                    walletBalanceEGP = doc.getDouble("walletBalanceEGP") ?: 12500.0,
                                    dailyLimitEGP = doc.getDouble("dailyLimitEGP") ?: 25000.0,
                                    lastActiveTimestamp = doc.getLong("lastActiveTimestamp") ?: System.currentTimeMillis(),
                                    linkedWalletsCount = (doc.getLong("linkedWalletsCount") ?: 2L).toInt()
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (accounts.isNotEmpty()) {
                            _realtimeUserAccounts.value = accounts
                        }
                    } else if (snapshot != null && snapshot.isEmpty) {
                        // Seed default user accounts into Firestore
                        seedInitialAccountsToFirestore()
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error attaching userAccountsListener", e)
        }
    }

    private fun seedInitialAccountsToFirestore() {
        val firestore = db ?: return
        _realtimeUserAccounts.value.forEach { acc ->
            val data = hashMapOf(
                "userId" to acc.userId,
                "name" to acc.name,
                "email" to acc.email,
                "status" to acc.status,
                "walletBalanceEGP" to acc.walletBalanceEGP,
                "dailyLimitEGP" to acc.dailyLimitEGP,
                "lastActiveTimestamp" to acc.lastActiveTimestamp,
                "linkedWalletsCount" to acc.linkedWalletsCount
            )
            firestore.collection(USER_ACCOUNTS_COLLECTION).document(acc.userId).set(data)
        }
    }

    fun stopRealtimeMonitoring() {
        txListener?.remove()
        companyListener?.remove()
        userAccountsListener?.remove()
    }

    /**
     * Publishes a transaction record directly to Firestore in real-time
     */
    fun pushTransactionToFirestore(tx: TransactionRecord) {
        // Also update local list immediately for seamless offline/online responsiveness
        _realtimeTransactions.value = listOf(tx) + _realtimeTransactions.value.filter { it.id != tx.id }

        val firestore = db ?: return
        val docData = hashMapOf(
            "id" to tx.id,
            "title" to tx.title,
            "type" to tx.type.name,
            "amountEGP" to tx.amountEGP,
            "recipient" to tx.recipient,
            "source" to tx.source,
            "timestamp" to tx.timestamp,
            "status" to tx.status.name,
            "cbeReference" to tx.cbeReference,
            "authMethod" to tx.authMethod,
            "terminalLocation" to (tx.terminalLocation ?: "Cairo POS Terminal"),
            "apduCode" to (tx.apduCode ?: "9000 SW1SW2 OK"),
            "notes" to (tx.notes ?: "Real-time CBE IPN Audit")
        )
        firestore.collection(TRANSACTIONS_COLLECTION)
            .document(tx.id)
            .set(docData)
            .addOnSuccessListener {
                Log.d(TAG, "Transaction ${tx.id} synced to Firestore successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to sync transaction ${tx.id} to Firestore", e)
            }
    }

    /**
     * Updates user account status in Firestore (e.g., ACTIVE, SUSPENDED, FLAGGED_RISK)
     */
    fun updateUserAccountStatusInFirestore(userId: String, newStatus: String, dailyLimit: Double? = null) {
        // Update local list for instant visual responsiveness
        _realtimeUserAccounts.value = _realtimeUserAccounts.value.map { acc ->
            if (acc.userId == userId) {
                acc.copy(
                    status = newStatus,
                    dailyLimitEGP = dailyLimit ?: acc.dailyLimitEGP,
                    lastActiveTimestamp = System.currentTimeMillis()
                )
            } else {
                acc
            }
        }

        val firestore = db ?: return
        val updates = mutableMapOf<String, Any>(
            "status" to newStatus,
            "lastActiveTimestamp" to System.currentTimeMillis()
        )
        if (dailyLimit != null) {
            updates["dailyLimitEGP"] = dailyLimit
        }
        firestore.collection(USER_ACCOUNTS_COLLECTION)
            .document(userId)
            .update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "User $userId status updated to $newStatus in Firestore")
            }
            .addOnFailureListener {
                val acc = _realtimeUserAccounts.value.find { it.userId == userId }
                val docData = hashMapOf(
                    "userId" to userId,
                    "name" to (acc?.name ?: "Egyptian Citizen User"),
                    "email" to (acc?.email ?: "$userId@nilepay.eg"),
                    "status" to newStatus,
                    "walletBalanceEGP" to (acc?.walletBalanceEGP ?: 15000.0),
                    "dailyLimitEGP" to (dailyLimit ?: acc?.dailyLimitEGP ?: 25000.0),
                    "lastActiveTimestamp" to System.currentTimeMillis(),
                    "linkedWalletsCount" to (acc?.linkedWalletsCount ?: 2)
                )
                firestore.collection(USER_ACCOUNTS_COLLECTION).document(userId).set(docData)
            }
    }

    /**
     * Creates a new user account entry in Firestore
     */
    fun createNewUserAccountInFirestore(name: String, email: String, initialBalance: Double, dailyLimit: Double) {
        val newId = "usr-eg-${(1000..9999).random()}"
        val newAcc = FirestoreUserAccountStatus(
            userId = newId,
            name = name,
            email = email,
            status = "ACTIVE",
            walletBalanceEGP = initialBalance,
            dailyLimitEGP = dailyLimit,
            lastActiveTimestamp = System.currentTimeMillis(),
            linkedWalletsCount = 2
        )
        _realtimeUserAccounts.value = listOf(newAcc) + _realtimeUserAccounts.value

        val firestore = db ?: return
        val docData = hashMapOf(
            "userId" to newId,
            "name" to name,
            "email" to email,
            "status" to "ACTIVE",
            "walletBalanceEGP" to initialBalance,
            "dailyLimitEGP" to dailyLimit,
            "lastActiveTimestamp" to System.currentTimeMillis(),
            "linkedWalletsCount" to 2
        )
        firestore.collection(USER_ACCOUNTS_COLLECTION).document(newId).set(docData)
    }

    /**
     * Syncs company tester profile to Firestore
     */
    fun pushCompanyProfileToFirestore(company: CompanyTesterProfile) {
        val firestore = db ?: return
        val docData = hashMapOf(
            "companyId" to company.companyId,
            "companyName" to company.companyName,
            "contactPerson" to company.contactPerson,
            "contactEmail" to company.contactEmail,
            "isActive" to company.isActive,
            "grantedPermissions" to company.grantedPermissions.map { it.code },
            "maxSingleTxLimitEGP" to company.maxSingleTxLimitEGP,
            "testEnvironment" to company.testEnvironment,
            "notes" to company.notes
        )
        firestore.collection(COMPANIES_COLLECTION)
            .document(company.companyId)
            .set(docData)
    }
}
