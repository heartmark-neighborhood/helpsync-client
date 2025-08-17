package com.example.helpsync.help_mark_holder_home_screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.helpsync.blescanner.BLEScanner
import com.example.helpsync.bleadvertiser.BLEAdvertiser
import java.util.UUID

@SuppressLint("NewApi")
@Composable
fun HelpMarkHolderHomeScreen(
    onMatchingClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var isNearSupporter by remember { mutableStateOf(false) } // 近くにサポーターがいるか
    var isMatching by remember { mutableStateOf(false) } // マッチング中か

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val allGranted = perms.entries.all { it.value }
        if (!allGranted) {
            Toast.makeText(context, "permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    val helpMarkServiceUUID = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB")
    val bleAdvertiser = remember { BLEAdvertiser(context, "0000180A-0000-1000-8000-00805F9B34FB") }

    val bleScanReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "com.example.SCAN_RESULT") {
                    val bundle: Bundle? = intent.extras
                    val found = bundle?.getBoolean("result") ?: false
                    isNearSupporter = found
                }
            }
        }
    }

    DisposableEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.BLUETOOTH_ADVERTISE,
            )
        )

        bleAdvertiser.startAdvertise("help_mark") { status ->
        }

        val scanIntent = Intent(context, BLEScanner::class.java)
        scanIntent.putExtra("UUID", helpMarkServiceUUID.toString())
        ContextCompat.startForegroundService(context, scanIntent)

        val filter = IntentFilter("com.example.SCAN_RESULT")
        context.registerReceiver(bleScanReceiver, filter, Context.RECEIVER_EXPORTED)

        onDispose {
            bleAdvertiser.stopAdvertise()
            context.unregisterReceiver(bleScanReceiver)
            context.stopService(scanIntent)
        }
    }

    val backgroundColor by animateColorAsState(
        targetValue = if (isNearSupporter) Color(0xFFE3F2FD) else Color.White,
        animationSpec = tween(500),
        label = "background_color"
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(16.dp)
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(80.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isNearSupporter) Color.White else Color(0xFFF5F5F5)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                if (isNearSupporter) Color(0xFF2196F3) else Color(0xFFBDBDBD)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isNearSupporter) Icons.Default.Wifi else Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = if (isNearSupporter) "近くに支援者がいます" else "支援者を探しています...",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isNearSupporter) Color(0xFF2196F3) else Color(0xFF757575),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isNearSupporter) "マッチング可能です" else "しばらくお待ちください",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isNearSupporter) Color(0xFF4CAF50) else Color(0xFF757575),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isNearSupporter) {
                Button(
                    onClick = onMatchingClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Handshake,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "マッチング！",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Bluetoothとロケーション機能をオンにして\n近くの支援者を探しています",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF757575),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(onClick = onHomeClick) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = Color(0xFF757575),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "Home",
                        color = Color(0xFF757575),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF757575),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "Settings",
                        color = Color(0xFF757575),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}