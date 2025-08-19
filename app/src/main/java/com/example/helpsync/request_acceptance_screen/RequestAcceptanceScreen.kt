package com.example.helpsync.request_acceptance_screen

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@SuppressLint("DefaultLocale")
@Composable
fun RequestAcceptanceScreen(
    // ✅ 引数を現在の仕様に合わせます
    nickname: String,
    content: String,
    onAcceptClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    var time by remember { mutableIntStateOf(60) }

    // タイマー処理：0秒になったら前の画面へ戻る
    LaunchedEffect(Unit) {
        while (time > 0) {
            delay(1000)
            time--
        }
        // ✅ onCancelClickを呼び出して戻る
        onCancelClick()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 64.dp) // キャンセルボタンとのスペースを確保
            ) {
                // ✅ 依頼者と内容を表示するカードを追加
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Face, "依頼者")
                            Spacer(Modifier.width(8.dp))
                            Text(nickname, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Info, "内容")
                            Spacer(Modifier.width(8.dp))
                            Text(content, fontSize = 16.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 助けますボタン
                Button(
                    // ✅ onAcceptClick を呼び出す
                    onClick = onAcceptClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = CircleShape,
                    modifier = Modifier.size(140.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "助けます",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Text("助けます", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 残り時間表示カード
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(15.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("承認残り時間", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${time}秒",
                            fontSize = 28.sp,
                            color = if (time <= 10) Color.Red else Color(0xFF1976D2),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // キャンセルボタン
            OutlinedButton(
                // ✅ onCancelClick を呼び出す
                onClick = onCancelClick,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Color.DarkGray),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("キャンセル", color = Color.Black)
            }
        }
    }
}