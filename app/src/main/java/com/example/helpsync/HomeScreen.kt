package com.example.helpsync

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.helpsync.supporter_home_screen.SupporterHomeScreen
import com.example.helpsync.supporter_setting_screen.SupporterSettingsScreen

enum class MainScreenTab(
    val icon: ImageVector,
    val label: String,
    val route: String
) {
    Home(Icons.Outlined.Home, "Home", "main/home"),
    Settings(Icons.Outlined.Settings, "Settings", "main/settings")
}

@Composable
fun MainScreen(
    navController: NavHostController,
    nickname: String,
    onNicknameChange: (String) -> Unit
) {
    val tabNavController = rememberNavController()
    val currentDestination by tabNavController.currentBackStackEntryAsState()
    val currentRoute = currentDestination?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainScreenTab.entries.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                tabNavController.navigate(item.route) {
                                    popUpTo(MainScreenTab.Home.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = MainScreenTab.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(MainScreenTab.Home.route) {
                SupporterHomeScreen(
                    onDoneClick = {
                        navController.navigate(AppScreen.RequestAcceptanceScreen.name)
                    }
                )
            }
            composable(MainScreenTab.Settings.route) {
                SupporterSettingsScreen(
                    nickname = nickname,
                    onNicknameChange = onNicknameChange
                )
            }
        }
    }
}
