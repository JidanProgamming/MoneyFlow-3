package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val email: String,
    val passwordHash: String,
    val fullName: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: String, // "INCOME" or "EXPENSE"
    val category: String, // Makanan, Transportasi, Hiburan, E-Wallet, Gaji, Minuman, Lainnya
    val note: String,
    val date: Long, // Epoch millisecond
    val userId: Int = 1,
    val walletAccount: String = "Tunai"
)
