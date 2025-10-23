package com.example.helpsync.repository

import android.net.Uri
import android.util.Log
import com.example.helpsync.data.HelpRequest
import com.example.helpsync.data.RequestStatus
import com.example.helpsync.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import java.util.Date
import java.util.UUID

class UserRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore("helpsync-db")
    private val usersCollection = db.collection("users")
    private val helpRequestsCollection = db.collection("helpRequests") // 追加
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
            Log.d(TAG, "About to call createUserWithEmailAndPassword")
            
            val result = withTimeout(30000) { // 30秒のタイムアウト
                auth.createUserWithEmailAndPassword(email, password).await()
            }
            
            Log.d(TAG, "createUserWithEmailAndPassword completed")
            result.user?.let { user ->
                Log.d(TAG, "Sign up successful for user: ${user.uid}")
                Result.success(user)
            } ?: run {
                Log.e(TAG, "Sign up failed: User is null")
                Result.failure(Exception("User creation failed"))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Sign up timeout: Request took longer than 30 seconds", e)
            Result.failure(Exception("リクエストがタイムアウトしました。ネットワーク接続を確認してください。"))
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

            val contentResolver = storageRef.storage.app.applicationContext.contentResolver

            Log.d(TAG, "=== File Analysis ===")
            try {
                val mimeType = contentResolver.getType(imageUri)
                Log.d(TAG, "📄 MIME type: $mimeType")

                val inputStream = contentResolver.openInputStream(imageUri)
                val fileSize = inputStream?.available() ?: 0
                inputStream?.close()
                Log.d(TAG, "📏 File size: $fileSize bytes")

                val previewStream = contentResolver.openInputStream(imageUri)
                val buffer = ByteArray(64)
                val bytesRead = previewStream?.read(buffer) ?: 0
                previewStream?.close()

                val hexString = buffer.take(bytesRead).joinToString(" ") {
                    String.format("%02X", it)
                }
                Log.d(TAG, "🔍 File header (first $bytesRead bytes): $hexString")

                val fileTypeFromContent = when {
                    buffer.size >= 2 && buffer[0] == 0xFF.toByte() && buffer[1] == 0xD8.toByte() -> "JPEG"
                    buffer.size >= 8 && buffer[1] == 'P'.toByte() && buffer[2] == 'N'.toByte() && buffer[3] == 'G'.toByte() -> "PNG"
                    buffer.size >= 12 && buffer[8] == 'W'.toByte() && buffer[9] == 'E'.toByte() && buffer[10] == 'B'.toByte() && buffer[11] == 'P'.toByte() -> "WEBP"
                    else -> "UNKNOWN"
                }
                Log.d(TAG, "🎯 Content-based file type: $fileTypeFromContent")

                val isLikelyText = buffer.take(bytesRead).all { byte ->
                    byte in 0x09..0x0D || byte in 0x20..0x7E || byte < 0 // ASCII範囲内またはUTF-8
                }
                Log.d(TAG, "📝 Appears to be text file: $isLikelyText")

                if (isLikelyText) {
                    val textContent = String(buffer, 0, bytesRead)
                    Log.w(TAG, "⚠️ WARNING: File appears to be text content: '$textContent'")
                }

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

            val mimeType = contentResolver.getType(imageUri)
            val fileExtension = when (mimeType) {
                "image/jpeg", "image/jpg" -> "jpg"
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg" // デフォルト
            }

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

            Log.d(TAG, "Starting upload task with InputStream (ID: $uploadId)...")

            val inputStream = contentResolver.openInputStream(imageUri)
                ?: return Result.failure(Exception("ファイルストリームを開けません"))

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

    suspend fun deleteOldProfileImage(oldImageUrl: String): Result<Unit> {
        return suspendCoroutine { continuation ->
            try {
                Log.d(TAG, "=== Deleting old profile image ===")
                Log.d(TAG, "Old image URL: $oldImageUrl")

                if (oldImageUrl.isEmpty() || !oldImageUrl.contains("firebase")) {
                    Log.d(TAG, "No valid Firebase Storage URL to delete")
                    continuation.resume(Result.success(Unit))
                    return@suspendCoroutine
                }

                val oldImageRef = Firebase.storage.getReferenceFromUrl(oldImageUrl)
                Log.d(TAG, "Old image reference path: ${oldImageRef.path}")

                oldImageRef.delete()
                    .addOnSuccessListener {
                        Log.d(TAG, "✅ Old profile image deleted successfully")
                        continuation.resume(Result.success(Unit))
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "❌ Failed to delete old profile image: ${exception.message}", exception)
                        Log.d(TAG, "Treating deletion failure as non-critical error")
                        continuation.resume(Result.success(Unit))
                    }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error setting up image deletion: ${e.message}", e)
                continuation.resume(Result.success(Unit))
            }
        }
    }

    // ▼▼▼ ここから下の3つの関数を新規追加 ▼▼▼

    /**
     * 新しいヘルプリクエストを作成し、Firestoreに保存する
     */
    suspend fun createHelpRequest(requesterId: String, requesterNickname: String): Result<HelpRequest> {
        return try {
            val newUuid = UUID.randomUUID().toString()
            val newRequestDoc = helpRequestsCollection.document() // 自動でIDを生成

            val request = HelpRequest(
                id = newRequestDoc.id,
                requesterId = requesterId,
                requesterNickname = requesterNickname,
                proximityUuid = newUuid,
                status = RequestStatus.PENDING
            )

            newRequestDoc.set(request).await()
            Log.d(TAG, "✅ HelpRequest created with ID: ${newRequestDoc.id}")
            Result.success(request)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create help request", e)
            Result.failure(e)
        }
    }

    /**
     * スキャンしたサポーターが、近接確認結果をサーバーに送信する
     */
    suspend fun handleProximityVerificationResult(requestId: String, supporterId: String): Result<Unit> {
        return try {
            val requestRef = helpRequestsCollection.document(requestId)
            // トランザクションを使い、他のサポーターと同時に更新しないようにする
            db.runTransaction { transaction ->
                val snapshot = transaction.get(requestRef)
                val currentStatus = snapshot.toObject(HelpRequest::class.java)?.status

                // まだ誰もマッチングしていない場合のみ、情報を更新
                if (currentStatus == RequestStatus.PENDING) {
                    transaction.update(
                        requestRef,
                        "status", RequestStatus.MATCHED,
                        "matchedSupporterId", supporterId
                    )
                }
            }.await()
            Log.d(TAG, "✅ HelpRequest $requestId matched with supporter $supporterId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to handle proximity result for $requestId", e)
            Result.failure(e)
        }
    }

    /**
     * ヘルプリクエストの状態変更をリアルタイムで監視する
     * @return ListenerRegistrationを返すようにして、監視を解除できるようにする
     */
    fun listenForRequestUpdates(requestId: String, onUpdate: (HelpRequest?) -> Unit): ListenerRegistration {
        return helpRequestsCollection.document(requestId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen failed.", error)
                    onUpdate(null)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val request = snapshot.toObject(HelpRequest::class.java)
                    onUpdate(request)
                } else {
                    Log.d(TAG, "Current data: null or document deleted")
                    onUpdate(null)
                }
            }
    }
    suspend fun getRequest(requestId: String): Result<HelpRequest> {
        return try {
            val document = helpRequestsCollection.document(requestId).get().await()
            val request = document.toObject(HelpRequest::class.java)
            if (request != null) {
                Result.success(request)
            } else {
                Result.failure(Exception("Request not found or failed to parse."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPendingHelpRequests(): Result<List<HelpRequest>> {
        return try {
            val querySnapshot = helpRequestsCollection
                .whereEqualTo("status", RequestStatus.PENDING)
                .get()
                .await()
            val requests = querySnapshot.documents.mapNotNull { document ->
                document.toObject(HelpRequest::class.java)
            }
            Log.d(TAG, "✅ Found ${requests.size} pending help requests")
            Result.success(requests)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get pending help requests", e)
            Result.failure(e)
        }
    }
}