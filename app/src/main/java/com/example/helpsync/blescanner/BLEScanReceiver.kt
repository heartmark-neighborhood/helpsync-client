package com.example.helpsync.blescanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.helpsync.repository.CloudMessageRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BLEScanReceiver : BroadcastReceiver(), KoinComponent {

    companion object {
        private const val TAG = "BLEScanReceiver"
    }

    // Koinを使ってRepositoryを注入 (ViewModelに依存しないため)
    private val cloudMessageRepository: CloudMessageRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        // バックグラウンドでの実行時間を確保するための goAsync
        val pendingResult = goAsync()
        // ViewModelScopeではなく、独自のScopeで実行
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        val result = intent.getBooleanExtra("result", false)
        Log.d(TAG, "onReceive: result=$result")

        scope.launch {
            try {
                // 【修正点】ViewModelを介さず直接リポジトリとCloud Functionsを叩く
                val helpRequestId = cloudMessageRepository.getHelpRequestId()

                if (!helpRequestId.isNullOrEmpty()) {
                    Log.d(TAG, "Processing verification for helpRequestId: $helpRequestId")
                    val functions = Firebase.functions("asia-northeast2")
                    val uid: String? = FirebaseAuth.getInstance().currentUser?.uid

                    val data = hashMapOf(
                        "verificationResult" to result,
                        "helpRequestId" to helpRequestId,
                        "userId" to uid
                    )

                    // 完了を待機
                    functions.getHttpsCallable("handleProximityVerificationResult")
                        .call(data)
                        .await()

                    Log.d(TAG, "Successfully called handleProximityVerificationResult from background")
                } else {
                    Log.w(TAG, "HelpRequestId is null, skipping verification")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in background processing", e)
            } finally {
                // 処理完了をシステムに通知
                pendingResult.finish()
            }
        }
    }
}