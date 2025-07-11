package com.example.helpsync.viewmodel

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

class UserViewModel : ViewModel() {
    private val userRepository = UserRepository()
    
    companion object {
        private const val TAG = "UserViewModel"
    }
    
    var currentUser by mutableStateOf<User?>(null)
        private set
    
    var isLoading by mutableStateOf(false)
        private set
    
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    var isSignedIn by mutableStateOf(false)
        private set

    init {
        Log.d(TAG, "=== UserViewModel Init ===")
        
        // 毎回ログインを求めるため、アプリ起動時に自動サインアウト
        userRepository.signOut()
        Log.d(TAG, "Auto sign out on app startup")
        
        // 認証状態を初期化
        isSignedIn = false
        currentUser = null
        
        // 認証状態の変更を監視
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
    }

    fun signUp(email: String, password: String, nickname: String = "", roles: List<String> = emptyList(), physicalFeatures: String = "") {
        viewModelScope.launch {
            Log.d(TAG, "Starting signUp process")
            isLoading = true
            errorMessage = null
            
            userRepository.signUp(email, password)
                .onSuccess { firebaseUser ->
                    Log.d(TAG, "Authentication successful, creating user document")
                    val user = User(
                        uid = firebaseUser.uid,
                        email = email,
                        roles = roles,
                        nickname = nickname,
                        physicalFeatures = physicalFeatures
                    )
                    
                    userRepository.createUser(user)
                        .onSuccess {
                            Log.d(TAG, "✅ User document created successfully")
                            currentUser = user
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
                    errorMessage = error.message
                }
            
            isLoading = false
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            userRepository.signIn(email, password)
                .onSuccess { firebaseUser ->
                    loadUserData(firebaseUser.uid)
                }
                .onFailure { error ->
                    errorMessage = error.message
                }
            
            isLoading = false
        }
    }

    fun signOut() {
        userRepository.signOut()
        currentUser = null
    }

    private fun loadUserData(uid: String) {
        viewModelScope.launch {
            isLoading = true
            
            userRepository.getUser(uid)
                .onSuccess { user ->
                    currentUser = user
                }
                .onFailure { error ->
                    errorMessage = error.message
                }
            
            isLoading = false
        }
    }

    fun updateUser(user: User) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            userRepository.updateUser(user)
                .onSuccess {
                    currentUser = user
                }
                .onFailure { error ->
                    errorMessage = error.message
                }
            
            isLoading = false
        }
    }

    fun updateNickname(nickname: String) {
        currentUser?.let { user ->
            val updatedUser = user.copy(nickname = nickname)
            updateUser(updatedUser)
        }
    }

    fun updatePhysicalFeatures(physicalFeatures: String) {
        currentUser?.let { user ->
            val updatedUser = user.copy(physicalFeatures = physicalFeatures)
            updateUser(updatedUser)
        }
    }

    fun updateRoles(roles: List<String>) {
        currentUser?.let { user ->
            val updatedUser = user.copy(roles = roles)
            updateUser(updatedUser)
        }
    }

    fun clearError() {
        errorMessage = null
    }
}
