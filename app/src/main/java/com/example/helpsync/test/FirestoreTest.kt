package com.example.helpsync.test

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreTest {
    companion object {
        private const val TAG = "FirestoreTest"
        
        suspend fun testBasicWrite(uid: String): Boolean {
            return try {
                Log.d(TAG, "=== Testing basic Firestore write ===")
                
                val firestore = FirebaseFirestore.getInstance()
                val testData = hashMapOf(
                    "uid" to uid,
                    "test" to "hello world",
                    "timestamp" to System.currentTimeMillis()
                )
                
                Log.d(TAG, "Writing test data for UID: $uid")
                
                // 最もシンプルな書き込み
                firestore.collection("test")
                    .document(uid)
                    .set(testData)
                    .await()
                
                Log.d(TAG, "✅ Basic write successful!")
                true
            } catch (e: Exception) {
                Log.e(TAG, "❌ Basic write failed: ${e.message}", e)
                false
            }
        }
    }
}
