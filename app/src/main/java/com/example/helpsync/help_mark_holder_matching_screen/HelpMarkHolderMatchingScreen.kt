package com.example.helpsync.help_mark_holder_matching_screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.helpsync.data.RequestStatus
import com.example.helpsync.viewmodel.UserViewModel

@Composable
fun HelpMarkHolderMatchingScreen(
    requestId: String,
    viewModel: UserViewModel,
    onMatchingComplete: (String) -> Unit, // requestIdを渡せるように変更
    onCancel: () -> Unit
) {
    // ★ 変更点2: ViewModelからアクティブなリクエストの状態を監視する
    val activeRequest by viewModel.activeHelpRequest.collectAsState()

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

    // ★ 変更点3: サーバーからの状態変更を監視するロジック
    LaunchedEffect(activeRequest) {
        // リクエストの状態がMATCHEDに変わったら、完了コールバックを呼ぶ
        if (activeRequest?.status == RequestStatus.MATCHED) {
            onMatchingComplete(requestId)
        }
    }

    // ★ 変更点4: ダミーのタイマーとランダム遅延を削除
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3E5F5))
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
                .rotate(rotationAngle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Handshake,
                contentDescription = "マッチング中",
                modifier = Modifier
                    .size(60.dp)
                    .then(Modifier.rotate(-rotationAngle)),
                tint = Color(0xFF9C27B0)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "マッチング中...",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF9C27B0)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "近くの支援者を探しています",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF757575),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = onCancel, // キャンセル処理はシンプルに
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("キャンセル", fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}