package com.chuledigita.mobile.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chuledigita.mobile.ChuleApplication
import com.chuledigita.mobile.data.remote.ApiService
import com.chuledigita.mobile.domain.repository.CustomerRepository
import com.chuledigita.mobile.domain.repository.FinancialRepository
import com.chuledigita.mobile.domain.repository.NoteRepository
import com.chuledigita.mobile.domain.repository.OrderRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * ViewModel for the Dashboard, managing real overview metrics and recent records from backend.
 * Provides foreground periodic refresh and silent lifecycle sync to keep metrics up to date
 * even when Socket.IO is unavailable.
 */
class DashboardViewModel(
    private val _apiService: ApiService,
    private val _noteRepository: NoteRepository,
    private val _orderRepository: OrderRepository,
    private val _financialRepository: FinancialRepository,
    private val _customerRepository: CustomerRepository,
    private val _notificationRepository: com.chuledigita.mobile.domain.repository.NotificationRepository? = null,
    private val _socketManager: com.chuledigita.mobile.data.remote.SocketManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard(isSilent = false)

        // 1. Refresh whenever a real-time notification event arrives
        _notificationRepository?.let { repo ->
            viewModelScope.launch {
                repo.realtimeEventFlow.collect { event ->
                    Log.d("DashboardVM", "Real-time notification event received (${event.type}); silently refreshing dashboard")
                    loadDashboard(isSilent = true)
                }
            }
        }

        // 2. Refresh whenever Socket.IO dashboard event arrives (for persistent socket environments)
        _socketManager?.let { sm ->
            viewModelScope.launch {
                sm.dashboardRefreshFlow.collect {
                    Log.d("DashboardVM", "Socket dashboard refresh event received; silently refreshing dashboard")
                    loadDashboard(isSilent = true)
                }
            }
        }

        // 3. Foreground periodic refresh loop: while ViewModel is active and Socket.IO is not connected,
        // periodically refresh dashboard data so metrics stay up to date without user pull-to-refresh
        viewModelScope.launch {
            while (isActive) {
                delay(30_000L)
                if (!isActive) break
                val isFg = _notificationRepository?.isForeground ?: true
                val socketActive = _socketManager?.isConnected() ?: false
                if (isFg && !socketActive) {
                    Log.d("DashboardVM", "Foreground periodic dashboard refresh tick triggered")
                    loadDashboard(isSilent = true)
                }
            }
        }
    }

    fun loadDashboard(isSilent: Boolean = false) {
        viewModelScope.launch {
            if (!isSilent || _uiState.value !is DashboardUiState.Success) {
                _uiState.value = DashboardUiState.Loading
            }
            Log.d("DashboardVM", "Loading dashboard summary (isSilent=$isSilent)")
            try {
                var customersCount = 0
                var ordersCount = 0
                var notesCount = 0
                var currentBalance = 0.0
                var totalDebit = 0.0
                var totalCredit = 0.0
                var recentOrders = emptyList<DashboardRecentOrder>()
                var recentNotes = emptyList<DashboardRecentNote>()

                val response = _apiService.getDashboardSummary()
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data
                    customersCount = data?.customers?.total ?: 0
                    ordersCount = data?.orders?.total ?: 0
                    notesCount = data?.notes?.total ?: 0
                    currentBalance = data?.financialMovements?.balance ?: 0.0
                    totalDebit = data?.financialMovements?.totalDebit ?: 0.0
                    totalCredit = data?.financialMovements?.totalCredit ?: 0.0

                    recentOrders = data?.recentOrders?.map { orderDto ->
                        val displayTitle = orderDto.title?.takeIf { it.isNotBlank() }
                            ?: orderDto.customerName?.takeIf { it.isNotBlank() }
                            ?: orderDto.orderNumber?.takeIf { it.isNotBlank() }
                            ?: "ትዕዛዝ #${orderDto.id}"

                        DashboardRecentOrder(
                            id = orderDto.id.toString(),
                            title = displayTitle,
                            date = formatDisplayDate(orderDto.createdAt)
                        )
                    } ?: emptyList()

                    recentNotes = data?.recentNotes?.map { noteDto ->
                        val displayTitle = noteDto.noteText?.lineSequence()?.firstOrNull { it.isNotBlank() }
                            ?: "ማስታወሻ #${noteDto.id}"

                        DashboardRecentNote(
                            id = noteDto.id.toString(),
                            title = displayTitle,
                            date = formatDisplayDate(noteDto.createdAt)
                        )
                    } ?: emptyList()
                    Log.d("DashboardVM", "getDashboardSummary success: customers=$customersCount, orders=$ordersCount, notes=$notesCount, balance=$currentBalance")
                } else {
                    Log.w("DashboardVM", "getDashboardSummary HTTP error: ${response.code()}")
                }

                // Explicitly refresh global notes count to guarantee 100% freshness when refreshing dashboard
                val globalNotesResult = _noteRepository.getGlobalNotes()
                if (globalNotesResult.isSuccess) {
                    val freshNotes = globalNotesResult.getOrNull() ?: emptyList()
                    notesCount = freshNotes.size
                }

                // Explicitly refresh customers count to guarantee 100% real count directly from database
                val customersResult = _customerRepository.getCustomers()
                if (customersResult.isSuccess) {
                    val freshCustomers = customersResult.getOrNull() ?: emptyList()
                    customersCount = freshCustomers.size
                }

                _uiState.value = DashboardUiState.Success(
                    notesCount = notesCount,
                    ordersCount = ordersCount,
                    customersCount = customersCount,
                    currentBalance = currentBalance,
                    totalDebit = totalDebit,
                    totalCredit = totalCredit,
                    recentOrders = recentOrders,
                    recentNotes = recentNotes
                )
                Log.i("DashboardVM", "Dashboard UI state successfully updated (isSilent=$isSilent)")
            } catch (e: Exception) {
                Log.e("DashboardVM", "Exception loading dashboard (isSilent=$isSilent): ${e.message}", e)
                if (isSilent && _uiState.value is DashboardUiState.Success) {
                    // Do not replace successful state with error on silent background refresh
                    Log.d("DashboardVM", "Preserving existing dashboard state after silent refresh failure")
                    return@launch
                }
                // Fallback: try loading from repositories if dashboard summary fails
                try {
                    val globalNotesResult = _noteRepository.getGlobalNotes()
                    val notesCount = globalNotesResult.getOrNull()?.size ?: 0
                    val customersResult = _customerRepository.getCustomers()
                    val custCount = customersResult.getOrNull()?.size ?: 0
                    _uiState.value = DashboardUiState.Success(
                        notesCount = notesCount,
                        ordersCount = 0,
                        customersCount = custCount,
                        currentBalance = 0.0,
                        totalDebit = 0.0,
                        totalCredit = 0.0
                    )
                } catch (_: Exception) {
                    _uiState.value = DashboardUiState.Error(
                        e.message ?: "የኔትወርክ ስህተት አጋጥሟል። እባክዎ የኢንተርኔት ግንኙነትዎን ያረጋግጡ።"
                    )
                }
            }
        }
    }

    private fun formatDisplayDate(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return ""
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = inputFormat.parse(dateStr)
            if (date != null) {
                val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                outputFormat.format(date)
            } else {
                dateStr.substringBefore("T")
            }
        } catch (e: Exception) {
            try {
                dateStr.substringBefore("T")
            } catch (ex: Exception) {
                dateStr
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ChuleApplication)
                val container = application.container
                DashboardViewModel(
                    _apiService = container.apiService,
                    _noteRepository = container.noteRepository,
                    _orderRepository = container.orderRepository,
                    _financialRepository = container.financialRepository,
                    _customerRepository = container.customerRepository,
                    _notificationRepository = container.notificationRepository,
                    _socketManager = container.socketManager
                )
            }
        }
    }
}
