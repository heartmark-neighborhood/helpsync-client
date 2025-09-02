package com.example.helpsync.viewmodel

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helpsync.data.User
import com.example.helpsync.repository.UserRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import java.util.Date

class UserViewModel : ViewModel() {
    private val userRepository = UserRepository()
    
    companion object {
        private const val TAG = "UserViewModel"
    }

    var currentUser by mutableStateOf<User?>(null)
        private set
    
    var isLoading by mutableStateOf(false)
        private set

    var isUploadingImage by mutableStateOf(false)
        private set


    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    var isSignedIn by mutableStateOf(false)
        private set

    init {
        Log.d(TAG, "=== UserViewModel Init ===")
        
        // 開発時のみ自動サインアウト（本番では無効化）
        val isDevelopment = false // Storage テストのため一時的に無効化
        if (isDevelopment) {
            userRepository.signOut()
            Log.d(TAG, "Auto sign out on app startup (development mode)")
        } else {
            Log.d(TAG, "Development mode disabled, preserving auth state")
        }
        
        // 認証状態を初期化
        isSignedIn = false
        currentUser = null
        
        // 現在の認証状態をチェック
        val currentFirebaseUser = userRepository.getCurrentUser()
        if (currentFirebaseUser != null) {
            Log.d(TAG, "Found existing authenticated user: ${currentFirebaseUser.uid}")
            isSignedIn = true
            // ユーザーデータを読み込み
            viewModelScope.launch {
                loadUserData(currentFirebaseUser.uid)
            }
        } else {
            Log.d(TAG, "No authenticated user found")
        }
        
        // TODO: 認証状態の変更監視を後で追加
        /*
        userRepository.addAuthStateListener { firebaseUser ->
            Log.d(TAG, "=== Auth State Changed ===")
            Log.d(TAG, "Firebase User: ${firebaseUser?.uid}")
            Log.d(TAG, "Is authenticated: ${firebaseUser != null}")
            
            isSignedIn = firebaseUser != null
            
            if (firebaseUser != null) {
                Log.d(TAG, "User signed in, loading data...")
                loadUserData(firebaseUser.uid)
            } else {
                Log.d(TAG, "User signed out, clearing data...")
                currentUser = null
            }
        }
        */
    }

    fun signUp(email: String, password: String, nickname: String = "", role: String = "", physicalFeatures: String = "") {
        viewModelScope.launch {
            Log.d(TAG, "Starting signUp process")
            isLoading = true
            errorMessage = null
            
            userRepository.signUp(email, password)
                .onSuccess { firebaseUser ->
                    Log.d(TAG, "Authentication successful, creating user document")
                    val currentTime = Date()
                    val user = User(
                        email = email,
                        role = role,
                        nickname = nickname,
                        physicalFeatures = physicalFeatures,
                        createdAt = currentTime,
                        updatedAt = currentTime
                    )
                    
                    userRepository.createUser(firebaseUser.uid, user)
                        .onSuccess {
                            Log.d(TAG, "✅ User document created successfully")
                            currentUser = user
                            isSignedIn = true  // サインアップ成功時に認証状態を更新
                        }
                        .onFailure { error ->
                            Log.e(TAG, "❌ Failed to create user document: ${error.message}")
                            Log.e(TAG, "Error details: ${error.localizedMessage}")
                            Log.e(TAG, "Error cause: ${error.cause}")
                            
                            // より詳細なエラーメッセージを表示
                            val detailedError = """
                                データベース保存エラー:
                                メッセージ: ${error.message}
                                詳細: ${error.localizedMessage}
                                原因: ${error.cause}
                                タイプ: ${error.javaClass.simpleName}
                            """.trimIndent()
                            
                            errorMessage = detailedError
                        }
                }
                .onFailure { error ->
                    Log.e(TAG, "Authentication failed: ${error.message}")
                    isSignedIn = false
                    errorMessage = error.message
                }
            
            isLoading = false
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            Log.d(TAG, "Starting signIn process")
            isLoading = true
            errorMessage = null
            
            userRepository.signIn(email, password)
                .onSuccess { firebaseUser ->
                    Log.d(TAG, "✅ SignIn successful for user: ${firebaseUser.uid}")
                    isSignedIn = true  // 認証状態を更新
                    loadUserData(firebaseUser.uid)
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ SignIn failed: ${error.message}")
                    isSignedIn = false
                    errorMessage = error.message
                }
            
            isLoading = false
        }
    }

    fun signOut() {
        Log.d(TAG, "SignOut requested")
        userRepository.signOut()
        isSignedIn = false
        currentUser = null
        Log.d(TAG, "✅ SignOut completed")
    }

    private fun loadUserData(uid: String) {
        viewModelScope.launch {
            Log.d(TAG, "=== loadUserData called ===")
            Log.d(TAG, "Loading user data for UID: $uid")
            
            isLoading = true
            
            userRepository.getUser(uid)
                .onSuccess { user ->
                    Log.d(TAG, "✅ User data loaded successfully: $user")
                    currentUser = user
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Failed to load user data: ${error.message}")
                    errorMessage = error.message
                }
            
            isLoading = false
        }
    }

