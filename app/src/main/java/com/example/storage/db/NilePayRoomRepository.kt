package com.example.storage.db

import com.example.model.CardToken
import com.example.model.CardType
import com.example.model.EgyptianWalletAccount
import com.example.model.TokenStatus
import com.example.model.TransactionRecord
import com.example.model.TransactionStatus
import com.example.model.TransactionType
import com.example.model.WalletProvider
import com.example.security.SecurityCipherManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository pattern implementation for NilePay Room Database with hardware AES-256 field encryption.
 */
class NilePayRoomRepository(private val database: NilePayDatabase) {

    val allCardsFlow: Flow<List<CardToken>> = database.cardDao().getAllCardsFlow().map { entities ->
        entities.map { it.toDomainModel() }
    }

    val allTransactionsFlow: Flow<List<TransactionRecord>> = database.transactionDao().getAllTransactionsFlow().map { entities ->
        entities.map { it.toDomainModel() }
    }

    val allWalletsFlow: Flow<List<EgyptianWalletAccount>> = database.walletDao().getAllWalletsFlow().map { entities ->
        entities.map { it.toDomainModel() }
    }

    suspend fun saveCard(card: CardToken) {
        database.cardDao().insertOrUpdate(card.toEntity())
    }

    suspend fun saveAllCards(cards: List<CardToken>) {
        database.cardDao().insertOrUpdateAll(cards.map { it.toEntity() })
    }

    suspend fun loadAllCards(): List<CardToken> {
        return database.cardDao().getAllCards().map { it.toDomainModel() }
    }

    suspend fun removeCard(cardId: String) {
        database.cardDao().deleteById(cardId)
    }

    suspend fun saveTransaction(tx: TransactionRecord) {
        database.transactionDao().insertOrUpdate(tx.toEntity())
    }

    suspend fun saveAllTransactions(txs: List<TransactionRecord>) {
        database.transactionDao().insertOrUpdateAll(txs.map { it.toEntity() })
    }

    suspend fun loadAllTransactions(): List<TransactionRecord> {
        return database.transactionDao().getAllTransactions().map { it.toDomainModel() }
    }

    suspend fun saveWallet(wallet: EgyptianWalletAccount) {
        database.walletDao().insertOrUpdate(wallet.toEntity())
    }

    suspend fun saveAllWallets(wallets: List<EgyptianWalletAccount>) {
        database.walletDao().insertOrUpdateAll(wallets.map { it.toEntity() })
    }

    suspend fun loadAllWallets(): List<EgyptianWalletAccount> {
        return database.walletDao().getAllWallets().map { it.toDomainModel() }
    }

    suspend fun clearAllData() {
        database.cardDao().deleteAll()
        database.transactionDao().deleteAll()
        database.walletDao().deleteAll()
    }
}

// Domain <-> Entity Mappers with AES-256 Encryption & Decryption
fun CardToken.toEntity(): CardEntity {
    return CardEntity(
        id = id,
        last4 = last4,
        encryptedHolderName = SecurityCipherManager.encrypt(holderName),
        issuerBank = issuerBank,
        cardType = cardType.name,
        expiryMonth = expiryMonth,
        expiryYear = expiryYear,
        encryptedDpan = SecurityCipherManager.encrypt(dpan),
        encryptedFpanMasked = SecurityCipherManager.encrypt(fpanMasked),
        isNfcDefault = isNfcDefault,
        tokenStatus = tokenStatus.name,
        cryptogramAlgorithm = cryptogramAlgorithm,
        dailySpendLimitEGP = dailySpendLimitEGP,
        currentSpentTodayEGP = currentSpentTodayEGP
    )
}

fun CardEntity.toDomainModel(): CardToken {
    val cardTypeEnum = try { CardType.valueOf(cardType) } catch (_: Exception) { CardType.VISA }
    val statusEnum = try { TokenStatus.valueOf(tokenStatus) } catch (_: Exception) { TokenStatus.ACTIVE }

    return CardToken(
        id = id,
        last4 = last4,
        holderName = SecurityCipherManager.decrypt(encryptedHolderName),
        issuerBank = issuerBank,
        cardType = cardTypeEnum,
        expiryMonth = expiryMonth,
        expiryYear = expiryYear,
        dpan = SecurityCipherManager.decrypt(encryptedDpan),
        fpanMasked = SecurityCipherManager.decrypt(encryptedFpanMasked),
        isNfcDefault = isNfcDefault,
        tokenStatus = statusEnum,
        cryptogramAlgorithm = cryptogramAlgorithm,
        dailySpendLimitEGP = dailySpendLimitEGP,
        currentSpentTodayEGP = currentSpentTodayEGP
    )
}

fun TransactionRecord.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        title = title,
        type = type.name,
        amountEGP = amountEGP,
        recipient = recipient,
        source = source,
        timestamp = timestamp,
        status = status.name,
        encryptedCbeReference = SecurityCipherManager.encrypt(cbeReference),
        authMethod = authMethod,
        terminalLocation = terminalLocation,
        apduCode = apduCode,
        encryptedNotes = notes?.let { SecurityCipherManager.encrypt(it) }
    )
}

fun TransactionEntity.toDomainModel(): TransactionRecord {
    val typeEnum = try { TransactionType.valueOf(type) } catch (_: Exception) { TransactionType.NFC_TAP_PAY }
    val statusEnum = try { TransactionStatus.valueOf(status) } catch (_: Exception) { TransactionStatus.SUCCESS }

    return TransactionRecord(
        id = id,
        title = title,
        type = typeEnum,
        amountEGP = amountEGP,
        recipient = recipient,
        source = source,
        timestamp = timestamp,
        status = statusEnum,
        cbeReference = SecurityCipherManager.decrypt(encryptedCbeReference),
        authMethod = authMethod,
        terminalLocation = terminalLocation,
        apduCode = apduCode,
        notes = encryptedNotes?.let { SecurityCipherManager.decrypt(it) }
    )
}

fun EgyptianWalletAccount.toEntity(): WalletEntity {
    return WalletEntity(
        id = id,
        provider = provider.name,
        accountTitle = accountTitle,
        encryptedIdentifier = SecurityCipherManager.encrypt(identifier),
        encryptedNationalIdMasked = SecurityCipherManager.encrypt(nationalIdMasked),
        balanceEGP = balanceEGP,
        isLinked = isLinked,
        isVerifiedByCbe = isVerifiedByCbe,
        lastSyncedTimestamp = lastSyncedTimestamp
    )
}

fun WalletEntity.toDomainModel(): EgyptianWalletAccount {
    val providerEnum = try { WalletProvider.valueOf(provider) } catch (_: Exception) { WalletProvider.VODAFONE_CASH }

    return EgyptianWalletAccount(
        id = id,
        provider = providerEnum,
        accountTitle = accountTitle,
        identifier = SecurityCipherManager.decrypt(encryptedIdentifier),
        nationalIdMasked = SecurityCipherManager.decrypt(encryptedNationalIdMasked),
        balanceEGP = balanceEGP,
        isLinked = isLinked,
        isVerifiedByCbe = isVerifiedByCbe,
        lastSyncedTimestamp = lastSyncedTimestamp
    )
}
