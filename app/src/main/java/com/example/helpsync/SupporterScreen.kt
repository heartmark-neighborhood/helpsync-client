package com.example.helpsync

import android.net.Uri
import android.util.Log
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
import com.example.helpsync.viewmodel.UserViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.helpsync.viewmodel.SupporterViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

enum class MainScreenTab(
    val icon: ImageVector,
    val label: String,
    val route: String
) {
    Home(Icons.Outlined.Home, "ホーム", "main/home"),
    profile(Icons.Outlined.Settings, "プロフィール", "main/profile")
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
    val supporterViewModel: SupporterViewModel = viewModel()

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
                    viewModel = supporterViewModel,
                    onNavigateToAcceptance = { requestId ->
                        if (requestId.isNotEmpty()) {
                            tabNavController.navigate("main/matched_detail/$requestId")
                        } else {
                            Log.e("SupporterScreen", "Cannot navigate, requestId is empty!")
                        }
                    }
                )
            }

            composable(MainScreenTab.profile.route) {
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
                route = "main/matched_detail/{requestId}",
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
                        viewModel = supporterViewModel, // 正しい ViewModel を渡す
                        onDoneClick = { // ★ 完了ボタンが押されたときの処理
                            // ViewModelのデータをクリア (任意だが推奨)
                            supporterViewModel.clearViewedRequest() // ViewModelにこの関数が必要
                            // ホームタブに戻る
                            tabNavController.navigate(MainScreenTab.Home.route) {
                                // 現在の画面 (matched_detail) をスタックから削除
                                popUpTo(tabNavController.graph.startDestinationId) { inclusive = false } // ホームまで戻る
                                launchSingleTop = true
                            }
                        }
                    )
                } else {
                    CircularProgressIndicator()
                }
            }
        }
    }
}