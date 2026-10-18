package com.chuledigita.mobile.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.chuledigita.mobile.ui.navigation.Screen

/**
 * Represents a navigation item in the bottom navigation bar.
 *
 * @property screen The target [Screen] associated with this navigation item.
 * @property filledIcon The icon to display when the item is selected (can be an [ImageVector] or Drawable [Int] resource).
 * @property outlinedIcon The icon to display when the item is unselected (can be an [ImageVector] or Drawable [Int] resource).
 * @property label The display label for the navigation item.
 */
data class AppNavigationItem(
    val screen: Screen,
    val filledIcon: Any, // Can be ImageVector or Int resource
    val outlinedIcon: Any,
    val label: String
)

/**
 * A reusable scaffold component that provides a consistent layout structure across the app,
 * including a top app bar, bottom navigation bar, floating action button, and surface container.
 *
 * @param navController The [NavHostController] used for handling navigation actions.
 * @param title The title text displayed in the center of the top app bar.
 * @param onLogout Callback triggered when the user initiates a logout action.
 * @param showBottomBar Whether to display the bottom navigation bar. Defaults to `true`.
 * @param floatingActionButton Optional floating action button composable to display over the content.
 * @param content The main screen content composable, receiving [PaddingValues] to apply proper window insets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    navController: NavHostController,
    title: String,
    onLogout: () -> Unit = {},
    showBottomBar: Boolean = true,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    if (navController.previousBackStackEntry != null && !showBottomBar) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Notifications.route) }) {
                        Icon(
                            imageVector = if (currentRoute == Screen.Notifications.route) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                            contentDescription = "Notifications"
                        )
                    }
                    IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                        Icon(
                            imageVector = if (currentRoute == Screen.Profile.route) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle,
                            contentDescription = "Profile"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    tonalElevation = 8.dp,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    val items = listOf(
                        AppNavigationItem(Screen.Dashboard, Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "Dashboard"),
                        AppNavigationItem(Screen.Notes, Icons.AutoMirrored.Filled.Notes, Icons.AutoMirrored.Outlined.Notes, "Notes"),
                        AppNavigationItem(Screen.Orders, Icons.Filled.Receipt, Icons.Outlined.Receipt, "Orders"),
                        AppNavigationItem(Screen.Finance, Icons.Filled.Payments, Icons.Outlined.Payments, "Finance")
                    )

                    items.forEach { item ->
                        val isSelected = currentRoute == item.screen.route
                        NavigationBarItem(
                            icon = { 
                                when (val icon = if (isSelected) item.filledIcon else item.outlinedIcon) {
                                    is ImageVector -> Icon(imageVector = icon, contentDescription = item.label)
                                    is Int -> Icon(
                                        painter = painterResource(id = icon), 
                                        contentDescription = item.label,
                                        tint = if (item.label == "Notes") Color.Unspecified else LocalContentColor.current
                                    )
                                }
                            },
                            label = { 
                                val localizedLabel = when(item.label) {
                                    "Dashboard" -> "ዳሽቦርድ"
                                    "Notes" -> "ማስታወሻዎች"
                                    "Orders" -> "ትዕዛዞች"
                                    "Finance" -> "ፋይናንስ"
                                    else -> item.label
                                }
                                Text(
                                    text = localizedLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ) 
                            },
                            selected = isSelected,
                            onClick = {
                                if (!isSelected) {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(Screen.Dashboard.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = floatingActionButton,
        content = { padding ->
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                content(padding)
            }
        }
    )
}
