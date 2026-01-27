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
    // onSignOut は親の SupporterScreen で管理するためここでは削除
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
                Log.d("SupporterHome", "No valid UUID to scan yet or scan finished. Waiting...")
            }
        }
    }

    // --- BroadcastReceiver Setup ---
    DisposableEffect(Unit) {
        val bleScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "com.example.SCAN_RESULT") {
                    val bundle: Bundle? = intent.extras
                    val scanSuccess = bundle?.getBoolean("result") ?: false

                    if (scanSuccess) {
                        Toast.makeText(context, "ヘルプ要請を発見！", Toast.LENGTH_SHORT).show()
                        context.stopService(Intent(context, BLEScanner::class.java))
                    } else {
                        context.stopService(Intent(context, BLEScanner::class.java))
                    }
                }
            }
        }

        val filter = IntentFilter("com.example.SCAN_RESULT")
        ContextCompat.registerReceiver(context, bleScanReceiver, filter, ContextCompat.RECEIVER_EXPORTED)

        onDispose {
            try {
                context.unregisterReceiver(bleScanReceiver)
                context.stopService(Intent(context, BLEScanner::class.java))
            } catch (e: Exception) {
                Log.e("SupporterHome", "Error during cleanup: ${e.message}")
            }
        }
    }

    val helpRequestJson by viewModel.helpRequestJson.collectAsState()
    LaunchedEffect(helpRequestJson) {
        helpRequestJson?.let {
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