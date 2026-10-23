package com.chuledigita.mobile.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chuledigita.mobile.ChuleApplication
import com.chuledigita.mobile.domain.repository.AuthRepository
import com.chuledigita.mobile.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface SplashUiState {
    object Loading : SplashUiState
    object Authenticated : SplashUiState
    object Unauthenticated : SplashUiState
    data class Error(val message: String) : SplashUiState
}

class SplashViewModel(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        verifySession()
    }

    fun verifySession() {
        viewModelScope.launch {
            _uiState.value = SplashUiState.Loading
            
            val session = sessionRepository.userSessionFlow.first()
            if (session == null || session.token.isBlank()) {
                _uiState.value = SplashUiState.Unauthenticated
                return@launch
            }

            // Verify with backend
            val result = authRepository.getProfile()
            if (result.isSuccess) {
                _uiState.value = SplashUiState.Authenticated
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                if (errorMsg.contains("Failed to connect") || errorMsg.contains("Network error") || errorMsg.contains("Unable to connect")) {
                    _uiState.value = SplashUiState.Error(errorMsg)
                } else {
                    // Token is invalid/expired
                    sessionRepository.clearSession()
                    _uiState.value = SplashUiState.Unauthenticated
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ChuleApplication)
                SplashViewModel(
                    authRepository = application.container.authRepository,
                    sessionRepository = application.container.sessionRepository
                )
            }
        }
    }
}
