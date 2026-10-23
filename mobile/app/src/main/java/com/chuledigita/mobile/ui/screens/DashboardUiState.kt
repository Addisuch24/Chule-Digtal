package com.chuledigita.mobile.ui.screens

data class DashboardRecentOrder(
    val id: String,
    val title: String,
    val date: String
)

data class DashboardRecentNote(
    val id: String,
    val title: String,
    val date: String
)

/**
 * UI state for the Dashboard matching the web overview.
 */
sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    
    data class Success(
        val notesCount: Int,
        val ordersCount: Int,
        val customersCount: Int,
        val currentBalance: Double,
        val totalDebit: Double = 0.0,
        val totalCredit: Double = 0.0,
        val recentOrders: List<DashboardRecentOrder> = emptyList(),
        val recentNotes: List<DashboardRecentNote> = emptyList()
    ) : DashboardUiState

    data class Error(val message: String) : DashboardUiState
}
