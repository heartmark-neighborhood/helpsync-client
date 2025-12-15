package com.example.helpsync

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.helpsync.auth.SignInScreen
import com.example.helpsync.request_acceptance_screen.RequestAcceptanceScreen
import com.example.helpsync.auth.SignUpScreen
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
import com.example.helpsync.supporter_home_screen.SupporterHomeScreen
import com.example.helpsync.ui.theme.HelpSyncTheme
import com.example.helpsync.viewmodel.HelpMarkHolderViewModel
import com.example.helpsync.viewmodel.UserViewModel
import com.google.android.gms.location.LocationServices
import com.google.firebase.FirebaseApp
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import androidx.navigation.navDeepLink



class MainActivity : ComponentActivity() {
    companion object { private const val TAG = "MainActivity" }
    private lateinit var bleReceiver: BLEScanReceiver
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    private var pendingNotificationData by mutableStateOf<String?>(null)

    private fun onScanResult(found: Boolean) {
        if (found) Log.d(TAG, "Help request found!") else Log.d(TAG, "No help request found.")
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == "ACTION_SHOW_ACCEPTANCE_SCREEN") {
            val infoJson = intent.getStringExtra("HELPMARKHOLDER_INFORMATION")
            if (infoJson != null) {
                Log.d(TAG, "handleIntent: 通知データを受信 -> $infoJson")
                pendingNotificationData = infoJson
            }
        }
    }

    override fun onNewIntent(intent: Intent) { // 引数の型を Intent に修正(nullable除去)
        super.onNewIntent(intent)
        setIntent(intent) // 新しいIntentをセット
        handleIntent(intent)
    }

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) { }

        handleIntent(intent)

        bleReceiver = BLEScanReceiver(::onScanResult)
        registerReceiver(bleReceiver, IntentFilter("com.example.SCAN_RESULT"), RECEIVER_NOT_EXPORTED)
        permissionLauncher.launch(arrayOf(
            Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.BLUETOOTH_ADVERTISE
        ))
        enableEdgeToEdge()

        if (intent?.action == "ACTION_SHOW_ACCEPTANCE_SCREEN") {
            val infoJson = intent.getStringExtra("HELPMARKHOLDER_INFORMATION")
            if (infoJson != null) {
                Log.d(TAG, "Launched from notification with data: $infoJson")
                // ※ここで本当は ViewModel にデータを渡したいが、KoinのViewModel取得はsetContent内で行うため
                //   一時的に保存するか、LaunchedEffectで処理する必要があります。
            }
        }

        setContent {
            HelpSyncTheme {
                val navController = rememberNavController()
                val supporterViewModel: com.example.helpsync.viewmodel.SupporterViewModel = koinViewModel()
                val userViewModel: UserViewModel = koinViewModel()
                val deviceViewModel: com.example.helpsync.viewmodel.DeviceManagementVewModel = koinViewModel()
                val helpMarkHolderViewModel: HelpMarkHolderViewModel = koinViewModel()
                val bleAdvertiser: com.example.helpsync.bleadvertiser.BLEAdvertiser = remember {
                    com.example.helpsync.bleadvertiser.BLEAdvertiser(this, "0000180A-0000-1000-8000-00805F9B34FB")
                }

                var photoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
                var selectedRole by rememberSaveable { mutableStateOf<String?>(null) }
                var hasNavigatedOnStartup by rememberSaveable { mutableStateOf(false) }


                // 通知からの起動を処理
                var notificationData by rememberSaveable { mutableStateOf<String?>(null) }
                
                LaunchedEffect(Unit) {
                    intent?.let { receivedIntent ->
                        if (receivedIntent.action == "ACTION_SHOW_ACCEPTANCE_SCREEN") {
                            val data = receivedIntent.getStringExtra("HELPMARKHOLDER_INFORMATION")
                            Log.d(TAG, "🔔 Notification tapped - data: $data")
                            notificationData = data
                        }
                    }
                }

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
                    if (isSignedIn && currentUser != null && !hasNavigatedOnStartup) {
                        if (!deviceViewModel.isDeviceRegistered()) {
                            deviceViewModel.callRegisterNewDevice(0.0, 0.0)
                        }
                        val target = when {
                            currentUser?.role.isNullOrEmpty() -> AppScreen.RoleSelection.name
                            currentUser?.nickname.isNullOrEmpty() -> AppScreen.NicknameSetting.name
                            currentUser?.role == "supporter" -> AppScreen.SupporterHome.name
                            else -> AppScreen.HelpMarkHolderScreen.name
                        }
                        navController.navigate(target) { popUpTo(AppScreen.SignIn.name) { inclusive = true } }
                        hasNavigatedOnStartup = true
                    }
                }

                
                // 通知からの画面遷移処理
                val supporterViewModel: com.example.helpsync.viewmodel.SupporterViewModel = koinViewModel()
                LaunchedEffect(notificationData, isSignedIn, currentUser) {
                    if (notificationData != null && isSignedIn && currentUser?.role == "supporter") {
                        Log.d(TAG, "🔔 Processing notification data and storing in ViewModel")
                        
                        // ViewModelにデータを設定（SupporterHomeが表示されたときに自動遷移する）
                        val dataMap = mapOf(
                            "type" to "help-request",
                            "data" to notificationData!!
                        )
                        supporterViewModel.handleFCMData(dataMap)
                        
                        // SupporterHomeに遷移（そこから自動的に受け入れ画面へ）
                        navController.navigate(AppScreen.SupporterHome.name) {
                            popUpTo(AppScreen.SignIn.name) { inclusive = true }
                            launchSingleTop = true
                        }
                        
                        // データをクリア
                        notificationData = null
                    }
                }
                

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = AppScreen.SignIn.name,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        // --- 認証・設定 ---
                        composable(AppScreen.SignIn.name) {
                            LaunchedEffect(Unit) { hasNavigatedOnStartup = false }
                            SignInScreen(
                                onNavigateToSignUp = { navController.navigate(AppScreen.SignUp.name) },
                                onSignInSuccess = {}, userViewModel = userViewModel
                            )
                        }
                        composable(AppScreen.SignUp.name) {
                            LaunchedEffect(Unit) { hasNavigatedOnStartup = false }
                            SignUpScreen(
                                onNavigateToSignIn = { navController.navigate(AppScreen.SignIn.name) },
                                onSignUpSuccess = {}, userViewModel = userViewModel
                            )
                        }
                        composable(AppScreen.RoleSelection.name) {
                            RoleSelectionScreen { roleType ->
                                val roleString = if(roleType == RoleType.SUPPORTER) "supporter" else "requester"
                                selectedRole = roleString
                                userViewModel.updateRole(roleString)
                                val next = if(roleType == RoleType.SUPPORTER) AppScreen.NicknameSetting.name else AppScreen.HelpMarkHolderProfile.name
                                navController.navigate(next)
                            }
                        }

                        // --- ヘルプマーク所持者フロー ---
                        composable(AppScreen.HelpMarkHolderScreen.name) {
                            // 1. 新規リクエストIDが生成されたらマッチング画面へ
                            val createdRequestId by helpMarkHolderViewModel.createdHelpRequestId.collectAsState()
                            LaunchedEffect(createdRequestId) {
                                if (createdRequestId != null) {
                                    Log.d(TAG, "ID生成完了: $createdRequestId -> マッチング画面へ")
                                    navController.navigate("${AppScreen.HelpMarkHolderMatching.name}/$createdRequestId")
                                    helpMarkHolderViewModel.consumeHelpRequestId()
                                }
                            }

                            HelpMarkHolderScreen(
                                mainNavController = navController,
                                userViewModel = userViewModel,
                                helpMarkHolderViewModel = helpMarkHolderViewModel,
                                locationClient = fusedLocationClient,
                                onSignOut = { hasNavigatedOnStartup = false },
                                onMatchingEstablished = { } // 自動遷移するためここは空でOK
                            )
                        }

                        composable(
                            route = "${AppScreen.HelpMarkHolderMatching.name}/{requestId}",
                            arguments = listOf(navArgument("requestId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
                            val matchedSupporterInfo by helpMarkHolderViewModel.matchedSupporterNavInfo.collectAsState()
                            Log.d(TAG, "【監視中】HelpMarkHolderViewModelのデータ: $matchedSupporterInfo")

                            LaunchedEffect(Unit) {
                                Log.d(TAG, "【監視開始】HelpMarkHolderViewModelのデータ変化を待ちます...")

                                // helpMarkHolderViewModel のデータを監視
                                helpMarkHolderViewModel.matchedSupporterNavInfo.collect { info ->
                                    Log.d(TAG, "【受信】データ状態: $info")

                                    if (info != null) {
                                        Log.d(TAG, "★データあり！ 画面遷移を実行します")

                                        navController.navigate("${AppScreen.HelpMarkHolderMatchingComplete.name}/$requestId") {
                                            popUpTo(AppScreen.HelpMarkHolderMatching.name) { inclusive = true }
                                        }

                                        // ※ViewModelのデータはComplete画面で使うので、ここではクリアしない（consumeはComplete画面離脱時などに行う）
                                    }
                                }
                            }

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

                        // ★修正: URL引数の {supporterInfo} を削除し、{requestId} だけに戻しました
                        composable(
                            route = "${AppScreen.HelpMarkHolderMatchingComplete.name}/{requestId}",
                            arguments = listOf(navArgument("requestId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""

                            val savedSupporterInfo = helpMarkHolderViewModel.matchedSupporterNavInfo.collectAsState().value
                            HelpMarkHolderMatchingCompleteScreen(
                                requestId = requestId,
                                navSupporterInfo = savedSupporterInfo, // ここに渡す
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
                                onNicknameChange = { userViewModel.updateNickname(it) },
                                photoUri = photoUri,
                                onPhotoChange = { photoUri = it },
                                onPhotoSave = { uri ->
                                    userViewModel.uploadProfileImage(uri) { url ->
                                        if (url.isNotEmpty()) userViewModel.updateUserIconUrl(url)
                                    }
                                },
                                userViewModel = userViewModel,
                                onSignOut = {
                                    hasNavigatedOnStartup = false
                                    navController.navigate(AppScreen.SignIn.name) { popUpTo(0) { inclusive = true } }
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
                                    navController.navigate(AppScreen.SignIn.name) { popUpTo(0) { inclusive = true } }
                                }
                            )
                        }

                        composable(AppScreen.Profile.name) {
                            ProfileScreen(
                                onNavigateToEdit = { navController.navigate(AppScreen.ProfileEdit.name) },
                                onSignOut = {
                                    hasNavigatedOnStartup = false
                                    navController.navigate(AppScreen.SignIn.name) { popUpTo(0) { inclusive = true } }
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
                                onNicknameChange = { },
                                photoUri = photoUri,
                                onPhotoChange = { photoUri = it },
                                userViewModel = userViewModel,
                                onBackClick = {
                                    navController.navigate(AppScreen.RoleSelection.name) {
                                        popUpTo(AppScreen.NicknameSetting.name) { inclusive = true }
                                    }
                                },
                                onDoneClick = { nickname ->
                                    userViewModel.updateNickname(nickname)
                                    if (userViewModel.currentUser?.role.isNullOrEmpty()) {
                                        selectedRole?.let { userViewModel.updateRole(it) }
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
                                    navController.navigate(AppScreen.SignIn.name) { popUpTo(0) { inclusive = true } }
                                }
                            )
                        }

                        composable(
                            route = "HelpRequestDetailScreen/{supporterInformation}",
                            arguments = listOf(navArgument("supporterInformation") { type = NavType.StringType }),
                            deepLinks = listOf(navDeepLink {
                                uriPattern = "app://helpsync/HelpRequestDetailScreen/{supporterInformation}"
                                action = "ACTION_SHOW_ACCEPTANCE_SCREEN"
                            })
                        ) {
                            Text("Acceptance Screen (実装中)")
                        }

                        composable(AppScreen.HelpMarkHolderProfileFromSettings.name) {
                            HelpMarkHolderProfileScreen(
                                onBackClick = { navController.popBackStack() },
                                onCompleteClick = { navController.popBackStack() },
                                onSignOut = {
                                    hasNavigatedOnStartup = false
                                    navController.navigate(AppScreen.SignIn.name) { popUpTo(0) { inclusive = true } }
                                }
                            )
                        }
                        
                        // ディープリンク対応：通知からの遷移
                        composable(
                            route = "RequestAcceptance/{requestId}",
                            arguments = listOf(navArgument("requestId") { type = NavType.StringType }),
                            deepLinks = listOf(navDeepLink {
                                uriPattern = "app://helpsync/RequestAcceptance/{requestId}"
                                action = "ACTION_SHOW_ACCEPTANCE_SCREEN"
                            })
                        ) { backStackEntry ->
                            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
                            RequestAcceptanceScreen(
                                onDoneClick = {
                                    navController.navigate(AppScreen.SupporterHome.name) {
                                        popUpTo("RequestAcceptance/{requestId}") { inclusive = true }
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