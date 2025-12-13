package com.example.helpsync.helprequest_cloudmessage

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.helpsync.MainActivity
import com.example.helpsync.repository.CloudMessageRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val serviceJob = SupervisorJob()
private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

class HelpRequestMessageReceiver : FirebaseMessagingService(), KoinComponent{
    private val repository: CloudMessageRepository by inject()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if(remoteMessage.data.isNotEmpty()) {
            val receivedData = remoteMessage.data

            Log.d("FCM_Service", "received data: $receivedData")

            repository.postCloudMessage(receivedData)

            if(receivedData["type"] == "help-request")
            {
                val rawData = receivedData["data"]
                val data = JSONObject(rawData)
                //ヘルプマーク所持者側の場合通知を送る必要は無い
                if(data.has("requester")) sendNotification(data)
            }
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendNotification(data: JSONObject) {
        val channelId = "help_request_notifictaion_channel"
        val notificationId = 23

        val intent = Intent(this, MainActivity::class.java).apply {
            action = "ACTION_SHOW_ACCEPTANCE_SCREEN"
            putExtra("HELPMARKHOLDER_INFORMATION", data.toString())
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ヘルプ要請を受信しました")
            .setContentText("近くで助けを求めている人が居ます。")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as  NotificationManager

        val channel = NotificationChannel(
            channelId,
            "ヘルプ要請受信通知",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)
        notificationManager.notify(notificationId, notificationBuilder.build())
        Log.d("FCMService", "通知を作成しました。")
    }
}