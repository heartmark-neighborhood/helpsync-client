package com.example.helpsync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.helpsync.nickname_setting.NicknameSetting
import com.example.helpsync.request_acceptance_screen.RequestAcceptanceScreen
import com.example.helpsync.ui.theme.HelpSyncTheme
import com.example.helpsync.support_details_confirmation_screen.SupportRequestDetailScreen
import com.example.helpsync.role_selection_screen.RoleSelectionScreen
import com.example.helpsync.role_selection_screen.RoleType
import androidx.compose.runtime.*


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HelpSyncTheme {
                val navController = rememberNavController()
                var nickname by remember { mutableStateOf("") }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = AppScreen.RoleSelection.name,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        // 役割選択画面
                        composable(AppScreen.RoleSelection.name) {
                            RoleSelectionScreen(
                                onRoleSelected = { roleType ->
                                    when (roleType) {
                                        RoleType.SUPPORTER -> {
                                            navController.navigate(AppScreen.NicknameSetting.name)
                                        }
                                        RoleType.HELP_MARK_HOLDER -> {
                                            // TODO: ヘルプマーク所持者用の画面に遷移
                                            // navController.navigate(AppScreen.HelpMarkHolderHome.name)
                                        }
                                    }
                                }
                            )
                        }
                        
                        // ニックネーム設定画面
                        composable(AppScreen.NicknameSetting.name) {
                            NicknameSetting(
                                nickname = nickname,
                                onNicknameChange = { nickname = it },
                                onBackClick = { 
                                    navController.navigate(AppScreen.RoleSelection.name) {
                                        popUpTo(AppScreen.NicknameSetting.name) { inclusive = true }
                                    }
                                },
                                onDoneClick = {
                                    navController.navigate(AppScreen.SupporterHome.name) {
                                        popUpTo(AppScreen.NicknameSetting.name) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // サポーターホーム画面
                        composable(AppScreen.SupporterHome.name) {
                            MainScreen(
                                navController = navController,
                                nickname = nickname,
                                onNicknameChange = { nickname = it }
                            )
                        }

                        // 支援受諾確認画面
                        composable(AppScreen.RequestAcceptanceScreen.name) {
                            RequestAcceptanceScreen(
                                navController = navController,
                                onDoneClick = {
                                    navController.navigate(AppScreen.SupporterHome.name) {
                                        popUpTo(AppScreen.RequestAcceptanceScreen.name) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 支援詳細確認画面
                        composable(AppScreen.RequestDetail.name) {
                            SupportRequestDetailScreen(
                                onDoneClick = {
                                    navController.navigate(AppScreen.SupporterHome.name){
                                        popUpTo(AppScreen.RequestDetail.name){inclusive = true}
                                    }
                                }
                            )
                        }

                        // TODO: ヘルプマーク所持者用ホーム画面
                        /*
                        composable(AppScreen.HelpMarkHolderHome.name) {
                            // HelpMarkHolderHomeScreen()
                        }
                        */
                    }
                }
            }
        }
    }
}