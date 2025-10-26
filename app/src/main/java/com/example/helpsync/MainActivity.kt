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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.helpsync.auth.SignInScreen
import com.example.helpsync.auth.SignUpScreen
import com.example.helpsync.bleadvertiser.BLEAdvertiser
import com.example.helpsync.blescanner.BLEScanReceiver
import com.example.helpsync.help_mark_holder_matching_complete_screen.HelpMarkHolderMatchingCompleteScreen
import com.example.helpsync.help_mark_holder_matching_screen.HelpMarkHolderMatchingScreen
import com.example.helpsync.help_mark_holder_home_screen.HelpMarkHolderHomeScreen
import com.example.helpsync.help_mark_holder_profile_screen.HelpMarkHolderProfileScreen
import com.example.helpsync.nickname_setting.NicknameSetting
import com.example.helpsync.profile.ProfileEditScreen
import com.example.helpsync.profile.ProfileScreen
import com.example.helpsync.role_selection_screen.RoleSelectionScreen
import com.example.helpsync.role_selection_screen.RoleType
import com.example.helpsync.settings_screen.SettingsScreen
import com.example.helpsync.support_details_confirmation_screen.SupportDetailsConfirmationScreen
import com.example.helpsync.ui.theme.HelpSyncTheme
import com.example.helpsync.viewmodel.HelpMarkHolderViewModel
import com.example.helpsync.viewmodel.UserViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.koin.androidx.compose.koinViewModel
import java.net.URLEncoder
import java.net.URLDecoder

@Serializable
data class SupporterNavInfo(
    val requestId: String,
    val supporterInfo: SupporterInfo
)

