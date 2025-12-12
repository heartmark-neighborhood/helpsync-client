package com.example.helpsync.helprequest_cloudmessage

import android.util.Log
import com.example.helpsync.repository.CloudMessageRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val serviceJob = SupervisorJob()
private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

class HelpRequestMessageReceiver : FirebaseMessagingService(), KoinComponent{
    private val repository: CloudMessageRepository by inject()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if(remoteMessage.data.isNotEmpty()) {
            val receivedData = remoteMessage.data

            Log.d("FCM_Service", "received data: $receivedData")

            repository.postCloudMessage(receivedData)
        }
    }

    override fun onNewToken(token: String) {
        serviceScope.launch {
            try {
                repository.callRenewDeviceToken(token = token)
            } catch (e: Exception) {
                Log.e("debug", "Failed to send FCM token")
            }
        }
    }
}