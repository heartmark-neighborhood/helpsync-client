package com.example.helpsync.support_details_confirmation_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SupportRequestDetailScreen(
    nickname: String,
    supportContent: String,
    modifier: Modifier = Modifier,
    onDoneClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp), // 縦のpaddingを調整
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ✅ メインコンテンツを weight(1f) を持つColumnで囲む
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp)) // スペースを調整

            // ニックネームボックス
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f) // 少し幅を広げる
                    .height(48.dp)
                    .background(Color(0xFFEEEEEE), shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = nickname,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(48.dp)) // スペースを調整

            // 顔写真表示エリア
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Text("顔写真", fontSize = 16.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(48.dp)) // スペースを調整

            // 支援内容表示エリア
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f) // 少し幅を広げる
                    .height(100.dp)
                    .background(Color(0xFFEEEEEE), shape = RoundedCornerShape(8.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = supportContent,
                    fontSize = 16.sp
                )
            }
        }

        // ✅ このボタンは weight のおかげで常に画面下部に配置される
        OutlinedButton(
            onClick = onDoneClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = ButtonDefaults.outlinedShape
        ) {
            Text("× 閉じる", fontSize = 16.sp)
        }
    }
}