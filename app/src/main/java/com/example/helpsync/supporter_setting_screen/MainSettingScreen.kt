package com.example.helpsync.supporter_setting_screen

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.example.helpsync.nickname_setting.NicknameSetting
import com.example.helpsync.viewmodel.UserViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun MainSettingScreen(
    userViewModel: UserViewModel = koinViewModel()
) {
    val currentUser = userViewModel.currentUser
    var nickname by rememberSaveable { mutableStateOf(currentUser?.nickname ?: "") }
    var photoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var isEditing by remember { mutableStateOf(true) }

    // デバッグログ
    LaunchedEffect(Unit) {
        Log.d("MainSettingsScreen", "🎯 MainSettingsScreen started")
        Log.d("MainSettingsScreen", "📱 Current user: ${currentUser?.nickname}")
        
        // Firebase認証状態の確認
        Log.d("MainSettingsScreen", "🔐 Checking Firebase Auth state...")
        val firebaseUser = userViewModel.getCurrentFirebaseUser()
        Log.d("MainSettingsScreen", "Firebase user: $firebaseUser")
        Log.d("MainSettingsScreen", "Firebase user ID: ${firebaseUser?.uid}")
        Log.d("MainSettingsScreen", "Firebase user email: ${firebaseUser?.email}")
        Log.d("MainSettingsScreen", "Is user logged in: ${firebaseUser != null}")
        
        // UserViewModelの状態確認
        Log.d("MainSettingsScreen", "UserViewModel isSignedIn: ${userViewModel.isSignedIn}")
        Log.d("MainSettingsScreen", "UserViewModel isLoading: ${userViewModel.isLoading}")
        Log.d("MainSettingsScreen", "UserViewModel errorMessage: ${userViewModel.errorMessage}")
    }

    // ユーザーデータが読み込まれたら初期値を設定
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            Log.d("MainSettingsScreen", "👤 User data loaded: ${user.nickname}")
            nickname = user.nickname
        }
    }

    if (isEditing) {
        NicknameSetting(
            nickname = nickname,
            onNicknameChange = { newNickname: String -> 
                Log.d("MainSettingsScreen", "✏️ Nickname changing to: $newNickname")
                nickname = newNickname 
            },
            photoUri = photoUri,
            onPhotoChange = { uri: Uri? -> photoUri = uri },
            userViewModel = userViewModel,
            onBackClick = { 
                Log.d("MainSettingsScreen", "⬅️ Back button clicked")
                isEditing = false 
            },
            onDoneClick = { doneNickname: String -> 
                Log.d("MainSettingsScreen", "✅ Done button clicked, saving nickname: $doneNickname")
                // ニックネームをデータベースに保存
                userViewModel.updateNickname(doneNickname)
                isEditing = false 
            }
        )
    } else {
        SupporterSettingScreen(
            nickname = nickname,
            onNicknameChange = { newNickname: String -> 
                Log.d("MainSettingsScreen", "🔄 SupporterSettings nickname changed to: $newNickname")
                nickname = newNickname
                // ニックネーム変更時にデータベースに保存
                userViewModel.updateNickname(newNickname)
            },
            photoUri = photoUri,
            onPhotoChange = { uri -> photoUri = uri },
            modifier = Modifier,
            onEditClick = { newNickname: String -> 
                Log.d("MainSettingsScreen", "✏️ Edit button clicked")
                isEditing = true 
            },
            onPhotoSave = { uri: Uri ->
                Log.d("MainSettingsScreen", "📸 Photo save clicked")
                Log.d("MainSettingsScreen", "URI: $uri")
                Log.d("MainSettingsScreen", "URI scheme: ${uri.scheme}")
                Log.d("MainSettingsScreen", "URI path: ${uri.path}")
                Log.d("MainSettingsScreen", "Current user: ${userViewModel.currentUser}")
                Log.d("MainSettingsScreen", "Is loading: ${userViewModel.isLoading}")
                
                // 写真をFirebase StorageにアップロードしてからFirestoreに保存
                try {
                    Log.d("MainSettingsScreen", "Calling userViewModel.uploadProfileImage...")
                    userViewModel.uploadProfileImage(uri) { downloadUrl ->
                        Log.d("MainSettingsScreen", "✅ 画像アップロード完了: $downloadUrl")
                        if (downloadUrl.isNotEmpty()) {
                            Log.d("MainSettingsScreen", "💾 iconUrlをデータベースに保存中...")
                            userViewModel.updateUserIconUrl(downloadUrl)
                        } else {
                            Log.e("MainSettingsScreen", "❌ ダウンロードURLが空です")
                        }
                    }
                    Log.d("MainSettingsScreen", "✅ uploadProfileImage called successfully")
                } catch (e: Exception) {
                    Log.e("MainSettingsScreen", "❌ Error calling uploadProfileImage: ${e.message}", e)
                }
            },
            userViewModel = userViewModel // UserViewModelを明示的に渡す
        )
    }
}