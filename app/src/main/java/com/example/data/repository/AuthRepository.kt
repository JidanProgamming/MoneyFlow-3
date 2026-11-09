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
        // Seed default demo user in database if empty
        try {
            if (userDao.getUserCount() == 0) {
                userDao.insertUser(
                    User(
                        email = "user@moneyflow.com",
                        passwordHash = "123456",
                        fullName = "Pengguna MoneyFlow"
                    )
                )
            }
        } catch (_: Exception) {}

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
        val cleanEmail = email.trim().lowercase()
        val cleanPassword = passwordHash.trim()

        if (cleanEmail.isBlank() || cleanPassword.isBlank()) {
            return Result.failure(Exception("Email dan password tidak boleh kosong"))
        }

        val user = userDao.getUserByEmail(cleanEmail)
            ?: return Result.failure(Exception("Email tidak ditemukan dalam database. Silakan daftar akun baru."))

        if (user.passwordHash != cleanPassword) {
            return Result.failure(Exception("Password yang Anda masukkan salah. Silakan coba lagi."))
        }

        _currentUser.value = user
        prefs.edit().putInt("logged_in_user_id", user.id).apply()
        return Result.success(user)
    }

    suspend fun register(fullName: String, email: String, passwordHash: String): Result<User> {
        val cleanName = fullName.trim()
        val cleanEmail = email.trim().lowercase()
        val cleanPassword = passwordHash.trim()

        if (cleanName.isBlank() || cleanEmail.isBlank() || cleanPassword.isBlank()) {
            return Result.failure(Exception("Semua kolom (Nama Pengguna, Email, Password) wajib diisi"))
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return Result.failure(Exception("Format email tidak valid (contoh: nama@domain.com)"))
        }

        if (cleanPassword.length < 4) {
            return Result.failure(Exception("Password minimal harus 4 karakter"))
        }

        val existingUser = userDao.getUserByEmail(cleanEmail)
        if (existingUser != null) {
            return Result.failure(Exception("Email sudah terdaftar dalam database. Silakan login."))
        }

        val newUser = User(
            fullName = cleanName,
            email = cleanEmail,
            passwordHash = cleanPassword
        )

        try {
            val generatedId = userDao.insertUser(newUser)
            val finalUser = newUser.copy(id = generatedId.toInt())
            _currentUser.value = finalUser
            prefs.edit().putInt("logged_in_user_id", finalUser.id).apply()
            return Result.success(finalUser)
        } catch (e: Exception) {
            return Result.failure(Exception("Gagal menyimpan akun ke database: ${e.message}"))
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
