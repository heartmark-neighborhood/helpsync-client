package com.example.helpsync.repository

import android.util.Log
import com.example.helpsync.data.User
import com.example.helpsync.test.FirestoreTest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class UserRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

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
        val user = auth.currentUser
        Log.d(TAG, "=== getCurrentUser ===")
        Log.d(TAG, "Current user: ${user?.uid}")
        Log.d(TAG, "User email: ${user?.email}")
        Log.d(TAG, "Is email verified: ${user?.isEmailVerified}")
        Log.d(TAG, "User anonymous: ${user?.isAnonymous}")
        return user
    }

    suspend fun createUser(user: User): Result<String> {
        return try {
            Log.d(TAG, "=== Starting createUser process ===")
            Log.d(TAG, "User UID: ${user.uid}")
            Log.d(TAG, "User email: ${user.email}")
            
            // Step 1: 基本的なFirestoreテストを実行
            Log.d(TAG, "Step 1: Testing basic Firestore connectivity...")
            val testResult = FirestoreTest.testBasicWrite(user.uid)
            if (!testResult) {
                Log.e(TAG, "❌ Basic Firestore test failed!")
                return Result.failure(Exception("Firestore connectivity test failed"))
            }
            
            // Step 2: Users コレクションにシンプルなデータを書き込み
            Log.d(TAG, "Step 2: Writing to users collection...")
            val simpleUserData = hashMapOf(
                "uid" to user.uid,
                "email" to user.email,
                "created" to System.currentTimeMillis()
            )
            
            val docRef = usersCollection.document(user.uid)
            docRef.set(simpleUserData).await()
            Log.d(TAG, "✅ Simple user data written successfully!")
            
            // Step 3: 完全なユーザーデータを書き込み
            Log.d(TAG, "Step 3: Writing complete user data...")
            val completeUserData = hashMapOf(
                "uid" to user.uid,
                "email" to user.email,
                "roles" to user.roles,
                "nickname" to user.nickname,
                "iconUrl" to user.iconUrl,
                "physicalFeatures" to user.physicalFeatures,
                "createdAt" to user.createdAt,
                "updatedAt" to user.updatedAt
            )
            
            docRef.set(completeUserData).await()
            Log.d(TAG, "✅ Complete user data written successfully!")
            
            Result.success(user.uid)
        } catch (e: Exception) {
            Log.e(TAG, "❌ createUser failed: ${e.message}", e)
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Exception cause: ${e.cause}")
            Result.failure(e)
        }
    }

    suspend fun getUser(uid: String): Result<User> {
        return try {
            val document = usersCollection.document(uid).get().await()
            if (document.exists()) {
                val user = document.toObject(User::class.java)
                user?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("User data conversion failed"))
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUser(user: User): Result<Unit> {
        return try {
            val updatedUser = user.copy(updatedAt = System.currentTimeMillis())
            usersCollection.document(user.uid).set(updatedUser).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUser(uid: String): Result<Unit> {
        return try {
            usersCollection.document(uid).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUsersByRole(role: String): Result<List<User>> {
        return try {
            val query = usersCollection.whereArrayContains("roles", role).get().await()
            val users = query.documents.mapNotNull { document ->
                document.toObject(User::class.java)
            }
            Result.success(users)
        } catch (e: Exception) {
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
