package com.chuledigita.mobile.data.repository

import com.chuledigita.mobile.data.remote.ApiService
import com.chuledigita.mobile.data.remote.model.CreateCustomerRequest
import com.chuledigita.mobile.data.remote.model.CreatePaymentRequest
import com.chuledigita.mobile.data.remote.model.CreateNoteRequest
import com.chuledigita.mobile.data.remote.model.ApiErrorResponse
import com.chuledigita.mobile.domain.model.Customer
import com.chuledigita.mobile.domain.model.CustomerNote
import com.chuledigita.mobile.domain.model.CreditStatus
import com.chuledigita.mobile.domain.model.RemainingBalanceStatus
import com.chuledigita.mobile.domain.repository.CustomerRepository
import com.google.gson.Gson

/**
 * Concrete implementation of [CustomerRepository] executing network calls to the backend via Retrofit [ApiService].
 */
class CustomerRepositoryImpl(
    private val apiService: ApiService
) : CustomerRepository {

    private val gson = Gson()

    override suspend fun getCustomers(): Result<List<Customer>> {
        return try {
            val response = apiService.getAllCustomers()
            if (response.isSuccessful && response.body()?.success == true) {
                val list = response.body()?.data?.map { dto ->
                    Customer(
                        id = dto.id.toString(),
                        name = dto.fullName,
                        phone = dto.phone,
                        createdBy = dto.createdBy.toString(),
                        createdDate = dto.createdAt,
                        updatedDate = dto.createdAt,
                        registrationDate = dto.createdAt
                    )
                } ?: emptyList()
                Result.success(list)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), "Failed to fetch customers")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error while loading customers. Please verify backend availability."))
        }
    }

    override suspend fun getCustomerById(customerId: String): Result<Customer> {
        return try {
            val idLong = customerId.toLongOrNull() ?: return Result.failure(Exception("Invalid customer unique identifier format"))
            val response = apiService.getCustomerById(idLong)
            if (response.isSuccessful && response.body()?.success == true) {
                val dto = response.body()?.data
                if (dto != null) {
                    val customer = Customer(
                        id = dto.id.toString(),
                        name = dto.fullName,
                        phone = dto.phone,
                        createdBy = dto.createdBy.toString(),
                        createdDate = dto.createdAt,
                        updatedDate = dto.updatedAt,
                        registrationDate = dto.createdAt,
                        creditAmount = dto.totalDebit,
                        creditStatus = if (dto.balance > 0) CreditStatus.UNPAID_CREDIT else CreditStatus.PAID_CREDIT,
                        remainingBalanceAmount = dto.balance,
                        remainingBalanceStatus = if (dto.balance > 0) RemainingBalanceStatus.REMAINING_BALANCE else RemainingBalanceStatus.BALANCE_COLLECTED
                    )
                    Result.success(customer)
                } else {
                    Result.failure(Exception("Customer details payload empty"))
                }
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), "Customer profile look up failed")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error while looking up customer profile details."))
        }
    }

    override suspend fun createCustomer(name: String, phone: String?): Result<Customer> {
        return try {
            val response = apiService.createCustomer(CreateCustomerRequest(fullName = name, phone = phone))
            if (response.isSuccessful && response.body()?.success == true) {
                val dto = response.body()?.data
                if (dto != null) {
                    val customer = Customer(
                        id = dto.id.toString(),
                        name = dto.fullName,
                        phone = dto.phone,
                        createdBy = dto.createdBy.toString(),
                        createdDate = dto.createdAt,
                        updatedDate = dto.createdAt,
                        registrationDate = dto.createdAt
                    )
                    Result.success(customer)
                } else {
                    Result.failure(Exception("Empty payload on customer profile setup creation"))
                }
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), "Failed to create customer profile")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error while creating new customer profile profile."))
        }
    }

    override suspend fun recordPayment(
        customerId: String,
        amount: Double,
        description: String?,
        idempotencyKey: String?
    ): Result<Unit> {
        return try {
            val idLong = customerId.toLongOrNull() ?: return Result.failure(Exception("Invalid identifier format"))
            val response = apiService.createPayment(
                CreatePaymentRequest(
                    customerId = idLong,
                    amount = amount,
                    description = description,
                    referenceType = "CASH",
                    referenceId = null,
                    idempotencyKey = idempotencyKey
                )
            )
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), "Payment registration rejected by backend")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network connection error during payment collection processing."))
        }
    }

    override suspend fun getCustomerNotes(customerId: String): Result<List<CustomerNote>> {
        return try {
            val idLong = customerId.toLongOrNull() ?: return Result.failure(Exception("Invalid identifier format"))
            val response = apiService.getCustomerNotes(idLong)
            if (response.isSuccessful && response.body()?.success == true) {
                val notes = response.body()?.data?.map { dto ->
                    CustomerNote(
                        id = dto.id.toString(),
                        customerId = dto.customerId.toString(),
                        note = dto.noteText,
                        date = dto.createdAt,
                        authorName = "Family Member ID: ${dto.createdBy}"
                    )
                } ?: emptyList()
                Result.success(notes)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), "Failed to fetch tracking notes")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error while loading user narrative tracking notes."))
        }
    }

    override suspend fun createCustomerNote(customerId: String, text: String): Result<CustomerNote> {
        return try {
            val idLong = customerId.toLongOrNull() ?: return Result.failure(Exception("Invalid identifier format"))
            val response = apiService.createCustomerNote(idLong, CreateNoteRequest(noteText = text))
            if (response.isSuccessful && response.body()?.success == true) {
                val dto = response.body()?.data
                if (dto != null) {
                    val note = CustomerNote(
                        id = dto.id.toString(),
                        customerId = dto.customerId.toString(),
                        note = dto.noteText,
                        date = dto.createdAt,
                        authorName = "Family Member ID: ${dto.createdBy}"
                    )
                    Result.success(note)
                } else {
                    Result.failure(Exception("Empty data payload returned during customer tracking note submission"))
                }
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), "Failed to attach custom note")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error while submitting tracking entry note to backend."))
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
