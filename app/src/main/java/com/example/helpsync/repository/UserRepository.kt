package com.example.helpsync.repository

import android.net.Uri
import android.util.Log
import com.example.helpsync.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import com.google.firebase.Timestamp
import java.util.Date
import java.util.UUID

class UserRepository {
    private val auth = FirebaseAuth.getInstance()
    // asia-northeast2のデータベースを明示的に指定
    private val db = Firebase.firestore("helpsync-db")
    private val usersCollection = db.collection("users")
    
    // Storageをasia-northeast2リージョンで明示的に指定
    // フルバケットURLを使用してリージョンを明確に指定
    private val storage = Firebase.storage("gs://heartmark-neighborhood.firebasestorage.app")
    private val storageRef = storage.reference

    companion object {
        private const val TAG = "UserRepository"
        private const val STORAGE_REGION = "asia-northeast2"
        private const val STORAGE_BUCKET = "heartmark-neighborhood.firebasestorage.app"
    }

    init {
        Log.d(TAG, "=== UserRepository initialized ===")
        Log.d(TAG, "Target region: $STORAGE_REGION")
        Log.d(TAG, "Storage bucket: $STORAGE_BUCKET")
        Log.d(TAG, "Firestore database: ${db.app.name}")
        Log.d(TAG, "Storage bucket URL: ${storage.reference.bucket}")
        Log.d(TAG, "Storage app name: ${storage.app.name}")
        Log.d(TAG, "Full storage URL: gs://$STORAGE_BUCKET")
        
        // Storage設定の詳細確認
        try {
            Log.d(TAG, "Storage reference path: ${storageRef.path}")
            Log.d(TAG, "Storage reference bucket: ${storageRef.bucket}")
            Log.d(TAG, "Storage app options: ${storage.app.options}")
            Log.d(TAG, "✅ Storage initialized successfully with explicit region")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Storage initialization error: ${e.message}", e)
        }
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
            Log.d(TAG, "🔄 Updating user: $uid with nickname: '${user.nickname}'")
            
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
            
            Log.d(TAG, "💾 Sending data to Firestore: $userData")
            
            usersCollection.document(uid)
                .set(userData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "✅ User updated successfully in Firestore")
                    continuation.resume(Result.success(Unit))
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Failed to update user in Firestore: ${e.message}")
                    Log.e(TAG, "❌ Exception details: ${e}")
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

    fun getCurrentUserId(): String? {
        val currentUser = auth.currentUser
        Log.d(TAG, "=== getCurrentUserId called ===")
        Log.d(TAG, "Auth current user: $currentUser")
        Log.d(TAG, "User ID: ${currentUser?.uid}")
        Log.d(TAG, "User email: ${currentUser?.email}")
        Log.d(TAG, "Is anonymous: ${currentUser?.isAnonymous}")
        return currentUser?.uid
    }

    suspend fun uploadProfileImage(imageUri: Uri, userId: String): Result<String> {
        return try {
            val uploadId = UUID.randomUUID().toString().take(8)
            Log.d(TAG, "=== Starting image upload (ID: $uploadId) ===")
            Log.d(TAG, "User ID: $userId")
            Log.d(TAG, "Image URI: $imageUri")
            Log.d(TAG, "Image URI scheme: ${imageUri.scheme}")
            Log.d(TAG, "Image URI path: ${imageUri.path}")
            Log.d(TAG, "Image URI toString: ${imageUri.toString()}")
            Log.d(TAG, "Storage bucket: ${storage.app.options.storageBucket}")
            Log.d(TAG, "Upload ID: $uploadId")
            
            // ContentResolverを取得
            val contentResolver = storageRef.storage.app.applicationContext.contentResolver
            
            // まず、ファイルが存在するかチェック
            Log.d(TAG, "=== File Analysis ===")
            try {
                // MIMEタイプを取得
                val mimeType = contentResolver.getType(imageUri)
                Log.d(TAG, "📄 MIME type: $mimeType")
                
                // ファイルサイズを取得
                val inputStream = contentResolver.openInputStream(imageUri)
                val fileSize = inputStream?.available() ?: 0
                inputStream?.close()
                Log.d(TAG, "📏 File size: $fileSize bytes")
                
                // ファイルの実際の内容を最初の64バイト読んで詳細分析
                val previewStream = contentResolver.openInputStream(imageUri)
                val buffer = ByteArray(64)
                val bytesRead = previewStream?.read(buffer) ?: 0
                previewStream?.close()
                
                val hexString = buffer.take(bytesRead).joinToString(" ") { 
                    String.format("%02X", it) 
                }
                Log.d(TAG, "🔍 File header (first $bytesRead bytes): $hexString")
                
                // ファイルタイプを内容から推測
                val fileTypeFromContent = when {
                    buffer.size >= 2 && buffer[0] == 0xFF.toByte() && buffer[1] == 0xD8.toByte() -> "JPEG"
                    buffer.size >= 8 && buffer[1] == 'P'.toByte() && buffer[2] == 'N'.toByte() && buffer[3] == 'G'.toByte() -> "PNG"
                    buffer.size >= 12 && buffer[8] == 'W'.toByte() && buffer[9] == 'E'.toByte() && buffer[10] == 'B'.toByte() && buffer[11] == 'P'.toByte() -> "WEBP"
                    else -> "UNKNOWN"
                }
                Log.d(TAG, "🎯 Content-based file type: $fileTypeFromContent")
                
                // テキストファイルの可能性をチェック
                val isLikelyText = buffer.take(bytesRead).all { byte ->
                    byte in 0x09..0x0D || byte in 0x20..0x7E || byte < 0 // ASCII範囲内またはUTF-8
                }
                Log.d(TAG, "📝 Appears to be text file: $isLikelyText")
                
                if (isLikelyText) {
                    val textContent = String(buffer, 0, bytesRead)
                    Log.w(TAG, "⚠️ WARNING: File appears to be text content: '$textContent'")
                }
                
                // 実際の画像ファイルかどうかの警告
                if (fileTypeFromContent == "UNKNOWN" && isLikelyText) {
                    Log.e(TAG, "❌ ERROR: Selected file is not an image! It appears to be a text file.")
                    return Result.failure(Exception("選択されたファイルは画像ではありません。テキストファイルが選択されています。"))
                }
                
                if (fileTypeFromContent == "UNKNOWN" && mimeType?.startsWith("image/") != true) {
                    Log.e(TAG, "❌ ERROR: Selected file does not appear to be an image. MIME: $mimeType, Content: $fileTypeFromContent")
                    return Result.failure(Exception("選択されたファイルは有効な画像ファイルではありません。"))
                }
                
                Log.d(TAG, "✅ File validation passed - proceeding with upload")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ File access error: ${e.message}")
                return Result.failure(Exception("ファイルにアクセスできません: ${e.message}"))
            }
            
            // MIMEタイプに基づいて適切なファイル拡張子を決定
            val mimeType = contentResolver.getType(imageUri)
            val fileExtension = when (mimeType) {
                "image/jpeg", "image/jpg" -> "jpg"
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg" // デフォルト
            }
            
            // ユニークなファイル名を生成
            val fileName = "profile_${userId}_${UUID.randomUUID()}.$fileExtension"
            val imageRef = storageRef.child("profile_images/$fileName")
            
            Log.d(TAG, "📤 Upload details (ID: $uploadId):")
            Log.d(TAG, "   Path: profile_images/$fileName")
            Log.d(TAG, "   Extension: $fileExtension")
            Log.d(TAG, "   MIME type: $mimeType")
            Log.d(TAG, "   Storage reference: ${imageRef.path}")
            Log.d(TAG, "   Storage bucket: ${imageRef.bucket}")
            Log.d(TAG, "   Reference name: ${imageRef.name}")
            Log.d(TAG, "   Upload ID: $uploadId")
            
            // 画像をアップロード（InputStream + Metadata方式）
            Log.d(TAG, "Starting upload task with InputStream (ID: $uploadId)...")
            
            // InputStreamを開く
            val inputStream = contentResolver.openInputStream(imageUri)
                ?: return Result.failure(Exception("ファイルストリームを開けません"))
            
            // メタデータを作成してMIMEタイプを明示的に指定
            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType(mimeType ?: "image/jpeg")
                .build()
                
            Log.d(TAG, "Upload metadata content type: ${metadata.contentType}")
            
            try {
                val uploadTask = imageRef.putStream(inputStream, metadata).await()
                Log.d(TAG, "✅ Upload completed successfully with InputStream (ID: $uploadId)")
                Log.d(TAG, "Upload metadata: ${uploadTask.metadata}")
                Log.d(TAG, "Upload content type: ${uploadTask.metadata?.contentType}")
                Log.d(TAG, "Upload state: ${uploadTask.task.isSuccessful}")
                Log.d(TAG, "Final file name: ${imageRef.name}")
            } finally {
                inputStream.close()
            }
            
            // ダウンロードURLを取得
            Log.d(TAG, "Getting download URL (ID: $uploadId)...")
            val downloadUrl = imageRef.downloadUrl.await()
            val urlString = downloadUrl.toString()
            
            Log.d(TAG, "✅ Image uploaded successfully (ID: $uploadId)")
            Log.d(TAG, "Download URL: $urlString")
            Log.d(TAG, "=== Upload complete (ID: $uploadId) ===")
            
            Result.success(urlString)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Image upload failed: ${e.message}", e)
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Exception cause: ${e.cause}")
            Log.e(TAG, "Stack trace: ${e.stackTrace.take(5).joinToString("\n")}")
            Result.failure(e)
        }
    }
    
    // 古いプロフィール画像を削除する機能
    suspend fun deleteOldProfileImage(oldImageUrl: String): Result<Unit> {
        return suspendCoroutine { continuation ->
            try {
                Log.d(TAG, "=== Deleting old profile image ===")
                Log.d(TAG, "Old image URL: $oldImageUrl")
                
                // Firebase Storage URLからファイルパスを抽出
                if (oldImageUrl.isEmpty() || !oldImageUrl.contains("firebase")) {
                    Log.d(TAG, "No valid Firebase Storage URL to delete")
                    continuation.resume(Result.success(Unit))
                    return@suspendCoroutine
                }
                
                // URLからStorageReferenceを作成
                val oldImageRef = Firebase.storage.getReferenceFromUrl(oldImageUrl)
                Log.d(TAG, "Old image reference path: ${oldImageRef.path}")
                
                // ファイルを削除
                oldImageRef.delete()
                    .addOnSuccessListener {
                        Log.d(TAG, "✅ Old profile image deleted successfully")
                        continuation.resume(Result.success(Unit))
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "❌ Failed to delete old profile image: ${exception.message}", exception)
                        // 削除に失敗してもアプリの動作に影響しないよう、成功として扱う
                        Log.d(TAG, "Treating deletion failure as non-critical error")
                        continuation.resume(Result.success(Unit))
                    }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error setting up image deletion: ${e.message}", e)
                // 削除のセットアップに失敗してもアプリの動作に影響しないよう、成功として扱う
                continuation.resume(Result.success(Unit))
            }
        }
    }
}
