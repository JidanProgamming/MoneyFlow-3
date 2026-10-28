package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.AuthRepository
import com.example.data.repository.TransactionRepository

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    
    private val database by lazy { AppDatabase.getDatabase(context) }
    
    private val authRepository by lazy { 
        AuthRepository(database.userDao(), context) 
    }
    
    private val transactionRepository by lazy { 
        TransactionRepository(database.transactionDao()) 
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(authRepository) as T
            }
            modelClass.isAssignableFrom(FinanceViewModel::class.java) -> {
                FinanceViewModel(transactionRepository, authRepository, context) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
