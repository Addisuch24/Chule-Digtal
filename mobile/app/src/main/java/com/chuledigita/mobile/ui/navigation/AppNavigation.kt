package com.chuledigita.mobile.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    activityIntentFlow: kotlinx.coroutines.flow.SharedFlow<android.content.Intent>? = null
) {
    val context = LocalContext.current
    val application = context.applicationContext as ChuleApplication
    val notificationRepo = application.container.notificationRepository
    val notifNavManager = application.container.notificationNavigationManager
    val deviceTokenManager = application.container.deviceTokenManager
    val sessionRepo = application.container.sessionRepository
    val inAppManager = notificationRepo.inAppNotificationManager
    val popUpState by inAppManager.currentPopUp.collectAsStateWithLifecycle()

    val logoutAction: () -> Unit = {
        SessionPreferences.setLoggedIn(context, false)
        inAppManager.clearSession()
        deviceTokenManager.logoutDevice()
        CoroutineScope(Dispatchers.IO).launch {
            sessionRepo.clearSession()
        }
        navController.navigate(Screen.Login.route) {
            popUpTo(0) { inclusive = true }
        }
    }

    val processIntent: (android.content.Intent) -> Unit = { intent ->
        val notifId = intent.getStringExtra("NOTIFICATION_ID")
        val notifType = intent.getStringExtra("NOTIFICATION_TYPE")
        val entityType = intent.getStringExtra("ENTITY_TYPE")
        val entityId = intent.getStringExtra("ENTITY_ID")

        if (!notifId.isNullOrBlank() || !notifType.isNullOrBlank() || !entityId.isNullOrBlank()) {
            val intentKey = "${notifId ?: "none"}_${notifType}_${entityId}"
            if (!notifNavManager.isIntentAlreadyHandled(intentKey)) {
                notifNavManager.markIntentHandled(intentKey)

                // Clean extras from intent to prevent accidental duplicate reads
                intent.removeExtra("NOTIFICATION_TYPE")
                intent.removeExtra("ENTITY_ID")
                intent.removeExtra("NOTIFICATION_ID")
                intent.removeExtra("ENTITY_TYPE")

                val targetRoute = notifNavManager.resolveRoute(notifType, entityType, entityId)
                val isCurrentlyOnLoginScreen = navController.currentDestination?.route == Screen.Login.route
                val isLoggedIn = SessionPreferences.isLoggedIn(context) && !sessionRepo.getCachedToken().isNullOrBlank()

                if (isLoggedIn && !isCurrentlyOnLoginScreen) {
                    // Valid active session: navigate directly to target without displaying login
                    navController.navigate(targetRoute)
                } else {
                    // Logged out or expired session: securely preserve target destination
                    notifNavManager.setPendingDestination(targetRoute, notifId)
                    if (!isCurrentlyOnLoginScreen) {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            }
        }
    }

    // 1. Handle intent emissions via activityIntentFlow
    if (activityIntentFlow != null) {
        androidx.compose.runtime.LaunchedEffect(activityIntentFlow) {
            activityIntentFlow.collect { intent ->
                processIntent(intent)
            }
        }
    } else {
        // Fallback for previews or standalone Compose tests
        val activity = context as? androidx.activity.ComponentActivity
        androidx.compose.runtime.LaunchedEffect(activity?.intent) {
            activity?.intent?.let { processIntent(it) }
        }
    }

    val handlePopUpClick: (InAppPopUpState) -> Unit = { state ->
        inAppManager.dismiss()
        val targetRoute: String? = when (state) {
            is InAppPopUpState.Single -> {
                val item = state.item
                CoroutineScope(Dispatchers.IO).launch {
                    notificationRepo.markAsRead(item.id)
                }
                notifNavManager.resolveRoute(item.type.name, item.entityType, item.entityId)
            }
            is InAppPopUpState.Summary -> {
                Screen.Notifications.route
            }
        }

        if (targetRoute != null) {
            val isCurrentlyOnLoginScreen = navController.currentDestination?.route == Screen.Login.route
            val isLoggedIn = SessionPreferences.isLoggedIn(context) && !sessionRepo.getCachedToken().isNullOrBlank()
            if (isLoggedIn && !isCurrentlyOnLoginScreen) {
                navController.navigate(targetRoute)
            } else {
                notifNavManager.setPendingDestination(targetRoute)
                if (!isCurrentlyOnLoginScreen) {
                    navController.navigate(Screen.Login.route)
                }
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
                    // Associate user session with device push token
                    CoroutineScope(Dispatchers.IO).launch {
                        val session = sessionRepo.userSessionFlow.first()
                        session?.let { deviceTokenManager.registerDevice(it.id) }
                    }
                    val target = notifNavManager.consumePendingDestination()
                    val destination = target ?: Screen.Dashboard.route
                    navController.navigate(destination) {
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
