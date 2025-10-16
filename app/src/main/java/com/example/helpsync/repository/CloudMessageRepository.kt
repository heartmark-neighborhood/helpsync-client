package com.example.helpsync.repository

import kotlinx.coroutines.flow.SharedFlow

interface CloudMessageRepository {
    val bleRequestMessageFlow: SharedFlow<Map<String, String>>
    val helpRequestMessageFlow: SharedFlow<Map<String, String>>
    val helpRequestSuccessedMessageFlow: SharedFlow<Map<String, String>>

    fun postCloudMessage(data: Map<String, String>)
}