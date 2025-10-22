package com.example.helpsync

import android.net.Uri
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
import com.example.helpsync.request_acceptance_screen.RequestAcceptanceScreen
import com.example.helpsync.supporter_home_screen.SupporterHomeScreen
import com.example.helpsync.supporter_setting_screen.SupporterSettingScreen
import com.example.helpsync.support_details_confirmation_screen.SupportDetailsConfirmationScreen
import com.example.helpsync.viewmodel.UserViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument

enum class MainScreenTab(
    val icon: ImageVector,
    val label: String,
    val route: String
) {
    Home(Icons.Outlined.Home, "ホーム", "main/home"),
    Settings(Icons.Outlined.Settings, "設定", "main/settings")
}

@Composable
fun SupporterScreen(
    navController: NavHostController,
    nickname: String,
    onNicknameChange: (String) -> Unit,
    photoUri: Uri?,
    onPhotoChange: (Uri?) -> Unit,
    onPhotoSave: (Uri) -> Unit = {},
    userViewModel: UserViewModel,
    onSignOut: () -> Unit = {}
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
                        selected = currentRoute?.startsWith(item.route) ?: false,
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
                    viewModel = userViewModel,
                    onNavigateToAcceptance = { requestId ->
                        tabNavController.navigate("main/request_acceptance/$requestId")
                    }
                )
            }

            composable(MainScreenTab.Settings.route) {
                SupporterSettingScreen(
                    nickname = nickname,
                    onNicknameChange = {},
                    photoUri = photoUri,
                    onPhotoChange = onPhotoChange,
                    onEditClick = { newNickname: String -> onNicknameChange(newNickname) },
                    onPhotoSave = { uri: Uri -> onPhotoSave(uri) },
                    userViewModel = userViewModel,
                    onSignOut = onSignOut
                )
            }

            composable(
                route = "main/request_acceptance/{requestId}",
                arguments = listOf(navArgument("requestId") { type = NavType.StringType })
            ) { backStackEntry ->
                val requestId = backStackEntry.arguments?.getString("requestId") ?: ""

                LaunchedEffect(requestId) {
                    if (requestId.isNotEmpty()) {
                        userViewModel.getRequestDetails(requestId)
                    }
                }

                val request by userViewModel.viewedHelpRequest.collectAsState()

                if (request != null) {
                    RequestAcceptanceScreen(
                        nickname = request!!.requesterNickname,
                        content = "支援を求めています",
                        onAcceptClick = {
                            tabNavController.navigate("main/request_detail/$requestId")
                        },
                        onCancelClick = {
                            userViewModel.clearViewedRequest()
                            tabNavController.popBackStack()
                        }
                    )
                } else {
                    CircularProgressIndicator()
                }
            }

            // --- ▼▼▼ 修正箇所 ▼▼▼ ---
            composable(
                route = "main/request_detail/{requestId}",
                arguments = listOf(navArgument("requestId") { type = NavType.StringType })
            ) { backStackEntry ->
                val requestId = backStackEntry.arguments?.getString("requestId") ?: ""

                SupportDetailsConfirmationScreen(
                    requestId = requestId,
                    viewModel = userViewModel,
                    onDoneClick = {
                        userViewModel.clearViewedRequest()
                        tabNavController.popBackStack(MainScreenTab.Home.route, inclusive = false)
                    }
                )
            }
            // --- ▲▲▲ 修正箇所ここまで ▲▲▲ ---
        }
    }
}