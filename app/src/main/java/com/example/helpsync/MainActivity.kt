package com.example.helpsync

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.helpsync.auth.SignInScreen
import com.example.helpsync.auth.SignUpScreen
import com.example.helpsync.nickname_setting.NicknameSetting
import com.example.helpsync.profile.ProfileEditScreen
import com.example.helpsync.profile.ProfileScreen
import com.example.helpsync.role_selection_screen.RoleSelectionScreen
import com.example.helpsync.role_selection_screen.RoleType
import com.example.helpsync.ui.theme.HelpSyncTheme
import com.example.helpsync.help_mark_holder_home_screen.HelpMarkHolderHomeScreen
import com.example.helpsync.help_mark_holder_profile_screen.HelpMarkHolderProfileScreen
import com.example.helpsync.help_mark_holder_matching_screen.HelpMarkHolderMatchingScreen
import com.example.helpsync.help_mark_holder_matching_complete_screen.HelpMarkHolderMatchingCompleteScreen
import com.example.helpsync.help_mark_holder_matching_complete_screen.SupporterInfo
import com.example.helpsync.settings_screen.SettingsScreen
import com.example.helpsync.viewmodel.UserViewModel
import androidx.compose.runtime.saveable.rememberSaveable
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.example.helpsync.blescanner.BLEScanReceiver
import android.content.IntentFilter
import com.example.helpsync.bleadvertiser.BLEAdvertiser
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.net.URLEncoder

// ナビゲーションの引数として渡すためのデータクラス
@Serializable
data class SupporterNavInfo(
    val nickname: String,
    val eta: String,
    val rating: Int,
    val iconUrl: String? = null
)

