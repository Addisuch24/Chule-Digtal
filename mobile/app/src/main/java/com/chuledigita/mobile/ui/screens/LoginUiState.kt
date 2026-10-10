package com.chuledigita.mobile.ui.screens

import com.chuledigita.mobile.domain.model.UserSession

/**
 * Sealed interface representing UI state for [LoginScreen].
 */
sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Success(val userSession: UserSession) : LoginUiState
    data class Error(val message: String) : LoginUiState
}
