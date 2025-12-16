package com.example.helpsync.blescanner

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import androidx.work.WorkerParameters
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.helpsync.repository.CloudMessageRepository
import com.example.helpsync.worker.CallCloudFunctionWorker
import com.google.android.gms.common.wrappers.Wrappers.packageManager
import kotlinx.coroutines.delay
import kotlin.collections.forEach
import java.util.UUID

class BLEScanWorker (
    context: Context,
    workerParams: WorkerParameters,
    private val repository: CloudMessageRepository
): CoroutineWorker(context, workerParams){
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    val uuidString = inputData.getString("SCAN_UUID")
    val serviceUuid = ParcelUuid(UUID.fromString(uuidString))
    val context = this;
    var isDeviceFound = false
    private val scanResults = mutableStateOf<Boolean>(false)

    val scanner = bluetoothAdapter?.bluetoothLeScanner
    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (isDeviceFound) return
            try {
                isDeviceFound = true
                val address = result.device?.address
                val rssi = result.rssi
                val raw = result.scanRecord?.getServiceData(serviceUuid)
                val msgUtf8 = raw?.toString(Charsets.UTF_8)
                val msgHex = raw?.joinToString(separator = " ") { String.format("%02X", it) }

                scanResults.value = true

                // MATCH FOUND! Send success broadcast and stop scanning
                Log.d("debug", "onScanResult: MATCH FOUND! callbackType=$callbackType device=$address rssi=$rssi msgUtf8=$msgUtf8 msgHex=$msgHex")
            } catch (ex: Exception) {
                Log.e("BLEScanError", "onScanResult: failed to process scan result", ex)
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            Log.d("BatchScanResult", "onBatchScanResults: ${'$'}{results?.size ?: 0} results")
            results?.forEach { r ->
                val address = r.device?.address
                val rssi = r.rssi
                Log.d("BatchScanResult", "batch result device=$address rssi=$rssi")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BLEScanWorker", "onScanFailed: errorCode=$errorCode")
        }
    }

    override suspend fun doWork(): Result {
        Log.d("BLEScanWorker", "🔍 doWork() started")
        Log.d("BLEScanWorker", "📝 UUID to scan: $serviceUuid")
        
        if (scanner == null) {
            Log.e("BLEScanWorker", "❌ BluetoothLeScanner is null! Cannot start scan.")
            return Result.failure()
        }
        
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        // Changed to setServiceData to match BLEAdvertiser's packet structure (UUID in Service Data, not Service UUID list)
        val filter = ScanFilter.Builder()
            .setServiceData(serviceUuid, null)
            .build()
        try {
            if(ActivityCompat.checkSelfPermission(applicationContext, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                Log.d("BLEScanWorker", "✅ BLUETOOTH_SCAN permission granted")
                scanner?.startScan(listOf(filter), settings, scanCallback)
                Log.d("BLEScanWorker", "🚀 BLE scan started successfully")
            } else {
                Log.e("BLEScanWorker", "❌ BLUETOOTH_SCAN permission denied!")
                return Result.failure()
            }
            Log.d("BLEScanWorker", "⏱️ Scanning for 30 seconds...")
            delay(30_000L)
            scanner?.stopScan(scanCallback)
            Log.d("BLEScanWorker", "BLEScanを停止しました")
            var outputData = workDataOf(
                "IS_FOUND" to false
            )
            if(scanResults.value == true)
            {
                outputData = workDataOf(
                    "IS_FOUND" to true
                )
                try {
                    repository.callHandleProximityVerificationResultBackGround(true)
                } catch(e: Exception){
                    Log.d("BLEScanWorker", "handleProximityVerificationResultの呼び出しに失敗しました")
                    Log.d("BLEScanWorker", "${e.message}")
                }
            }
            return Result.success(outputData)
        } catch (e: Exception) {
            Log.d("Error", "BLE Scanに失敗しました")
            Log.d("Error", "${e.message}")
            return Result.failure()
        }
    }

}