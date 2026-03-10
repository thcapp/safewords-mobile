package com.thc.safewords.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.thc.safewords.ui.groups.GroupDetailScreen
import com.thc.safewords.ui.groups.GroupsScreen
import com.thc.safewords.ui.home.HomeScreen
import com.thc.safewords.ui.qr.QRDisplayScreen
import com.thc.safewords.ui.qr.QRScannerScreen
import com.thc.safewords.ui.settings.SettingsScreen
import com.thc.safewords.ui.theme.Background
import com.thc.safewords.ui.theme.Surface
import com.thc.safewords.ui.theme.Teal
import com.thc.safewords.ui.theme.TextMuted
import com.thc.safewords.ui.theme.TextSecondary

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Groups : Screen("groups")
    data object Settings : Screen("settings")
    data object GroupDetail : Screen("group/{groupId}") {
        fun createRoute(groupId: String) = "group/$groupId"
    }
    data object QRDisplay : Screen("qr_display/{groupId}") {
        fun createRoute(groupId: String) = "qr_display/$groupId"
    }
    data object QRScanner : Screen("qr_scanner")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.Groups, "Groups", Icons.Filled.Groups, Icons.Outlined.Groups),
    BottomNavItem(Screen.Settings, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun SafewordsNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Surface,
                    contentColor = Teal
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.screen.route
                        } == true

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Teal,
                                selectedTextColor = Teal,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted,
                                indicatorColor = Surface
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToGroups = {
                        navController.navigate(Screen.Groups.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(Screen.Groups.route) {
                GroupsScreen(
                    onGroupClick = { groupId ->
                        navController.navigate(Screen.GroupDetail.createRoute(groupId))
                    },
                    onScanQR = {
                        navController.navigate(Screen.QRScanner.route)
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            composable(
                route = Screen.GroupDetail.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                GroupDetailScreen(
                    groupId = groupId,
                    onBack = { navController.popBackStack() },
                    onInvite = {
                        navController.navigate(Screen.QRDisplay.createRoute(groupId))
                    },
                    onDeleted = {
                        navController.popBackStack(Screen.Groups.route, inclusive = false)
                    }
                )
            }

            composable(
                route = Screen.QRDisplay.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                QRDisplayScreen(
                    groupId = groupId,
                    onDismiss = { navController.popBackStack() }
                )
            }

            composable(Screen.QRScanner.route) {
                QRScannerScreen(
                    onGroupJoined = { groupId ->
                        navController.popBackStack(Screen.Groups.route, inclusive = false)
                        navController.navigate(Screen.GroupDetail.createRoute(groupId))
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
