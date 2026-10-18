package com.chuledigita.mobile.ui.screens

import android.util.Patterns
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
 * ViewModel managing registration form state and family member creation for [SignUpScreen].
 */
class SignUpViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SignUpUiState>(SignUpUiState.Idle)
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun register(
        fullName: String,
        phone: String,
        email: String,
        username: String,
        password: String
    ) {
        val trimmedFullName = fullName.trim()
        val trimmedPhone = phone.trim().ifBlank { null }
        val trimmedEmail = email.trim()
        val trimmedUsername = username.trim()

        if (trimmedFullName.isBlank()) {
            _uiState.value = SignUpUiState.Error("Name is required.")
            return
        }

        if (trimmedEmail.isBlank()) {
            _uiState.value = SignUpUiState.Error("Google account email is required.")
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            _uiState.value = SignUpUiState.Error("Please enter a valid Google account email address.")
            return
        }

        if (trimmedUsername.isBlank()) {
            _uiState.value = SignUpUiState.Error("Username is required.")
            return
        }

        if (password.isBlank()) {
            _uiState.value = SignUpUiState.Error("Password is required.")
            return
        }

        if (password.length < 4) {
            _uiState.value = SignUpUiState.Error("Password must be at least 4 characters.")
            return
        }

        viewModelScope.launch {
            _uiState.value = SignUpUiState.Loading
            val result = authRepository.register(
                fullName = trimmedFullName,
                phone = trimmedPhone,
                email = trimmedEmail,
                username = trimmedUsername,
                password = password
            )

            result.fold(
                onSuccess = { member ->
                    _uiState.value = SignUpUiState.Success(
                        familyMember = member,
                        message = "Account registered successfully! Please sign in with your username and password."
                    )
                },
                onFailure = { error ->
                    _uiState.value = SignUpUiState.Error(
                        error.message ?: "Registration failed. Please try again."
                    )
                }
            )
        }
    }

    fun resetState() {
        _uiState.value = SignUpUiState.Idle
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ChuleApplication)
                SignUpViewModel(authRepository = application.container.authRepository)
            }
        }
    }
}
