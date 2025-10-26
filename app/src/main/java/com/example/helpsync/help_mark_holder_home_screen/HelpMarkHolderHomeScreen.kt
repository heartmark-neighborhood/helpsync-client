package com.example.helpsync.help_mark_holder_home_screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
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
import androidx.core.content.ContextCompat
import androidx.core.content.PackageManagerCompat
import com.example.helpsync.bleadvertiser.BLEAdvertiser
import com.example.helpsync.data.RequestStatus
import com.example.helpsync.viewmodel.HelpMarkHolderViewModel
import com.example.helpsync.viewmodel.UserViewModel
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient

@SuppressLint("NewApi")
@Composable
fun HelpMarkHolderHomeScreen(
    userViewModel: UserViewModel,
    onMatchingStarted: () -> Unit,
    helpMarkHolderViewModel : HelpMarkHolderViewModel,
    locationClient: FusedLocationProviderClient
) {
    val context = LocalContext.current

    val helpRequest by viewModel.activeHelpRequest.collectAsState()
    val isLoading by remember { derivedStateOf { viewModel.isLoading } }
    val bleRequestUuid by　viewModel.bleRequestUuid.collectAsState()

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

    LaunchedEffect(bleRequestUuid) {
        // UUIDがnullでない場合のみAdvertiseを開始
        bleRequestUuid?.let { uuid ->
            Log.d("HOLDER_BLE", "BLE Advertise開始: UUID=$uuid")
        
            try {
                // 既存のAdvertiserがあれば停止
                bleAdvertiser?.stopAdvertise()
            
                // 新しいAdvertiserインスタンスを作成
                val advertiser = BLEAdvertiser(context, uuid)
                bleAdvertiser = advertiser
            
                // BLE Advertiseを開始
                advertiser.startAdvertise(
                    message = uuid  // UUIDをメッセージとして使用
                ) { status ->
                    when (status) {
                        "ADVERTISING_STARTED" -> {
                            Log.d("HOLDER_BLE", "Advertise開始成功")
                            Toast.makeText(
                                context, 
                                "支援者を探しています...", 
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        "ADVERTISING_FAILED" -> {
                            Log.e("HOLDER_BLE", "Advertise開始失敗")
                            Toast.makeText(
                                context, 
                                "Bluetooth Advertiseの開始に失敗しました", 
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        "ADVERTISING_STOPPED" -> {
                            Log.d("HOLDER_BLE", "Advertise停止")
                        }
                        else -> {
                            Log.d("HOLDER_BLE", "Advertise状態: $status")
                        }
                    }
                }
            
                // タイムアウト設定（例: 30秒後に自動停止）
                kotlinx.coroutines.delay(30 * 1000L) // 30秒
                advertiser.stopAdvertise()
                Log.d("HOLDER_BLE", "Advertiseタイムアウトにより停止")
                Toast.makeText(
                    context, 
                    "支援者が見つかりませんでした", 
                    Toast.LENGTH_SHORT
                ).show()
            
            } catch (e: Exception) {
                Log.e("HOLDER_BLE", "BLE Advertise処理エラー: ${e.message}")
                Toast.makeText(
                    context, 
                    "エラーが発生しました: ${e.message}", 
                    Toast.LENGTH_SHORT
                ).show()
            }
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
                onClick = {
                    when {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            Log.d("HelpMarkHolderHomeScreen", "Permission already granted, fetching location...")
                            val request = CurrentLocationRequest.Builder()
                                .setPriority(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY)
                                .setDurationMillis(5000)
                                .build()
                            locationClient.getCurrentLocation(request, null)
                                .addOnSuccessListener { location: Location? ->
                                    val lat = location?.latitude ?: 0.0
                                    val lon = location?.longitude ?: 0.0

                                    Log.d("LocationClient", "Location acquired: $lat, $lon")
                                    helpMarkHolderViewModel.callCreateHelpRequest(lat, lon)
                                }
                        }

                        //`ActivityCompat.shouldShowRequestPermissionRationale()`であれそれするのも良さげ

                        else -> {
                            // 権限がない場合、パーミッションリクエストを起動
                            Log.d("LocationButton", "Permission not granted, launching request...")
                            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                        }
                    }
                },
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