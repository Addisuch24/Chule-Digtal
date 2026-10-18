package com.chuledigita.mobile.ui.screens

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * ViewModel for the Dashboard, managing real overview metrics and recent records from backend.
 */
class DashboardViewModel(
    private val _apiService: ApiService,
    private val _noteRepository: NoteRepository,
    private val _orderRepository: OrderRepository,
    private val _financialRepository: FinancialRepository,
    private val _customerRepository: CustomerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            try {
                val response = _apiService.getDashboardSummary()
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data
                    val customersCount = data?.customers?.total ?: 0
                    val ordersCount = data?.orders?.total ?: 0
                    val notesCount = data?.notes?.total ?: 0
                    val currentBalance = data?.financialMovements?.balance ?: 0.0

                    val recentOrders = data?.recentOrders?.map { orderDto ->
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

                    val recentNotes = data?.recentNotes?.map { noteDto ->
                        val displayTitle = noteDto.noteText?.lineSequence()?.firstOrNull { it.isNotBlank() }
                            ?: "ማስታወሻ #${noteDto.id}"

                        DashboardRecentNote(
                            id = noteDto.id.toString(),
                            title = displayTitle,
                            date = formatDisplayDate(noteDto.createdAt)
                        )
                    } ?: emptyList()

                    _uiState.value = DashboardUiState.Success(
                        notesCount = notesCount,
                        ordersCount = ordersCount,
                        customersCount = customersCount,
                        currentBalance = currentBalance,
                        recentOrders = recentOrders,
                        recentNotes = recentNotes
                    )
                } else {
                    val message = response.message().takeIf { !it.isNullOrBlank() }
                        ?: "ዳሽቦርድ መረጃን መጫን አልተቻለም። እባክዎ እንደገና ይሞክሩ።"
                    _uiState.value = DashboardUiState.Error(message)
                }
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error(
                    e.message ?: "የኔትወርክ ስህተት አጋጥሟል። እባክዎ የኢንተርኔት ግንኙነትዎን ያረጋግጡ።"
                )
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
                    _customerRepository = container.customerRepository
                )
            }
        }
    }
}
