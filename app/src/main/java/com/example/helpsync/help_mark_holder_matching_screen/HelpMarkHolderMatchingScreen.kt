package com.example.helpsync.help_mark_holder_matching_screen

import android.content.Intent
import android.widget.Toast
import com.example.helpsync.blescanner.BLEScanner

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun HelpMarkHolderMatchingScreen(
    onMatchingComplete: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val context = LocalContext.current
    var timeRemaining by remember { mutableIntStateOf(30) }
    var isMatching by remember { mutableStateOf(true) }

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

    LaunchedEffect(isMatching) {
        if (isMatching) {
            while (timeRemaining > 0 && isMatching) {
                delay(1000)
                timeRemaining--
            }
            if (timeRemaining <= 0) {
                Toast.makeText(context, "マッチングに失敗しました", Toast.LENGTH_SHORT).show()
                onCancel()
            }
        }
    }

    LaunchedEffect(Unit) {
        delay((10000..25000).random().toLong())
        if (isMatching) {
            onMatchingComplete()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val stopIntent = Intent(context, BLEScanner::class.java)
            context.stopService(stopIntent)
        }
    }

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

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.padding(horizontal = 32.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "承認残り時間",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF757575)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${timeRemaining}秒",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (timeRemaining <= 10) Color(0xFFFF5722) else Color(0xFF2196F3),
                    fontSize = 36.sp
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = {
                isMatching = false
                onCancel()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF757575)
            )
        ) {
            Text(
                text = "キャンセル",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}