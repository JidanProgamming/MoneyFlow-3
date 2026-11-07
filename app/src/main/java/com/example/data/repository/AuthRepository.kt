package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.UserDao
import com.example.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepository(
    private val userDao: UserDao,
    context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("moneyflow_auth_prefs", Context.MODE_PRIVATE)
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    suspend fun loadSavedUser(): User? {
        // Ensure default demo account exists if database is fresh
        val existingDemo = userDao.getUserByEmail("user@moneyflow.com")
        if (existingDemo == null) {
            val demoUser = User(
                fullName = "MoneyFlow User",
                email = "user@moneyflow.com",
                passwordHash = "password123"
            )
            userDao.insertUser(demoUser)
        }

        val savedUserId = prefs.getInt("logged_in_user_id", -1)
        if (savedUserId != -1) {
            val user = userDao.getUserById(savedUserId)
            if (user != null) {
                _currentUser.value = user
                return user
            } else {
                prefs.edit().remove("logged_in_user_id").apply()
            }
        }
        return null
    }

    suspend fun login(email: String, passwordHash: String): Result<User> {
        val trimmedEmail = email.trim()
        val trimmedPass = passwordHash.trim()
        if (trimmedEmail.isBlank() || trimmedPass.isBlank()) {
            return Result.failure(Exception("Email dan password tidak boleh kosong"))
        }
        val user = userDao.getUserByEmail(trimmedEmail) 
            ?: return Result.failure(Exception("Akun tidak ditemukan. Silakan buat akun di tab Daftar."))
        if (user.passwordHash != trimmedPass) {
            return Result.failure(Exception("Password salah. Silakan periksa kembali kata sandi Anda."))
        }
        _currentUser.value = user
        prefs.edit().putInt("logged_in_user_id", user.id).apply()
        return Result.success(user)
    }

    suspend fun register(fullName: String, email: String, passwordHash: String): Result<User> {
        val trimmedName = fullName.trim()
        val trimmedEmail = email.trim()
        val trimmedPass = passwordHash.trim()
        if (trimmedName.isBlank() || trimmedEmail.isBlank() || trimmedPass.isBlank()) {
            return Result.failure(Exception("Semua formulir (Nama, Email, Password) harus diisi"))
        }
        if (!trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
            return Result.failure(Exception("Format email tidak valid"))
        }
        if (trimmedPass.length < 4) {
            return Result.failure(Exception("Password minimal harus 4 karakter"))
        }
        val existingUser = userDao.getUserByEmail(trimmedEmail)
        if (existingUser != null) {
            return Result.failure(Exception("Email sudah terdaftar. Silakan masuk di tab Login."))
        }
        val newUser = User(fullName = trimmedName, email = trimmedEmail, passwordHash = trimmedPass)
        try {
            val generatedId = userDao.insertUser(newUser)
            val finalUser = newUser.copy(id = generatedId.toInt())
            _currentUser.value = finalUser
            prefs.edit().putInt("logged_in_user_id", finalUser.id).apply()
            return Result.success(finalUser)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun loginOrRegisterGoogleUser(email: String, fullName: String): Result<User> {
        if (email.isBlank()) {
            return Result.failure(Exception("Email Google tidak valid"))
        }
        val existingUser = userDao.getUserByEmail(email)
        if (existingUser != null) {
            _currentUser.value = existingUser
            prefs.edit().putInt("logged_in_user_id", existingUser.id).apply()
            return Result.success(existingUser)
        }
        // Create user if not exists
        val newUser = User(
            fullName = fullName,
            email = email,
            passwordHash = "google_authenticated_" + System.currentTimeMillis()
        )
        try {
            val generatedId = userDao.insertUser(newUser)
            val finalUser = newUser.copy(id = generatedId.toInt())
            _currentUser.value = finalUser
            prefs.edit().putInt("logged_in_user_id", finalUser.id).apply()
            return Result.success(finalUser)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    fun logout() {
        _currentUser.value = null
        prefs.edit().remove("logged_in_user_id").apply()
    }

    fun getLoggedUserId(): Int {
        return prefs.getInt("logged_in_user_id", -1)
    }
}
