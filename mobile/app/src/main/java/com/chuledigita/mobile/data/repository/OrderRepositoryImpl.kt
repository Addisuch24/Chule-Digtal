package com.chuledigita.mobile.data.repository

import com.chuledigita.mobile.data.remote.ApiService
import com.chuledigita.mobile.data.remote.model.CreateOrderRequest
import com.chuledigita.mobile.data.remote.model.UpdateOrderStatusRequest
import com.chuledigita.mobile.data.remote.model.ApiErrorResponse
import com.chuledigita.mobile.domain.model.Order
import com.chuledigita.mobile.domain.model.OrderStatus
import com.chuledigita.mobile.domain.model.VerificationStatus
import com.chuledigita.mobile.domain.repository.OrderRepository
import com.chuledigita.mobile.utils.DateTimeUtils
import com.google.gson.Gson

/**
 * Concrete implementation of [OrderRepository] executing network calls to the backend via Retrofit [ApiService].
 * Maps backend OrderDto to domain Order model with clear creator/actor distinction.
 */
class OrderRepositoryImpl(
    private val apiService: ApiService
) : OrderRepository {

    private val gson = Gson()

    override suspend fun getOrders(): Result<List<Order>> {
        return try {
            val response = apiService.getAllOrders()
            if (response.isSuccessful && response.body()?.success == true) {
                val list = response.body()?.data?.map { dto ->
                    mapDtoToOrder(dto)
                } ?: emptyList()
                Result.success(list)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), "Failed to retrieve order listings")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error while accessing order list registry from server."))
        }
    }

    override suspend fun getOrderById(orderId: String): Result<Order> {
        return try {
            val idLong = orderId.toLongOrNull() ?: return Result.failure(Exception("Invalid order lookup identifier format"))
            val response = apiService.getOrderById(idLong)
            if (response.isSuccessful && response.body()?.success == true) {
                val dto = response.body()?.data
                if (dto != null) {
                    Result.success(mapDtoToOrder(dto))
                } else {
                    Result.failure(Exception("Empty payload body returned for specified order identifier lookup"))
                }
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), "Order details lookup failed")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error while pulling order transaction detail data record."))
        }
    }

    override suspend fun createOrder(customerId: String, orderNumber: String, description: String?): Result<Order> {
        return try {
            val custIdLong = customerId.toLongOrNull() ?: return Result.failure(Exception("Invalid customer identifier specification"))
            val response = apiService.createOrder(
                customerId = custIdLong,
                request = CreateOrderRequest(orderNumber = orderNumber, description = description)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val dto = response.body()?.data
                if (dto != null) {
                    Result.success(mapDtoToOrder(dto))
                } else {
                    Result.failure(Exception("Empty confirmation payload body returned on order creation"))
                }
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), "Failed to provision new customer order")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error during new order registry setup pipeline."))
        }
    }

    override suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Order> {
        return try {
            val idLong = orderId.toLongOrNull() ?: return Result.failure(Exception("Invalid order target"))
            val response = apiService.updateOrderStatus(
                orderId = idLong,
                request = UpdateOrderStatusRequest(orderStatus = status.name)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val dto = response.body()?.data
                if (dto != null) {
                    Result.success(mapDtoToOrder(dto))
                } else {
                    Result.failure(Exception("Empty state modification package returned"))
                }
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), "Failed to update order status")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error while updating state for current order item."))
        }
    }

    private fun mapDtoToOrder(dto: com.chuledigita.mobile.data.remote.model.OrderDto): Order {
        val domainStatus = try {
            OrderStatus.valueOf(dto.orderStatus)
        } catch (e: Exception) {
            OrderStatus.PENDING
        }

        val domainVerification = try {
            VerificationStatus.valueOf(dto.verificationStatus)
        } catch (e: Exception) {
            VerificationStatus.PENDING
        }

        return Order(
            id = dto.id.toString(),
            orderNumber = dto.orderNumber,
            customerId = dto.customerId.toString(),
            customerName = dto.customerName ?: "Customer #${dto.customerId}",
            description = dto.description ?: "",
            orderDate = DateTimeUtils.formatDate(dto.createdAt),
            orderTime = DateTimeUtils.formatTime(dto.createdAt),
            totalAmount = 0.0, 
            status = domainStatus,
            verificationStatus = domainVerification,
            verificationNote = dto.verificationNote,
            takenByFamilyMember = dto.takenByName ?: "Family Member #${dto.takenBy}",
            // Backend gap: completion actor and time not explicitly tracked in order table for non-verification status changes.
            // Using updated_at as a fallback for completion time if status is COMPLETED.
            completionActor = if (domainStatus == OrderStatus.COMPLETED) (dto.verifiedByName ?: dto.takenByName) else null,
            completionDate = if (domainStatus == OrderStatus.COMPLETED) DateTimeUtils.formatDate(dto.updatedAt) else null,
            completionTime = if (domainStatus == OrderStatus.COMPLETED) DateTimeUtils.formatTime(dto.updatedAt) else null,
            isVerified = domainVerification == VerificationStatus.VERIFIED
        )
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
