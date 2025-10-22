package com.example.helpsync

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.helpsync.help_mark_holder_home_screen.HelpMarkHolderHomeScreen
import com.example.helpsync.help_mark_holder_profile_screen.HelpMarkHolderProfileScreen
import com.example.helpsync.viewmodel.UserViewModel

// タブの定義
enum class HelpMarkHolderScreenTab(
    val icon: ImageVector,
    val label: String,
    val route: String
) {
    Home(Icons.Outlined.Home, "ホーム", "holder/home"),
    Profile(Icons.Outlined.Person, "プロフィール", "holder/profile")
}

@Composable
fun HelpMarkHolderScreen(
    mainNavController: NavHostController, // MainActivityのNavController
    userViewModel: UserViewModel
) {
    // タブ内ナビゲーション用のNavController
    val tabNavController = rememberNavController()
    val currentDestination by tabNavController.currentBackStackEntryAsState()
    val currentRoute = currentDestination?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                HelpMarkHolderScreenTab.entries.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentRoute?.startsWith(item.route) ?: false,
                        onClick = {
                            if (currentRoute != item.route) {
                                tabNavController.navigate(item.route) {
                                    popUpTo(HelpMarkHolderScreenTab.Home.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        // タブ内コンテンツ用のNavHost
        NavHost(
            navController = tabNavController,
            startDestination = HelpMarkHolderScreenTab.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(HelpMarkHolderScreenTab.Home.route) {
                HelpMarkHolderHomeScreen(
                    viewModel = userViewModel,
                    onMatchingStarted = {
                        mainNavController.navigate("HelpMarkHolderMatching") // ルート名を文字列で指定
                    }
                )
            }
            composable(HelpMarkHolderScreenTab.Profile.route) {
                HelpMarkHolderProfileScreen(
                    // ViewModelを使用するため、多くの引数は不要
                    userViewModel = userViewModel,
                    onBackClick = { /* タブ画面なので基本的に不要 */ },
                    onCompleteClick = {
                        // 保存が完了したらホームタブに戻る
                        tabNavController.navigate(HelpMarkHolderScreenTab.Home.route) {
                            // popUpToでProfile画面をバックスタックから削除
                            popUpTo(HelpMarkHolderScreenTab.Home.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}