package com.example.helpsync

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.helpsync.auth.SignInScreen
import com.example.helpsync.auth.SignUpScreen
import com.example.helpsync.nickname_setting.NicknameSetting
import com.example.helpsync.profile.ProfileEditScreen
import com.example.helpsync.profile.ProfileScreen
import com.example.helpsync.request_acceptance_screen.RequestAcceptanceScreen
import com.example.helpsync.role_selection_screen.RoleSelectionScreen
import com.example.helpsync.role_selection_screen.RoleType
import com.example.helpsync.support_details_confirmation_screen.SupportRequestDetailScreen
import com.example.helpsync.ui.theme.HelpSyncTheme
import com.example.helpsync.help_mark_holder_home_screen.HelpMarkHolderHomeScreen
import com.example.helpsync.help_mark_holder_profile_screen.HelpMarkHolderProfileScreen
import com.example.helpsync.help_mark_holder_matching_screen.HelpMarkHolderMatchingScreen
import com.example.helpsync.help_mark_holder_matching_complete_screen.HelpMarkHolderMatchingCompleteScreen
import com.example.helpsync.settings_screen.SettingsScreen
import com.example.helpsync.viewmodel.UserViewModel
import androidx.compose.runtime.saveable.rememberSaveable
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth



class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)            // Firebase初期化確認
            try {
                FirebaseApp.initializeApp(this)
                Log.d(TAG, "✅ Firebase initialized successfully")
                
                // Firebase Auth確認
                val auth = FirebaseAuth.getInstance()
                Log.d(TAG, "✅ FirebaseAuth instance created")
                
                // 毎回ログインを求めるため、アプリ起動時にサインアウト
                auth.signOut()
                Log.d(TAG, "✅ Auto sign out on app startup")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firebase initialization failed: ${e.message}", e)
        }
        
        enableEdgeToEdge()
        setContent {
            HelpSyncTheme {
                val navController = rememberNavController()
                val userViewModel: UserViewModel = viewModel()

                var nickname by rememberSaveable { mutableStateOf("") }
                var photoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
                var selectedRole by rememberSaveable { mutableStateOf<String?>(null) }

                // 認証状態を監視
                val isSignedIn by remember { derivedStateOf { userViewModel.isSignedIn } }
                
                // 認証状態に応じて自動遷移（サインイン成功時のみ）
                LaunchedEffect(isSignedIn) {
                    if (isSignedIn) {
                        navController.navigate(AppScreen.RoleSelection.name) {
                            popUpTo(AppScreen.SignIn.name) { inclusive = true }
                        }
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = AppScreen.SignIn.name,
                        modifier = Modifier.padding(innerPadding)
                    ) {

                        // 認証画面
                        composable(AppScreen.SignIn.name) {
                            SignInScreen(
                                onNavigateToSignUp = {
                                    navController.navigate(AppScreen.SignUp.name)
                                },
                                onSignInSuccess = {
                                    navController.navigate(AppScreen.RoleSelection.name) {
                                        popUpTo(AppScreen.SignIn.name) { inclusive = true }
                                    }
                                },
                                userViewModel = userViewModel
                            )
                        }

                        composable(AppScreen.SignUp.name) {
                            SignUpScreen(
                                onNavigateToSignIn = {
                                    navController.navigate(AppScreen.SignIn.name)
                                },
                                onSignUpSuccess = {
                                    navController.navigate(AppScreen.RoleSelection.name) {
                                        popUpTo(AppScreen.SignUp.name) { inclusive = true }
                                    }
                                },
                                userViewModel = userViewModel
                            )
                        }

                        // プロフィール画面
                        composable(AppScreen.Profile.name) {
                            ProfileScreen(
                                onNavigateToEdit = {
                                    navController.navigate(AppScreen.ProfileEdit.name)
                                },
                                onSignOut = {
                                    navController.navigate(AppScreen.SignIn.name) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                userViewModel = userViewModel
                            )
                        }

                        composable(AppScreen.ProfileEdit.name) {
                            ProfileEditScreen(
                                onNavigateBack = {
                                    navController.navigate(AppScreen.Profile.name)
                                },
                                userViewModel = userViewModel
                            )
                        }

                        // 役割選択画面
                        composable(AppScreen.RoleSelection.name) {                       

                            RoleSelectionScreen(
                                onRoleSelected = { roleType ->
                                    // 選択されたロールを一時保存（データベースには保存しない）
                                    selectedRole = when (roleType) {
                                        RoleType.SUPPORTER -> "supporter"
                                        RoleType.HELP_MARK_HOLDER -> "requester"
                                    }
                                    
                                    // ニックネーム設定画面に遷移
                                    navController.navigate(AppScreen.NicknameSetting.name)
                                }
                            )
                        }

                        composable(AppScreen.NicknameSetting.name) {
                            NicknameSetting(
                                nickname = nickname,
                                onNicknameChange = { nickname = it },
                                photoUri = photoUri,
                                onPhotoChange = { photoUri = it },
                                onBackClick = {
                                    navController.navigate(AppScreen.RoleSelection.name) {
                                        popUpTo(AppScreen.NicknameSetting.name) { inclusive = true }
                                    }
                                },
                                onDoneClick = {
                                    // ニックネームを保存
                                    userViewModel.updateNickname(nickname)
                                    
                                    // 選択されたロールも保存
                                    selectedRole?.let { role ->
                                        userViewModel.updateRole(role)
                                    }
                                    
                                    // ロールに応じたホーム画面に遷移
                                    val nextScreen = when (selectedRole) {
                                        "supporter" -> AppScreen.SupporterHome.name
                                        "requester" -> AppScreen.HelpMarkHolderHome.name
                                        else -> AppScreen.SupporterHome.name // デフォルト
                                    }
                                    
                                    navController.navigate(nextScreen) {
                                        popUpTo(AppScreen.NicknameSetting.name) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(AppScreen.SupporterHome.name) {
                            MainScreen(
                                navController = navController,
                                nickname = nickname,
                                onNicknameChange = { nickname = it },
                                photoUri = photoUri,
                                onPhotoChange = { photoUri = it }
                            )
                        }

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

                        composable(AppScreen.RequestDetail.name) {
                            SupportRequestDetailScreen(
                                onDoneClick = {
                                    navController.navigate(AppScreen.SupporterHome.name) {
                                        popUpTo(AppScreen.RequestDetail.name) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // ヘルプマーク所持者ホーム画面
                        composable(AppScreen.HelpMarkHolderHome.name) {
                            HelpMarkHolderHomeScreen(
                                onMatchingClick = {
                                    navController.navigate(AppScreen.HelpMarkHolderProfile.name)
                                },
                                onHomeClick = {
                                    // 既にホーム画面なので何もしない、または画面をリフレッシュ
                                },
                                onSettingsClick = {
                                    navController.navigate(AppScreen.Settings.name)
                                }
                            )
                        }

                        // ヘルプマーク所持者プロフィール入力画面
                        composable(AppScreen.HelpMarkHolderProfile.name) {
                            HelpMarkHolderProfileScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onCompleteClick = {
                                    navController.navigate(AppScreen.HelpMarkHolderMatching.name) {
                                        popUpTo(AppScreen.HelpMarkHolderProfile.name) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // ヘルプマーク所持者マッチング画面
                        composable(AppScreen.HelpMarkHolderMatching.name) {
                            HelpMarkHolderMatchingScreen(
                                onMatchingComplete = {
                                    navController.navigate(AppScreen.HelpMarkHolderMatchingComplete.name) {
                                        popUpTo(AppScreen.HelpMarkHolderMatching.name) { inclusive = true }
                                    }
                                },
                                onCancel = {
                                    navController.navigate(AppScreen.HelpMarkHolderHome.name) {
                                        popUpTo(AppScreen.HelpMarkHolderMatching.name) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // ヘルプマーク所持者マッチング完了画面
                        composable(AppScreen.HelpMarkHolderMatchingComplete.name) {
                            HelpMarkHolderMatchingCompleteScreen(
                                onChatClick = {
                                    // TODO: チャット画面に遷移
                                },
                                onHomeClick = {
                                    navController.navigate(AppScreen.HelpMarkHolderHome.name) {
                                        popUpTo(AppScreen.HelpMarkHolderMatchingComplete.name) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 設定画面
                        composable(AppScreen.Settings.name) {
                            SettingsScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
