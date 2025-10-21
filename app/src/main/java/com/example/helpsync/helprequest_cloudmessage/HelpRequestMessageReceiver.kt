package com.example.helpsync.helprequest_cloudmessage

import android.util.Log
import com.example.helpsync.repository.CloudMessageRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class HelpRequestMessageReceiver : FirebaseMessagingService(), KoinComponent{
    private val repository: CloudMessageRepository by inject()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if(remoteMessage.data.isNotEmpty()) {
            val receivedData = remoteMessage.data

            Log.d("FCM_Service", "received data: $receivedData")

            repository.postCloudMessage(receivedData)
        }
    }
}