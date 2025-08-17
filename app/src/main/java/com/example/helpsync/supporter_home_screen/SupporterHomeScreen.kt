package com.example.helpsync.supporter_home_screen

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.helpsync.blescanner.BLEScanner
import androidx.core.content.ContextCompat
import java.util.UUID

data class SupportRequest(val id: String, val nickname: String, val content: String)

@SuppressLint("NewApi")
@Composable
fun SupporterHomeScreen(
    // ✅ エラーの原因: この引数の名前と型を MainActivity と一致させます
    onSupportRequestClick: (nickname: String, content: String) -> Unit
) {
    val context = LocalContext.current
    var requests by remember { mutableStateOf(emptyList<SupportRequest>()) }

    // (BroadcastReceiverとDisposableEffectのコードは変更なし)
    val bleScanReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "com.example.SCAN_RESULT") {
                    val bundle: Bundle? = intent.extras
                    val found = bundle?.getBoolean("result") ?: false
                    if (found) {
                        val deviceAddress = bundle?.getString("DEVICE_ADDRESS") ?: "Unknown"
                        val deviceName = bundle?.getString("DEVICE_NAME") ?: "Unknown Device"

                        val newRequest = SupportRequest(
                            id = deviceAddress,
                            nickname = deviceName,
                            content = "ヘルプが必要です"
                        )

                        if (!requests.any { it.id == newRequest.id }) {
                            requests = requests + newRequest
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        val uuidToScan = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB")
        val intent = Intent(context, BLEScanner::class.java)
        intent.putExtra("UUID", uuidToScan.toString())
        ContextCompat.startForegroundService(context, intent)

        val filter = IntentFilter("com.example.SCAN_RESULT")
        context.registerReceiver(bleScanReceiver, filter, Context.RECEIVER_EXPORTED)

        onDispose {
            context.unregisterReceiver(bleScanReceiver)
            context.stopService(intent)
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text("支援依頼リスト", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (requests.isEmpty()) {
            Text("現在、支援依頼はありません。", style = MaterialTheme.typography.bodyLarge)
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(requests) { request ->
                    Card(
                        // ✅ ここで onSupportRequestClick を呼び出します
                        onClick = { onSupportRequestClick(request.nickname, request.content) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("依頼者: ${request.nickname}", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("内容: ${request.content}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}