package com.chuledigita.mobile.domain.model

enum class MockOrderStatus {
    REGISTERED,
    IN_PROGRESS,
    DELIVERED
}

data class MockOrder(
    val id: String,
    val orderTitle: String,
    val orderDescription: String,
    val orderSize: String,
    val createdById: String,
    val createdByName: String,
    val createdDate: String,
    val createdTime: String,
    var status: MockOrderStatus,
    var statusUpdatedById: String? = null,
    var statusUpdatedByName: String? = null,
    var statusUpdatedDate: String? = null,
    var statusUpdatedTime: String? = null,
    val actionHistory: MutableList<OrderAction>
)

data class OrderAction(
    val id: String,
    val orderId: String,
    val actionType: String,
    val actorFamilyMemberId: String,
    val actorFamilyMemberName: String,
    val description: String,
    val previousStatus: MockOrderStatus?,
    val newStatus: MockOrderStatus?,
    val date: String,
    val time: String
)
