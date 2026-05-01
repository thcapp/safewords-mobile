package com.thc.safewords.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.thc.safewords.service.GroupRepository
import com.thc.safewords.ui.drills.DrillsScreen
import com.thc.safewords.ui.generator.GeneratorScreen
import com.thc.safewords.ui.groups.GroupDetailScreen
import com.thc.safewords.ui.groups.GroupsScreen
import com.thc.safewords.ui.home.HomeScreen
import com.thc.safewords.ui.onboarding.OnboardingScreen
import com.thc.safewords.ui.onboarding.RecoveryPhraseScreen
import com.thc.safewords.ui.plain.PlainRoot
import com.thc.safewords.ui.qr.QRDisplayScreen
import com.thc.safewords.ui.qr.QRScannerScreen
import com.thc.safewords.ui.cards.SafetyCardsScreen
import com.thc.safewords.ui.settings.RecoveryBackupScreen
import com.thc.safewords.ui.settings.SettingsScreen
import com.thc.safewords.ui.verify.ChallengeSheet
import com.thc.safewords.ui.verify.OverrideRevealScreen
import com.thc.safewords.ui.theme.Ink
import com.thc.safewords.ui.verify.VerifyScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Groups : Screen("groups")
    data object Verify : Screen("verify")
    data object Settings : Screen("settings")
    data object Onboarding : Screen("onboarding")
    data object RecoveryPhrase : Screen("recovery_phrase")
    data object Drills : Screen("drills")
    data object Generator : Screen("generator")
    data object RecoveryBackup : Screen("recovery_backup")
    data object SafetyCards : Screen("safety_cards")
    data object Challenge : Screen("challenge/{groupId}") {
        fun createRoute(groupId: String) = "challenge/$groupId"
    }
    data object OverrideReveal : Screen("override_reveal/{groupId}") {
        fun createRoute(groupId: String) = "override_reveal/$groupId"
    }
    data object GroupDetail : Screen("group/{groupId}") {
        fun createRoute(groupId: String) = "group/$groupId"
    }
    data object QRDisplay : Screen("qr_display/{groupId}") {
        fun createRoute(groupId: String) = "qr_display/$groupId"
    }
    data object QRScanner : Screen("qr_scanner")
}

private data class TabItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

private val tabs = listOf(
    TabItem(Screen.Home,     "Word",     Icons.Outlined.Shield),
    TabItem(Screen.Groups,   "Groups",   Icons.Outlined.Groups),
    TabItem(Screen.Verify,   "Verify",   Icons.Outlined.Phone),
    TabItem(Screen.Settings, "Settings", Icons.Outlined.Settings),
)

