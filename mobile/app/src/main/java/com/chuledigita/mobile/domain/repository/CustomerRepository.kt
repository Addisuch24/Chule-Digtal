package com.chuledigita.mobile.domain.repository

import com.chuledigita.mobile.domain.model.Customer
import com.chuledigita.mobile.domain.model.CustomerNote

/**
 * Domain repository contract defining data operations for business customers and their notes.
 */
interface CustomerRepository {

    /**
     * Fetches all registered business customers from the backend.
     */
    suspend fun getCustomers(): Result<List<Customer>>

    /**
     * Fetches details and financial summary balance for a single customer.
     */
    suspend fun getCustomerById(customerId: String): Result<Customer>

    /**
     * Creates a new business customer profile on the backend server.
     */
    suspend fun createCustomer(name: String, phone: String?): Result<Customer>

    /**
     * Updates an existing customer profile without changing ID or creating duplicate rows.
     */
    suspend fun updateCustomer(customerId: String, name: String, phone: String?): Result<Customer>

    /**
     * Records a secure credit payment for an outstanding balance amount.
     */
    suspend fun recordPayment(
        customerId: String,
        amount: Double,
        description: String?,
        idempotencyKey: String?
    ): Result<Unit>

    /**
     * Retrieves chronological communication or tracking notes for a customer profile.
     */
    suspend fun getCustomerNotes(customerId: String): Result<List<CustomerNote>>

    /**
     * Appends a real tracking text note entry into a customer's history logs.
     */
    suspend fun createCustomerNote(customerId: String, text: String): Result<CustomerNote>

    /**
     * Deletes a single customer by identifier. Fails if linked financial movements exist.
     */
    suspend fun deleteCustomer(customerId: String): Result<Unit>

    /**
     * Deletes multiple customers, skipping customers with linked financial movements to preserve accounting audit integrity.
     */
    suspend fun bulkDeleteCustomers(customerIds: List<String>): Result<com.chuledigita.mobile.data.remote.model.BulkDeleteData>
}