    // 公開メソッド：現在認証されているユーザーのデータを再読み込み
    fun refreshCurrentUserData() {
        Log.d(TAG, "=== refreshCurrentUserData called ===")
        val firebaseUser = userRepository.getCurrentUser()
        if (firebaseUser != null) {
            Log.d(TAG, "🔄 Refreshing user data for UID: ${firebaseUser.uid}")
            loadUserData(firebaseUser.uid)
        } else {
            Log.e(TAG, "❌ No authenticated user found for refresh")
            errorMessage = "認証されたユーザーが見つかりません。再ログインしてください。"
        }
    }

    fun updateUser(user: User) {
        viewModelScope.launch {
            Log.d(TAG, "=== updateUser called ===")
            Log.d(TAG, "User to update: $user")
            
            isLoading = true
            errorMessage = null
            
            val currentFirebaseUser = userRepository.getCurrentUser()
            Log.d(TAG, "Current Firebase user: ${currentFirebaseUser?.uid}")
            
            if (currentFirebaseUser != null) {
                userRepository.updateUser(currentFirebaseUser.uid, user)
                    .onSuccess {
                        currentUser = user
                        Log.d(TAG, "✅ User updated successfully in Firebase")
                        Log.d(TAG, "Updated currentUser: $currentUser")
                    }
                    .onFailure { error ->
                        Log.e(TAG, "❌ Failed to update user: ${error.message}")
                        errorMessage = error.message
                    }
            } else {
                Log.e(TAG, "❌ No authenticated user found")
                errorMessage = "ユーザーが認証されていません"
            }
            
            isLoading = false
        }
    }

    fun updateNickname(nickname: String) {
        Log.d(TAG, "=== updateNickname called ===")
        Log.d(TAG, "New nickname: '$nickname'")
        Log.d(TAG, "Current user: $currentUser")
        
        currentUser?.let { user ->
            Log.d(TAG, "Current user exists, updating...")
            val updatedUser = user.copy(nickname = nickname)
            Log.d(TAG, "Updated user: $updatedUser")
            updateUser(updatedUser)
        } ?: run {
            Log.e(TAG, "❌ No current user found for nickname update")
            errorMessage = "ユーザー情報が見つかりません。再ログインしてください。"
        }
    }

    fun updatePhysicalFeatures(physicalFeatures: String) {
        currentUser?.let { user ->
            val updatedUser = user.copy(physicalFeatures = physicalFeatures)
            updateUser(updatedUser)
        }
    }

    fun updateRole(role: String) {
        Log.d(TAG, "=== updateRole called ===")
        Log.d(TAG, "New role: '$role'")
        Log.d(TAG, "Current user: $currentUser")
        
        currentUser?.let { user ->
            Log.d(TAG, "Current user exists, updating role...")
            val updatedUser = user.copy(role = role)
            Log.d(TAG, "Updated user with role: $updatedUser")
            updateUser(updatedUser)
        } ?: run {
            Log.e(TAG, "❌ No current user found for role update")
            errorMessage = "ユーザー情報が見つかりません。再ログインしてください。"
        }
    }

    fun updateIconUrl(iconUrl: String) {
        Log.d(TAG, "=== updateIconUrl called ===")
        Log.d(TAG, "New iconUrl: '$iconUrl'")
        Log.d(TAG, "Current user: $currentUser")
        
        currentUser?.let { user ->
            Log.d(TAG, "Current user exists, updating iconUrl...")
            val updatedUser = user.copy(iconUrl = iconUrl)
            Log.d(TAG, "Updated user with iconUrl: $updatedUser")
            updateUser(updatedUser)
        } ?: run {
            Log.e(TAG, "❌ No current user found for iconUrl update")
            errorMessage = "ユーザー情報が見つかりません。再ログインしてください。"
        }
    }

    fun clearError() {
        errorMessage = null
    }
    
