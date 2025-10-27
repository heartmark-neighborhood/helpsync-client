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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.helpsync.blescanner.BLEScanner
import com.example.helpsync.data.HelpRequest
import com.example.helpsync.viewmodel.UserViewModel

@SuppressLint("NewApi")
@Composable
fun SupporterHomeScreen(
    viewModel: UserViewModel,
    onNavigateToAcceptance: (requestId: String) -> Unit
) {
    val context = LocalContext.current
    val pendingRequests by viewModel.pendingHelpRequests.collectAsState()
    val isLoading by remember { derivedStateOf { viewModel.isLoading } }
    var selectedRequest by remember { mutableStateOf<HelpRequest?>(null) }

    // --- 権限要求の処理 (初回のみ) ---
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
        // 画面表示時にPENDINGのリクエスト一覧を取得
        viewModel.fetchPendingHelpRequests()
    }

    LaunchedEffect(selectedRequest) {
        val requestToScan = selectedRequest
        if (requestToScan != null) {
            // BroadcastReceiverを定義
            val bleScanReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == "com.example.SCAN_RESULT") {
                        val bundle: Bundle? = intent.extras
                        val found = bundle?.getBoolean("SCAN_SUCCESS") ?: false
                        if (found) {
                            // ▼▼▼【修正点】null許容のbundleに対して、安全にアクセスするように変更 ▼▼▼
                            val foundRequestId = bundle?.getString("REQUEST_ID")
                            if (foundRequestId == requestToScan.id) {
                                Toast.makeText(context, "${requestToScan.requesterNickname}さんを発見！", Toast.LENGTH_SHORT).show()
                                viewModel.handleProximityVerificationResult(requestToScan.id)
                                onNavigateToAcceptance(requestToScan.id)
                            }
                        }
                    }
                }
            }
            // スキャンサービスを開始
            val scanIntent = Intent(context, BLEScanner::class.java).apply {
                putExtra("UUID", requestToScan.proximityUuid)
            }
            ContextCompat.startForegroundService(context, scanIntent)
            // レシーバーを登録
            val filter = IntentFilter("com.example.SCAN_RESULT")
            ContextCompat.registerReceiver(context, bleScanReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
            Log.d("SUPPORTER_HOME", "Scan started for ${requestToScan.requesterNickname}")
        }
    }

    // 画面から離れるときにスキャンサービスとレシーバーを停止
    DisposableEffect(Unit) {
        onDispose {
            val stopIntent = Intent(context, BLEScanner::class.java)
            context.stopService(stopIntent)
            Log.d("SUPPORTER_HOME", "Screen disposed. Scan service stopped.")
        }
    }

    // --- UI部分 ---
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading && pendingRequests.isEmpty() -> {
                CircularProgressIndicator()
            }

            selectedRequest != null -> {
                // スキャン中の表示
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${selectedRequest!!.requesterNickname}さんを\n探しています...",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            pendingRequests.isEmpty() -> {
                Text("現在、助けを求めている人はいません。")
            }

            else -> {
                // リクエスト一覧の表示
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            "近くで助けを求めている人",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(pendingRequests) { request ->
                        HelpRequestCard(request = request) {
                            selectedRequest = request // 項目をタップでスキャン開始
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HelpRequestCard(
    request: HelpRequest,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.requesterNickname,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                // 必要ならここにリクエスト時刻などを表示
            }
            Button(onClick = onClick) {
                Text("支援する")
            }
        }
    }
}