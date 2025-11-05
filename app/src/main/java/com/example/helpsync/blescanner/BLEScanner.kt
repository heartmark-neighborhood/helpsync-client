package com.example.helpsync.blescanner

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.Service.START_STICKY
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.HttpsCallableResult
import com.google.android.gms.tasks.OnCompleteListener
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import java.util.UUID

class BLEScanner() : Service() {
    private val SCAN_DURATION_MS = 18_000L
    private val CHANNEL_ID = "ble_scan_channel"
    private lateinit var scanner: BluetoothLeScanner
    private var handler: Handler? = null

    private var scanCallback: ScanCallback? = null
    private var helpRequestId: String? = null

    companion object {
        private const val TAG = "BLEScanner"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "onCreate: initializing BLEScanner service")
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        scanner = bluetoothManager.adapter.bluetoothLeScanner
        handler = Handler(Looper.getMainLooper())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uuidString = intent?.getStringExtra("UUID")
        helpRequestId = intent?.getStringExtra("REQUEST_ID")
        Log.d(TAG, "onStartCommand: helpRequestId=$helpRequestId")
        Log.d(TAG, "onStartCommand: intent=$intent, UUID_EXTRA=$uuidString, flags=$flags, startId=$startId")
        val serviceUuid = try {
            UUID.fromString(uuidString)
        } catch (e: Exception) {
            Log.e(TAG, "onStartCommand: invalid UUID string: $uuidString", e)
            null
        }
        startForeground(1, createNotification("bluetooth scanning"))
        serviceUuid?.let { startScan(it) } ?: Log.w(TAG, "onStartCommand: no valid service UUID, skipping startScan")
        return START_STICKY
    }

    fun startScan(serviceUuid: UUID) {
        Log.d(TAG, "startScan: requested for serviceUuid=$serviceUuid")
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "startScan: permission BLUETOOTH_SCAN denied")
            return
        }
        val context = this;
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                try {
                    val address = result.device?.address
                    val rssi = result.rssi
                    val raw = result.scanRecord?.getServiceData(ParcelUuid(serviceUuid))
                    val msgUtf8 = raw?.toString(Charsets.UTF_8)
                    val msgHex = raw?.joinToString(separator = " ") { String.format("%02X", it) }
                    
                    // Log all scan results for debugging
                    if (raw == null) {
                        Log.v(TAG, "onScanResult: device=$address rssi=$rssi (no matching service data)")
                        // Don't send broadcast for non-matching devices
                        return
                    }
                    
                    // MATCH FOUND! Send success broadcast and stop scanning
                    Log.d(TAG, "onScanResult: MATCH FOUND! callbackType=$callbackType device=$address rssi=$rssi msgUtf8=$msgUtf8 msgHex=$msgHex")
                    
                    val intent = Intent("com.example.SCAN_RESULT")
                    intent.setPackage(context.packageName)
                    intent.putExtra("result", true)
                    intent.putExtra("rssi", rssi)
                    intent.putExtra("device", address)
                    intent.putExtra("msgUtf8", msgUtf8)
                    intent.putExtra("msgHex", msgHex)
                    sendBroadcast(intent)
                    
                        // Call Cloud Function directly to report verification success
                        sendVerificationToCloud(true)
                    
                    // Stop scanning immediately after finding target
                    Log.d(TAG, "onScanResult: stopping scan after successful match")
                    handler?.removeCallbacksAndMessages(null)  // Cancel timeout
                    try {
                        scanner.stopScan(scanCallback)
                    } catch (e: Exception) {
                        Log.w(TAG, "onScanResult: stopScan threw", e)
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "onScanResult: failed to process scan result", ex)
                }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                super.onBatchScanResults(results)
                Log.d(TAG, "onBatchScanResults: ${'$'}{results?.size ?: 0} results")
                results?.forEach { r ->
                    val address = r.device?.address
                    val rssi = r.rssi
                    Log.d(TAG, "batch result device=$address rssi=$rssi")
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "onScanFailed: errorCode=$errorCode")
                val intent = Intent("com.example.SCAN_RESULT")
                intent.setPackage(context.packageName)
                intent.putExtra("result", false)
                intent.putExtra("errorCode", errorCode)
                sendBroadcast(intent)
            }
        }

        // Note: We scan without UUID filter because advertiser uses only addServiceData
        // which doesn't expose UUID in the same way as addServiceUuid
        // Instead, we filter in onScanResult by checking if service data exists
        val filter = ScanFilter.Builder()
            .build()  // No filter - scan all BLE devices
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        Log.d(TAG, "startScan: starting scanner with no UUID filter (will filter by service data in callback)")
        scanner.startScan(listOf(filter), settings, scanCallback)

        handler?.postDelayed({
            Log.d(TAG, "startScan: scan timeout reached, stopping scan")
            try {
                scanner.stopScan(scanCallback)
            } catch (e: Exception) {
                Log.w(TAG, "startScan: stopScan threw", e)
            }
            // send timeout broadcast
            val intent = Intent("com.example.SCAN_RESULT")
            intent.setPackage(context.packageName)
            intent.putExtra("result", false)
            sendBroadcast(intent)

            // Report verification failure to Cloud Function
            sendVerificationToCloud(false)
        }, SCAN_DURATION_MS)
    }

    fun stopScan() {
        scanCallback?.let {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                == PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "stopScan: stopping scan callback")
                try {
                    scanner.stopScan(it)
                } catch (e: Exception) {
                    Log.w(TAG, "stopScan: stopScan threw", e)
                }
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: destroying service, attempting to stop scan")
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            try {
                scanner.stopScan(scanCallback)
            } catch (e: Exception) {
                Log.w(TAG, "onDestroy: stopScan threw", e)
            }
        }
        super.onDestroy()
    }

    private fun sendVerificationToCloud(verificationResult: Boolean) {
        try {
            val functions = FirebaseFunctions.getInstance("asia-northeast2")
            val uid: String? = FirebaseAuth.getInstance().currentUser?.uid
            val data = hashMapOf<String, Any?>(
                "verificationResult" to verificationResult,
                "helpRequestId" to helpRequestId,
                "userId" to uid
            )
            Log.d(TAG, "sendVerificationToCloud: calling Cloud Function with data=$data")
            functions.getHttpsCallable("handleProximityVerificationResult")
                .call(data)
                .addOnCompleteListener(OnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val result = task.result
                        Log.d(TAG, "Cloud Function call successful: result=${result?.data}")
                    } else {
                        Log.e(TAG, "Cloud Function call failed", task.exception)
                    }
                })
        } catch (e: Exception) {
            Log.e(TAG, "sendVerificationToCloud: exception preparing function call", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(text: String): Notification {
        Log.d(TAG, "createNotification: text=$text")
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ヘルプ検索中")
            .setContentText("付近のヘルプ要請が無いか検索しています")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "HelpSync",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }
}
