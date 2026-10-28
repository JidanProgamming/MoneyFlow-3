package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.User
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent: SharedFlow<String> = _errorEvent.asSharedFlow()

    private val _authSuccessEvent = MutableSharedFlow<Unit>()
    val authSuccessEvent: SharedFlow<Unit> = _authSuccessEvent.asSharedFlow()

    init {
        checkAutoLogin()
    }

    fun checkAutoLogin() {
        viewModelScope.launch {
            val user = authRepository.loadSavedUser()
            _currentUser.value = user
        }
    }

    fun login(email: String, passwordHash: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.login(email, passwordHash)
            _isLoading.value = false
            result.onSuccess {
                _currentUser.value = it
                _authSuccessEvent.emit(Unit)
            }.onFailure {
                _errorEvent.emit(it.message ?: "Login gagal")
            }
        }
    }

    fun register(fullName: String, email: String, passwordHash: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.register(fullName, email, passwordHash)
            _isLoading.value = false
            result.onSuccess {
                _currentUser.value = it
                _authSuccessEvent.emit(Unit)
            }.onFailure {
                _errorEvent.emit(it.message ?: "Pendaftaran gagal")
            }
        }
    }

    fun loginWithGoogle(email: String, fullName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.loginOrRegisterGoogleUser(email, fullName)
            _isLoading.value = false
            result.onSuccess {
                _currentUser.value = it
                _authSuccessEvent.emit(Unit)
            }.onFailure {
                _errorEvent.emit(it.message ?: "Google login gagal")
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _currentUser.value = null
    }
}
