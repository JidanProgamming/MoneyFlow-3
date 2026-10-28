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
        // For premium user experience, handle simple input checks first
        if (email.isBlank() || passwordHash.isBlank()) {
            return Result.failure(Exception("Email dan password tidak boleh kosong"))
        }
        val user = userDao.getUserByEmail(email) ?: return Result.failure(Exception("Akun tidak ditemukan"))
        if (user.passwordHash != passwordHash) {
            return Result.failure(Exception("Password salah"))
        }
        _currentUser.value = user
        prefs.edit().putInt("logged_in_user_id", user.id).apply()
        return Result.success(user)
    }

    suspend fun register(fullName: String, email: String, passwordHash: String): Result<User> {
        if (fullName.isBlank() || email.isBlank() || passwordHash.isBlank()) {
            return Result.failure(Exception("Semua formulir harus diisi"))
        }
        val existingUser = userDao.getUserByEmail(email)
        if (existingUser != null) {
            return Result.failure(Exception("Email sudah terdaftar"))
        }
        val newUser = User(fullName = fullName, email = email, passwordHash = passwordHash)
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
