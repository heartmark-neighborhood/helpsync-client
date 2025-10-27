import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class LocationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "LocationTrackingChannel"
        const val NOTIFICATION_ID = 1
        const val WORK_NAME = "periodicLocationUpload" // WorkManagerのタスク名
    }

    override suspend fun doWork(): Result {
        Log.d("LocationWorker", "doWork: 処理を開始します")

        // 権限チェック（WorkManager起動時にも行うが、念のため）
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e("LocationWorker", "位置情報の権限がありません")
            return Result.failure()
        }

        // --- 1. フォアグラウンドサービスとして実行を開始 ---
        try {
            setForeground(createForegroundInfo())
        } catch (e: Exception) {
            Log.e("LocationWorker", "フォアグラウンドサービスの開始に失敗", e)
            return Result.failure()
        }

        // --- 2. 位置情報を取得 ---
        val location = try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            // 高精度な現在位置を要求
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .await() // Kotlin Coroutinesで待機
        } catch (e: Exception) {
            Log.e("LocationWorker", "位置情報の取得に失敗", e)
            return Result.failure()
        }

        if (location == null) {
            Log.w("LocationWorker", "取得した位置情報がnullです")
            return Result.failure()
        }

        Log.d("LocationWorker", "位置情報取得成功: ${location.latitude}, ${location.longitude}")

        // --- 3. Firebase Cloud Functionに送信 ---
        return try {
            sendToFirebase(location.latitude, location.longitude)
            Log.d("LocationWorker", "Firebaseへの送信成功")
            Result.success()
        } catch (e: Exception) {
            Log.e("LocationWorker", "Firebaseへの送信に失敗", e)
            // ネットワークエラーなどはリトライが望ましい
            Result.retry()
        }
    }

    // --- フォアグラウンドサービスのための通知を作成 ---
    private fun createForegroundInfo(): ForegroundInfo {
        createNotificationChannel() // チャンネル登録

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("位置情報を追跡中")
            .setContentText("アプリがバックグラウンドで位置情報を記録しています。")
            .setOngoing(true) // ユーザーがスワイプで消せないようにする
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "位置情報追跡",
                NotificationManager.IMPORTANCE_LOW // ユーザーの邪魔にならないようLOWに設定
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    // --- Firebase Cloud Functions への送信ロジック ---
    private suspend fun sendToFirebase(lat: Double, lon: Double) {
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
            .getHttpsCallable("updateDeviceLocation") // あなたが作成したCFの名前
            .call(data)
            .await()
    }
}