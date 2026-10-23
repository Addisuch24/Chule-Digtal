package com.chuledigita.mobile.domain.model

enum class OrderStatus {
    DRAFT, PENDING, CONFIRMED, PROCESSING, READY, COMPLETED, CANCELLED
}

enum class VerificationStatus {
    NOT_REQUIRED, PENDING, VERIFIED, REJECTED
}

enum class MovementType {
    DEBIT, CREDIT
}

enum class PaymentMethod {
    CASH, BANK, MOBILE_MONEY, OTHER
}

/**
 * Maps to backend activity and notification types.
 */
enum class NotificationType {
    CUSTOMER_CREATED,
    CUSTOMER_UPDATED,
    CUSTOMER_DELETED,
    CUSTOMER_BULK_DELETED,
    CUSTOMER_NOTE_CREATED,
    BUSINESS_NOTE_CREATED,
    ORDER_CREATED,
    ORDER_VERIFIED,
    ORDER_REJECTED,
    ORDER_STATUS_UPDATED,
    FINANCIAL_MOVEMENT_CREATED,
    FINANCIAL_MOVEMENT_VERIFIED,
    FINANCIAL_MOVEMENT_REJECTED,
    FINANCIAL_MOVEMENT_DELETED,
    FINANCIAL_MOVEMENT_BULK_DELETED,
    PAYMENT_RECORDED,
    SYSTEM
}

enum class ActivityType {
    CUSTOMER_CREATED, CUSTOMER_EDITED, CUSTOMER_NOTE_ADDED,
    ORDER_CREATED, ORDER_STATUS_CHANGED, ORDER_VERIFIED, ORDER_REJECTED,
    CREDIT_RECORDED, CREDIT_PAID, BALANCE_RECORDED, BALANCE_COLLECTED,
    BUSINESS_NOTE_CREATED, ORDER_STATUS_UPDATED
}

enum class CreditStatus {
    UNPAID_CREDIT, // ተበድሮታል
    PAID_CREDIT    // ከፍሏል
}

enum class RemainingBalanceStatus {
    REMAINING_BALANCE, // ቀሪ ሂሳብ
    BALANCE_COLLECTED  // ተቀብሏል
}

data class Customer(
    val id: String,
    val name: String,
    val phone: String? = null,
    val detailNote: String? = null,
    val createdBy: String,
    val updatedBy: String? = null,
    val createdDate: String,
    val updatedDate: String,
    val registrationDate: String = "",
    
    // Independent Financial Flows
    val creditAmount: Double = 0.0,
    val creditStatus: CreditStatus? = null,
    
    val remainingBalanceAmount: Double = 0.0,
    val remainingBalanceStatus: RemainingBalanceStatus? = null,
    
    val actionHistory: List<ActionHistory> = emptyList()
)

data class CustomerNote(
    val id: String,
    val customerId: String,
    val note: String,
    val date: String,
    val time: String = "",
    val authorName: String
)

data class Order(
    val id: String,
    val orderNumber: String,
    val customerId: String,
    val customerName: String,
    val description: String,
    val orderDate: String,
    val orderTime: String = "",
    val totalAmount: Double,
    val status: OrderStatus,
    val verificationStatus: VerificationStatus,
    val verificationNote: String? = null,
    val takenByFamilyMember: String,
    val completionActor: String? = null,
    val completionDate: String? = null,
    val completionTime: String? = null,
    val isVerified: Boolean = false,
    val itemsCount: Int = 0,
    val history: List<Activity> = emptyList(),
    val title: String? = null
)

data class FinancialMovement(
    val id: String,
    val customerId: String,
    val customerName: String,
    val type: MovementType,
    val amount: Double,
    val date: String,
    val description: String,
    val recordedBy: String,
    val verificationStatus: VerificationStatus,
    val verificationNote: String? = null,
    val paymentMethod: PaymentMethod? = null
)

data class ActionHistory(
    val actionId: String,
    val actionType: ActivityType,
    val actorFamilyMemberId: String,
    val actorFamilyMemberName: String,
    val actionDescription: String,
    val relatedCustomerId: String,
    val amount: Double? = null,
    val previousStatus: String? = null,
    val newStatus: String? = null,
    val date: String,
    val time: String
)

data class Activity(
    val id: String,
    val actor: String,
    val type: ActivityType,
    val relatedId: String,
    val relatedName: String,
    val timestamp: String,
    val description: String,
    val previousValue: String? = null,
    val newValue: String? = null,
    val createdBy: String
)

data class RecentActivity(
    val id: String,
    val title: String,
    val actor: String,
    val time: String,
    val type: String,
    val status: String? = null
)

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val date: String,
    val time: String = "",
    val isRead: Boolean,
    val type: NotificationType,
    val actor: String? = null,
    val activityId: String? = null,
    val entityType: String? = null,
    val entityId: String? = null
)

data class FinanceSummary(
    val totalDebit: Double,
    val totalCredit: Double,
    val outstandingBalance: Double,
    val pendingVerifications: Int
)

data class DashboardSummary(
    val totalCustomers: Int,
    val pendingOrders: Int,
    val outstandingBalance: Double,
    val unreadNotifications: Int
)
