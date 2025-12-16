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
import com.example.helpsync.repository.CloudMessageRepository
import com.example.helpsync.repository.UserRepository
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import kotlinx.coroutines.tasks.await

import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase

class UserViewModel(
    private val cloudMessageRepository: CloudMessageRepository
) : ViewModel() {
    private val userRepository = UserRepository()
    private val functions = Firebase.functions("asia-northeast2")

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
        
        // ログイン状態を保持するため、自動サインアウトを完全に削除
        Log.d(TAG, "Preserving auth state on app startup")
        
        // 現在の認証状態をチェック
        val currentFirebaseUser = userRepository.getCurrentUser()
        if (currentFirebaseUser != null) {
            Log.d(TAG, "Found existing authenticated user: ${currentFirebaseUser.uid}")
            isSignedIn = true
            viewModelScope.launch {
                loadUserData(currentFirebaseUser.uid)
            }
        } else {
            Log.d(TAG, "No authenticated user found")
            isSignedIn = false
            currentUser = null
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
                            // currentUserを先に設定
                            currentUser = user
                            // 最後にisSignedInをtrueにする（これでMainActivityのLaunchedEffectが発火）
                            isSignedIn = true
                            isLoading = false
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
                            isLoading = false
                        }
                }
                .onFailure { error ->
                    Log.e(TAG, "Authentication failed: ${error.message}")
                    isSignedIn = false
                    errorMessage = error.message
                    isLoading = false
                }
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
                    isLoading = false
                }
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
                    isLoading = false
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Failed to load user data: ${error.message}")
                    errorMessage = error.message
                    isLoading = false
                }
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
    fun createHelpRequest(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            Log.d(TAG, "🚀 createHelpRequest called in UserViewModel")
            isLoading = true
            errorMessage = null

            val deviceId = try {
                cloudMessageRepository.getDeviceId()
            } catch (e: Exception) {
                Log.e(TAG, "❌ deviceIdの取得に失敗しました: ${e.message}")
                errorMessage = "デバイスIDの取得に失敗しました。"
                isLoading = false
                return@launch
            }

            if (deviceId.isNullOrBlank()) {
                Log.e(TAG, "❌ Cannot create help request: deviceId is null or blank")
                errorMessage = "デバイスIDがありません。"
                isLoading = false
                return@launch
            }

            try {
                Log.d(TAG, "📡 Calling createHelpRequest cloud function...")
                val locationMap = hashMapOf("latitude" to latitude, "longitude" to longitude)
                val data = hashMapOf("deviceId" to deviceId, "location" to locationMap)

                val callResult = functions.getHttpsCallable("createHelpRequest").call(data).await()
                Log.d(TAG, "✅ Cloud function returned successfully")

                val responseData = callResult.data as? Map<String, Any>
                val helpRequestId = (responseData?.get("helpRequestId") as? Map<String, String>)?.get("value")

                if (!helpRequestId.isNullOrBlank()) {
                    Log.d(TAG, "✅ Help request created with ID: $helpRequestId")
                    // listenForRequestUpdatesを呼び出す
                    listenForRequestUpdates(helpRequestId)
                } else {
                    Log.e(TAG, "❌ HelpRequestId not found in response: $responseData")
                    errorMessage = "ヘルプリクエストのID取得に失敗しました。"
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ createHelpRequestの呼びだしに失敗しました", e)
                errorMessage = "ヘルプリクエストの作成に失敗しました: ${e.message}"
            } finally {
                isLoading = false
            }
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
        Log.d(TAG, "🎧 ViewModel is attaching listener for requestId: $requestId") // ログ追加
        requestListener = userRepository.listenForRequestUpdates(requestId) { updatedRequest ->
            Log.d(TAG, "🔔 ViewModel received update from listener. New status: ${updatedRequest?.status}") // ログ追加
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
        Log.d("UserViewModel", "🔍 loadMatchedRequestDetails called with requestId: $requestId")
        if (requestId.isBlank()) {
            Log.w("UserViewModel", "⚠️ requestId is blank, returning")
            return
        }
        viewModelScope.launch {
            isLoading = true
            Log.d("UserViewModel", "📡 Fetching request details...")
            // まずリクエスト自体の詳細を取得
            userRepository.getRequest(requestId)
                .onSuccess { request ->
                    Log.d("UserViewModel", "✅ Request fetched successfully")
                    Log.d("UserViewModel", "  - requesterId: ${request.requesterId}")
                    Log.d("UserViewModel", "  - matchedSupporterId: ${request.matchedSupporterId}")
                    Log.d("UserViewModel", "  - status: ${request.status}")
                    _matchedRequestDetails.value = request
                    
                    // リクエスターのプロフィールを取得
                    if (request.requesterId.isNotBlank()) {
                        Log.d("UserViewModel", "📡 Fetching requester profile...")
                        userRepository.getUser(request.requesterId)
                            .onSuccess { user -> 
                                Log.d("UserViewModel", "✅ Requester profile fetched: ${user.nickname}")
                                _requesterProfile.value = user 
                            }
                            .onFailure { e ->
                                Log.e("UserViewModel", "❌ Failed to fetch requester profile: ${e.message}")
                                clearMatchedDetails() 
                            }
                    }
                    
                    // サポーターのプロフィールを取得
                    if (!request.matchedSupporterId.isNullOrBlank()) {
                        Log.d("UserViewModel", "📡 Fetching supporter profile...")
                        userRepository.getUser(request.matchedSupporterId)
                            .onSuccess { user -> 
                                Log.d("UserViewModel", "✅ Supporter profile fetched: ${user.nickname}")
                                Log.d("UserViewModel", "  - iconUrl: ${user.iconUrl}")
                                Log.d("UserViewModel", "  - physicalFeatures: ${user.physicalFeatures}")
                                _supporterProfile.value = user 
                            }
                            .onFailure { e ->
                                Log.e("UserViewModel", "❌ Failed to fetch supporter profile: ${e.message}")
                                clearMatchedDetails() 
                            }
                    } else {
                        Log.w("UserViewModel", "⚠️ matchedSupporterId is null or blank")
                    }
                }
                .onFailure { e ->
                    Log.e("UserViewModel", "❌ Failed to fetch request: ${e.message}")
                    errorMessage = "リクエスト詳細の取得に失敗しました。"
                    clearMatchedDetails()
                }
            isLoading = false
            Log.d("UserViewModel", "✅ loadMatchedRequestDetails completed")
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

    // SupporterDetailsScreenで使うためのサポーター情報
    private val _supporterDetailsJson = MutableStateFlow<Map<String, String>?>(null)
    val supporterDetailsJson = _supporterDetailsJson.asStateFlow()

    /**
     * requestIdを元にサポーター情報を読み込み、Map形式でStateFlowを更新する
     */
    fun loadSupporterDetails(requestId: String?) {
        Log.d(TAG, "🔍 loadSupporterDetails called with requestId: $requestId")
        if (requestId.isNullOrBlank()) {
            Log.w(TAG, "⚠️ requestId is null or blank, returning")
            return
        }

        viewModelScope.launch {
            isLoading = true
            userRepository.getRequest(requestId)
                .onSuccess { request ->
                    val supporterId = request.matchedSupporterId
                    if (supporterId.isNullOrBlank()) {
                        Log.e(TAG, "❌ Supporter ID not found in the request.")
                        errorMessage = "支援者情報が見つかりません。"
                        isLoading = false
                        return@launch
                    }

                    userRepository.getUser(supporterId)
                        .onSuccess { supporter ->
                            val supporterMap = mapOf(
                                "nickname" to (supporter.nickname ?: "ニックネーム不明"),
                                "iconUrl" to (supporter.iconUrl ?: ""),
                                "physicalFeatures" to (supporter.physicalFeatures ?: "追加情報なし")
                            )
                            _supporterDetailsJson.value = supporterMap
                            Log.d(TAG, "✅ Supporter details loaded and converted to map: $supporterMap")
                        }
                        .onFailure { e ->
                            Log.e(TAG, "❌ Failed to fetch supporter profile: ${e.message}")
                            errorMessage = "支援者情報の取得に失敗しました。"
                        }
                }
                .onFailure { e ->
                    Log.e(TAG, "❌ Failed to fetch request details: ${e.message}")
                    errorMessage = "リクエスト詳細の取得に失敗しました。"
                }
            isLoading = false
        }
    }

    /**
     * 表示しているサポーター詳細情報をクリアする
     */
    fun clearSupporterDetails() {
        _supporterDetailsJson.value = null
        Log.d(TAG, "🧹 Supporter details cleared.")
    }
}
