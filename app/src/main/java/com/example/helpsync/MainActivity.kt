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
import androidx.navigation.navDeepLink
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
import com.example.helpsync.request_acceptance_screen.RequestAcceptanceScreen
import com.example.helpsync.role_selection_screen.RoleSelectionScreen
import com.example.helpsync.role_selection_screen.RoleType
import com.example.helpsync.settings_screen.SettingsScreen
import com.example.helpsync.supporter_home_screen.SupporterHomeScreen
import com.example.helpsync.ui.theme.HelpSyncTheme
import com.example.helpsync.viewmodel.HelpMarkHolderViewModel
import com.example.helpsync.viewmodel.UserViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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
    private lateinit var fusedLocationClient: FusedLocationProviderClient

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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        try {
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "✅ Firebase initialized successfully")
            val auth = FirebaseAuth.getInstance()
            Log.d(TAG, "✅ FirebaseAuth instance created")
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
                val deviceViewModel: com.example.helpsync.viewmodel.DeviceManagementVewModel = koinViewModel()
                val helpMarkHolderViewModel: HelpMarkHolderViewModel = koinViewModel()

                val bleAdvertiser: BLEAdvertiser = remember {
                    BLEAdvertiser(this, "0000180A-0000-1000-8000-00805F9B34FB")
                }

                var photoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
                var selectedRole by rememberSaveable { mutableStateOf<String?>(null) }
                var hasNavigatedOnStartup by rememberSaveable { mutableStateOf(false) }

                // アプリ起動時の自動ナビゲーション
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(100)
                    if (!hasNavigatedOnStartup && userViewModel.isSignedIn && userViewModel.currentUser != null) {
                        Log.d(TAG, "🚀 Auto-navigation on startup")
                        val targetScreen = when {
                            userViewModel.currentUser?.role.isNullOrEmpty() -> AppScreen.RoleSelection.name
                            userViewModel.currentUser?.nickname.isNullOrEmpty() -> AppScreen.NicknameSetting.name
                            userViewModel.currentUser?.role == "supporter" -> AppScreen.SupporterHome.name
                            userViewModel.currentUser?.role == "requester" -> AppScreen.HelpMarkHolderScreen.name
                            else -> AppScreen.RoleSelection.name
                        }
                        navController.navigate(targetScreen) {
                            popUpTo(AppScreen.SignIn.name) { inclusive = true }
                        }
                        hasNavigatedOnStartup = true
                    }
                }

                // ログイン成功時の処理
                val isSignedIn by remember { derivedStateOf { userViewModel.isSignedIn } }
                val currentUser by remember { derivedStateOf { userViewModel.currentUser } }

                LaunchedEffect(isSignedIn, currentUser) {
                    if (isSignedIn && currentUser != null) {
                        val isRegistered = deviceViewModel.isDeviceRegistered()
                        if (!isRegistered) {
                            deviceViewModel.callRegisterNewDevice(0.0, 0.0)
                        }

                        if (hasNavigatedOnStartup) return@LaunchedEffect

                        val targetScreen = when {
                            currentUser?.role.isNullOrEmpty() -> AppScreen.RoleSelection.name
                            currentUser?.nickname.isNullOrEmpty() -> AppScreen.NicknameSetting.name
                            currentUser?.role == "supporter" -> AppScreen.SupporterHome.name
                            currentUser?.role == "requester" -> AppScreen.HelpMarkHolderScreen.name
                            else -> AppScreen.RoleSelection.name
                        }

                        navController.navigate(targetScreen) {
                            popUpTo(AppScreen.SignIn.name) { inclusive = true }
                        }
                        hasNavigatedOnStartup = true
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
                            LaunchedEffect(Unit) { hasNavigatedOnStartup = false }
                            SignInScreen(
                                onNavigateToSignUp = { navController.navigate(AppScreen.SignUp.name) },
                                onSignInSuccess = { },
                                userViewModel = userViewModel
                            )
                        }

                        composable(AppScreen.SignUp.name) {
                            LaunchedEffect(Unit) { hasNavigatedOnStartup = false }
                            SignUpScreen(
                                onNavigateToSignIn = { navController.navigate(AppScreen.SignIn.name) },
                                onSignUpSuccess = { },
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
                            // ★修正: 引数をHelpMarkHolderScreen.ktの定義に合わせました
                            HelpMarkHolderScreen(
                                mainNavController = navController,
                                userViewModel = userViewModel,
                                locationClient = fusedLocationClient,
                                onSignOut = {
                                    hasNavigatedOnStartup = false
                                },
                                onMatchingEstablished = { requestId ->
                                    navController.navigate("${AppScreen.HelpMarkHolderMatchingComplete.name}/$requestId")
                                }
                            )
                        }

                        // マッチング待機画面
                        composable(
                            route = "${AppScreen.HelpMarkHolderMatching.name}/{requestId}",
                            arguments = listOf(navArgument("requestId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
                            HelpMarkHolderMatchingScreen(
                                requestId = requestId,
                                viewModel = userViewModel,
                                helpMarkHolderViewModel = helpMarkHolderViewModel,
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

                        // マッチング完了画面
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

                        composable(AppScreen.SupporterHome.name) {
                            // SupporterHomeScreen ではなく、Scaffoldを持つ SupporterScreen を呼び出します
                            SupporterScreen(
                                navController = navController,
                                nickname = userViewModel.currentUser?.nickname ?: "",
                                onNicknameChange = { newNickname ->
                                    userViewModel.updateNickname(newNickname)
                                },
                                photoUri = photoUri,
                                onPhotoChange = { newUri -> photoUri = newUri },
                                onPhotoSave = { uriToSave ->
                                    userViewModel.uploadProfileImage(uriToSave) { downloadUrl ->
                                        if (downloadUrl.isNotEmpty()) {
                                            userViewModel.updateUserIconUrl(downloadUrl)
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

                        composable(AppScreen.HelpMarkHolderHome.name) {
                            HelpMarkHolderHomeScreen(
                                userViewModel = userViewModel,
                                onMatchingStarted = {
                                    val currentId = userViewModel.activeHelpRequest.value?.id ?: "temp"
                                    navController.navigate("${AppScreen.HelpMarkHolderMatching.name}/$currentId")
                                },
                                helpMarkHolderViewModel = helpMarkHolderViewModel,
                                locationClient = fusedLocationClient,
                                onMatchingEstablished = { requestId ->
                                    navController.navigate("${AppScreen.HelpMarkHolderMatchingComplete.name}/$requestId")
                                }
                            )
                        }

                        composable(AppScreen.HelpMarkHolderProfile.name) {
                            HelpMarkHolderProfileScreen(
                                onBackClick = { navController.popBackStack() },
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

                        // 通知からのディープリンク対応 (RequestAcceptanceScreen)
                        composable(
                            route = "HelpRequestDetailScreen/{supporterInformation}",
                            arguments = listOf(navArgument("supporterInformation") { type = NavType.StringType }),
                            deepLinks = listOf(navDeepLink {
                                uriPattern = "app://helpsync/HelpRequestDetailScreen/{supporterInformation}"
                                action = "ACTION_SHOW_ACCEPTANCE_SCREEN"
                            })
                        ) {
                            // ★修正: 引数をRequestAcceptanceScreen.ktの定義に合わせました
                            // supporterInformationはViewModel経由か、通知の仕組みで処理する前提で、画面には渡しません。
                            RequestAcceptanceScreen(
                                onDoneClick = {
                                    // 完了したらサポーターホームに戻る
                                    navController.navigate(AppScreen.SupporterHome.name) {
                                        popUpTo(AppScreen.SupporterHome.name) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(AppScreen.HelpMarkHolderProfileFromSettings.name) {
                            HelpMarkHolderProfileScreen(
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
                    }
                }
            }
        }
    }
}