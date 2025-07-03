package com.example.helpsync.blescanner

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.ActivityCompat
import java.util.UUID

class BLEScanner(private val context: Context, private val uuid_string: String) {
    private val serviceUuid = UUID.fromString(uuid_string)
    private val scanner: BluetoothLeScanner by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter.bluetoothLeScanner
    }

    private var scanCallback: ScanCallback? = null

    fun startScan(onMessageReceived: (String) -> Unit) {
        if(ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("BLE", "permission denied")
            return
        }

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val data = result.scanRecord?.getServiceData(ParcelUuid(serviceUuid))
                val msg = data?.toString(Charsets.UTF_8)
                if(msg != null) {
                    onMessageReceived(msg)
                }
            }
        }

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(serviceUuid))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        scanner.startScan(listOf(filter), settings, scanCallback)
    }

    fun stopScan() {
        scanCallback?.let {
            if(ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                == PackageManager.PERMISSION_GRANTED
            ) {
                scanner.stopScan(it)
            }
        }
    }
}