package com.example.helpsync.viewmodel

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helpsync.data.HelpRequest
import com.example.helpsync.data.User
import com.example.helpsync.repository.UserRepository
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    // ▼▼▼ ここから下の3つを新規追加 ▼▼▼
    private val _activeHelpRequest = MutableStateFlow<HelpRequest?>(null)
    val activeHelpRequest = _activeHelpRequest.asStateFlow()

    private val _pendingHelpRequests = MutableStateFlow<List<HelpRequest>>(emptyList())
    val pendingHelpRequests = _pendingHelpRequests.asStateFlow()

    /**
     * PENDING状態のヘルプリクエスト一覧を取得してStateFlowを更新する
     */
    fun fetchPendingHelpRequests() {
        viewModelScope.launch {
            isLoading = true
            userRepository.getPendingHelpRequests()
                .onSuccess { requests ->
                    _pendingHelpRequests.value = requests
                }
                .onFailure { error ->
                    errorMessage = "リクエスト一覧の取得に失敗: ${error.message}"
                }
            isLoading = false
        }
    }

    // Firestoreのリスナーを保持するための変数
    private var requestListener: ListenerRegistration? = null
    // ▲▲▲ ここまで新規追加 ▲▲▲

    init {
        Log.d(TAG, "=== UserViewModel Init ===")

        val isDevelopment = false
        if (isDevelopment) {
            userRepository.signOut()
            Log.d(TAG, "Auto sign out on app startup (development mode)")
        } else {
            Log.d(TAG, "Development mode disabled, preserving auth state")
        }

        isSignedIn = false
        currentUser = null

        val currentFirebaseUser = userRepository.getCurrentUser()
        if (currentFirebaseUser != null) {
            Log.d(TAG, "Found existing authenticated user: ${currentFirebaseUser.uid}")
            isSignedIn = true
            viewModelScope.launch {
                loadUserData(currentFirebaseUser.uid)
            }
        } else {
            Log.d(TAG, "No authenticated user found")
        }
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
                            isSignedIn = true
                        }
                        .onFailure { error ->
                            Log.e(TAG, "❌ Failed to create user document: ${error.message}")
                            Log.e(TAG, "Error details: ${error.localizedMessage}")
                            Log.e(TAG, "Error cause: ${error.cause}")

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
                    isSignedIn = true
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

    fun uploadProfileImage(imageUri: Uri, onComplete: (String) -> Unit) {
        Log.d(TAG, "=== uploadProfileImage with callback called ===")
        Log.d(TAG, "Image URI: $imageUri")

        if (isUploadingImage) {
            Log.d(TAG, "⚠️ Upload already in progress, ignoring request")
            onComplete("")
            return
        }

        currentUser?.let { user ->
            Log.d(TAG, "Current user exists: ${user.email}")
            Log.d(TAG, "Current user existing iconUrl: ${user.iconUrl}")

            val oldImageUrl = user.iconUrl

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

    fun updateUserIconUrl(iconUrl: String) {
        Log.d(TAG, "=== updateUserIconUrl called ===")
        Log.d(TAG, "New iconUrl: $iconUrl")

        currentUser?.let { user ->
            Log.d(TAG, "Current user: ${user.email}")
            Log.d(TAG, "Old iconUrl: ${user.iconUrl}")

            val updatedUser = user.copy(iconUrl = iconUrl)
            Log.d(TAG, "Updated user iconUrl: ${updatedUser.iconUrl}")

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

    fun getCurrentFirebaseUser() = userRepository.getCurrentUser()

    // ▼▼▼ ここから下の4つの関数を新規追加 ▼▼▼

    /**
     * ヘルプマーク所持者が支援を要請する
     */
    fun createHelpRequest() {
        viewModelScope.launch {
            val user = currentUser ?: return@launch
            val uid = userRepository.getCurrentUserId() ?: return@launch
            isLoading = true
            errorMessage = null

            userRepository.createHelpRequest(uid, user.nickname)
                .onSuccess { newRequest ->
                    _activeHelpRequest.value = newRequest
                    // リアルタイムでリクエストの更新を監視開始
                    listenForRequestUpdates(newRequest.id)
                }
                .onFailure { error ->
                    errorMessage = "リクエストの作成に失敗しました: ${error.message}"
                }
            isLoading = false
        }
    }

    /**
     * サポーターが近接確認をサーバーに通知する
     */
    fun handleProximityVerificationResult(requestId: String) {
        viewModelScope.launch {
            val supporterId = userRepository.getCurrentUserId() ?: return@launch
            isLoading = true
            userRepository.handleProximityVerificationResult(requestId, supporterId)
                .onFailure { error ->
                    errorMessage = "マッチング処理に失敗しました: ${error.message}"
                }
            isLoading = false
        }
    }

    /**
     * リクエストの状態変更の監視を開始する
     */
    private fun listenForRequestUpdates(requestId: String) {
        // 既存のリスナーがあれば解除
        requestListener?.remove()
        requestListener = userRepository.listenForRequestUpdates(requestId) { updatedRequest ->
            _activeHelpRequest.value = updatedRequest
        }
    }

    /**
     * ViewModelが破棄されるときにリスナーを解除する
     */
    override fun onCleared() {
        requestListener?.remove()
        super.onCleared()
    }

    // ▼▼▼ ここから下のコードをUserViewModelの末尾に追加 ▼▼▼

    // サポーターが見つけたリクエストの詳細を保持するStateFlow
    private val _viewedHelpRequest = MutableStateFlow<HelpRequest?>(null)
    val viewedHelpRequest = _viewedHelpRequest.asStateFlow()

    /**
     * サポーターが特定のヘルプリクエストの詳細を取得する
     */
    fun getRequestDetails(requestId: String) {
        viewModelScope.launch {
            isLoading = true
            userRepository.getRequest(requestId)
                .onSuccess { request ->
                    _viewedHelpRequest.value = request
                }
                .onFailure { error ->
                    errorMessage = "リクエストの取得に失敗: ${error.message}"
                }
            isLoading = false
        }
    }

    /**
     * 表示中のリクエスト詳細をクリアする
     */
    fun clearViewedRequest() {
        _viewedHelpRequest.value = null
    }

    // マッチングしたリクエストの詳細
    private val _matchedRequestDetails = MutableStateFlow<HelpRequest?>(null)
    val matchedRequestDetails = _matchedRequestDetails.asStateFlow()
    // リクエスター（助けを求めた人）のプロフィール情報
    private val _requesterProfile = MutableStateFlow<User?>(null)
    val requesterProfile = _requesterProfile.asStateFlow()
    // サポーター（支援者）のプロフィール情報
    private val _supporterProfile = MutableStateFlow<User?>(null)
    val supporterProfile = _supporterProfile.asStateFlow()
    /**
     * マッチングが成立したリクエストIDを元に、関連するすべての情報（リクエスト、双方のプロフィール）を読み込む
     */
    fun loadMatchedRequestDetails(requestId: String) {
        if (requestId.isBlank()) return
        viewModelScope.launch {
            isLoading = true
            // まずリクエスト自体の詳細を取得
            userRepository.getRequest(requestId)
                .onSuccess { request ->
                    _matchedRequestDetails.value = request
                    // リクエスターのプロフィールを取得
                    if (request.requesterId.isNotBlank()) {
                        userRepository.getUser(request.requesterId)
                            .onSuccess { user -> _requesterProfile.value = user }
                            .onFailure { clearMatchedDetails() /* エラー時はクリア */ }
                    }
                    // サポーターのプロフィールを取得
                    if (!request.matchedSupporterId.isNullOrBlank()) {
                        userRepository.getUser(request.matchedSupporterId)
                            .onSuccess { user -> _supporterProfile.value = user }
                            .onFailure { clearMatchedDetails() /* エラー時はクリア */ }
                    }
                }
                .onFailure {
                    errorMessage = "リクエスト詳細の取得に失敗しました。"
                    clearMatchedDetails()
                }
            isLoading = false
        }
    }
    /**
     * 表示しているマッチング詳細情報をすべてクリアする
     */
    fun clearMatchedDetails() {
        _matchedRequestDetails.value = null
        _requesterProfile.value = null
        _supporterProfile.value = null
    }
}
