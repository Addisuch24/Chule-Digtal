package com.chuledigita.mobile.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object SignUp : Screen("signup")
    data object Dashboard : Screen("dashboard")
    data object Customers : Screen("customers")
    data object AddCustomer : Screen("add_customer")
    data object EditCustomer : Screen("edit_customer/{customerId}") {
        fun createRoute(customerId: String) = "edit_customer/$customerId"
    }
    data object CustomerDetails : Screen("customer_details/{customerId}") {
        fun createRoute(customerId: String) = "customer_details/$customerId"
    }
    data object CustomerNotes : Screen("customer_notes/{customerId}") {
        fun createRoute(customerId: String) = "customer_notes/$customerId"
    }
    data object Orders : Screen("orders")
    data object CreateOrder : Screen("create_order")
    data object OrderDetails : Screen("order_details/{orderId}") {
        fun createRoute(orderId: String) = "order_details/$orderId"
    }
    data object Notes : Screen("notes")
    data object NoteDetails : Screen("note_details/{noteId}") {
        fun createRoute(noteId: String) = "note_details/$noteId"
    }
    data object Finance : Screen("finance")
    data object FinancialMovements : Screen("financial_movements")
    data object RecordFinancialMovement : Screen("record_financial_movement")
    data object RecordDebit : Screen("record_debit")
    data object RecordCredit : Screen("record_credit")
    data object RecordPayment : Screen("record_payment")
    data object VerificationQueue : Screen("verification_queue")
    data object FinancialMovementDetails : Screen("financial_movement_details/{movementId}") {
        fun createRoute(movementId: String) = "financial_movement_details/$movementId"
    }
    data object Notifications : Screen("notifications")
    data object Profile : Screen("profile")
    data object Settings : Screen("settings")
    
    // For backwards compatibility during transition
    @Deprecated("Use Notes", ReplaceWith("Notes"))
    data object Activity : Screen("notes")
    @Deprecated("Use NoteDetails", ReplaceWith("NoteDetails"))
    data object ActivityDetails : Screen("note_details/{noteId}") {
        fun createRoute(noteId: String) = "note_details/$noteId"
    }
}
