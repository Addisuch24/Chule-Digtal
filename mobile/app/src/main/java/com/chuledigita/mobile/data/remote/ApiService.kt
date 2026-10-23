package com.chuledigita.mobile.data.remote

import com.chuledigita.mobile.data.remote.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API Service interface for Chule Digital backend.
 */
interface ApiService {

    // ============================================================
    // AUTHENTICATION
    // ============================================================

    @GET("health")
    suspend fun getHealthStatus(): Response<HealthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @GET("auth/me")
    suspend fun getProfile(): Response<ProfileResponse>

    // ============================================================
    // CUSTOMERS
    // ============================================================

    @POST("customers")
    suspend fun createCustomer(@Body request: CreateCustomerRequest): Response<CustomerResponse>

    @GET("customers")
    suspend fun getAllCustomers(): Response<CustomerListResponse>

    @GET("customers/{customerId}")
    suspend fun getCustomerById(@Path("customerId") customerId: Long): Response<CustomerDetailResponse>

    @GET("customers/{customerId}/balance")
    suspend fun getCustomerBalance(@Path("customerId") customerId: Long): Response<CustomerBalanceResponse>

    @DELETE("customers/{customerId}")
    suspend fun deleteCustomer(@Path("customerId") customerId: Long): Response<Void>

    @PATCH("customers/{customerId}")
    suspend fun updateCustomer(
        @Path("customerId") customerId: Long,
        @Body request: CreateCustomerRequest
    ): Response<CustomerResponse>

    @POST("customers/bulk-delete")
    suspend fun bulkDeleteCustomers(@Body request: BulkDeleteCustomersRequest): Response<BulkDeleteResponse>

    // ============================================================
    // DASHBOARD
    // ============================================================

    @GET("dashboard")
    suspend fun getDashboardSummary(): Response<DashboardApiResponse>

    // ============================================================
    // ORDERS
    // ============================================================

    @POST("orders/customers/{customerId}/orders")
    suspend fun createOrder(
        @Path("customerId") customerId: Long,
        @Body request: CreateOrderRequest
    ): Response<OrderResponse>

    @POST("orders")
    suspend fun createOrderDirect(
        @Body request: CreateOrderDirectRequest
    ): Response<OrderResponse>

    @GET("orders")
    suspend fun getAllOrders(): Response<OrderListResponse>

    @GET("orders/{orderId}")
    suspend fun getOrderById(@Path("orderId") orderId: Long): Response<OrderResponse>

    @PATCH("orders/{orderId}")
    suspend fun updateOrder(
        @Path("orderId") orderId: Long,
        @Body request: UpdateOrderRequest
    ): Response<OrderResponse>

    @DELETE("orders/{orderId}")
    suspend fun deleteOrder(@Path("orderId") orderId: Long): Response<Void>

    @POST("orders/bulk-delete")
    suspend fun bulkDeleteOrders(@Body request: BulkDeleteOrdersRequest): Response<BulkDeleteResponse>

    @PATCH("orders/{orderId}/status")
    suspend fun updateOrderStatus(
        @Path("orderId") orderId: Long,
        @Body request: UpdateOrderStatusRequest
    ): Response<OrderResponse>

    @PATCH("orders/{orderId}/verify")
    suspend fun verifyOrder(
        @Path("orderId") orderId: Long,
        @Body request: VerificationRequest
    ): Response<OrderResponse>

    @PATCH("orders/{orderId}/reject")
    suspend fun rejectOrder(
        @Path("orderId") orderId: Long,
        @Body request: VerificationRequest
    ): Response<OrderResponse>

    // ============================================================
    // CUSTOMER NOTES
    // ============================================================

    @POST("customers/{customerId}/notes")
    suspend fun createCustomerNote(
        @Path("customerId") customerId: Long,
        @Body request: CreateNoteRequest
    ): Response<NoteResponse>

    @GET("customers/{customerId}/notes")
    suspend fun getCustomerNotes(@Path("customerId") customerId: Long): Response<NoteListResponse>

    @DELETE("customers/{customerId}/notes/{noteId}")
    suspend fun deleteCustomerNote(
        @Path("customerId") customerId: Long,
        @Path("noteId") noteId: Long
    ): Response<Void>

