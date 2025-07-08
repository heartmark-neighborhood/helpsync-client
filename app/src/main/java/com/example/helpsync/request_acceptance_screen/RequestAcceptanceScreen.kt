package com.example.helpsync.request_acceptance_screen

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    var timeLeft by remember { mutableIntStateOf(60) }

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
                modifier = Modifier.align(Alignment.Center)
            ) {
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

                Spacer(modifier = Modifier.height(16.dp))

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
                            text = "${timeLeft}秒",
                            fontSize = 28.sp,
                            color = Color(0xFF1976D2)
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onDoneClick,
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