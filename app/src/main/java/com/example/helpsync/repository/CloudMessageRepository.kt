package com.example.helpsync.repository

import android.util.Log
import com.example.helpsync.data.DeviceIdDataSource
import com.example.helpsync.viewmodel.Evaluation
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.tasks.await

interface CloudMessageRepository {
    val bleRequestMessageFlow: SharedFlow<Map<String, String>>
    val helpRequestMessageFlow: SharedFlow<Map<String, String>>

    fun postCloudMessage(data: Map<String, String>)
    suspend fun getDeviceid() : String?
    suspend fun callRenewDeviceToken(token: String)
}

class CloudMessageRepositoryImpl (
    private val deviceIdDataSource: DeviceIdDataSource
): CloudMessageRepository {

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
                val success = _bleRequestMessageFlow.tryEmit(data)
                if(!success) {
                    Log.w("proximity-verificationFCM", "Failed to emit proximity-verificationFCM data")
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

    override suspend fun getDeviceid(): String? {
        return deviceIdDataSource.getDeviceID()
    }
}