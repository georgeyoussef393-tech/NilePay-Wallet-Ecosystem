package com.example.storage

import android.content.Context
import android.util.Log
import com.example.model.CardToken
import com.example.model.CardType
import com.example.model.CompanyTesterProfile
import com.example.model.EgyptianWalletAccount
import com.example.model.FeaturePermission
import com.example.model.TokenStatus
import com.example.model.TransactionRecord
import com.example.model.TransactionStatus
import com.example.model.TransactionType
import com.example.model.WalletProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

data class StorageStatistics(
    val cardsCount: Int = 0,
    val walletsCount: Int = 0,
    val transactionsCount: Int = 0,
    val companiesCount: Int = 0,
    val totalSizeBytes: Long = 0,
    val lastSaveTimestamp: Long = 0,
    val isInitialized: Boolean = false
)

object NilePayStorageManager {

    private const val TAG = "NilePayStorage"
    private const val FILE_CARDS = "nilepay_cards.json"
    private const val FILE_WALLETS = "nilepay_wallets.json"
    private const val FILE_TRANSACTIONS = "nilepay_transactions.json"
    private const val FILE_COMPANIES = "nilepay_companies.json"
    private const val FILE_SETTINGS = "nilepay_storage_meta.json"

    private val storageScope = CoroutineScope(Dispatchers.IO)
    private var appContext: Context? = null
    private var roomRepository: com.example.storage.db.NilePayRoomRepository? = null

    private val _stats = MutableStateFlow(StorageStatistics())
    val stats: StateFlow<StorageStatistics> = _stats.asStateFlow()

    @Volatile
    private var isInitialized = false

    fun getRoomRepository(): com.example.storage.db.NilePayRoomRepository? = roomRepository

    fun init(context: Context) {
        if (isInitialized && appContext != null) return
        appContext = context.applicationContext
        val db = com.example.storage.db.NilePayDatabase.getDatabase(context.applicationContext)
        roomRepository = com.example.storage.db.NilePayRoomRepository(db)
        isInitialized = true
        Log.i(TAG, "NilePay Storage & Encrypted Room Database initialized at: ${context.filesDir.absolutePath}")
    }

    private fun getFile(fileName: String): File? {
        val ctx = appContext ?: return null
        return File(ctx.filesDir, fileName)
    }

    // ==========================================
    // CARDS PERSISTENCE
    // ==========================================

    fun saveCards(cards: List<CardToken>) {
        storageScope.launch {
            try {
                roomRepository?.saveAllCards(cards)
                val file = getFile(FILE_CARDS) ?: return@launch
                val jsonArray = JSONArray()
                cards.forEach { card ->
                    val obj = JSONObject().apply {
                        put("id", card.id)
                        put("last4", card.last4)
                        put("holderName", card.holderName)
                        put("issuerBank", card.issuerBank)
                        put("cardType", card.cardType.name)
                        put("expiryMonth", card.expiryMonth)
                        put("expiryYear", card.expiryYear)
                        put("dpan", card.dpan)
                        put("fpanMasked", card.fpanMasked)
                        put("isNfcDefault", card.isNfcDefault)
                        put("tokenStatus", card.tokenStatus.name)
                        put("cryptogramAlgorithm", card.cryptogramAlgorithm)
                        put("dailySpendLimitEGP", card.dailySpendLimitEGP)
                        put("currentSpentTodayEGP", card.currentSpentTodayEGP)
                    }
                    jsonArray.put(obj)
                }
                writeFileAtomic(file, jsonArray.toString(2))
                updateStats()
            } catch (e: Exception) {
                Log.e(TAG, "Error saving cards", e)
            }
        }
    }

