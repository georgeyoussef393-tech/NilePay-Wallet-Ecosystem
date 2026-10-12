package com.example.storage.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey val id: String,
    val provider: String,
    val accountTitle: String,
    val encryptedIdentifier: String,
    val encryptedNationalIdMasked: String,
    val balanceEGP: Double,
    val isLinked: Boolean,
    val isVerifiedByCbe: Boolean,
    val lastSyncedTimestamp: Long
)
