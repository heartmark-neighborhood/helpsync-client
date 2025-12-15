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
    suspend fun callRenewDeviceToken(token: String)
    suspend fun callHandleProximityVerificationResultBackGround(scanResult: Boolean)
    suspend fun deleteDevice()
    suspend fun callRegisterNewDevice(token: String)
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
        when (data["type"]) {
            "help-request" -> {
                val success = _helpRequestMessageFlow.tryEmit(data)
                if(!success) {
                    Log.w("helpRequestFCM", "Failed to emit helpRequestFCM data")
                }
            }
            "proximity-verification" -> {
                val rawData = data["data"]
                val json = JSONObject(rawData)
                val helpRequestId = json.getString("helpRequestId")
                repositoryScope.launch {
                    try {
                        saveHelpRequestId(helpRequestId)
                        val success = _bleRequestMessageFlow.tryEmit(data)
                        if(!success) {
                            Log.w("proximity-verificationFCM", "Failed to emit proximity-verificationFCM data")
                        }
                    } catch(e: Exception) {
                        Log.d("Debug", "HelpRequestIdの保存に失敗しました")
                        Log.d("Debug", "${e.message}")
                    }
                }

            }

        }
    }

    override suspend fun callRenewDeviceToken(token: String) {
        val functions = Firebase.functions("asis-northeast2")
        val deviceId = deviceIdDataSource.getDeviceID()
        val data = hashMapOf(
            "deviceId" to deviceId,
            "deviceToken" to token
        )

        val callResult = functions.getHttpsCallable("RenewDeviceToken").call(data).await()
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
    override suspend fun deleteDevice() {
        try {
            val functions = Firebase.functions("asia-northeast2") // または Firebase.functions()
            // ローカルのIDを消去
            deviceIdDataSource.saveDeviceId(null)
            Log.d("CloudMessageRepo", "Device ID deleted locally")
        } catch (e: Exception) {
            Log.e("CloudMessageRepo", "Failed to delete device: ${e.message}")
        }
    }

    override suspend fun getHelpRequestId(): String? {
        return helpRequestIdDataSource.getHelpRequestId()
    }

    override suspend fun saveHelpRequestId(helpRequestId: String?) {
        helpRequestIdDataSource.saveHelpRequestId(helpRequestId)
    }

    override suspend fun callRegisterNewDevice(token: String) {
        try {
            val functions = Firebase.functions("asia-northeast2")
            // サーバー側が期待するパラメータ名に合わせて送信
            // (通常は deviceToken や id などを送ります)
            val data = hashMapOf(
                "deviceToken" to token
            )
            functions.getHttpsCallable("registerNewDevice").call(data).await()
            Log.d("CloudMessageRepo", "Device registered to server successfully")
        } catch (e: Exception) {
            Log.e("CloudMessageRepo", "Failed to register device: ${e.message}")
            // 登録済みエラーなどの場合は無視して良い場合もあるが、一旦ログに出す
        }
    }
}