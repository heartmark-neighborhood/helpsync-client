package com.example.helpsync.worker

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.helpsync.repository.CloudMessageRepository

class CallCloudFunctionWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val repository: CloudMessageRepository
) : CoroutineWorker(appContext, workerParams){
    override suspend fun doWork(): Result {
        val bleResult = inputData.getBoolean("SCAN_RESULT_DATA", false)

        return try {
            repository.callHandleProximityVerificationResultBackGround(bleResult)
            Result.success()
        } catch(e: Exception) {
            if(runAttemptCount < 3) Result.retry()
            else Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notificationId = 101
        val notification = NotificationCompat.Builder(applicationContext, "WORK_CHANNEL_ID")
            .setContentTitle("Bluetoothスキャン結果送信中")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        return ForegroundInfo(notificationId, notification)
    }
}