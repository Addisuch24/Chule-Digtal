package com.chuledigita.mobile.ui.screens

import com.chuledigita.mobile.domain.model.FamilyMember

/**
 * Sealed interface representing UI state for [SignUpScreen].
 */
sealed interface SignUpUiState {
    data object Idle : SignUpUiState
    data object Loading : SignUpUiState
    data class Success(val familyMember: FamilyMember, val message: String) : SignUpUiState
    data class Error(val message: String) : SignUpUiState
}
