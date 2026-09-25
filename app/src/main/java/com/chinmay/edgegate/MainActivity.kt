package com.chinmay.edgegate

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chinmay.edgegate.ui.dashboard.DashboardScreen
import com.chinmay.edgegate.ui.dashboard.DashboardViewModel
import com.chinmay.edgegate.ui.log.LogScreen
import com.chinmay.edgegate.ui.log.LogViewModel
import com.chinmay.edgegate.ui.scan.ScanScreen
import com.chinmay.edgegate.ui.scan.ScanViewModel
import com.chinmay.edgegate.ui.settings.SettingsScreen
import com.chinmay.edgegate.ui.settings.SettingsViewModel
import com.chinmay.edgegate.ui.theme.EdgeGateTheme
import com.chinmay.edgegate.ui.theme.EdgeTheme
import com.chinmay.edgegate.ui.vehicles.AddVehicleScreen
import com.chinmay.edgegate.ui.vehicles.VehiclesScreen
import com.chinmay.edgegate.ui.vehicles.VehiclesViewModel

private enum class Tab(val route: String, val label: String, val selectedIcon: ImageVector, val icon: ImageVector) {
    Home("home", "Home", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    Scan("scan", "Scan", Icons.Filled.QrCodeScanner, Icons.Outlined.QrCodeScanner),
    Log("log", "Gate log", Icons.AutoMirrored.Filled.ListAlt, Icons.AutoMirrored.Outlined.ListAlt),
    Vehicles("vehicles", "Vehicles", Icons.Filled.DirectionsCar, Icons.Outlined.DirectionsCar),
}

private object Routes {
    const val ADD_VEHICLE = "vehicles/add"
    const val SETTINGS = "settings"
}

private fun CreationExtras.container() =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as EdgeGateApp).container

private val factory = viewModelFactory {
    initializer { DashboardViewModel(container()) }
    initializer { ScanViewModel(container()) }
    initializer { LogViewModel(container().repository) }
    initializer { VehiclesViewModel(container().repository) }
    initializer { SettingsViewModel(container().settings) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EdgeGateTheme {
                val nav = rememberNavController()
                val backStack by nav.currentBackStackEntryAsState()
                val route = backStack?.destination?.route
                val showBar = Tab.entries.any { it.route == route }

                // Light status-bar icons over the dark camera; theme default elsewhere.
                LaunchedEffect(route) {
                    if (route == Tab.Scan.route) {
                        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT))
                    } else {
                        enableEdgeToEdge()
                    }
                }

                Scaffold(
                    contentWindowInsets = WindowInsets(0),
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = { if (showBar) BottomBar(nav, route) },
                ) { padding ->
                    Box(Modifier.padding(padding)) {
                        NavHost(nav, startDestination = Tab.Home.route) {
                            composable(Tab.Home.route) {
                                DashboardScreen(
                                    viewModel<DashboardViewModel>(factory = factory),
                                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                                    onOpenLog = { nav.switchTab(Tab.Log) },
                                )
                            }
                            composable(Tab.Scan.route) { ScanScreen(viewModel<ScanViewModel>(factory = factory)) }
                            composable(Tab.Log.route) { LogScreen(viewModel<LogViewModel>(factory = factory)) }
                            composable(Tab.Vehicles.route) {
                                VehiclesScreen(viewModel<VehiclesViewModel>(factory = factory), onAdd = { nav.navigate(Routes.ADD_VEHICLE) })
                            }
                            composable(Routes.ADD_VEHICLE) {
                                // Share the Vehicles tab's ViewModel so the list updates on return.
                                val parent = nav.getBackStackEntry(Tab.Vehicles.route)
                                val vm = viewModel<VehiclesViewModel>(parent, factory = factory)
                                AddVehicleScreen(onSave = { vm.save(it); nav.popBackStack() }, onBack = { nav.popBackStack() })
                            }
                            composable(Routes.SETTINGS) {
                                SettingsScreen(viewModel<SettingsViewModel>(factory = factory), onBack = { nav.popBackStack() })
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun NavHostController.switchTab(tab: Tab) = navigate(tab.route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
private fun BottomBar(nav: NavHostController, route: String?) {
    Column {
        HorizontalDivider(color = EdgeTheme.colors.cardLine)
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            Tab.entries.forEach { tab ->
                val selected = route == tab.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { nav.switchTab(tab) },
                    icon = { Icon(if (selected) tab.selectedIcon else tab.icon, null) },
                    label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = EdgeTheme.colors.textMuted,
                        unselectedTextColor = EdgeTheme.colors.textMuted,
                    ),
                )
            }
        }
    }
}
