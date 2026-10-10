package com.chuledigita.mobile.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chuledigita.mobile.ChuleApplication
import com.chuledigita.mobile.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing login form state and authentication operations for [LoginScreen].
 */
class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(username: String, password: String) {
        val trimmedUsername = username.trim()
        
        if (trimmedUsername.isBlank()) {
            _uiState.value = LoginUiState.Error("Username is required.")
            return
        }

        if (password.isBlank()) {
            _uiState.value = LoginUiState.Error("Password is required.")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = authRepository.login(trimmedUsername, password)
            result.fold(
                onSuccess = { session ->
                    _uiState.value = LoginUiState.Success(session)
                },
                onFailure = { exception ->
                    _uiState.value = LoginUiState.Error(exception.message ?: "Invalid username or password.")
                }
            )
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ChuleApplication)
                LoginViewModel(authRepository = application.container.authRepository)
            }
        }
    }
}
