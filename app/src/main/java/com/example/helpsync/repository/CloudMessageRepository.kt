package com.example.helpsync.repository

import android.util.Log
import com.example.helpsync.data.DeviceIdDataSource
import com.example.helpsync.data.HelpRequestIdDataSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

interface CloudMessageRepository {
    val bleRequestMessageFlow: SharedFlow<Map<String, String>>
    val helpRequestMessageFlow: SharedFlow<Map<String, String>>

    fun postCloudMessage(data: Map<String, String>)
    suspend fun getDeviceId() : String?
    suspend fun saveDeviceId(deviceID: String?)

    suspend fun getHelpRequestId() : String?
    suspend fun saveHelpRequestId(helpRequestId: String?)
    suspend fun callrenewDeviceToken(token: String)
    suspend fun callHandleProximityVerificationResultBackGround(scanResult: Boolean)
}

class CloudMessageRepositoryImpl (
    private val deviceIdDataSource: DeviceIdDataSource,
    private val helpRequestIdDataSource: HelpRequestIdDataSource
): CloudMessageRepository {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _bleRequestMessageFlow = MutableSharedFlow<Map<String, String>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val bleRequestMessageFlow: SharedFlow<Map<String, String>> = _bleRequestMessageFlow

    private val _helpRequestMessageFlow = MutableSharedFlow<Map<String, String>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val helpRequestMessageFlow: SharedFlow<Map<String,String>> = _helpRequestMessageFlow

    override fun postCloudMessage(data: Map<String, String>)
    {
        Log.d("CloudMessageRepo", "📬 postCloudMessage called, type: ${data["type"]}")
        when (data["type"]) {
            "help-request" -> {
                Log.d("CloudMessageRepo", "🆘 Processing help-request")
                val success = _helpRequestMessageFlow.tryEmit(data)
                if(!success) {
                    Log.w("CloudMessageRepo", "❌ Failed to emit helpRequestFCM data")
                } else {
                    Log.d("CloudMessageRepo", "✅ help-request emitted successfully")
                }
            }
            "proximity-verification" -> {
                Log.d("CloudMessageRepo", "🔍 Processing proximity-verification")
                val rawData = data["data"]
                Log.d("CloudMessageRepo", "📝 Raw proximity-verification data: $rawData")
                try {
                    val json = JSONObject(rawData)
                    val helpRequestId = json.getString("helpRequestId")
                    Log.d("CloudMessageRepo", "📋 HelpRequestId: $helpRequestId")
                    repositoryScope.launch {
                        try {
                            saveHelpRequestId(helpRequestId)
                            Log.d("CloudMessageRepo", "💾 HelpRequestId saved")
                            val success = _bleRequestMessageFlow.tryEmit(data)
                            if(!success) {
                                Log.w("CloudMessageRepo", "❌ Failed to emit proximity-verification data")
                            } else {
                                Log.d("CloudMessageRepo", "✅ proximity-verification emitted successfully")
                            }
                        } catch(e: Exception) {
                            Log.e("CloudMessageRepo", "❌ HelpRequestIdの保存に失敗しました: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                } catch(e: Exception) {
                    Log.e("CloudMessageRepo", "❌ proximity-verificationの解析に失敗しました: ${e.message}")
                    e.printStackTrace()
                }
            }
            else -> {
                Log.w("CloudMessageRepo", "⚠️ Unknown message type: ${data["type"]}")
            }
        }
    }

    override suspend fun callrenewDeviceToken(token: String) {
        val functions = Firebase.functions("asia-northeast2")
        val deviceId = deviceIdDataSource.getDeviceID()
        
        Log.d("CloudMessageRepo", "🔄 callrenewDeviceToken called")
        Log.d("CloudMessageRepo", "📱 DeviceId: $deviceId")
        Log.d("CloudMessageRepo", "🔑 Token (first 30 chars): ${token.take(30)}...")
        
        if (deviceId.isNullOrBlank()) {
            Log.e("CloudMessageRepo", "❌ Cannot renew token: deviceId is null or blank")
            return
        }
        
        val data = hashMapOf(
            "deviceId" to deviceId,
            "deviceToken" to token
        )

        try {
            val callResult = functions.getHttpsCallable("renewDeviceToken").call(data).await()
            Log.d("CloudMessageRepo", "✅ renewDeviceToken cloud function completed successfully")
            Log.d("CloudMessageRepo", "📊 Result: ${callResult.data}")
        } catch (e: Exception) {
            Log.e("CloudMessageRepo", "❌ renewDeviceToken failed: ${e.message}")
            e.printStackTrace()
        }
    }

    override suspend fun callHandleProximityVerificationResultBackGround(scanResult: Boolean) {
        val helpRequestId = try {
            getHelpRequestId()
        } catch (e:Exception) {
            Log.d("Error", "HelpRequestIdの取得に失敗しました")
            Log.d("Error", "Error Message:${e.message}")
        }
        try {
            val functions = Firebase.functions("asia-northeast2")
            val uid: String? = FirebaseAuth.getInstance().currentUser?.uid
            val data = hashMapOf(
                "verificationResult" to scanResult,
                "helpRequestId" to helpRequestId,
                "userId" to uid
            )

            Log.d("Supporter", "call HandleProximityVerificationResult in background")
            val callResult = functions.getHttpsCallable("handleProximityVerificationResult").call(data).await()
        } catch(e: Exception) {
            Log.d("Error", "handleProximityVerificationResultの実行に失敗しました")
            Log.d("Error", "Error message: ${e.message}")
        }
    }

    override suspend fun getDeviceId(): String? {
        return deviceIdDataSource.getDeviceID()
    }

    override suspend fun saveDeviceId(deviceId: String?) {
        deviceIdDataSource.saveDeviceId(deviceId)
    }

    override suspend fun getHelpRequestId(): String? {
        return helpRequestIdDataSource.getHelpRequestId()
    }

    override suspend fun saveHelpRequestId(helpRequestId: String?) {
        helpRequestIdDataSource.saveHelpRequestId(helpRequestId)
    }
}