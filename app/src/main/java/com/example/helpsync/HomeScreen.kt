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
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.helpsync.request_acceptance_screen.RequestAcceptanceScreen
import com.example.helpsync.supporter_home_screen.SupporterHomeScreen
import com.example.helpsync.supporter_setting_screen.SupporterSettingScreen
import com.example.helpsync.support_details_confirmation_screen.SupportRequestDetailScreen
import com.example.helpsync.viewmodel.UserViewModel
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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
    onNicknameChange: (String) -> Unit,
    photoUri: Uri?,
    onPhotoChange: (Uri?) -> Unit,
    onPhotoSave: (Uri) -> Unit = {},
    userViewModel: UserViewModel
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
                    onSupportRequestClick = { nickname, content ->
                        // ✅ このログがクリック時に表示されるか確認
                        Log.d("MainScreen", "Card clicked! Navigating with: $nickname")

                        val requestInfo = RequestNavInfo(nickname, content)
                        val infoJson = Json.encodeToString(requestInfo)
                        val encodedJson = URLEncoder.encode(infoJson, StandardCharsets.UTF_8.toString())

                        tabNavController.navigate("main/request_acceptance/$encodedJson")
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
                    onSignOut = {
                        navController.navigate(AppScreen.SignIn.name) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = "main/request_acceptance/{requestInfo}",
                arguments = listOf(navArgument("requestInfo") { type = NavType.StringType })
            ) { backStackEntry ->
                val requestInfoJson = backStackEntry.arguments?.getString("requestInfo")
                val requestInfo = requestInfoJson?.let {
                    val decodedJson = URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
                    Json.decodeFromString<RequestNavInfo>(decodedJson)
                } ?: RequestNavInfo("エラー", "情報の取得に失敗しました")

                RequestAcceptanceScreen(
                    nickname = requestInfo.nickname,
                    content = requestInfo.content,
                    onAcceptClick = {
                        val encodedJson = URLEncoder.encode(requestInfoJson, StandardCharsets.UTF_8.toString())
                        tabNavController.navigate("main/request_detail/$encodedJson")
                    },
                    onCancelClick = { tabNavController.popBackStack() }
                )
            }

            composable(
                route = "main/request_detail/{requestInfo}",
                arguments = listOf(navArgument("requestInfo") { type = NavType.StringType })
            ) { backStackEntry ->
                val requestInfoJson = backStackEntry.arguments?.getString("requestInfo")
                val requestInfo = requestInfoJson?.let {
                    val decodedJson = URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
                    Json.decodeFromString<RequestNavInfo>(decodedJson)
                } ?: RequestNavInfo("エラー", "情報の取得に失敗しました")

                SupportRequestDetailScreen(
                    nickname = requestInfo.nickname,
                    supportContent = requestInfo.content,
                    onDoneClick = {
                        tabNavController.popBackStack(MainScreenTab.Home.route, inclusive = false)
                    }
                )
            }
        }
    }
}