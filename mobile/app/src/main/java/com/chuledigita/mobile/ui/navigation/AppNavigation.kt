package com.chuledigita.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chuledigita.mobile.ui.screens.*

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Customers.route) {
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
                    navController.navigate(Screen.Customers.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                },
                onBackToLogin = {
                    navController.popBackStack()
                }
            )
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

        composable(route = Screen.Orders.route) {
            OrdersScreen(navController = navController)
        }

        composable(
            route = Screen.OrderDetails.route,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            OrderDetailsScreen(orderId = orderId, navController = navController)
        }

        composable(route = Screen.Notes.route) {
            NotesScreen(navController = navController)
        }

        composable(
            route = Screen.NoteDetails.route,
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
            NoteDetailsScreen(noteId = noteId, navController = navController)
        }

        // Keep existing routes for compatibility, but they are not in main nav
        composable(route = Screen.Dashboard.route) {
            DashboardScreen(navController = navController, onLogout = {})
        }
        composable(route = Screen.Finance.route) {
            FinanceScreen(navController = navController, onLogout = {})
        }
        composable(route = Screen.Notifications.route) {
            NotificationsScreen(navController = navController, onLogout = {})
        }
        composable(route = Screen.Profile.route) {
            ProfileScreen(navController = navController, onLogout = {})
        }
        composable(route = Screen.Settings.route) {
            SettingsScreen(navController = navController)
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
}
