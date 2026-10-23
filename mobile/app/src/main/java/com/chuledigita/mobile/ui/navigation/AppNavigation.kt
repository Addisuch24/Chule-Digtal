package com.chuledigita.mobile.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chuledigita.mobile.ChuleApplication
import com.chuledigita.mobile.data.local.SessionPreferences
import com.chuledigita.mobile.domain.model.NotificationType
import com.chuledigita.mobile.notifications.InAppPopUpState
import com.chuledigita.mobile.ui.components.InAppNotificationBanner
import com.chuledigita.mobile.ui.screens.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val application = context.applicationContext as ChuleApplication
    val notificationRepo = application.container.notificationRepository
    val inAppManager = notificationRepo.inAppNotificationManager
    val popUpState by inAppManager.currentPopUp.collectAsStateWithLifecycle()

    val logoutAction: () -> Unit = {
        SessionPreferences.setLoggedIn(context, false)
        inAppManager.clearSession()
        CoroutineScope(Dispatchers.IO).launch {
            application.container.sessionRepository.clearSession()
        }
        navController.navigate(Screen.Login.route) {
            popUpTo(0) { inclusive = true }
        }
    }

    val handlePopUpClick: (InAppPopUpState) -> Unit = { state ->
        inAppManager.dismiss()
        when (state) {
            is InAppPopUpState.Single -> {
                val item = state.item
                CoroutineScope(Dispatchers.IO).launch {
                    notificationRepo.markAsRead(item.id)
                }
                when (item.type) {
                    NotificationType.ORDER_CREATED,
                    NotificationType.ORDER_VERIFIED,
                    NotificationType.ORDER_REJECTED,
                    NotificationType.ORDER_STATUS_UPDATED -> {
                        val entityId = item.entityId
                        if (!entityId.isNullOrBlank()) {
                            navController.navigate(Screen.OrderDetails.createRoute(entityId))
                        } else {
                            navController.navigate(Screen.Orders.route)
                        }
                    }
                    NotificationType.BUSINESS_NOTE_CREATED -> {
                        val entityId = item.entityId
                        if (!entityId.isNullOrBlank()) {
                            navController.navigate(Screen.NoteDetails.createRoute(entityId))
                        } else {
                            navController.navigate(Screen.Notes.route)
                        }
                    }
                    NotificationType.CUSTOMER_NOTE_CREATED,
                    NotificationType.CUSTOMER_CREATED,
                    NotificationType.CUSTOMER_UPDATED,
                    NotificationType.CUSTOMER_DELETED,
                    NotificationType.CUSTOMER_BULK_DELETED -> {
                        val entityId = item.entityId
                        if (!entityId.isNullOrBlank() && item.type != NotificationType.CUSTOMER_DELETED && item.type != NotificationType.CUSTOMER_BULK_DELETED) {
                            navController.navigate(Screen.CustomerDetails.createRoute(entityId))
                        } else {
                            navController.navigate(Screen.Customers.route)
                        }
                    }
                    NotificationType.FINANCIAL_MOVEMENT_CREATED,
                    NotificationType.FINANCIAL_MOVEMENT_VERIFIED,
                    NotificationType.FINANCIAL_MOVEMENT_REJECTED,
                    NotificationType.FINANCIAL_MOVEMENT_DELETED,
                    NotificationType.FINANCIAL_MOVEMENT_BULK_DELETED,
                    NotificationType.PAYMENT_RECORDED -> {
                        navController.navigate(Screen.Finance.route)
                    }
                    NotificationType.SYSTEM -> {
                        navController.navigate(Screen.Notifications.route)
                    }
                }
            }
            is InAppPopUpState.Summary -> {
                navController.navigate(Screen.Notifications.route)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route
        ) {
        composable(route = Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    SessionPreferences.setLoggedIn(context, true)
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onSignUpClick = {
                    navController.navigate(Screen.SignUp.route)
                }
            )
        }

        composable(route = Screen.SignUp.route) {
            SignUpScreen(
                onSignUpSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                },
                onBackToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.Dashboard.route) {
            DashboardScreen(navController = navController, onLogout = logoutAction)
        }

        composable(route = Screen.Notes.route) {
            NotesScreen(navController = navController)
        }

        composable(route = Screen.Orders.route) {
            OrdersScreen(navController = navController)
        }

        composable(route = Screen.Finance.route) {
            FinanceScreen(navController = navController)
        }

        composable(route = Screen.Customers.route) {
            CustomersScreen(navController = navController)
        }

        composable(route = Screen.AddCustomer.route) {
            AddCustomerScreen(navController = navController)
        }

        composable(
            route = Screen.EditCustomer.route,
            arguments = listOf(navArgument("customerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
            EditCustomerScreen(customerId = customerId, navController = navController)
        }

        composable(
            route = Screen.CustomerDetails.route,
            arguments = listOf(navArgument("customerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
            CustomerDetailsScreen(customerId = customerId, navController = navController)
        }

        composable(
            route = Screen.OrderDetails.route,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            OrderDetailsScreen(orderId = orderId, navController = navController)
        }

        composable(
            route = Screen.NoteDetails.route,
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
            NoteDetailsScreen(noteId = noteId, navController = navController)
        }

        composable(route = Screen.Notifications.route) {
            NotificationsScreen(navController = navController, onLogout = logoutAction)
        }
        composable(route = Screen.Profile.route) {
            ProfileScreen(navController = navController, onLogout = logoutAction)
        }
        composable(route = Screen.Settings.route) {
            SettingsScreen(navController = navController, onLogout = logoutAction)
        }
        composable(route = Screen.FinancialMovements.route) {
            FinancialMovementsScreen(navController = navController)
        }
        composable(route = Screen.RecordFinancialMovement.route) {
            RecordFinancialMovementScreen(navController = navController)
        }
        composable(route = Screen.RecordDebit.route) {
            RecordDebitScreen(navController = navController)
        }
        composable(route = Screen.RecordCredit.route) {
            RecordCreditScreen(navController = navController)
        }
        composable(route = Screen.RecordPayment.route) {
            RecordPaymentScreen(navController = navController)
        }
        composable(route = Screen.VerificationQueue.route) {
            VerificationQueueScreen(navController = navController)
        }
        composable(
            route = Screen.FinancialMovementDetails.route,
            arguments = listOf(navArgument("movementId") { type = NavType.StringType })
        ) { backStackEntry ->
            val movementId = backStackEntry.arguments?.getString("movementId") ?: ""
            FinancialMovementDetailsScreen(
                movementId = movementId,
                navController = navController
            )
        }
        composable(
            route = Screen.CustomerNotes.route,
            arguments = listOf(navArgument("customerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
            CustomerNotesScreen(
                customerId = customerId,
                navController = navController
            )
        }
        composable(route = Screen.CreateOrder.route) {
            CreateOrderScreen(navController = navController)
        }
    }

    InAppNotificationBanner(
        popUpState = popUpState,
        onDismiss = { inAppManager.dismiss() },
        onPopUpClick = handlePopUpClick,
        modifier = Modifier.align(Alignment.TopCenter)
    )
}
}
