package com.chuledigita.mobile.ui.screens

import com.chuledigita.mobile.domain.model.DashboardSummary
import com.chuledigita.mobile.domain.model.Order
import com.chuledigita.mobile.domain.model.FamilyMember
import com.chuledigita.mobile.domain.model.RecentActivity

/**
 * UI state for the Dashboard, supporting loading, success, and empty states.
 */
sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    
    data class Success(
        val familyMember: FamilyMember,
        val summary: DashboardSummary,
        val recentActivities: List<RecentActivity>,
        val pendingVerifications: List<Order>,
        val quickSummaryMessage: String
    ) : DashboardUiState

    data object Empty : DashboardUiState
}
