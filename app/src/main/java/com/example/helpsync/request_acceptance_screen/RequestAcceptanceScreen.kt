package com.example.helpsync.request_acceptance_screen

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.helpsync.AppScreen
import kotlinx.coroutines.delay

@SuppressLint("DefaultLocale")
@Composable
fun RequestAcceptanceScreen(
    navController: NavController,
    onDoneClick: () -> Unit = {}
) {
    var timeLeft by remember { mutableStateOf(60) }

    // タイマー処理：0秒になったらホームへ戻る
    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
        navController.navigate(AppScreen.SupporterHome.name) {
            popUpTo(AppScreen.RequestAcceptanceScreen.name) { inclusive = true }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Timer",
                tint = Color.Black
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = String.format("%02d:%02d", timeLeft / 60, timeLeft % 60),
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("支援要請を承認しますか？", fontSize = 18.sp, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))

        // ✅ 「助けます」ボタン押下時に RequestDetail 画面へ遷移
        Button(
            onClick = {
                navController.navigate(AppScreen.RequestDetail.name)
            },
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

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onDoneClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
        ) {
            Text("拒否", color = Color.Black)
        }
    }
}