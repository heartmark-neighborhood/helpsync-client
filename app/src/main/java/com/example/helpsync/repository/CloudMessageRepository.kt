package com.example.helpsync.repository

import android.util.Log
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

interface CloudMessageRepository {
    val bleRequestMessageFlow: SharedFlow<Map<String, String>>
    val helpRequestMessageFlow: SharedFlow<Map<String, String>>

    fun postCloudMessage(data: Map<String, String>)
}

class CloutMessageRepositoryImpl : CloudMessageRepository {

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
}