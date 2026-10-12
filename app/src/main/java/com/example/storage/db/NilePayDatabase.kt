package com.example.storage.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CardEntity::class, TransactionEntity::class, WalletEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NilePayDatabase : RoomDatabase() {

    abstract fun cardDao(): CardDao
    abstract fun transactionDao(): TransactionDao
    abstract fun walletDao(): WalletDao

    companion object {
        @Volatile
        private var INSTANCE: NilePayDatabase? = null

        fun getDatabase(context: Context): NilePayDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NilePayDatabase::class.java,
                    "nilepay_encrypted_vault.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
