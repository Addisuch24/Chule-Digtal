package com.chuledigita.mobile.notifications

import com.chuledigita.mobile.domain.model.NotificationItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Collections

sealed class InAppPopUpState {
    data class Single(val item: NotificationItem) : InAppPopUpState()
    data class Summary(val count: Int, val latestItem: NotificationItem) : InAppPopUpState()
}

/**
 * Manages the presentation, deduplication, queuing, and auto-dismissal
 * of in-app notification pop-ups.
 */
class InAppNotificationManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    companion object {
        const val POPUP_DURATION_MS = 4500L
        const val MAX_OFFLINE_INDIVIDUAL_SHOW = 2
    }

    private val _currentPopUp = MutableStateFlow<InAppPopUpState?>(null)
    val currentPopUp: StateFlow<InAppPopUpState?> = _currentPopUp.asStateFlow()

    // Thread-safe set of notification IDs displayed during this client session
    val presentedNotificationIds: MutableSet<String> = Collections.synchronizedSet(mutableSetOf<String>())

    private val pendingQueue = ArrayDeque<NotificationItem>()
    private var dismissJob: Job? = null

    /**
     * Shows a single notification as an in-app pop-up if it has not been presented yet.
     */
    fun showNotification(item: NotificationItem) {
        if (!presentedNotificationIds.add(item.id)) {
            // Already presented this session; do not duplicate
            return
        }

        if (_currentPopUp.value == null) {
            displayPopUp(InAppPopUpState.Single(item))
        } else {
            synchronized(pendingQueue) {
                if (pendingQueue.size < MAX_OFFLINE_INDIVIDUAL_SHOW) {
                    pendingQueue.add(item)
                } else {
                    // Too many queued; aggregate into summary
                    val total = pendingQueue.size + 1
                    pendingQueue.clear()
                    displayPopUp(InAppPopUpState.Summary(total, item))
                }
            }
        }
    }

    /**
     * Handles a batch of notifications retrieved from server (e.g., on app launch or reconnect).
     * Prevents flooding the screen:
     * - If 0 unread: do nothing.
     * - If 1-2: queue individually.
     * - If 3+: show a clean aggregated summary pop-up.
     */
    fun showBatchFromRecovery(items: List<NotificationItem>) {
        val unpresented = items.filter { !presentedNotificationIds.contains(it.id) }
        if (unpresented.isEmpty()) return

        // Mark all as presented in this session to prevent future duplicate prompts
        unpresented.forEach { presentedNotificationIds.add(it.id) }

        if (unpresented.size <= MAX_OFFLINE_INDIVIDUAL_SHOW) {
            unpresented.forEach { item ->
                if (_currentPopUp.value == null) {
                    displayPopUp(InAppPopUpState.Single(item))
                } else {
                    synchronized(pendingQueue) {
                        pendingQueue.add(item)
                    }
                }
            }
        } else {
            // Aggregate summary pop-up
            val latest = unpresented.first()
            displayPopUp(InAppPopUpState.Summary(count = unpresented.size, latestItem = latest))
        }
    }

    /**
     * Dismisses the currently displayed pop-up and advances the queue.
     */
    fun dismiss() {
        dismissJob?.cancel()
        dismissJob = null
        advanceQueue()
    }

    private fun displayPopUp(state: InAppPopUpState) {
        dismissJob?.cancel()
        _currentPopUp.value = state

        dismissJob = scope.launch {
            delay(POPUP_DURATION_MS)
            advanceQueue()
        }
    }

    private fun advanceQueue() {
        synchronized(pendingQueue) {
            val next = pendingQueue.removeFirstOrNull()
            if (next != null) {
                displayPopUp(InAppPopUpState.Single(next))
            } else {
                _currentPopUp.value = null
            }
        }
    }

    /**
     * Clears all session tracking on logout.
     */
    fun clearSession() {
        dismissJob?.cancel()
        dismissJob = null
        synchronized(pendingQueue) {
            pendingQueue.clear()
        }
        presentedNotificationIds.clear()
        _currentPopUp.value = null
    }
}