    // コールバック機能付きの画像アップロードメソッド（古い画像削除機能付き）
    fun uploadProfileImage(imageUri: Uri, onComplete: (String) -> Unit) {
        Log.d(TAG, "=== uploadProfileImage with callback called ===")
        Log.d(TAG, "Image URI: $imageUri")
        
        // 既にアップロード中の場合は処理をスキップ
        if (isUploadingImage) {
            Log.d(TAG, "⚠️ Upload already in progress, ignoring request")
            onComplete("")
            return
        }
        
        currentUser?.let { user ->
            Log.d(TAG, "Current user exists: ${user.email}")
            Log.d(TAG, "Current user existing iconUrl: ${user.iconUrl}")
            
            // 既存の画像URLを保存（後で削除するため）
            val oldImageUrl = user.iconUrl
            
            // FirebaseユーザーのUIDを取得
            val userId = userRepository.getCurrentUserId()
            if (userId == null) {
                Log.e(TAG, "❌ No Firebase user ID found")
                errorMessage = "認証情報が見つかりません。再ログインしてください。"
                return
            }
            
            Log.d(TAG, "Firebase User ID: $userId")
            
            isLoading = true
            errorMessage = null
            
            viewModelScope.launch {
                try {
                    Log.d(TAG, "Starting Firebase Storage upload with callback...")
                    val result = userRepository.uploadProfileImage(imageUri, userId)
                    
                    result.onSuccess { downloadUrl ->
                        Log.d(TAG, "✅ Upload successful: $downloadUrl")
                        
                        // 古い画像を削除（新しい画像のアップロードが成功した場合のみ）
                        if (!oldImageUrl.isNullOrEmpty() && oldImageUrl != downloadUrl) {
                            Log.d(TAG, "🗑️ Deleting old profile image: $oldImageUrl")
                            viewModelScope.launch {
                                try {
                                    val deleteResult = userRepository.deleteOldProfileImage(oldImageUrl)
                                    deleteResult.onSuccess {
                                        Log.d(TAG, "✅ Old image deleted successfully")
                                    }.onFailure { deleteException ->
                                        Log.w(TAG, "⚠️ Failed to delete old image (non-critical): ${deleteException.message}")
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, "⚠️ Exception during old image deletion (non-critical): ${e.message}")
                                }
                            }
                        } else {
                            Log.d(TAG, "🔄 No old image to delete (oldImageUrl: '$oldImageUrl')")
                        }
                        
                        onComplete(downloadUrl)
                    }.onFailure { exception ->
                        Log.e(TAG, "❌ Upload failed: ${exception.message}")
                        errorMessage = "画像のアップロードに失敗しました: ${exception.message}"
                        onComplete("")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Unexpected error during image upload", e)
                    errorMessage = "予期しないエラーが発生しました: ${e.message}"
                    onComplete("")
                } finally {
                    isLoading = false
                }
            }
        } ?: run {
            Log.e(TAG, "❌ No current user found for image upload")
            errorMessage = "ユーザー情報が見つかりません。再ログインしてください。"
            onComplete("")
        }
    }
    
    // ユーザーのiconUrlを更新する専用メソッド
    fun updateUserIconUrl(iconUrl: String) {
        Log.d(TAG, "=== updateUserIconUrl called ===")
        Log.d(TAG, "New iconUrl: $iconUrl")
        
        currentUser?.let { user ->
            Log.d(TAG, "Current user: ${user.email}")
            Log.d(TAG, "Old iconUrl: ${user.iconUrl}")
            
            // ユーザー情報を更新
            val updatedUser = user.copy(iconUrl = iconUrl)
            Log.d(TAG, "Updated user iconUrl: ${updatedUser.iconUrl}")
            
            // Firestoreに保存
            updateUser(updatedUser)
            
            Log.d(TAG, "✅ User iconUrl updated successfully")
        } ?: run {
            Log.e(TAG, "❌ No current user found for iconUrl update")
            errorMessage = "ユーザー情報が見つかりません。"
        }
    }

    fun saveProfileChanges(
        nickname: String,
        physicalFeatures: String,
        imageUri: Uri?,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val userId = userRepository.getCurrentUserId()
                    ?: throw IllegalStateException("User not authenticated")

                val initialUser = currentUser
                    ?: throw IllegalStateException("Current user data not found")

                val finalIconUrl = if (imageUri != null) {
                    Log.d(TAG, "Uploading new profile image...")
                    val result = userRepository.uploadProfileImage(imageUri, userId)
                    val downloadUrl = result.getOrThrow()

                    // Delete old image if upload is successful and URL is different
                    val oldImageUrl = initialUser.iconUrl
                    if (!oldImageUrl.isNullOrEmpty() && oldImageUrl != downloadUrl) {
                        Log.d(TAG, "Deleting old profile image: $oldImageUrl")
                        userRepository.deleteOldProfileImage(oldImageUrl)
                    }
                    downloadUrl
                } else {
                    initialUser.iconUrl
                }

                val updatedUser = initialUser.copy(
                    nickname = nickname,
                    physicalFeatures = physicalFeatures,
                    iconUrl = finalIconUrl
                )

                if (updatedUser != initialUser) {
                    Log.d(TAG, "Updating user profile information...")
                    userRepository.updateUser(userId, updatedUser).getOrThrow()
                    currentUser = updatedUser
                    Log.d(TAG, "User profile updated successfully.")
                } else {
                    Log.d(TAG, "No changes detected, skipping update.")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to save profile changes", e)
                errorMessage = "プロフィールの更新に失敗しました: ${e.message}"
            } finally {
                isLoading = false
                onComplete()
            }
        }
    }
    
    // デバッグ用：認証状態確認
    fun getCurrentFirebaseUser() = userRepository.getCurrentUser()
}
