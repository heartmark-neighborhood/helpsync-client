package com.example.helpsync.supporter_home_screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.helpsync.blescanner.BLEScanWorker
import com.example.helpsync.blescanner.BLEScanner
import com.example.helpsync.viewmodel.SupporterViewModel
import org.json.JSONObject
import org.koin.androidx.compose.koinViewModel

@SuppressLint("NewApi")
@Composable
fun SupporterHomeScreen(
    viewModel: SupporterViewModel = koinViewModel(),
    onNavigateToAcceptance: (requestId: String) -> Unit
) {
    val context = LocalContext.current
    val bleRequestUuid by viewModel.bleRequestUuid.collectAsState()

    // --- Permissions ---
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val allGranted = perms.entries.all { it.value }
        if (!allGranted) {
            Toast.makeText(context, "スキャンには権限が必要です", Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
            )
        )
    }

    // --- BLE Scan Start Trigger ---
    LaunchedEffect(bleRequestUuid) {
        bleRequestUuid?.let { result ->
            val rawData = result["data"]
            val data = JSONObject(rawData)
            val uuidToScan = data.getString("proximityVerificationId")
            if (uuidToScan == null) {
                Log.e("HOLDER_BLE", "UUID is null")
                return@let
            }

            if (!uuidToScan.isNullOrBlank() && uuidToScan != "string") {
                val inputData = workDataOf("SCAN_UUID" to uuidToScan)
                val bleScanWorkRequest = OneTimeWorkRequestBuilder<BLEScanWorker>()
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setInputData(inputData)
                    .build()
                val workManager = WorkManager.getInstance(context)
                workManager.enqueueUniqueWork(
                    "BLEScanWork",
                    ExistingWorkPolicy.REPLACE,
                    bleScanWorkRequest
                )
            } else {
                Log.d(
                    "SupporterHome",
                    "No valid UUID to scan yet or scan finished ($uuidToScan). Waiting..."
                )
            }
        }
    }

    // --- BroadcastReceiver Setup ---
    DisposableEffect(Unit) {
        val bleScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "com.example.SCAN_RESULT") {
                    Log.d("SupporterHome", "Received SCAN_RESULT broadcast, intent extras: ${intent.extras}")
                    
                    val bundle: Bundle? = intent.extras
                    // Use the new key names from BLEScanner
                    val scanSuccess = bundle?.getBoolean("result") ?: false
                    val device = bundle?.getString("device")
                    val rssi = bundle?.getInt("rssi", Int.MIN_VALUE)
                    val msgUtf8 = bundle?.getString("msgUtf8")
                    val msgHex = bundle?.getString("msgHex")
                    
                    Log.d("SupporterHome", "Received scan result: result=$scanSuccess, device=$device, rssi=$rssi, msgUtf8=$msgUtf8, msgHex=$msgHex")
                    Log.d("SupporterHome", "Expected helpRequestId=${viewModel.getHelpRequestId()}")

                    if (scanSuccess) {
                        Log.d("SupporterHome", "✅ Scan successful! Device found with matching service data")
                        Toast.makeText(context, "ヘルプ要請を発見！ device=$device rssi=$rssi", Toast.LENGTH_SHORT).show()
                        // Cloud Function call is handled by BLEScanner; just stop the service
                        context.stopService(Intent(context, BLEScanner::class.java))
                    } else {
                        Log.w("SupporterHome", "Scan failed or timed out (no matching device found)")
                        Toast.makeText(context, "ヘルプ要請が見つかりませんでした", Toast.LENGTH_SHORT).show()
                        // Cloud Function call for failure is handled by BLEScanner
                        context.stopService(Intent(context, BLEScanner::class.java))
                    }
                }
            }
        }

        // Register the receiver
        val filter = IntentFilter("com.example.SCAN_RESULT")
        ContextCompat.registerReceiver(context, bleScanReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        Log.d("SupporterHome", "Scan result receiver registered.")

        onDispose {
            try {
                context.unregisterReceiver(bleScanReceiver)
                context.stopService(Intent(context, BLEScanner::class.java))
                Log.d("SupporterHome", "Scan result receiver unregistered and service stopped on dispose.")
            } catch (e: Exception) {
                Log.e("SupporterHome", "Error during receiver cleanup: ${e.message}")
            }
        }
    }

    val helpRequestJson by viewModel.helpRequestJson.collectAsState()
    LaunchedEffect(helpRequestJson) {
        helpRequestJson?.let {
            Log.d("SupporterHome", "Received help request details, navigating...")
            onNavigateToAcceptance(viewModel.getHelpRequestId() ?: "")
            viewModel.clearViewedRequest()
        }
    }


    // --- UI ---
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val uuid = bleRequestUuid?.get("proximityVerificationId")
            if (uuid.isNullOrBlank() || uuid == "string") {
                Text("近くのヘルプ要請を待機中...")
            } else {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "ヘルプ要請をスキャン中...",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}