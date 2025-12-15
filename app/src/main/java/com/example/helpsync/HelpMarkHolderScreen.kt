package com.example.helpsync


import android.annotation.SuppressLint

import android.os.Build
import androidx.annotation.RequiresApi

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
import com.example.helpsync.viewmodel.HelpMarkHolderViewModel
import com.example.helpsync.viewmodel.UserViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import org.koin.androidx.compose.koinViewModel

// タブの定義
enum class HelpMarkHolderScreenTab(
    val icon: ImageVector,
    val label: String,
    val route: String
) {
    Home(Icons.Outlined.Home, "ホーム", "holder/home"),
    Profile(Icons.Outlined.Person, "プロフィール", "holder/profile")
}
@SuppressLint("NewApi")
@Composable
fun HelpMarkHolderScreen(
    mainNavController: NavHostController, // MainActivityのNavController
    userViewModel: UserViewModel,
    helpMarkHolderViewModel: HelpMarkHolderViewModel,
    locationClient: FusedLocationProviderClient,
    onSignOut: () -> Unit = {},
    onMatchingEstablished: (String) -> Unit
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
                    userViewModel = userViewModel,
                    onMatchingStarted = {
                        mainNavController.navigate("${AppScreen.HelpMarkHolderMatching.name}/pending")
                    },
                    helpMarkHolderViewModel = koinViewModel(),
                    locationClient = locationClient,
                    onMatchingEstablished = onMatchingEstablished
                )
            }
            composable(HelpMarkHolderScreenTab.Profile.route) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
                        },
                        onSignOut = {
                            onSignOut()
                            mainNavController.navigate(AppScreen.SignIn.name) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                } else {
                    // API 29未満の場合は簡易版のプロフィール画面を表示
                    Text("プロフィール画面はAndroid 10以降でサポートされています")
                }
            }
        }
    }
}