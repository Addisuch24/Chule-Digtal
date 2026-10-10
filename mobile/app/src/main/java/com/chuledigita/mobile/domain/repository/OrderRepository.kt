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
     * Updates the status of an existing order profile.
     */
    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Order>
}
