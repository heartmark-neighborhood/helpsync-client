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
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import java.util.UUID

class BLEScanner() : Service() {
    private val SCAN_DURATION_MS = 18_000L
    private val CHANNEL_ID = "ble_scan_channel"
    private lateinit var scanner : BluetoothLeScanner
    private var handler: Handler? = null

    private var scanCallback: ScanCallback? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        scanner = bluetoothManager.adapter.bluetoothLeScanner
        handler = Handler(Looper.getMainLooper())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val serviceUuid = UUID.fromString(intent?.getStringExtra("UUID"))
        startForeground(1, createNotification("bluetooth scanning"))
        startScan(serviceUuid)
        return START_STICKY
    }
    fun startScan(serviceUuid: UUID) {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("BLE", "permission denied")
            return
        }
        val context = this;
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val data = result.scanRecord?.getServiceData(ParcelUuid(serviceUuid))
                val msg = data?.toString(Charsets.UTF_8)
                val intent = Intent("com.example.SCAN_RESULT")
                if(msg != null) {
                    intent.setPackage(context.packageName)
                    intent.putExtra("result", true)
                    sendBroadcast(intent)
                }
                else
                {
                    intent.setPackage(context.packageName)
                    intent.putExtra("result", false)
                    sendBroadcast(intent)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                val intent = Intent("com.example.SCAN_RESULT")
                intent.setPackage(context.packageName)
                intent.putExtra("result", false)
                sendBroadcast(intent)
            }
        }

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(serviceUuid))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        scanner.startScan(listOf(filter), settings, scanCallback)

        handler?.postDelayed({
            scanner.stopScan(scanCallback)
            val intent = Intent("com.example.SCAN_RESULT")
            intent.setPackage(context.packageName)
            intent.putExtra("result", false)
            sendBroadcast(intent)
        }, SCAN_DURATION_MS)
    }

    fun stopScan() {
        scanCallback?.let {
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                == PackageManager.PERMISSION_GRANTED
            ) {
                scanner.stopScan(it)
            }
        }
    }

    override fun onDestroy() {
        if(checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED)
        {
            scanner.stopScan(scanCallback)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(text: String): Notification {
        Log.d("debug", "notif")
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