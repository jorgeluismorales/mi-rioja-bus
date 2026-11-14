package com.mibusrioja.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mibusrioja.app.ui.alerts.AlertsScreen
import com.mibusrioja.app.ui.balance.SaldoScreen
import com.mibusrioja.app.ui.map.MapScreen
import com.mibusrioja.app.ui.sales.SalePointsScreen
import com.mibusrioja.app.ui.planner.TripPlannerScreen
import com.mibusrioja.app.ui.tariffs.TarifasScreen
import com.mibusrioja.app.ui.theme.*

// ── Screen Routes ─────────────────────────────────────────────────────────────
sealed class Screen(val route: String) {
    data object Map : Screen("map")
    data object Planner : Screen("planner")
    data object Alerts : Screen("alerts")
    data object Tariffs : Screen("tariffs")
    data object Balance : Screen("balance")
    data object SalePoints : Screen("sales")
}

// ── Bottom Navigation Item Model ──────────────────────────────────────────────
private data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Map, "Mapa", Icons.Default.Map),
    BottomNavItem(Screen.Planner, "Planificar", Icons.Default.AltRoute),
    BottomNavItem(Screen.Alerts, "Alertas", Icons.Default.NotificationsActive),
    BottomNavItem(Screen.Tariffs, "Tarifas", Icons.Default.ConfirmationNumber),
    BottomNavItem(Screen.Balance, "Saldo", Icons.Default.CreditCard)
)

// ── AppBottomNav Composable ───────────────────────────────────────────────────
@Composable
fun AppBottomNav(
    navController: NavController,
    modifier: Modifier = Modifier,
    alertCount: Int = 0
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        modifier = modifier,
        containerColor = SurfaceDark,
        contentColor = TextPrimary,
        tonalElevation = 0.dp
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(item.screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    if (item.screen == Screen.Alerts && alertCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = RiojaOrange,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = if (alertCount > 99) "99+" else alertCount.toString(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label
                            )
                        }
                    } else {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label
                        )
                    }
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = RiojaOrange,
                    selectedTextColor = RiojaOrange,
                    indicatorColor = RiojaOrange.copy(alpha = 0.2f),
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                )
            )
        }
    }
}

// ── AppNavHost Composable ─────────────────────────────────────────────────────
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Map.route,
        modifier = modifier
    ) {
        composable(Screen.Map.route) { MapScreen() }
        composable(Screen.Planner.route) { TripPlannerScreen() }
        composable(Screen.Alerts.route) { AlertsScreen() }
        composable(Screen.Tariffs.route) { TarifasScreen() }
        composable(Screen.Balance.route) {
            SaldoScreen(
                onOpenSalePoints = { navController.navigate(Screen.SalePoints.route) }
            )
        }
        composable(Screen.SalePoints.route) {
            SalePointsScreen(onBack = { navController.popBackStack() })
        }
    }
}
