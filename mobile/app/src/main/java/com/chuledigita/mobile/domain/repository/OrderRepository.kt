package com.chuledigita.mobile.domain.repository

import com.chuledigita.mobile.domain.model.Order
import com.chuledigita.mobile.domain.model.OrderStatus

/**
 * Domain repository contract defining data operations for processing customer orders.
 */
interface OrderRepository {

    /**
     * Fetches all orders from the backend server.
     */
    suspend fun getOrders(): Result<List<Order>>

    /**
     * Fetches details for a specific order.
     */
    suspend fun getOrderById(orderId: String): Result<Order>

    /**
     * Creates a new order entry under a specific customer identifier.
     */
    suspend fun createOrder(customerId: String, orderNumber: String, description: String?): Result<Order>

    /**
     * Creates an order with title and details matching web interface.
     */
    suspend fun createOrderDirect(orderNumber: String, title: String, description: String, customerId: String? = null): Result<Order>

    /**
     * Updates an order's title and description.
     */
    suspend fun updateOrder(orderId: String, title: String?, description: String?): Result<Order>

    /**
     * Deletes an order.
     */
    suspend fun deleteOrder(orderId: String): Result<Unit>

    /**
     * Deletes multiple orders, preserving orders linked to financial records for accounting audit integrity.
     */
    suspend fun bulkDeleteOrders(orderIds: List<String>): Result<com.chuledigita.mobile.data.remote.model.BulkDeleteData>

    /**
     * Updates the status of an existing order profile.
     */
    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Order>

    /**
     * Verifies an order.
     */
    suspend fun verifyOrder(orderId: String, verificationNote: String? = null): Result<Order>

    /**
     * Rejects an order with a mandatory verification note.
     */
    suspend fun rejectOrder(orderId: String, verificationNote: String): Result<Order>
}
