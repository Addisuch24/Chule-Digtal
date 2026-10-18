package com.chuledigita.mobile.domain.repository

import com.chuledigita.mobile.domain.model.FinancialMovement
import com.chuledigita.mobile.domain.model.MovementType

/**
 * Domain repository for managing financial transactions and verifications.
 */
interface FinancialRepository {
    /**
     * Retrieves all financial movements (debits and credits).
     */
    suspend fun getAllMovements(): Result<List<FinancialMovement>>

    /**
     * Retrieves details for a specific financial movement.
     */
    suspend fun getMovementById(movementId: String): Result<FinancialMovement>

    /**
     * Records a new manual financial adjustment.
     */
    suspend fun createMovement(
        customerId: String,
        amount: Double,
        type: MovementType,
        description: String
    ): Result<FinancialMovement>

    /**
     * Updates an existing financial movement.
     */
    suspend fun updateMovement(
        movementId: String,
        customerId: String,
        amount: Double,
        type: MovementType,
        description: String
    ): Result<FinancialMovement>

    /**
     * Deletes a financial movement.
     */
    suspend fun deleteMovement(movementId: String): Result<Unit>

    /**
     * Verifies a pending financial movement.
     */
    suspend fun verifyMovement(movementId: String, note: String): Result<FinancialMovement>

    /**
     * Rejects a pending financial movement.
     */
    suspend fun rejectMovement(movementId: String, note: String): Result<FinancialMovement>
}
