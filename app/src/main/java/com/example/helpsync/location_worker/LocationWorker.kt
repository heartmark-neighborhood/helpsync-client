package com.example.helpsync.location_worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.helpsync.repository.CloudMessageRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class LocationWorker(
    private val context: Context,
    workerParams: WorkerParameters,
    private val repository: CloudMessageRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "periodicLocationUpload"
    }

    override suspend fun doWork(): Result {
        Log.d("LocationWorker", "doWork: タスクを開始します。")

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        val finePermissionGranted = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        Log.d("LocationWorker", "permission check: ACCESS_FINE_LOCATION granted=$finePermissionGranted")
        if (!finePermissionGranted) {
            Log.d("LocationWorker", "fetchAndSendLocation: 権限が足りないため失敗を返します。")
            return Result.failure()
        }

        try {
            var location = fusedLocationClient.lastLocation.await()
            Log.d("LocationWorker", "lastLocation fetched: ${location}")
            if (location == null) {
                Log.d("LocationWorker", "fetchAndSendLocation: lastLocation is null, promoting to foreground and requesting current location.")

                // Build and set a minimal ForegroundInfo so the worker runs as a foreground service
                val channelId = "location_worker_channel"
                val channelName = "Location Worker"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        channelId,
                        channelName,
                        NotificationManager.IMPORTANCE_LOW
                    )
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.createNotificationChannel(channel)
                }

                val notification = NotificationCompat.Builder(context, channelId)
                    .setContentTitle("位置情報を送信中")
                    .setContentText("位置情報を取得してサーバーへ送信します")
                    .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build()

                val foregroundInfo = ForegroundInfo(42, notification)
                // setForegroundAsync はワーカーをフォアグラウンドに昇格させる
                setForegroundAsync(foregroundInfo)

                try {
                    val cts = CancellationTokenSource()
                    Log.d("LocationWorker", "fetchAndSendLocation: calling getCurrentLocation()")
                    location = fusedLocationClient
                        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                        .await()
                    Log.d("LocationWorker", "getCurrentLocation returned: ${location}")
                } catch (e: Exception) {
                    Log.e("LocationWorker", "fetchAndSendLocation: getCurrentLocation failed", e)
                }

                if (location == null) {
                    Log.d("LocationWorker", "fetchAndSendLocation: 位置情報がnullなためリトライします。")
                    return Result.retry()
                }
            }

            val lat = location.latitude
            val lon = location.longitude

            Log.d("LocationWorker", "fetchAndSendLocation: 位置情報を取得しました。($lat, $lon)")

            val deviceId = repository.getDeviceId()
            Log.d("LocationWorker", "fetched deviceId=$deviceId")
            if (deviceId == null) {
                Log.d("LocationWorker", "fetchAndSendLocation: deviceIdがnullなためリトライします。")
                // deviceId 未設定の場合は、後で再試行するためリトライする
                return Result.retry()
            }

            val data = hashMapOf(
                "deviceId" to deviceId,
                "location" to hashMapOf(
                    "latitude" to lat,
                    "longitude" to lon,
                )
            )

            Log.d("LocationWorker", "fetchAndSendLocation: 位置情報をサーバーに送信します。($data)")

            // Firebase Functions SDK を使った呼び出し (HTTPSCallable)
            try {
                val callResult = Firebase.functions("asia-northeast2")
                    .getHttpsCallable("updateDeviceLocation")
                    .call(data)
                    .await()
                Log.d("LocationWorker", "sendToServer: 位置情報の送信が完了しました。 result=${callResult.data}")
                return Result.success()
            } catch (e: Exception) {
                Log.e("LocationWorker", "sendToServer: 位置情報の送信中にエラーが発生しました。", e)
                // ネットワークやサーバー側の一時的な問題は再試行する
                return Result.retry()
            }
        } catch (e: Exception) {
            Log.e("LocationWorker", "doWork: An error occurred during location update.", e)
            return Result.retry()
        }
    }
}