// 支援依頼情報を渡すためのデータクラス
@Serializable
data class RequestNavInfo(
    val nickname: String,
    val content: String
)

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var bleReceiver: BLEScanReceiver

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val allGranted = perms.entries.all {it.value}
        if(!allGranted) {
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
            auth.signOut()
            Log.d(TAG, "✅ Auto sign out on app startup")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firebase initialization failed: ${e.message}", e)
        }

        bleReceiver = BLEScanReceiver(::onScanResult)
        registerReceiver(bleReceiver, IntentFilter("com.example.SCAN_RESULT"), RECEIVER_NOT_EXPORTED)

        permissionLauncher.launch(arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.BLUETOOTH_ADVERTISE,
        ))

        enableEdgeToEdge()
        setContent {
            HelpSyncTheme {
                val navController = rememberNavController()
                val userViewModel: UserViewModel = viewModel()

                val bleAdvertiser = remember {
                    BLEAdvertiser(this, "0000180A-0000-1000-8000-00805F9B34FB")
                }

                var photoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
                var selectedRole by rememberSaveable { mutableStateOf<String?>(null) }

                val isSignedIn by remember { derivedStateOf { userViewModel.isSignedIn } }
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

                        composable(AppScreen.RoleSelection.name) {
                            RoleSelectionScreen(
                                onRoleSelected = { roleType ->
                                    val roleString = when (roleType) {
                                        RoleType.SUPPORTER -> "supporter"
                                        RoleType.HELP_MARK_HOLDER -> "requester"
                                    }
                                    selectedRole = roleString
                                    userViewModel.updateRole(roleString)
                                    when (roleType) {
                                        RoleType.SUPPORTER -> {
                                            navController.navigate(AppScreen.NicknameSetting.name)
                                        }
                                        RoleType.HELP_MARK_HOLDER -> {
                                            navController.navigate(AppScreen.HelpMarkHolderProfile.name)
                                        }
                                    }
                                }
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

                        composable(AppScreen.SupporterHome.name) {
                            MainScreen(
                                navController = navController,
                                nickname = userViewModel.currentUser?.nickname ?: "",
                                onNicknameChange = { nickname ->
                                    userViewModel.updateNickname(nickname)
                                },
                                photoUri = photoUri,
                                onPhotoChange = { uri: Uri? -> photoUri = uri },
                                onPhotoSave = { uri ->
                                    userViewModel.uploadProfileImage(uri) { downloadUrl ->
                                        Log.d(TAG, "✅ 画像アップロード完了: $downloadUrl")
                                        if (downloadUrl.isNotEmpty()) {
                                            Log.d(TAG, "💾 iconUrlをデータベースに保存中...")
                                            userViewModel.updateUserIconUrl(downloadUrl)
                                        } else {
                                            Log.e(TAG, "❌ ダウンロードURLが空です")
                                        }
                                    }
                                },
                                userViewModel = userViewModel
                            )
                        }

                        // RequestAcceptanceScreen と RequestDetail の定義は MainScreen.kt に移動したため、
                        // このファイルからは削除されています。

                        composable(AppScreen.HelpMarkHolderHome.name) {
                            HelpMarkHolderHomeScreen(
                                onMatchingClick = {
                                    navController.navigate(AppScreen.HelpMarkHolderMatching.name)
                                },
                                onHomeClick = {
                                },
                                onSettingsClick = {
                                    navController.navigate(AppScreen.Settings.name)
                                }
                            )
                        }

                        composable(AppScreen.HelpMarkHolderProfile.name) {
                            HelpMarkHolderProfileScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onCompleteClick = {
                                    navController.navigate(AppScreen.HelpMarkHolderHome.name) {
                                        popUpTo(AppScreen.RoleSelection.name) { inclusive = false }
                                    }
                                }
                            )
                        }

                        composable(AppScreen.HelpMarkHolderMatching.name) {
                            HelpMarkHolderMatchingScreen(
                                onMatchingComplete = {
                                    val dummySupporter = SupporterNavInfo(
                                        nickname = "やさしい人",
                                        eta = "3",
                                        rating = 4,
                                        iconUrl = "https://example.com/dummy-profile.jpg"
                                    )
                                    val infoJson = Json.encodeToString(dummySupporter)
                                    val encodedJson = URLEncoder.encode(infoJson, "UTF-8")
                                    navController.navigate(AppScreen.HelpMarkHolderMatchingComplete.name + "/$encodedJson") {
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
                            route = AppScreen.HelpMarkHolderMatchingComplete.name + "/{supporterInfo}",
                            arguments = listOf(
                                navArgument("supporterInfo") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val supporterInfoJson = backStackEntry.arguments?.getString("supporterInfo")

                            val supporterInfo = remember {
                                supporterInfoJson?.let {
                                    try {
                                        val decodedJson = URLDecoder.decode(it, "UTF-8")
                                        val navInfo = Json.decodeFromString<SupporterNavInfo>(decodedJson)
                                        SupporterInfo(
                                            nickname = navInfo.nickname,
                                            eta = navInfo.eta,
                                            rating = navInfo.rating,
                                            iconUrl = navInfo.iconUrl
                                        )
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to decode supporterInfo JSON: ${e.message}")
                                        null
                                    }
                                }
                            }

                            HelpMarkHolderMatchingCompleteScreen(
                                supporterInfo = supporterInfo ?: SupporterInfo(),
                                onHomeClick = {
                                    navController.navigate(AppScreen.HelpMarkHolderHome.name) {
                                        popUpTo(AppScreen.HelpMarkHolderMatchingComplete.name) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(
                            route = "HelpRequestDetailScreen/{supporterInformation}",
                            arguments = listOf(navArgument("supporterInformation") { type = NavType.StringType }),
                            // 通知からのディープリンクを定義
                            deepLinks = listOf(navDeepLink {
                                uriPattern = "app://helpsync/HelpRequestDetailScreen/{supporterInformation}"
                                action = "ACTION_SHOW_ACCEPTANCE_SCREEN" // Intent Actionと一致させる
                            })
                        ) { backStackEntry ->
                                val supporterInformation = backStackEntry.arguments?.getString("supporterInformation")
                                AcceptanceScreen(supporterInformation = supporterInformation ?: "")
                        }

                        composable(AppScreen.Settings.name) {
                            SettingsScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onCompleteClick = {
                                    navController.popBackStack()
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
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}