    fun loadCards(): List<CardToken>? {
        val file = getFile(FILE_CARDS) ?: return null
        if (!file.exists()) return null
        return try {
            val content = file.readText(StandardCharsets.UTF_8)
            val jsonArray = JSONArray(content)
            val list = mutableListOf<CardToken>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val cardType = try {
                    CardType.valueOf(obj.optString("cardType", CardType.VISA.name))
                } catch (_: Exception) { CardType.VISA }

                val tokenStatus = try {
                    TokenStatus.valueOf(obj.optString("tokenStatus", TokenStatus.ACTIVE.name))
                } catch (_: Exception) { TokenStatus.ACTIVE }

                list.add(
                    CardToken(
                        id = obj.getString("id"),
                        last4 = obj.getString("last4"),
                        holderName = obj.optString("holderName", "Cardholder"),
                        issuerBank = obj.optString("issuerBank", "Egyptian Bank"),
                        cardType = cardType,
                        expiryMonth = obj.optString("expiryMonth", "12"),
                        expiryYear = obj.optString("expiryYear", "28"),
                        dpan = obj.optString("dpan", "4159000000000000"),
                        fpanMasked = obj.optString("fpanMasked", "•••• 0000"),
                        isNfcDefault = obj.optBoolean("isNfcDefault", false),
                        tokenStatus = tokenStatus,
                        cryptogramAlgorithm = obj.optString("cryptogramAlgorithm", "EMV_3DS_CVC3_CBE"),
                        dailySpendLimitEGP = obj.optDouble("dailySpendLimitEGP", 25000.0),
                        currentSpentTodayEGP = obj.optDouble("currentSpentTodayEGP", 0.0)
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error loading cards", e)
            null
        }
    }

    // ==========================================
    // WALLETS PERSISTENCE
    // ==========================================

    fun saveWallets(wallets: List<EgyptianWalletAccount>) {
        storageScope.launch {
            try {
                roomRepository?.saveAllWallets(wallets)
                val file = getFile(FILE_WALLETS) ?: return@launch
                val jsonArray = JSONArray()
                wallets.forEach { w ->
                    val obj = JSONObject().apply {
                        put("id", w.id)
                        put("provider", w.provider.name)
                        put("accountTitle", w.accountTitle)
                        put("identifier", w.identifier)
                        put("nationalIdMasked", w.nationalIdMasked)
                        put("balanceEGP", w.balanceEGP)
                        put("isLinked", w.isLinked)
                        put("isVerifiedByCbe", w.isVerifiedByCbe)
                        put("lastSyncedTimestamp", w.lastSyncedTimestamp)
                    }
                    jsonArray.put(obj)
                }
                writeFileAtomic(file, jsonArray.toString(2))
                updateStats()
            } catch (e: Exception) {
                Log.e(TAG, "Error saving wallets", e)
            }
        }
    }

    fun loadWallets(): List<EgyptianWalletAccount>? {
        val file = getFile(FILE_WALLETS) ?: return null
        if (!file.exists()) return null
        return try {
            val content = file.readText(StandardCharsets.UTF_8)
            val jsonArray = JSONArray(content)
            val list = mutableListOf<EgyptianWalletAccount>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val provider = try {
                    WalletProvider.valueOf(obj.optString("provider", WalletProvider.VODAFONE_CASH.name))
                } catch (_: Exception) { WalletProvider.VODAFONE_CASH }

                list.add(
                    EgyptianWalletAccount(
                        id = obj.getString("id"),
                        provider = provider,
                        accountTitle = obj.optString("accountTitle", "Wallet"),
                        identifier = obj.optString("identifier", "01000000000"),
                        nationalIdMasked = obj.optString("nationalIdMasked", "294•••••••••18"),
                        balanceEGP = obj.optDouble("balanceEGP", 0.0),
                        isLinked = obj.optBoolean("isLinked", true),
                        isVerifiedByCbe = obj.optBoolean("isVerifiedByCbe", true),
                        lastSyncedTimestamp = obj.optLong("lastSyncedTimestamp", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error loading wallets", e)
            null
        }
    }

    // ==========================================
    // TRANSACTIONS PERSISTENCE
    // ==========================================

    fun saveTransactions(transactions: List<TransactionRecord>) {
        storageScope.launch {
            try {
                roomRepository?.saveAllTransactions(transactions)
                val file = getFile(FILE_TRANSACTIONS) ?: return@launch
                val jsonArray = JSONArray()
                // Keep up to 200 most recent transactions for fast persistence
                transactions.take(200).forEach { tx ->
                    val obj = JSONObject().apply {
                        put("id", tx.id)
                        put("title", tx.title)
                        put("type", tx.type.name)
                        put("amountEGP", tx.amountEGP)
                        put("recipient", tx.recipient)
                        put("source", tx.source)
                        put("timestamp", tx.timestamp)
                        put("status", tx.status.name)
                        put("cbeReference", tx.cbeReference)
                        put("authMethod", tx.authMethod)
                        if (tx.terminalLocation != null) put("terminalLocation", tx.terminalLocation)
                        if (tx.apduCode != null) put("apduCode", tx.apduCode)
                        if (tx.notes != null) put("notes", tx.notes)
                    }
                    jsonArray.put(obj)
                }
                writeFileAtomic(file, jsonArray.toString(2))
                updateStats()
            } catch (e: Exception) {
                Log.e(TAG, "Error saving transactions", e)
            }
        }
    }

    fun loadTransactions(): List<TransactionRecord>? {
        val file = getFile(FILE_TRANSACTIONS) ?: return null
        if (!file.exists()) return null
        return try {
            val content = file.readText(StandardCharsets.UTF_8)
            val jsonArray = JSONArray(content)
            val list = mutableListOf<TransactionRecord>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val type = try {
                    TransactionType.valueOf(obj.optString("type", TransactionType.NFC_TAP_PAY.name))
                } catch (_: Exception) { TransactionType.NFC_TAP_PAY }

                val status = try {
                    TransactionStatus.valueOf(obj.optString("status", TransactionStatus.SUCCESS.name))
                } catch (_: Exception) { TransactionStatus.SUCCESS }

                list.add(
                    TransactionRecord(
                        id = obj.getString("id"),
                        title = obj.optString("title", "Transaction"),
                        type = type,
                        amountEGP = obj.optDouble("amountEGP", 0.0),
                        recipient = obj.optString("recipient", "Unknown"),
                        source = obj.optString("source", "NilePay"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        status = status,
                        cbeReference = obj.optString("cbeReference", "CBE-REF"),
                        authMethod = obj.optString("authMethod", "PIN"),
                        terminalLocation = if (obj.has("terminalLocation")) obj.getString("terminalLocation") else null,
                        apduCode = if (obj.has("apduCode")) obj.getString("apduCode") else null,
                        notes = if (obj.has("notes")) obj.getString("notes") else null
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error loading transactions", e)
            null
        }
    }

    // ==========================================
    // COMPANIES / TESTING ACCESS CONTROL PERSISTENCE
    // ==========================================

    data class CompaniesStorageData(
        val companies: List<CompanyTesterProfile>,
        val activeCompanyId: String?
    )

    fun saveCompanies(companies: List<CompanyTesterProfile>, activeCompanyId: String?) {
        storageScope.launch {
            try {
                val file = getFile(FILE_COMPANIES) ?: return@launch
                val root = JSONObject()
                root.put("activeCompanyId", activeCompanyId ?: "")
                val array = JSONArray()
                companies.forEach { comp ->
                    val obj = JSONObject().apply {
                        put("companyId", comp.companyId)
                        put("companyName", comp.companyName)
                        put("contactPerson", comp.contactPerson)
                        put("contactEmail", comp.contactEmail)
                        put("isActive", comp.isActive)
                        put("maxSingleTxLimitEGP", comp.maxSingleTxLimitEGP)
                        put("testEnvironment", comp.testEnvironment)
                        put("notes", comp.notes)
                        val perms = JSONArray()
                        comp.grantedPermissions.forEach { p -> perms.put(p.name) }
                        put("grantedPermissions", perms)
                    }
                    array.put(obj)
                }
                root.put("companies", array)
                writeFileAtomic(file, root.toString(2))
                updateStats()
            } catch (e: Exception) {
                Log.e(TAG, "Error saving companies", e)
            }
        }
    }

    fun loadCompanies(): CompaniesStorageData? {
        val file = getFile(FILE_COMPANIES) ?: return null
        if (!file.exists()) return null
        return try {
            val content = file.readText(StandardCharsets.UTF_8)
            val root = JSONObject(content)
            val activeCompanyId = root.optString("activeCompanyId").takeIf { it.isNotEmpty() }
            val array = root.optJSONArray("companies") ?: JSONArray()
            val list = mutableListOf<CompanyTesterProfile>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val permsArray = obj.optJSONArray("grantedPermissions") ?: JSONArray()
                val permsSet = mutableSetOf<FeaturePermission>()
                for (j in 0 until permsArray.length()) {
                    try {
                        permsSet.add(FeaturePermission.valueOf(permsArray.getString(j)))
                    } catch (_: Exception) {}
                }
                list.add(
                    CompanyTesterProfile(
                        companyId = obj.getString("companyId"),
                        companyName = obj.optString("companyName", "Testing Company"),
                        contactPerson = obj.optString("contactPerson", "Representative"),
                        contactEmail = obj.optString("contactEmail", "test@domain.eg"),
                        isActive = obj.optBoolean("isActive", true),
                        grantedPermissions = permsSet,
                        maxSingleTxLimitEGP = obj.optDouble("maxSingleTxLimitEGP", 15000.0),
                        testEnvironment = obj.optString("testEnvironment", "Meeza Sandbox"),
                        notes = obj.optString("notes", "")
                    )
                )
            }
            CompaniesStorageData(list, activeCompanyId)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading companies", e)
            null
        }
    }

    // ==========================================
    // COMPLETE BACKUP / RESTORE SNAPSHOT
    // ==========================================

    fun generateCompleteBackupJson(
        cards: List<CardToken>,
        wallets: List<EgyptianWalletAccount>,
        transactions: List<TransactionRecord>,
        companies: List<CompanyTesterProfile>,
        activeCompanyId: String?
    ): String {
        val root = JSONObject()
        root.put("version", "1.0")
        root.put("app", "NilePay Egyptian Digital Wallet")
        root.put("timestamp", System.currentTimeMillis())
        root.put("cbeAuditTag", "CBE-NP-CLOUD-VAULT-2026")

        // Cards
        val cardsArray = JSONArray()
        cards.forEach { c ->
            cardsArray.put(JSONObject().apply {
                put("id", c.id)
                put("last4", c.last4)
                put("holderName", c.holderName)
                put("issuerBank", c.issuerBank)
                put("cardType", c.cardType.name)
                put("expiryMonth", c.expiryMonth)
                put("expiryYear", c.expiryYear)
                put("dpan", c.dpan)
                put("fpanMasked", c.fpanMasked)
                put("isNfcDefault", c.isNfcDefault)
                put("tokenStatus", c.tokenStatus.name)
                put("cryptogramAlgorithm", c.cryptogramAlgorithm)
                put("dailySpendLimitEGP", c.dailySpendLimitEGP)
                put("currentSpentTodayEGP", c.currentSpentTodayEGP)
            })
        }
        root.put("cards", cardsArray)

        // Wallets
        val walletsArray = JSONArray()
        wallets.forEach { w ->
            walletsArray.put(JSONObject().apply {
                put("id", w.id)
                put("provider", w.provider.name)
                put("accountTitle", w.accountTitle)
                put("identifier", w.identifier)
                put("nationalIdMasked", w.nationalIdMasked)
                put("balanceEGP", w.balanceEGP)
                put("isLinked", w.isLinked)
                put("isVerifiedByCbe", w.isVerifiedByCbe)
                put("lastSyncedTimestamp", w.lastSyncedTimestamp)
            })
        }
        root.put("wallets", walletsArray)

        // Transactions
        val txArray = JSONArray()
        transactions.take(200).forEach { tx ->
            txArray.put(JSONObject().apply {
                put("id", tx.id)
                put("title", tx.title)
                put("type", tx.type.name)
                put("amountEGP", tx.amountEGP)
                put("recipient", tx.recipient)
                put("source", tx.source)
                put("timestamp", tx.timestamp)
                put("status", tx.status.name)
                put("cbeReference", tx.cbeReference)
                put("authMethod", tx.authMethod)
                if (tx.terminalLocation != null) put("terminalLocation", tx.terminalLocation)
                if (tx.apduCode != null) put("apduCode", tx.apduCode)
                if (tx.notes != null) put("notes", tx.notes)
            })
        }
        root.put("transactions", txArray)

        // Companies
        val compArray = JSONArray()
        companies.forEach { comp ->
            compArray.put(JSONObject().apply {
                put("companyId", comp.companyId)
                put("companyName", comp.companyName)
                put("contactPerson", comp.contactPerson)
                put("contactEmail", comp.contactEmail)
                put("isActive", comp.isActive)
                put("maxSingleTxLimitEGP", comp.maxSingleTxLimitEGP)
                put("testEnvironment", comp.testEnvironment)
                put("notes", comp.notes)
                val perms = JSONArray()
                comp.grantedPermissions.forEach { p -> perms.put(p.name) }
                put("grantedPermissions", perms)
            })
        }
        root.put("companies", compArray)
        root.put("activeCompanyId", activeCompanyId ?: "")

        return root.toString(2)
    }

    data class RestoredSnapshotData(
        val cards: List<CardToken>,
        val wallets: List<EgyptianWalletAccount>,
        val transactions: List<TransactionRecord>,
        val companies: List<CompanyTesterProfile>,
        val activeCompanyId: String?
    )

    fun parseBackupJson(jsonString: String): RestoredSnapshotData? {
        return try {
            val root = JSONObject(jsonString)
            // Parse cards
            val cards = mutableListOf<CardToken>()
            val cardsArray = root.optJSONArray("cards") ?: JSONArray()
            for (i in 0 until cardsArray.length()) {
                val obj = cardsArray.getJSONObject(i)
                val cardType = try {
                    CardType.valueOf(obj.optString("cardType", CardType.VISA.name))
                } catch (_: Exception) { CardType.VISA }
                val tokenStatus = try {
                    TokenStatus.valueOf(obj.optString("tokenStatus", TokenStatus.ACTIVE.name))
                } catch (_: Exception) { TokenStatus.ACTIVE }
                cards.add(
                    CardToken(
                        id = obj.getString("id"),
                        last4 = obj.getString("last4"),
                        holderName = obj.optString("holderName", "Cardholder"),
                        issuerBank = obj.optString("issuerBank", "Egyptian Bank"),
                        cardType = cardType,
                        expiryMonth = obj.optString("expiryMonth", "12"),
                        expiryYear = obj.optString("expiryYear", "28"),
                        dpan = obj.optString("dpan", "4159000000000000"),
                        fpanMasked = obj.optString("fpanMasked", "•••• 0000"),
                        isNfcDefault = obj.optBoolean("isNfcDefault", false),
                        tokenStatus = tokenStatus,
                        cryptogramAlgorithm = obj.optString("cryptogramAlgorithm", "EMV_3DS_CVC3_CBE"),
                        dailySpendLimitEGP = obj.optDouble("dailySpendLimitEGP", 25000.0),
                        currentSpentTodayEGP = obj.optDouble("currentSpentTodayEGP", 0.0)
                    )
                )
            }

            // Parse wallets
            val wallets = mutableListOf<EgyptianWalletAccount>()
            val walletsArray = root.optJSONArray("wallets") ?: JSONArray()
            for (i in 0 until walletsArray.length()) {
                val obj = walletsArray.getJSONObject(i)
                val provider = try {
                    WalletProvider.valueOf(obj.optString("provider", WalletProvider.VODAFONE_CASH.name))
                } catch (_: Exception) { WalletProvider.VODAFONE_CASH }
                wallets.add(
                    EgyptianWalletAccount(
                        id = obj.getString("id"),
                        provider = provider,
                        accountTitle = obj.optString("accountTitle", "Wallet"),
                        identifier = obj.optString("identifier", "01000000000"),
                        nationalIdMasked = obj.optString("nationalIdMasked", "294•••••••••18"),
                        balanceEGP = obj.optDouble("balanceEGP", 0.0),
                        isLinked = obj.optBoolean("isLinked", true),
                        isVerifiedByCbe = obj.optBoolean("isVerifiedByCbe", true),
                        lastSyncedTimestamp = obj.optLong("lastSyncedTimestamp", System.currentTimeMillis())
                    )
                )
            }

            // Parse transactions
            val txList = mutableListOf<TransactionRecord>()
            val txArray = root.optJSONArray("transactions") ?: JSONArray()
            for (i in 0 until txArray.length()) {
                val obj = txArray.getJSONObject(i)
                val type = try {
                    TransactionType.valueOf(obj.optString("type", TransactionType.NFC_TAP_PAY.name))
                } catch (_: Exception) { TransactionType.NFC_TAP_PAY }
                val status = try {
                    TransactionStatus.valueOf(obj.optString("status", TransactionStatus.SUCCESS.name))
                } catch (_: Exception) { TransactionStatus.SUCCESS }
                txList.add(
                    TransactionRecord(
                        id = obj.getString("id"),
                        title = obj.optString("title", "Transaction"),
                        type = type,
                        amountEGP = obj.optDouble("amountEGP", 0.0),
                        recipient = obj.optString("recipient", "Unknown"),
                        source = obj.optString("source", "NilePay"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        status = status,
                        cbeReference = obj.optString("cbeReference", "CBE-REF"),
                        authMethod = obj.optString("authMethod", "PIN"),
                        terminalLocation = if (obj.has("terminalLocation")) obj.getString("terminalLocation") else null,
                        apduCode = if (obj.has("apduCode")) obj.getString("apduCode") else null,
                        notes = if (obj.has("notes")) obj.getString("notes") else null
                    )
                )
            }

            // Parse companies
            val companies = mutableListOf<CompanyTesterProfile>()
            val compArray = root.optJSONArray("companies") ?: JSONArray()
            for (i in 0 until compArray.length()) {
                val obj = compArray.getJSONObject(i)
                val permsArray = obj.optJSONArray("grantedPermissions") ?: JSONArray()
                val permsSet = mutableSetOf<FeaturePermission>()
                for (j in 0 until permsArray.length()) {
                    try {
                        permsSet.add(FeaturePermission.valueOf(permsArray.getString(j)))
                    } catch (_: Exception) {}
                }
                companies.add(
                    CompanyTesterProfile(
                        companyId = obj.getString("companyId"),
                        companyName = obj.optString("companyName", "Testing Company"),
                        contactPerson = obj.optString("contactPerson", "Representative"),
                        contactEmail = obj.optString("contactEmail", "test@domain.eg"),
                        isActive = obj.optBoolean("isActive", true),
                        grantedPermissions = permsSet,
                        maxSingleTxLimitEGP = obj.optDouble("maxSingleTxLimitEGP", 15000.0),
                        testEnvironment = obj.optString("testEnvironment", "Meeza Sandbox"),
                        notes = obj.optString("notes", "")
                    )
                )
            }
            val activeId = root.optString("activeCompanyId").takeIf { it.isNotEmpty() }

            RestoredSnapshotData(cards, wallets, txList, companies, activeId)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing backup JSON", e)
            null
        }
    }

    private fun writeFileAtomic(file: File, text: String) {
        val temp = File(file.parentFile, "${file.name}.tmp")
        FileOutputStream(temp).use { fos ->
            fos.write(text.toByteArray(StandardCharsets.UTF_8))
            fos.flush()
        }
        if (!temp.renameTo(file)) {
            temp.copyTo(file, overwrite = true)
            temp.delete()
        }
    }

    fun updateStats() {
        val ctx = appContext ?: return
        var size: Long = 0
        listOf(FILE_CARDS, FILE_WALLETS, FILE_TRANSACTIONS, FILE_COMPANIES).forEach { name ->
            val f = File(ctx.filesDir, name)
            if (f.exists()) size += f.length()
        }
        val cardsCount = loadCards()?.size ?: 0
        val walletsCount = loadWallets()?.size ?: 0
        val txCount = loadTransactions()?.size ?: 0
        val compCount = loadCompanies()?.companies?.size ?: 0

        _stats.value = StorageStatistics(
            cardsCount = cardsCount,
            walletsCount = walletsCount,
            transactionsCount = txCount,
            companiesCount = compCount,
            totalSizeBytes = size,
            lastSaveTimestamp = System.currentTimeMillis(),
            isInitialized = true
        )
    }

    fun clearAllStorage() {
        val ctx = appContext ?: return
        listOf(FILE_CARDS, FILE_WALLETS, FILE_TRANSACTIONS, FILE_COMPANIES, FILE_SETTINGS).forEach { name ->
            val f = File(ctx.filesDir, name)
            if (f.exists()) f.delete()
        }
        updateStats()
    }
}
