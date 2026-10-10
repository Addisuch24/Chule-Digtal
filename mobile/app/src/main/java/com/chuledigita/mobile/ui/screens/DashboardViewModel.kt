package com.chuledigita.mobile.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chuledigita.mobile.ChuleApplication
import com.chuledigita.mobile.data.mock.MockDataProvider
import com.chuledigita.mobile.domain.model.VerificationStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Dashboard, managing mock data state for the UI/UX prototype.
 */
class DashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            // Simulate loading for UI/UX demonstration
            delay(1000)
            
            val pendingList = MockDataProvider.orders.filter { it.verificationStatus == VerificationStatus.PENDING }
            _uiState.value = DashboardUiState.Success(
                familyMember = MockDataProvider.currentFamilyMember,
                summary = MockDataProvider.dashboardSummary,
                recentActivities = MockDataProvider.recentActivities,
                pendingVerifications = pendingList,
                quickSummaryMessage = "Everything looks good today! You have ${pendingList.size} items to verify."
            )
        }
    }

    fun verifyOrder(orderId: String) {
        // Mock verification logic
        val currentState = _uiState.value
        if (currentState is DashboardUiState.Success) {
            val updatedVerifications = currentState.pendingVerifications.filter { it.id != orderId }
            _uiState.value = currentState.copy(
                pendingVerifications = updatedVerifications,
                quickSummaryMessage = if (updatedVerifications.isEmpty()) "All caught up!" else "Verified order #$orderId."
            )
        }
    }

    fun rejectOrder(orderId: String) {
        // Mock rejection logic
        val currentState = _uiState.value
        if (currentState is DashboardUiState.Success) {
            val updatedVerifications = currentState.pendingVerifications.filter { it.id != orderId }
            _uiState.value = currentState.copy(
                pendingVerifications = updatedVerifications,
                quickSummaryMessage = "Rejected order #$orderId."
            )
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        // Simple mock logout
        onLoggedOut()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DashboardViewModel()
            }
        }
    }
}
