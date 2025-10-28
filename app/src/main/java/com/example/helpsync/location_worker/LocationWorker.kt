package com.example.helpsync.location_worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class LocationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "periodicLocationUpload"
    }

    override suspend fun doWork(): Result {
        Log.d("LocationWorker", "doWork: タスクを開始します。")

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("LocationWorker", "fetchAndSendLocation: 権限が足りないため失敗を返します。")
            return Result.failure()
        }

        try {
            val location = fusedLocationClient.lastLocation.await()
                ?: run {
                    Log.d("LocationWorker", "fetchAndSendLocation: 位置情報がnullなためリトライします。")
                    return Result.retry()
                }

            val lat = location.latitude
            val lon = location.longitude

            Log.d("LocationWorker", "fetchAndSendLocation: 位置情報を取得しました。($lat, $lon)")
            Log.d("LocationWorker", "fetchAndSendLocation: 位置情報をサーバーに送信します。")

            //TODO: ローカルに保存されているdeviceIdを取得し、cloud function呼び出し時に利用するように修正
            val data = hashMapOf(
                "deviceId" to "sample-id",
                "location" to hashMapOf(
                    "latitude" to lat,
                    "longitude" to lon,
                )
            )

            // Firebase Functions SDK を使った呼び出し (HTTPSCallable)
            Firebase.functions
                .getHttpsCallable("updateDeviceLocation")
                .call(data)
                .await()

            Log.d("LocationWorker", "sendToServer: 位置情報の送信が完了しました。")
            return Result.success()
        } catch (e: Exception) {
            Log.e("LocationWorker", "doWork: An error occurred during location update.", e)
            return Result.retry()
        }
    }
}