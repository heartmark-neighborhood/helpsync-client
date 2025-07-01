package com.example.helpsync.support_details_confirmation_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SupportRequestDetailScreen(
    modifier: Modifier = Modifier,
    onDoneClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(100.dp))

        // ニックネームボックス
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(48.dp)
                .background(Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            Text("ニックネーム", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(80.dp))

        Box(
            modifier = Modifier
                .size(180.dp)
                .background(Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            Text("顔写真", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(80.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(100.dp)
                .background(Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            Text("支援内容", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(80.dp))

        OutlinedButton(
            onClick = onDoneClick,
            modifier = Modifier
                .defaultMinSize(minWidth = 100.dp, minHeight = 36.dp),
            shape = ButtonDefaults.outlinedShape
        ) {
            Text("×閉じる", fontSize = 14.sp)
        }
    }
}