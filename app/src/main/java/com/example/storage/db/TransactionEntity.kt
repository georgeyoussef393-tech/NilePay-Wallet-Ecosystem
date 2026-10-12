package com.example.storage.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String,
    val amountEGP: Double,
    val recipient: String,
    val source: String,
    val timestamp: Long,
    val status: String,
    val encryptedCbeReference: String,
    val authMethod: String,
    val terminalLocation: String?,
    val apduCode: String?,
    val encryptedNotes: String?
)