@Serializable
data class SupporterInfo(
    val id: String,
    val nickname: String,
    val iconUrl: String?
)

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
            val auth = FirebaseAuth.getInstance()
            Log.d(TAG, "✅ FirebaseAuth instance created")
            // ログイン状態を保持するため、自動サインアウトを削除
            Log.d(TAG, "✅ Preserving login state")
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
                val userViewModel: UserViewModel = koinViewModel()
                val helpMarkHolderViewModel: HelpMarkHolderViewModel = koinViewModel()
                val bleAdvertiser: BLEAdvertiser = remember {
                    BLEAdvertiser(this, "0000180A-0000-1000-8000-00805F9B34FB")
                }

                var photoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
                var selectedRole by rememberSaveable { mutableStateOf<String?>(null) }
                var hasNavigatedOnStartup by rememberSaveable { mutableStateOf(false) }

                // アプリ起動時の自動ナビゲーション（既存ログイン時のみ）
                LaunchedEffect(Unit) {
                    // ユーザーデータの読み込みを待つ
                    kotlinx.coroutines.delay(100)
                    
                    if (!hasNavigatedOnStartup && userViewModel.isSignedIn && userViewModel.currentUser != null) {
                        Log.d(TAG, "🚀 Auto-navigation on startup (existing login)")
                        Log.d(TAG, "User role: ${userViewModel.currentUser?.role}")
                        Log.d(TAG, "User nickname: ${userViewModel.currentUser?.nickname}")
                        
                        val targetScreen = when {
                            userViewModel.currentUser?.role.isNullOrEmpty() -> {
                                Log.d(TAG, "→ Navigating to RoleSelection (no role)")
                                AppScreen.RoleSelection.name
                            }
                            userViewModel.currentUser?.nickname.isNullOrEmpty() -> {
                                Log.d(TAG, "→ Navigating to NicknameSetting (no nickname)")
                                AppScreen.NicknameSetting.name
                            }
                            userViewModel.currentUser?.role == "supporter" -> {
                                Log.d(TAG, "→ Navigating to SupporterHome")
                                AppScreen.SupporterHome.name
                            }
                            userViewModel.currentUser?.role == "requester" -> {
                                Log.d(TAG, "→ Navigating to HelpMarkHolderScreen")
                                AppScreen.HelpMarkHolderScreen.name
                            }
                            else -> {
                                Log.d(TAG, "→ Navigating to RoleSelection (default)")
                                AppScreen.RoleSelection.name
                            }
                        }
                        
                        navController.navigate(targetScreen) {
                            popUpTo(AppScreen.SignIn.name) { inclusive = true }
                        }
                        hasNavigatedOnStartup = true
                    } else {
                        Log.d(TAG, "No auto-navigation needed (not logged in or first time)")
                    }
                }

                // ログイン成功時の処理（初回ログインと2回目以降の起動の両方に対応）
                val isSignedIn by remember { derivedStateOf { userViewModel.isSignedIn } }
                val currentUser by remember { derivedStateOf { userViewModel.currentUser } }
                
                LaunchedEffect(isSignedIn, currentUser) {
                    // 初回ログイン時: hasNavigatedOnStartup = false
                    // 2回目起動時: hasNavigatedOnStartup = false (起動時のLaunchedEffectで設定)
                    Log.d(TAG, "LaunchedEffect triggered - isSignedIn: $isSignedIn, currentUser: ${currentUser?.email}, role: ${currentUser?.role}, nickname: ${currentUser?.nickname}")
                    
                    if (isSignedIn && currentUser != null) {
                        // 既に起動時の自動ナビゲーションが完了している場合はスキップ
                        if (hasNavigatedOnStartup) {
                            Log.d(TAG, "⏭️ Skipping navigation (already navigated on startup)")
                            return@LaunchedEffect
                        }
                        
                        // ログイン成功時、適切な画面に遷移
                        Log.d(TAG, "🔐 Login success, navigating to appropriate screen")
                        Log.d(TAG, "User details - role: ${currentUser?.role}, nickname: ${currentUser?.nickname}")
                        
                        val targetScreen = when {
                            currentUser?.role.isNullOrEmpty() -> {
                                Log.d(TAG, "→ Target: RoleSelection (no role)")
                                AppScreen.RoleSelection.name
                            }
                            currentUser?.nickname.isNullOrEmpty() -> {
                                Log.d(TAG, "→ Target: NicknameSetting (no nickname)")
                                AppScreen.NicknameSetting.name
                            }
                            currentUser?.role == "supporter" -> {
                                Log.d(TAG, "→ Target: SupporterHome (supporter role)")
                                AppScreen.SupporterHome.name
                            }
                            currentUser?.role == "requester" -> {
                                Log.d(TAG, "→ Target: HelpMarkHolderScreen (requester role)")
                                AppScreen.HelpMarkHolderScreen.name
                            }
                            else -> {
                                Log.d(TAG, "→ Target: RoleSelection (default/unknown role: ${currentUser?.role})")
                                AppScreen.RoleSelection.name
                            }
                        }
                        
                        Log.d(TAG, "Navigating to: $targetScreen")
                        navController.navigate(targetScreen) {
                            popUpTo(AppScreen.SignIn.name) { inclusive = true }
                        }
                        hasNavigatedOnStartup = true
                        Log.d(TAG, "Navigation completed, hasNavigatedOnStartup set to true")
                    } else {
                        Log.d(TAG, "Not navigating - isSignedIn: $isSignedIn, currentUser is null: ${currentUser == null}")
                    }
                }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = AppScreen.SignIn.name,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        // --- 認証フロー ---
                        composable(AppScreen.SignIn.name) {
                            // サインイン画面に戻った時、ナビゲーションフラグをリセット
                            LaunchedEffect(Unit) {
                                Log.d(TAG, "SignIn screen displayed, resetting hasNavigatedOnStartup")
                                hasNavigatedOnStartup = false
                            }
                            
                            SignInScreen(
                                onNavigateToSignUp = { navController.navigate(AppScreen.SignUp.name) },
                                onSignInSuccess = {
                                    // LaunchedEffectで自動遷移するため、ここでは何もしない
                                },
                                userViewModel = userViewModel
                            )
                        }

                        composable(AppScreen.SignUp.name) {
                            // サインアップ画面でもナビゲーションフラグをリセット
                            LaunchedEffect(Unit) {
                                Log.d(TAG, "SignUp screen displayed, resetting hasNavigatedOnStartup")
                                hasNavigatedOnStartup = false
                            }
                            
                            SignUpScreen(
                                onNavigateToSignIn = { navController.navigate(AppScreen.SignIn.name) },
                                onSignUpSuccess = {
                                    // LaunchedEffectで自動遷移するため、ここでは何もしない
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
                                selectedRole = roleString
                                userViewModel.updateRole(roleString)

                                val nextScreen = when (roleType) {
                                    RoleType.SUPPORTER -> AppScreen.NicknameSetting.name
                                    RoleType.HELP_MARK_HOLDER -> AppScreen.HelpMarkHolderProfile.name
                                }

                                navController.navigate(nextScreen)
                            }
                        }

                        // --- ヘルプマーク所持者フロー ---
                        composable(AppScreen.HelpMarkHolderScreen.name) {
                            HelpMarkHolderScreen(
                                mainNavController = navController,
                                userViewModel = userViewModel,
                                onSignOut = {
                                    hasNavigatedOnStartup = false
                                }
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
                                userViewModel = userViewModel,
                                helpMarkHolderViewModel = helpMarkHolderViewModel,
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
                                nickname = userViewModel.currentUser?.nickname ?: "",
                                onNicknameChange = { newNickname ->
                                    userViewModel.updateNickname(newNickname)
                                },
                                photoUri = photoUri,
                                onPhotoChange = { newUri ->
                                    photoUri = newUri
                                },
                                onPhotoSave = { uriToSave ->
                                    userViewModel.uploadProfileImage(uriToSave) { downloadUrl ->
                                        if (downloadUrl.isNotEmpty()) {
                                            userViewModel.updateUserIconUrl(downloadUrl)
                                        } else {
                                            Log.e(TAG, "❌ 画像のアップロードに失敗")
                                        }
                                    }
                                },
                                userViewModel = userViewModel,
                                onSignOut = {
                                    hasNavigatedOnStartup = false
                                    navController.navigate(AppScreen.SignIn.name) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
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
                                onCompleteClick = { navController.popBackStack() },
                                onSignOut = {
                                    hasNavigatedOnStartup = false
                                    navController.navigate(AppScreen.SignIn.name) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(AppScreen.Profile.name) {
                            ProfileScreen(
                                onNavigateToEdit = { navController.navigate(AppScreen.ProfileEdit.name) },
                                onSignOut = {
                                    hasNavigatedOnStartup = false
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

                        composable(AppScreen.NicknameSetting.name) {
                            NicknameSetting(
                                nickname = userViewModel.currentUser?.nickname ?: "",
                                onNicknameChange = { /* 使用しない */ },
                                photoUri = photoUri,
                                onPhotoChange = { uri: Uri? -> photoUri = uri },
                                userViewModel = userViewModel,
                                onBackClick = {
                                    navController.navigate(AppScreen.RoleSelection.name) {
                                        popUpTo(AppScreen.NicknameSetting.name) { inclusive = true }
                                    }
                                },
                                onDoneClick = { nickname ->
                                    userViewModel.updateNickname(nickname)
                                    if (userViewModel.currentUser?.role.isNullOrEmpty()) {
                                        selectedRole?.let { role ->
                                            userViewModel.updateRole(role)
                                        }
                                    }
                                    val nextScreen = when (selectedRole) {
                                        "supporter" -> AppScreen.SupporterHome.name
                                        "requester" -> AppScreen.HelpMarkHolderHome.name
                                        else -> AppScreen.SupporterHome.name
                                    }
                                    navController.navigate(nextScreen) {
                                        popUpTo(AppScreen.NicknameSetting.name) { inclusive = true }
                                    }
                                }
                            )
                        }


                        // RequestAcceptanceScreen と RequestDetail の定義は MainScreen.kt に移動したため、
                        // このファイルからは削除されています。

                        composable(AppScreen.HelpMarkHolderHome.name) {
                            HelpMarkHolderHomeScreen(
                                userViewModel = userViewModel,
                                onMatchingStarted = {
                                    navController.navigate(AppScreen.HelpMarkHolderMatching.name)
                                },
                                helpMarkHolderViewModel = helpMarkHolderViewModel
                            )
                        }

                        composable(AppScreen.HelpMarkHolderProfile.name) {
                            HelpMarkHolderProfileScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onCompleteClick = {
                                    navController.navigate(AppScreen.HelpMarkHolderScreen.name) {
                                        popUpTo(AppScreen.RoleSelection.name) { inclusive = false }
                                    }
                                },
                                onSignOut = {
                                    hasNavigatedOnStartup = false
                                    navController.navigate(AppScreen.SignIn.name) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(AppScreen.HelpMarkHolderMatching.name) {
                            HelpMarkHolderMatchingScreen(
                                requestId = userViewModel.activeHelpRequest.value?.id ?: "",
                                viewModel = userViewModel,
                                onMatchingComplete = { completedRequestId ->
                                    val dummySupporter = SupporterInfo(
                                        id = "dummy123",
                                        nickname = "やさしい人",
                                        iconUrl = "https://example.com/dummy-profile.jpg"
                                    )
                                    val navInfo = SupporterNavInfo(
                                        requestId = completedRequestId,
                                        supporterInfo = dummySupporter
                                    )
                                    val infoJson = Json.encodeToString(navInfo)
                                    val encodedJson = URLEncoder.encode(infoJson, "UTF-8")
                                    navController.navigate("${AppScreen.HelpMarkHolderMatchingComplete.name}/$encodedJson") {
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

                        composable(
                            route = "${AppScreen.HelpMarkHolderMatchingComplete.name}/{supporterInfo}",
                            arguments = listOf(
                                navArgument("supporterInfo") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val supporterInfoJson = backStackEntry.arguments?.getString("supporterInfo")

                            val navInfo = remember {
                                supporterInfoJson?.let {
                                    try {
                                        val decodedJson = URLDecoder.decode(it, "UTF-8")
                                        Json.decodeFromString<SupporterNavInfo>(decodedJson)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to decode supporterInfo JSON: ${e.message}")
                                        null
                                    }
                                }
                            }

                            HelpMarkHolderMatchingCompleteScreen(
                                requestId = navInfo?.requestId ?: "",
                                userViewModel = userViewModel,
                                helpMarkHolderViewModel = helpMarkHolderViewModel,
                                onHomeClick = {
                                    navController.navigate(AppScreen.HelpMarkHolderHome.name) {
                                        popUpTo(AppScreen.HelpMarkHolderMatchingComplete.name) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(AppScreen.HelpMarkHolderProfileFromSettings.name) {
                            HelpMarkHolderProfileScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onCompleteClick = {
                                    navController.popBackStack()
                                },
                                onSignOut = {
                                    hasNavigatedOnStartup = false
                                    navController.navigate(AppScreen.SignIn.name) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}