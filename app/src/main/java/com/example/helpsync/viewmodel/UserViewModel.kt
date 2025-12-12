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

    // サポーターが見つけたリクエストの詳細を保持するStateFlow
    private val _viewedHelpRequest = MutableStateFlow<HelpRequest?>(null)
    val viewedHelpRequest = _viewedHelpRequest.asStateFlow()

    // マッチングしたリクエストの詳細
    private val _matchedRequestDetails = MutableStateFlow<HelpRequest?>(null)
    val matchedRequestDetails = _matchedRequestDetails.asStateFlow()

    // リクエスター（助けを求めた人）のプロフィール情報
    private val _requesterProfile = MutableStateFlow<User?>(null)
    val requesterProfile = _requesterProfile.asStateFlow()

    // サポーター（支援者）のプロフィール情報
    private val _supporterProfile = MutableStateFlow<User?>(null)
    val supporterProfile = _supporterProfile.asStateFlow()

    // Firestoreのリスナーを保持するための変数
    private var requestListener: ListenerRegistration? = null

    init {
        Log.d(TAG, "=== UserViewModel Init ===")
        Log.d(TAG, "Preserving auth state on app startup")

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

<<<<<<< HEAD
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

=======
>>>>>>> 53a509a (サポーターの情報表示)
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
                            isLoading = false
                        }
                        .onFailure { error ->
                            Log.e(TAG, "❌ Failed to create user document: ${error.message}")
                            errorMessage = "データベース保存エラー: ${error.message}"
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
<<<<<<< HEAD
=======
                    try {
                        val token = FirebaseMessaging.getInstance().token.await()
                        Log.d(TAG, "Registering device to server with token: $token")
                        cloudMessageRepository.callRegisterNewDevice(token)
                        cloudMessageRepository.saveDeviceId(token)
                        Log.d(TAG, "Device ID saved successfully: $token")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to save Device ID", e)
                    }
>>>>>>> 53a509a (サポーターの情報表示)
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
<<<<<<< HEAD
        userRepository.signOut()
        isSignedIn = false
        currentUser = null
        Log.d(TAG, "✅ SignOut completed")
=======
        viewModelScope.launch {
            try {
                cloudMessageRepository.deleteDevice()
                Log.d(TAG, "Device deleted from server")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete device", e)
            } finally {
                userRepository.signOut()
                try {
                    cloudMessageRepository.saveDeviceId(null)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to clear local device id", e)
                }
                isSignedIn = false
                currentUser = null
                _activeHelpRequest.value = null
                Log.d(TAG, "✅ SignOut completed")
            }
        }
>>>>>>> 53a509a (サポーターの情報表示)
    }

    private fun loadUserData(uid: String) {
        viewModelScope.launch {
            Log.d(TAG, "=== loadUserData called ===")
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
        val firebaseUser = userRepository.getCurrentUser()
        if (firebaseUser != null) {
            loadUserData(firebaseUser.uid)
        } else {
            errorMessage = "認証されたユーザーが見つかりません。再ログインしてください。"
        }
    }

    fun updateUser(user: User) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            val currentFirebaseUser = userRepository.getCurrentUser()
            if (currentFirebaseUser != null) {
                userRepository.updateUser(currentFirebaseUser.uid, user)
                    .onSuccess {
                        currentUser = user
                    }
                    .onFailure { error ->
                        errorMessage = error.message
                    }
            } else {
                errorMessage = "ユーザーが認証されていません"
            }
            isLoading = false
        }
    }

    // 省略されていたupdateNicknameなども含めますが、長くなるので既存のままでOKな部分は省略しません。
    // 以下は重要な修正を含む部分です。

    fun updateNickname(nickname: String) {
        currentUser?.let { user ->
            val updatedUser = user.copy(nickname = nickname)
            updateUser(updatedUser)
        } ?: run { errorMessage = "ユーザー情報が見つかりません。" }
    }

    fun updatePhysicalFeatures(physicalFeatures: String) {
        currentUser?.let { user ->
            val updatedUser = user.copy(physicalFeatures = physicalFeatures)
            updateUser(updatedUser)
        }
    }

    fun updateRole(role: String) {
        currentUser?.let { user ->
            val updatedUser = user.copy(role = role)
            updateUser(updatedUser)
        } ?: run { errorMessage = "ユーザー情報が見つかりません。" }
    }

    fun updateIconUrl(iconUrl: String) {
        currentUser?.let { user ->
            val updatedUser = user.copy(iconUrl = iconUrl)
            updateUser(updatedUser)
        } ?: run { errorMessage = "ユーザー情報が見つかりません。" }
    }

    fun clearError() {
        errorMessage = null
    }

    fun uploadProfileImage(imageUri: Uri, onComplete: (String) -> Unit) {
        if (isUploadingImage) {
            onComplete("")
            return
        }
        currentUser?.let { user ->
            val oldImageUrl = user.iconUrl
            val userId = userRepository.getCurrentUserId()
            if (userId == null) {
                errorMessage = "認証情報が見つかりません。"
                return
            }
            isLoading = true
            errorMessage = null
            viewModelScope.launch {
                try {
                    val result = userRepository.uploadProfileImage(imageUri, userId)
                    result.onSuccess { downloadUrl ->
                        if (!oldImageUrl.isNullOrEmpty() && oldImageUrl != downloadUrl) {
                            viewModelScope.launch {
                                try {
                                    userRepository.deleteOldProfileImage(oldImageUrl)
                                } catch (e: Exception) { Log.w(TAG, "Failed to delete old image") }
                            }
                        }
                        onComplete(downloadUrl)
                    }.onFailure { exception ->
                        errorMessage = "画像のアップロードに失敗: ${exception.message}"
                        onComplete("")
                    }
                } catch (e: Exception) {
                    errorMessage = "予期しないエラー: ${e.message}"
                    onComplete("")
                } finally {
                    isLoading = false
                }
            }
        } ?: run {
            errorMessage = "ユーザー情報が見つかりません。"
            onComplete("")
        }
    }

    fun updateUserIconUrl(iconUrl: String) {
        currentUser?.let { user ->
            val updatedUser = user.copy(iconUrl = iconUrl)
            updateUser(updatedUser)
        } ?: run { errorMessage = "ユーザー情報が見つかりません。" }
    }

    fun saveProfileChanges(nickname: String, physicalFeatures: String, imageUri: Uri?, onComplete: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val userId = userRepository.getCurrentUserId() ?: throw IllegalStateException("User not authenticated")
                val initialUser = currentUser ?: throw IllegalStateException("Current user data not found")

                val finalIconUrl = if (imageUri != null) {
                    val result = userRepository.uploadProfileImage(imageUri, userId)
                    val downloadUrl = result.getOrThrow()
                    val oldImageUrl = initialUser.iconUrl
                    if (!oldImageUrl.isNullOrEmpty() && oldImageUrl != downloadUrl) {
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
                    userRepository.updateUser(userId, updatedUser).getOrThrow()
                    currentUser = updatedUser
                }
            } catch (e: Exception) {
                errorMessage = "プロフィールの更新に失敗しました: ${e.message}"
            } finally {
                isLoading = false
                onComplete()
            }
        }
    }

    fun getCurrentFirebaseUser() = userRepository.getCurrentUser()

    // --- ここから下が重要修正部分です ---

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

    private fun listenForRequestUpdates(requestId: String) {
        requestListener?.remove()
        Log.d(TAG, "Starting to listen for updates on request: $requestId")

        requestListener = userRepository.listenForRequestUpdates(requestId) { updatedRequest ->
            _activeHelpRequest.value = updatedRequest

            if (updatedRequest != null) {
                Log.d(TAG, "Request updated. Status: ${updatedRequest.status}")
                // マッチング成立時にサポーター情報を取得
                if (!updatedRequest.matchedSupporterId.isNullOrBlank()) {
                    Log.d(TAG, "Matched supporter found: ${updatedRequest.matchedSupporterId}. Loading details...")
                    loadMatchedRequestDetails(requestId)
                }
            }
        }
    }

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

    fun clearViewedRequest() {
        _viewedHelpRequest.value = null
    }

    fun loadMatchedRequestDetails(requestId: String) {
        if (requestId.isBlank()) return
        viewModelScope.launch {
            isLoading = true
            userRepository.getRequest(requestId)
                .onSuccess { request ->
                    _matchedRequestDetails.value = request
                    if (request.requesterId.isNotBlank()) {
                        userRepository.getUser(request.requesterId)
                            .onSuccess { user -> _requesterProfile.value = user }
                            .onFailure { clearMatchedDetails() }
                    }
                    if (!request.matchedSupporterId.isNullOrBlank()) {
                        userRepository.getUser(request.matchedSupporterId)
                            .onSuccess { user -> _supporterProfile.value = user }
                            .onFailure { clearMatchedDetails() }
                    }
                }
                .onFailure {
                    errorMessage = "リクエスト詳細の取得に失敗しました。"
                    clearMatchedDetails()
                }
            isLoading = false
        }
    }

    fun clearMatchedDetails() {
        _matchedRequestDetails.value = null
        _requesterProfile.value = null
        _supporterProfile.value = null
    }

    override fun onCleared() {
        requestListener?.remove()
        super.onCleared()
    }

    fun startMonitoringRequest(requestId: String) {
        if (requestId.isBlank()) return
        // すでに同じIDを監視中なら何もしない（重複防止）
        if (_activeHelpRequest.value?.id == requestId && requestListener != null) {
            return
        }
        listenForRequestUpdates(requestId)
    }
}