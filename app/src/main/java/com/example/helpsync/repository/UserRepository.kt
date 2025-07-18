package com.example.helpsync.repository

import android.util.Log
import com.example.helpsync.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import com.google.firebase.Timestamp
import java.util.Date

class UserRepository {
    private val auth = FirebaseAuth.getInstance()
    // asia-northeast2のデータベースを明示的に指定
    private val db = Firebase.firestore("helpsync-db")
    private val usersCollection = db.collection("users")

    companion object {
        private const val TAG = "UserRepository"
    }

    suspend fun signUp(email: String, password: String): Result<FirebaseUser> {
        return try {
            Log.d(TAG, "Starting sign up for email: $email")
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { user ->
                Log.d(TAG, "Sign up successful for user: ${user.uid}")
                Result.success(user)
            } ?: run {
                Log.e(TAG, "Sign up failed: User is null")
                Result.failure(Exception("User creation failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign up error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let { user ->
                Result.success(user)
            } ?: Result.failure(Exception("Sign in failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    suspend fun createUser(uid: String, user: User): Result<String> {
        return suspendCoroutine { continuation ->
            Log.d(TAG, "Creating user: $uid")
            
            val userData = hashMapOf(
                "email" to user.email,
                "role" to user.role,
                "nickname" to user.nickname,
                "iconUrl" to user.iconUrl,
                "physicalFeatures" to user.physicalFeatures,
                "createdAt" to Timestamp(user.createdAt),
                "updatedAt" to Timestamp(user.updatedAt)
            )
            
            usersCollection.document(uid)
                .set(userData)
                .addOnSuccessListener {
                    Log.d(TAG, "✅ User created successfully: $uid")
                    continuation.resume(Result.success(uid))
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Failed to create user: ${e.message}")
                    continuation.resume(Result.failure(e))
                }
        }
    }

    suspend fun getUser(uid: String): Result<User> {
        return try {
            Log.d(TAG, "Getting user: $uid")
            
            val document = usersCollection.document(uid).get().await()
            
            if (document.exists()) {
                val user = document.toObject(User::class.java)
                if (user != null) {
                    Log.d(TAG, "✅ User loaded successfully")
                    Result.success(user)
                } else {
                    Log.e(TAG, "❌ Failed to convert document to User")
                    Result.failure(Exception("User data conversion failed"))
                }
            } else {
                Log.e(TAG, "❌ User not found")
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get user: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateUser(uid: String, user: User): Result<Unit> {
        return suspendCoroutine { continuation ->
            Log.d(TAG, "Updating user: $uid")
            
            val updatedUser = user.copy(updatedAt = Date())
            
            val userData = hashMapOf(
                "email" to updatedUser.email,
                "role" to updatedUser.role,
                "nickname" to updatedUser.nickname,
                "iconUrl" to updatedUser.iconUrl,
                "physicalFeatures" to updatedUser.physicalFeatures,
                "createdAt" to Timestamp(updatedUser.createdAt),
                "updatedAt" to Timestamp(updatedUser.updatedAt)
            )
            
            usersCollection.document(uid)
                .update(userData as Map<String, Any>)
                .addOnSuccessListener {
                    Log.d(TAG, "✅ User updated successfully")
                    continuation.resume(Result.success(Unit))
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Failed to update user: ${e.message}")
                    continuation.resume(Result.failure(e))
                }
        }
    }

    suspend fun deleteUser(uid: String): Result<Unit> {
        return try {
            Log.d(TAG, "Deleting user: $uid")
            usersCollection.document(uid).delete().await()
            Log.d(TAG, "✅ User deleted successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to delete user: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getUsersByRole(role: String): Result<List<User>> {
        return try {
            Log.d(TAG, "Getting users with role: $role")
            
            val querySnapshot = usersCollection
                .whereArrayContains("role", role)
                .get()
                .await()
            
            val users = querySnapshot.documents.mapNotNull { document ->
                document.toObject(User::class.java)
            }
            
            Log.d(TAG, "✅ Found ${users.size} users with role: $role")
            Result.success(users)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get users by role: ${e.message}")
            Result.failure(e)
        }
    }

    fun addAuthStateListener(listener: (FirebaseUser?) -> Unit) {
        auth.addAuthStateListener { firebaseAuth ->
            listener(firebaseAuth.currentUser)
        }
    }

    fun removeAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth.removeAuthStateListener(listener)
    }
}
