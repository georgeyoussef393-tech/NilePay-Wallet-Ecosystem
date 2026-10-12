package com.example.storage.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val id: String,
    val last4: String,
    val encryptedHolderName: String,
    val issuerBank: String,
    val cardType: String,
    val expiryMonth: String,
    val expiryYear: String,
    val encryptedDpan: String,
    val encryptedFpanMasked: String,
    val isNfcDefault: Boolean,
    val tokenStatus: String,
    val cryptogramAlgorithm: String,
    val dailySpendLimitEGP: Double,
    val currentSpentTodayEGP: Double
)
