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
    val helpRequest by userViewModel.activeHelpRequest.collectAsState()
    val isLoading by remember { derivedStateOf { userViewModel.isLoading } }

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