    @POST("customers/{customerId}/notes/bulk-delete")
    suspend fun bulkDeleteCustomerNotes(
        @Path("customerId") customerId: Long,
        @Body request: BulkDeleteNotesRequest
    ): Response<BulkDeleteResponse>

    // ============================================================
    // GLOBAL BUSINESS NOTES
    // ============================================================

    @POST("notes")
    suspend fun createGlobalNote(@Body request: CreateNoteRequest): Response<BusinessNoteResponse>

    @GET("notes")
    suspend fun getAllGlobalNotes(): Response<BusinessNoteListResponse>

    @PATCH("notes/{noteId}")
    suspend fun updateGlobalNote(
        @Path("noteId") noteId: Long,
        @Body request: CreateNoteRequest
    ): Response<BusinessNoteResponse>

    @DELETE("notes/{noteId}")
    suspend fun deleteGlobalNote(
        @Path("noteId") noteId: Long
    ): Response<Void>

    @POST("notes/bulk-delete")
    suspend fun bulkDeleteGlobalNotes(
        @Body request: BulkDeleteNotesRequest
    ): Response<BulkDeleteResponse>

    // ============================================================
    // FINANCIAL MOVEMENTS
    // ============================================================

    @POST("financial-movements")
    suspend fun createFinancialMovement(@Body request: CreateFinancialMovementRequest): Response<FinancialMovementResponse>

    @GET("financial-movements")
    suspend fun getAllFinancialMovements(): Response<FinancialMovementListResponse>

    @GET("financial-movements/{movementId}")
    suspend fun getFinancialMovementById(@Path("movementId") movementId: Long): Response<FinancialMovementResponse>

    @PATCH("financial-movements/{movementId}")
    suspend fun updateFinancialMovement(
        @Path("movementId") movementId: Long,
        @Body request: CreateFinancialMovementRequest
    ): Response<FinancialMovementResponse>

    @DELETE("financial-movements/{movementId}")
    suspend fun deleteFinancialMovement(@Path("movementId") movementId: Long): Response<Void>

    @POST("financial-movements/bulk-delete")
    suspend fun bulkDeleteFinancialMovements(
        @Body request: BulkDeleteMovementsRequest
    ): Response<BulkDeleteResponse>

    @PATCH("financial-movements/{movementId}/verify")
    suspend fun verifyFinancialMovement(
        @Path("movementId") movementId: Long,
        @Body request: VerificationRequest
    ): Response<FinancialMovementResponse>

    @PATCH("financial-movements/{movementId}/reject")
    suspend fun rejectFinancialMovement(
        @Path("movementId") movementId: Long,
        @Body request: VerificationRequest
    ): Response<FinancialMovementResponse>

    @POST("financial-movements/payments")
    suspend fun createPayment(@Body request: CreatePaymentRequest): Response<PaymentResponse>

    @GET("financial-movements/customer/{customerId}/payments")
    suspend fun getCustomerPayments(@Path("customerId") customerId: Long): Response<FinancialMovementListResponse>

    // ============================================================
    // NOTIFICATIONS
    // ============================================================

    @GET("notifications/recipient/{recipientId}")
    suspend fun getNotifications(@Path("recipientId") recipientId: Long): Response<NotificationListResponse>

    @PATCH("notifications/{notificationId}/read")
    suspend fun markAsRead(@Path("notificationId") notificationId: Long): Response<NotificationResponse>

    @PATCH("notifications/read-all")
    suspend fun markAllAsRead(): Response<Void>

    @DELETE("notifications/{notificationId}")
    suspend fun deleteNotification(@Path("notificationId") notificationId: Long): Response<Void>

    @POST("notifications/bulk-delete")
    suspend fun bulkDeleteNotifications(
        @Body request: BulkDeleteNotificationsRequest
    ): Response<BulkDeleteResponse>

    @GET("notifications/recipient/{recipientId}/unread-count")
    suspend fun getUnreadCount(@Path("recipientId") recipientId: Long): Response<UnreadCountResponse>
}

data class HealthResponse(
    val success: Boolean,
    val message: String
)
