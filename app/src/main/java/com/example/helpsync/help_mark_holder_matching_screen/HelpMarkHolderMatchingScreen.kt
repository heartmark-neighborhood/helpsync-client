package com.example.helpsync.help_mark_holder_matching_screen

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.helpsync.data.RequestStatus
import com.example.helpsync.viewmodel.UserViewModel
import com.example.helpsync.viewmodel.HelpMarkHolderViewModel
import org.json.JSONObject

@Composable
fun HelpMarkHolderMatchingScreen(
    requestId: String,
    viewModel: UserViewModel,
    helpMarkHolderViewModel: HelpMarkHolderViewModel,
    onMatchingComplete: (String) -> Unit,
    onCancel: () -> Unit
) {
    val activeRequest by viewModel.activeHelpRequest.collectAsState()
    val bleRequestUuid by helpMarkHolderViewModel.bleRequestUuid.collectAsState()
    val errorMessage = viewModel.errorMessage
    val context = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition(label = "matching_animation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    LaunchedEffect(bleRequestUuid) {
        bleRequestUuid?.let { result ->
            val rawData = result["data"]
            if (rawData != null) {
                try {
                    val data = JSONObject(rawData)
                    if (data.has("id")) {
                        val newRequestId = data.getString("id")
                        Log.d("MATCHING_SCREEN", "リクエストID取得成功: $newRequestId -> 監視開始")
                        viewModel.startMonitoringRequest(newRequestId)
                    }
                } catch (e: Exception) {
                    Log.e("MATCHING_SCREEN", "JSONパースエラー", e)
                }
            }
        }
    }

    LaunchedEffect(activeRequest) {
        val status = activeRequest?.status

        when (status) {
            // マッチング成立時
            RequestStatus.MATCHED -> {
                activeRequest?.id?.let { realId ->
                    onMatchingComplete(realId)
                }
            }
            // 1分経過してサーバーが「失敗(failed)」にした場合
            RequestStatus.FAILED, RequestStatus.EXPIRED -> {
                Toast.makeText(context, "支援者が見つかりませんでした", Toast.LENGTH_LONG).show()
                onCancel()
            }
            else -> { /* 待機中 */ }
        }
    }

    LaunchedEffect(viewModel.errorMessage) {
        if (viewModel.errorMessage != null) {
            Toast.makeText(context, "エラーが発生しました: ${viewModel.errorMessage}", Toast.LENGTH_LONG).show()
            onCancel()
        }
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(60 * 1000L)
        if (activeRequest?.status != RequestStatus.MATCHED) {
            onCancel()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, Color.Black, CircleShape)
                .rotate(rotationAngle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Handshake,
                contentDescription = "マッチング中",
                modifier = Modifier
                    .size(60.dp)
                    .then(Modifier.rotate(-rotationAngle)),
                tint = Color.Red
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "マッチング中...",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Red
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "近くの支援者を探しています",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Black,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("キャンセル", fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}