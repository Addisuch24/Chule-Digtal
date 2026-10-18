package com.chuledigita.mobile.data.repository

import com.chuledigita.mobile.data.remote.ApiService
import com.chuledigita.mobile.data.remote.model.CreateOrderRequest
import com.chuledigita.mobile.data.remote.model.CreateOrderDirectRequest
import com.chuledigita.mobile.data.remote.model.UpdateOrderRequest
import com.chuledigita.mobile.data.remote.model.UpdateOrderStatusRequest
import com.chuledigita.mobile.data.remote.model.VerificationRequest
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

    override suspend fun createOrderDirect(
        orderNumber: String,
        title: String,
        description: String,
        customerId: String?
    ): Result<Order> {
        return try {
            val custIdLong = customerId?.toLongOrNull()
            val response = apiService.createOrderDirect(
                CreateOrderDirectRequest(
                    orderNumber = orderNumber,
                    title = title,
                    description = description,
                    customerId = custIdLong
                )
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val dto = response.body()?.data
                if (dto != null) {
                    Result.success(mapDtoToOrder(dto))
                } else {
                    Result.failure(Exception("ባዶ ምላሽ ደርሷል።"))
                }
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), "ትዕዛዝ መፍጠር አልተቻለም።")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("የአውታረ መረብ ችግር አጋጥሟል።"))
        }
    }

    override suspend fun updateOrder(orderId: String, title: String?, description: String?): Result<Order> {
        return try {
            val idLong = orderId.toLongOrNull() ?: return Result.failure(Exception("ትክክለኛ ያልሆነ የትዕዛዝ መለያ"))
            val response = apiService.updateOrder(
                orderId = idLong,
                request = UpdateOrderRequest(title = title, description = description)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val dto = response.body()?.data
                if (dto != null) {
                    Result.success(mapDtoToOrder(dto))
                } else {
                    Result.failure(Exception("ባዶ ምላሽ ደርሷል።"))
                }
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), "ትዕዛዝ ማስተካከል አልተቻለም።")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("የአውታረ መረብ ችግር አጋጥሟል።"))
        }
    }

    override suspend fun deleteOrder(orderId: String): Result<Unit> {
        return try {
            val idLong = orderId.toLongOrNull() ?: return Result.failure(Exception("ትክክለኛ ያልሆነ የትዕዛዝ መለያ"))
            val response = apiService.deleteOrder(idLong)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), "ትዕዛዝ መሰረዝ አልተቻለም።")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("የአውታረ መረብ ችግር አጋጥሟል።"))
        }
    }

    override suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Order> {
        return try {
            val idLong = orderId.toLongOrNull() ?: return Result.failure(Exception("ትክክለኛ ያልሆነ የትዕዛዝ መለያ"))
            val response = apiService.updateOrderStatus(
                orderId = idLong,
                request = UpdateOrderStatusRequest(orderStatus = status.name)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val dto = response.body()?.data
                if (dto != null) {
                    Result.success(mapDtoToOrder(dto))
                } else {
                    Result.failure(Exception("ባዶ ምላሽ ደርሷል።"))
                }
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), "የትዕዛዝ ሁኔታ መቀየር አልተቻለም።")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("የአውታረ መረብ ችግር አጋጥሟል።"))
        }
    }

    override suspend fun verifyOrder(orderId: String, verificationNote: String?): Result<Order> {
        return try {
            val idLong = orderId.toLongOrNull() ?: return Result.failure(Exception("ትክክለኛ ያልሆነ የትዕዛዝ መለያ"))
            val response = apiService.verifyOrder(
                orderId = idLong,
                request = VerificationRequest(verificationNote = verificationNote ?: "")
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val dto = response.body()?.data
                if (dto != null) {
                    Result.success(mapDtoToOrder(dto))
                } else {
                    Result.failure(Exception("ባዶ ምላሽ ደርሷል።"))
                }
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), "ትዕዛዝ ማረጋገጥ አልተቻለም።")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("የአውታረ መረብ ችግር አጋጥሟል።"))
        }
    }

    override suspend fun rejectOrder(orderId: String, verificationNote: String): Result<Order> {
        return try {
            val idLong = orderId.toLongOrNull() ?: return Result.failure(Exception("ትክክለኛ ያልሆነ የትዕዛዝ መለያ"))
            val response = apiService.rejectOrder(
                orderId = idLong,
                request = VerificationRequest(verificationNote = verificationNote)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val dto = response.body()?.data
                if (dto != null) {
                    Result.success(mapDtoToOrder(dto))
                } else {
                    Result.failure(Exception("ባዶ ምላሽ ደርሷል።"))
                }
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), "ትዕዛዝ ውድቅ ማድረግ አልተቻለም።")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("የአውታረ መረብ ችግር አጋጥሟል።"))
        }
    }

    private fun mapDtoToOrder(dto: com.chuledigita.mobile.data.remote.model.OrderDto): Order {
        val domainStatus = dto.orderStatus?.let { statusStr ->
            try {
                OrderStatus.valueOf(statusStr)
            } catch (e: Exception) {
                OrderStatus.PENDING
            }
        } ?: OrderStatus.PENDING

        val domainVerification = dto.verificationStatus?.let { verStr ->
            try {
                VerificationStatus.valueOf(verStr)
            } catch (e: Exception) {
                VerificationStatus.PENDING
            }
        } ?: VerificationStatus.PENDING

        val takenByText = dto.takenByName
            ?: dto.takenBy?.let { "Family Member #$it" }
            ?: "የቤተሰብ አባል"

        return Order(
            id = dto.id.toString(),
            title = dto.title,
            orderNumber = dto.orderNumber,
            customerId = dto.customerId?.toString() ?: "",
            customerName = dto.customerName ?: if (dto.customerId != null) "Customer #${dto.customerId}" else "አጠቃላይ ደንበኛ",
            description = dto.description ?: "",
            orderDate = DateTimeUtils.formatDate(dto.createdAt),
            orderTime = DateTimeUtils.formatTime(dto.createdAt),
            totalAmount = 0.0,
            status = domainStatus,
            verificationStatus = domainVerification,
            verificationNote = dto.verificationNote,
            takenByFamilyMember = takenByText,
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
