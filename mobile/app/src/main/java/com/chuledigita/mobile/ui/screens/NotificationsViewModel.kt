package com.chuledigita.mobile.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chuledigita.mobile.ChuleApplication
import com.chuledigita.mobile.domain.model.NotificationItem
import com.chuledigita.mobile.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI state for the Notifications screen.
 */
sealed interface NotificationsUiState {
    data object Loading : NotificationsUiState
    data class Success(val notifications: List<NotificationItem>) : NotificationsUiState
    data object Empty : NotificationsUiState
    data class Error(val message: String) : NotificationsUiState
}

class NotificationsViewModel(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val uiState: StateFlow<NotificationsUiState> = notificationRepository.notifications
        .combine(_isRefreshing) { notifications, refreshing ->
            if (notifications.isEmpty() && !refreshing) {
                NotificationsUiState.Empty
            } else if (notifications.isEmpty() && refreshing) {
                NotificationsUiState.Loading
            } else {
                NotificationsUiState.Success(notifications)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NotificationsUiState.Loading
        )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            notificationRepository.refreshNotifications()
            _isRefreshing.value = false
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ChuleApplication)
                NotificationsViewModel(notificationRepository = application.container.notificationRepository)
            }
        }
    }
}
