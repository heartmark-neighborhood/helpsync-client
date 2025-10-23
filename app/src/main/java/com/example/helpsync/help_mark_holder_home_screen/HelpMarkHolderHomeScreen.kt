package com.example.helpsync.help_mark_holder_home_screen

import android.Manifest
import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.helpsync.bleadvertiser.BLEAdvertiser
import com.example.helpsync.data.RequestStatus
import com.example.helpsync.viewmodel.UserViewModel

@SuppressLint("NewApi")
@Composable
fun HelpMarkHolderHomeScreen(
    viewModel: UserViewModel,
    onMatchingStarted: () -> Unit
    callviewModel : HelpMarkHolderViewModel = viewModel()
) {
    val context = LocalContext.current
    val helpRequest by viewModel.activeHelpRequest.collectAsState()
    val isLoading by remember { derivedStateOf { viewModel.isLoading } }

    // ★ 変更点1: Advertiserのインスタンスを保持するstateを定義
    var bleAdvertiser by remember { mutableStateOf<BLEAdvertiser?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val allGranted = perms.entries.all { it.value }
        if (!allGranted) {
            Toast.makeText(context, "支援の要請には権限が必要です。", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        )
    }

    LaunchedEffect(helpRequest) {
        val currentRequest = helpRequest
        if (currentRequest != null && currentRequest.status == RequestStatus.PENDING) {
            // ★ 変更点2: リクエスト成功後にUUIDを使ってAdvertiserを生成
            val advertiser = BLEAdvertiser(context, currentRequest.proximityUuid)
            bleAdvertiser = advertiser // stateにインスタンスを保存

            // ★ 変更点3: 新しいstartAdvertise関数を呼び出す
            advertiser.startAdvertise(
                message = currentRequest.id
            ) { status ->
                Log.d("HOLDER_ADVERTISER", "Status: $status")
            }

            onMatchingStarted()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // ★ 変更点4: stateに保存したAdvertiserを停止
            bleAdvertiser?.stopAdvertise()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = { callviewModel.callCreateHelpRequest() },
                modifier = Modifier.size(200.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                enabled = helpRequest == null || helpRequest?.status != RequestStatus.PENDING
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "助けを求める",
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "助けを求める",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}