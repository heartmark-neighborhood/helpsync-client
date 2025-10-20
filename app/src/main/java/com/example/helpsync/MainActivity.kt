package com.example.helpsync

import android.Manifest
import android.annotation.SuppressLint
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.helpsync.auth.SignInScreen
import com.example.helpsync.auth.SignUpScreen
import com.example.helpsync.blescanner.BLEScanReceiver
import com.example.helpsync.help_mark_holder_matching_complete_screen.HelpMarkHolderMatchingCompleteScreen
import com.example.helpsync.help_mark_holder_matching_screen.HelpMarkHolderMatchingScreen
import com.example.helpsync.help_mark_holder_home_screen.HelpMarkHolderHomeScreen
import com.example.helpsync.nickname_setting.NicknameSetting
import com.example.helpsync.profile.ProfileEditScreen
import com.example.helpsync.profile.ProfileScreen
import com.example.helpsync.role_selection_screen.RoleSelectionScreen
import com.example.helpsync.role_selection_screen.RoleType
import com.example.helpsync.settings_screen.SettingsScreen
import com.example.helpsync.supporter_home_screen.SupporterHomeScreen
import com.example.helpsync.support_details_confirmation_screen.SupportDetailsConfirmationScreen
import com.example.helpsync.ui.theme.HelpSyncTheme
import com.example.helpsync.viewmodel.UserViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var bleReceiver: BLEScanReceiver

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val allGranted = perms.entries.all { it.value }
        if (!allGranted) {
            Toast.makeText(this, "permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onScanResult(found: Boolean) {
        if (found) {
            Log.d(TAG, "Help request found!")
        } else {
            Log.d(TAG, "No help request found.")
        }
    }

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "✅ Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firebase initialization failed: ${e.message}", e)
        }

        bleReceiver = BLEScanReceiver(::onScanResult)
        registerReceiver(
            bleReceiver,
            IntentFilter("com.example.SCAN_RESULT"),
            RECEIVER_NOT_EXPORTED
        )

        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.BLUETOOTH_ADVERTISE,
            )
        )

        enableEdgeToEdge()

        setContent {
            HelpSyncTheme {
                val navController = rememberNavController()
                val userViewModel: UserViewModel = viewModel()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = if (userViewModel.getCurrentFirebaseUser() != null)
                            AppScreen.RoleSelection.name
                        else
                            AppScreen.SignIn.name,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        // --- 認証フロー ---
                        composable(AppScreen.SignIn.name) {
                            SignInScreen(
                                onNavigateToSignUp = { navController.navigate(AppScreen.SignUp.name) },
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
                                onNavigateToSignIn = { navController.navigate(AppScreen.SignIn.name) },
                                onSignUpSuccess = {
                                    navController.navigate(AppScreen.RoleSelection.name) {
                                        popUpTo(AppScreen.SignUp.name) { inclusive = true }
                                    }
                                },
                                userViewModel = userViewModel
                            )
                        }

                        // --- 初期設定フロー ---
                        composable(AppScreen.RoleSelection.name) {
                            RoleSelectionScreen { roleType ->
                                val roleString = when (roleType) {
                                    RoleType.SUPPORTER -> "supporter"
                                    RoleType.HELP_MARK_HOLDER -> "requester"
                                }

                                userViewModel.updateRole(roleString)

                                val nextScreen = when (roleType) {
                                    RoleType.SUPPORTER -> AppScreen.SupporterHome.name
                                    RoleType.HELP_MARK_HOLDER -> AppScreen.HelpMarkHolderScreen.name
                                }

                                navController.navigate(nextScreen) {
                                    popUpTo(AppScreen.RoleSelection.name) { inclusive = true }
                                }
                            }
                        }

                        // --- ヘルプマーク所持者フロー ---
                        composable(AppScreen.HelpMarkHolderScreen.name) {
                            HelpMarkHolderScreen(
                                mainNavController = navController,
                                userViewModel = userViewModel
                            )
                        }

                        composable(
                            route = "${AppScreen.HelpMarkHolderMatching.name}/{requestId}",
                            arguments = listOf(navArgument("requestId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
                            HelpMarkHolderMatchingScreen(
                                requestId = requestId,
                                viewModel = userViewModel,
                                onMatchingComplete = { completedRequestId ->
                                    navController.navigate("${AppScreen.HelpMarkHolderMatchingComplete.name}/$completedRequestId") {
                                        popUpTo(AppScreen.HelpMarkHolderMatching.name) { inclusive = true }
                                    }
                                },
                                onCancel = {
                                    navController.navigate(AppScreen.HelpMarkHolderScreen.name) {
                                        popUpTo(AppScreen.HelpMarkHolderMatching.name) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(
                            route = "${AppScreen.HelpMarkHolderMatchingComplete.name}/{requestId}",
                            arguments = listOf(navArgument("requestId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
                            HelpMarkHolderMatchingCompleteScreen(
                                requestId = requestId,
                                viewModel = userViewModel,
                                onHomeClick = {
                                    navController.navigate(AppScreen.HelpMarkHolderScreen.name) {
                                        popUpTo(AppScreen.HelpMarkHolderMatchingComplete.name) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // --- サポーターフロー ---
                        composable(AppScreen.SupporterHome.name) {
                            SupporterScreen(
                                navController = navController,
                                // 以下のパラメータはSupporterScreenの定義に合わせて調整が必要な場合があります
                                nickname = userViewModel.currentUser?.nickname ?: "",
                                onNicknameChange = { newNickname -> userViewModel.updateNickname(newNickname) },
                                photoUri = null, // 必要に応じてUriの状態管理を実装してください
                                onPhotoChange = {},
                                onPhotoSave = {},
                                userViewModel = userViewModel
                            )
                        }

                        composable(
                            route = "${AppScreen.SupportDetailsConfirmation.name}/{requestId}",
                            arguments = listOf(navArgument("requestId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
                            SupportDetailsConfirmationScreen(
                                requestId = requestId,
                                viewModel = userViewModel,
                                onDoneClick = {
                                    navController.navigate(AppScreen.SupporterHome.name) {
                                        popUpTo(AppScreen.SupportDetailsConfirmation.name) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // --- 共通画面 ---
                        composable(AppScreen.Settings.name) {
                            SettingsScreen(
                                onBackClick = { navController.popBackStack() },
                                onCompleteClick = { navController.popBackStack() }
                            )
                        }

                        composable(AppScreen.Profile.name) {
                            ProfileScreen(
                                onNavigateToEdit = { navController.navigate(AppScreen.ProfileEdit.name) },
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
                                onNavigateBack = { navController.popBackStack() },
                                userViewModel = userViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}