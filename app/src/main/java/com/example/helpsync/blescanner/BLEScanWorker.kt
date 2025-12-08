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
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.helpsync.worker.CallCloudFunctionWorker
import com.google.android.gms.common.wrappers.Wrappers.packageManager
import kotlinx.coroutines.delay
import kotlin.collections.forEach
import java.util.UUID

class BLEScanWorker (
    context: Context,
    workerParams: WorkerParameters
): CoroutineWorker(context, workerParams){
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    val uuidString = inputData.getString("SCAN_UUID")
    val serviceUuid = ParcelUuid(UUID.fromString(uuidString))
    val context = this;
    var isDeviceFound = false

    val scanner = bluetoothAdapter?.bluetoothLeScanner
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (isDeviceFound) return
            try {
                isDeviceFound = true
                val address = result.device?.address
                val rssi = result.rssi
                val raw = result.scanRecord?.getServiceData(serviceUuid)
                val msgUtf8 = raw?.toString(Charsets.UTF_8)
                val msgHex = raw?.joinToString(separator = " ") { String.format("%02X", it) }

                val inputDataForWorker = workDataOf(
                    "SCAN_RESULT_DATA" to true
                )

                val uploadRequest = OneTimeWorkRequestBuilder<CallCloudFunctionWorker>()
                    .setInputData(inputDataForWorker)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    "Upload_Scan_Job",
                    ExistingWorkPolicy.KEEP,
                    uploadRequest
                )
                // Log all scan results for debugging


                // MATCH FOUND! Send success broadcast and stop scanning
                Log.d("debug", "onScanResult: MATCH FOUND! callbackType=$callbackType device=$address rssi=$rssi msgUtf8=$msgUtf8 msgHex=$msgHex")

                // Stop scanning immediately after finding target

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
        Log.d("BLEScanner", "doWorkが実行されました")
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()
        val filter = ScanFilter.Builder()
            .setServiceUuid(serviceUuid)
            .build()
        try {
            if(ActivityCompat.checkSelfPermission(applicationContext, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                scanner?.startScan(listOf(filter), settings, scanCallback)
                Log.d("BLEScanWorker", "BLEScanを開始しました")
            }
            else
            {
                Log.d("Error", "権限が拒否されています")
            }
            delay(450_000L)
            Result.success()
        } catch (e: Exception) {
            Log.d("Error", "BLE Scanに失敗しました")
            Log.d("Error", "${e.message}")
            Result.failure()
        }
        return Result.success()
    }

}