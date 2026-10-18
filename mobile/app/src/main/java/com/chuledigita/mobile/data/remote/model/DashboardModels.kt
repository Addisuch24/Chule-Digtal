package com.chuledigita.mobile.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * Remote response models for GET /dashboard endpoint.
 */
data class DashboardApiResponse(
    val success: Boolean,
    val message: String? = null,
    val data: DashboardApiData? = null
)

data class DashboardApiData(
    val customers: DashboardCustomersData? = null,
    val orders: DashboardOrdersData? = null,
    val notes: DashboardNotesData? = null,
    @SerializedName("financialMovements") val financialMovements: DashboardFinancialData? = null,
    @SerializedName("recentOrders") val recentOrders: List<DashboardRecentOrderDto>? = null,
    @SerializedName("recentNotes") val recentNotes: List<DashboardRecentNoteDto>? = null
)

data class DashboardCustomersData(
    val total: Int = 0,
    val withBalance: Int = 0,
    val settled: Int = 0,
    val withCredit: Int = 0
)

data class DashboardOrdersData(
    val total: Int = 0,
    val recent: Int = 0
)

data class DashboardNotesData(
    val total: Int = 0
)

data class DashboardFinancialData(
    val pending: Int = 0,
    val recentPayments: Int = 0,
    val totalDebit: Double = 0.0,
    val totalCredit: Double = 0.0,
    val balance: Double = 0.0
)

data class DashboardRecentOrderDto(
    val id: Long,
    val title: String? = null,
    @SerializedName("order_number") val orderNumber: String? = null,
    @SerializedName("customer_id") val customerId: Long? = null,
    @SerializedName("customer_name") val customerName: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class DashboardRecentNoteDto(
    val id: Long,
    @SerializedName("note_text") val noteText: String? = null,
    @SerializedName("created_by_name") val createdByName: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)