@Composable
fun SafewordsNavigation() {
    // v1.3: default home is Plain Mode for everyone. Advanced view is the
    // opt-in tabbed UX. Legacy "plainMode" pref still exists as the explicit
    // visibility-mode toggle (separate from Advanced/Standard view).
    var advancedView by remember { mutableStateOf(GroupRepository.isAdvancedView()) }
    var plainMode by remember { mutableStateOf(GroupRepository.isPlainMode()) }
    val groups by GroupRepository.groups.collectAsState()

    if (plainMode) {
        PlainRoot(onExitPlain = {
            plainMode = false
            GroupRepository.setPlainMode(false)
        })
        return
    }

    if (!advancedView) {
        PlainRoot(
            onExitPlain = {
                advancedView = true
                GroupRepository.setAdvancedView(true)
            },
        )
        return
    }

    val navController = rememberNavController()
    val backEntry by navController.currentBackStackEntryAsState()
    val route = backEntry?.destination?.route

    val showTabBar = tabs.any { it.screen.route == route }
    val start = if (groups.isEmpty()) Screen.Onboarding.route else Screen.Home.route

    Scaffold(
        containerColor = Ink.bg,
        bottomBar = { if (showTabBar) CustomTabBar(navController, route) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = start,
            modifier = Modifier
                .padding(innerPadding)
                .systemBarsPadding()
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onComplete = { _ ->
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    },
                    onJoinWithQR = {
                        navController.navigate(Screen.QRScanner.route)
                    },
                    onJoinWithRecovery = {
                        navController.navigate(Screen.RecoveryPhrase.route)
                    }
                )
            }
            composable(Screen.RecoveryPhrase.route) {
                RecoveryPhraseScreen(
                    onBack = { navController.popBackStack() },
                    onJoined = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToGroups = {
                        navController.navigate(Screen.Groups.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    },
                    onShareInvite = { id ->
                        navController.navigate(Screen.QRDisplay.createRoute(id))
                    }
                )
            }
            composable(Screen.Groups.route) {
                GroupsScreen(
                    onGroupClick = { id -> navController.navigate(Screen.GroupDetail.createRoute(id)) },
                    onScanQR = { navController.navigate(Screen.QRScanner.route) },
                    onAddMember = { navController.navigate(Screen.Onboarding.route) }
                )
            }
            composable(Screen.Verify.route) {
                VerifyScreen(onRunChallenge = { id -> navController.navigate(Screen.Challenge.createRoute(id)) })
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    plainMode = plainMode,
                    onPlainModeChange = {
                        plainMode = it
                        GroupRepository.setPlainMode(it)
                    },
                    onRunDrill = { navController.navigate(Screen.Drills.route) },
                    onDrillHistory = { navController.navigate(Screen.Drills.route) },
                    onOpenGenerator = { navController.navigate(Screen.Generator.route) },
                    onBackupSeedPhrase = { navController.navigate(Screen.RecoveryBackup.route) },
                    onOpenSafetyCards = { navController.navigate(Screen.SafetyCards.route) },
                    onRunChallenge = { id -> navController.navigate(Screen.Challenge.createRoute(id)) },
                    onRevealOverride = { id -> navController.navigate(Screen.OverrideReveal.createRoute(id)) },
                )
            }
            composable(Screen.Drills.route) {
                DrillsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Generator.route) {
                GeneratorScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.RecoveryBackup.route) {
                RecoveryBackupScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.SafetyCards.route) {
                SafetyCardsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Screen.Challenge.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("groupId") ?: return@composable
                ChallengeSheet(groupId = id, onDone = { navController.popBackStack() })
            }
            composable(
                Screen.OverrideReveal.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("groupId") ?: return@composable
                OverrideRevealScreen(groupId = id, onBack = { navController.popBackStack() })
            }
            composable(
                Screen.GroupDetail.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("groupId") ?: return@composable
                GroupDetailScreen(
                    groupId = id,
                    onBack = { navController.popBackStack() },
                    onInvite = { navController.navigate(Screen.QRDisplay.createRoute(id)) },
                    onDeleted = { navController.popBackStack(Screen.Groups.route, inclusive = false) }
                )
            }
            composable(
                Screen.QRDisplay.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("groupId") ?: return@composable
                QRDisplayScreen(groupId = id, onDismiss = { navController.popBackStack() })
            }
            composable(Screen.QRScanner.route) {
                QRScannerScreen(
                    onGroupJoined = { id ->
                        // After joining, land on Home with the new group active.
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun CustomTabBar(navController: androidx.navigation.NavController, currentRoute: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 26.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Ink.bgElev)
            .border(0.5.dp, Ink.rule, RoundedCornerShape(28.dp))
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val on = tab.screen.route == currentRoute
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (on) Ink.bgInset else Color.Transparent)
                    .clickable {
                        navController.navigate(tab.screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    }
                    .padding(top = 10.dp, bottom = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        tab.icon, null,
                        tint = if (on) Ink.fg else Ink.fgMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        tab.label,
                        color = if (on) Ink.fg else Ink.fgMuted,
                        style = TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp)
                    )
                }
            }
        }
    }
}
