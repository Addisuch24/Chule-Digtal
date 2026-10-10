package com.chuledigita.mobile.data.repository

import com.chuledigita.mobile.data.remote.ApiService
import com.chuledigita.mobile.data.remote.model.CreateFinancialMovementRequest
import com.chuledigita.mobile.data.remote.model.VerificationRequest
import com.chuledigita.mobile.data.remote.model.ApiErrorResponse
import com.chuledigita.mobile.domain.model.FinancialMovement
import com.chuledigita.mobile.domain.model.MovementType
import com.chuledigita.mobile.domain.model.VerificationStatus
import com.chuledigita.mobile.domain.repository.FinancialRepository
import com.chuledigita.mobile.utils.DateTimeUtils
import com.google.gson.Gson

class FinancialRepositoryImpl(
    private val apiService: ApiService
) : FinancialRepository {

    private val gson = Gson()

    override suspend fun getAllMovements(): Result<List<FinancialMovement>> {
        return try {
            val response = apiService.getAllFinancialMovements()
            if (response.isSuccessful && response.body()?.success == true) {
                val list = response.body()?.data?.map { dto ->
                    FinancialMovement(
                        id = dto.id.toString(),
                        customerId = dto.customerId.toString(),
                        customerName = dto.customerName ?: "Customer #${dto.customerId}",
                        type = try { MovementType.valueOf(dto.movementType) } catch (e: Exception) { MovementType.DEBIT },
                        amount = dto.amount,
                        date = DateTimeUtils.formatDate(dto.transactionAt),
                        description = dto.description ?: "",
                        recordedBy = dto.recordedByName ?: "Member #${dto.recordedBy}",
                        verificationStatus = try { VerificationStatus.valueOf(dto.verificationStatus) } catch (e: Exception) { VerificationStatus.PENDING },
                        verificationNote = dto.verificationNote
                    )
                } ?: emptyList()
                Result.success(list)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), "Failed to fetch movements")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error while loading financial data"))
        }
    }

    override suspend fun getMovementById(movementId: String): Result<FinancialMovement> {
        return try {
            val idLong = movementId.toLongOrNull() ?: return Result.failure(Exception("Invalid ID"))
            val response = apiService.getFinancialMovementById(idLong)
            if (response.isSuccessful && response.body()?.success == true) {
                val dto = response.body()?.data!!
                Result.success(
                    FinancialMovement(
                        id = dto.id.toString(),
                        customerId = dto.customerId.toString(),
                        customerName = dto.customerName ?: "Customer #${dto.customerId}",
                        type = try { MovementType.valueOf(dto.movementType) } catch (e: Exception) { MovementType.DEBIT },
                        amount = dto.amount,
                        date = DateTimeUtils.formatDate(dto.transactionAt),
                        description = dto.description ?: "",
                        recordedBy = dto.recordedByName ?: "Member #${dto.recordedBy}",
                        verificationStatus = try { VerificationStatus.valueOf(dto.verificationStatus) } catch (e: Exception) { VerificationStatus.PENDING },
                        verificationNote = dto.verificationNote
                    )
                )
            } else {
                Result.failure(Exception("Movement not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createMovement(
        customerId: String,
        amount: Double,
        type: MovementType,
        description: String
    ): Result<FinancialMovement> {
        return try {
            val custIdLong = customerId.toLongOrNull() ?: return Result.failure(Exception("Invalid customer ID"))
            val response = apiService.createFinancialMovement(
                CreateFinancialMovementRequest(
                    customerId = custIdLong,
                    amount = amount,
                    movementType = type.name,
                    description = description
                )
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val dto = response.body()?.data!!
                Result.success(
                    FinancialMovement(
                        id = dto.id.toString(),
                        customerId = dto.customerId.toString(),
                        customerName = dto.customerName ?: "Customer",
                        type = try { MovementType.valueOf(dto.movementType) } catch (e: Exception) { MovementType.DEBIT },
                        amount = dto.amount,
                        date = DateTimeUtils.formatDate(dto.transactionAt),
                        description = dto.description ?: "",
                        recordedBy = dto.recordedByName ?: "Member",
                        verificationStatus = try { VerificationStatus.valueOf(dto.verificationStatus) } catch (e: Exception) { VerificationStatus.PENDING }
                    )
                )
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), "Failed to record movement")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error during transaction"))
        }
    }

    override suspend fun verifyMovement(movementId: String, note: String): Result<FinancialMovement> {
        return try {
            val idLong = movementId.toLongOrNull() ?: return Result.failure(Exception("Invalid ID"))
            val response = apiService.verifyFinancialMovement(idLong, VerificationRequest(note))
            if (response.isSuccessful && response.body()?.success == true) {
                val dto = response.body()?.data!!
                Result.success(
                    FinancialMovement(
                        id = dto.id.toString(),
                        customerId = dto.customerId.toString(),
                        customerName = dto.customerName ?: "Customer",
                        type = try { MovementType.valueOf(dto.movementType) } catch (e: Exception) { MovementType.DEBIT },
                        amount = dto.amount,
                        date = DateTimeUtils.formatDate(dto.transactionAt),
                        description = dto.description ?: "",
                        recordedBy = dto.recordedByName ?: "Member",
                        verificationStatus = VerificationStatus.VERIFIED,
                        verificationNote = dto.verificationNote
                    )
                )
            } else {
                Result.failure(Exception("Verification failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rejectMovement(movementId: String, note: String): Result<FinancialMovement> {
        return try {
            val idLong = movementId.toLongOrNull() ?: return Result.failure(Exception("Invalid ID"))
            val response = apiService.rejectFinancialMovement(idLong, VerificationRequest(note))
            if (response.isSuccessful && response.body()?.success == true) {
                val dto = response.body()?.data!!
                Result.success(
                    FinancialMovement(
                        id = dto.id.toString(),
                        customerId = dto.customerId.toString(),
                        customerName = dto.customerName ?: "Customer",
                        type = try { MovementType.valueOf(dto.movementType) } catch (e: Exception) { MovementType.DEBIT },
                        amount = dto.amount,
                        date = DateTimeUtils.formatDate(dto.transactionAt),
                        description = dto.description ?: "",
                        recordedBy = dto.recordedByName ?: "Member",
                        verificationStatus = VerificationStatus.REJECTED,
                        verificationNote = dto.verificationNote
                    )
                )
            } else {
                Result.failure(Exception("Rejection failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseErrorMessage(errorBodyJson: String?, fallback: String): String {
        if (errorBodyJson.isNullOrBlank()) return fallback
        return try {
            val errorObj = gson.fromJson(errorBodyJson, ApiErrorResponse::class.java)
            errorObj.message ?: fallback
        } catch (e: Exception) {
            fallback
        }
    }
}
