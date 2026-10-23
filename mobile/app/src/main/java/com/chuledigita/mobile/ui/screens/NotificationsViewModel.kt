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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * UI state for the Notifications screen.
 */
sealed interface NotificationsUiState {
    data object Loading : NotificationsUiState
    data class Success(
        val notifications: List<NotificationItem>,
        val selectedNotificationIds: Set<String> = emptySet(),
        val isDeletingBatch: Boolean = false,
        val actionMessage: String? = null,
        val actionError: String? = null
    ) : NotificationsUiState
    data object Empty : NotificationsUiState
    data class Error(val message: String) : NotificationsUiState
}

private data class NotificationExtraState(
    val selectedNotificationIds: Set<String> = emptySet(),
    val isDeletingBatch: Boolean = false,
    val actionMessage: String? = null,
    val actionError: String? = null
)

class NotificationsViewModel(
    private val notificationRepository: NotificationRepository,
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _extraState = MutableStateFlow(NotificationExtraState())

    val uiState: StateFlow<NotificationsUiState> = combine(
        notificationRepository.notifications,
        _isRefreshing,
        _extraState
    ) { notifications, refreshing, extra ->
        if (notifications.isEmpty() && !refreshing) {
            NotificationsUiState.Empty
        } else if (notifications.isEmpty() && refreshing) {
            NotificationsUiState.Loading
        } else {
            NotificationsUiState.Success(
                notifications = notifications,
                selectedNotificationIds = extra.selectedNotificationIds,
                isDeletingBatch = extra.isDeletingBatch,
                actionMessage = extra.actionMessage,
                actionError = extra.actionError
            )
        }
    }.stateIn(
        scope = scope,
        started = if (externalScope != null) SharingStarted.Eagerly else SharingStarted.WhileSubscribed(5000),
        initialValue = NotificationsUiState.Loading
    )

    init {
        refresh()
    }

    fun refresh() {
        scope.launch {
            _isRefreshing.value = true
            notificationRepository.refreshNotifications()
            _isRefreshing.value = false
        }
    }

    fun markAsRead(notificationId: String) {
        scope.launch {
            notificationRepository.markAsRead(notificationId)
        }
    }

    fun markAllAsRead() {
        scope.launch {
            notificationRepository.markAllAsRead()
        }
    }

    fun toggleSelectNotification(notificationId: String) {
        val current = _extraState.value.selectedNotificationIds
        val updated = if (current.contains(notificationId)) {
            current - notificationId
        } else {
            current + notificationId
        }
        _extraState.value = _extraState.value.copy(selectedNotificationIds = updated)
    }

    fun selectAllNotifications(ids: List<String>) {
        _extraState.value = _extraState.value.copy(selectedNotificationIds = ids.toSet())
    }

    fun clearNotificationSelection() {
        _extraState.value = _extraState.value.copy(selectedNotificationIds = emptySet())
    }

    fun clearActionMessages() {
        _extraState.value = _extraState.value.copy(actionMessage = null, actionError = null)
    }

    fun bulkDeleteSelectedNotifications(onComplete: ((Int, String) -> Unit)? = null) {
        val selected = _extraState.value.selectedNotificationIds.toList()
        if (selected.isEmpty()) return

        _extraState.value = _extraState.value.copy(
            isDeletingBatch = true,
            actionMessage = null,
            actionError = null
        )

        scope.launch {
            val result = notificationRepository.bulkDeleteNotifications(selected)
            result.onSuccess { data ->
                val count = data.deletedCount ?: data.deletedIds?.size ?: selected.size
                val msg = "$count ማሳወቂያዎች በተሳካ ሁኔታ ተሰርዘዋል።"
                _extraState.value = _extraState.value.copy(
                    selectedNotificationIds = emptySet(),
                    isDeletingBatch = false,
                    actionMessage = msg
                )
                onComplete?.invoke(count, msg)
            }.onFailure { err ->
                val errText = err.message ?: "Failed to delete notifications"
                _extraState.value = _extraState.value.copy(
                    isDeletingBatch = false,
                    actionError = errText
                )
                onComplete?.invoke(0, errText)
            }
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
