package com.example.helpsync.supporter_setting_screen

import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun SupporterSettingsScreen(
    nickname: String,
    onNicknameChange: (String) -> Unit,
    photoUri: Uri?,
    onPhotoChange: (Uri?) -> Unit,
    modifier: Modifier = Modifier,
    onEditClick: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    var notificationsEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "戻る"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "顔写真",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            onClick = { onPhotoChange(null) },
            shape = CircleShape,
            modifier = Modifier
                .size(120.dp)
                .border(
                    2.dp,
                    Color(0xFF4CAF50),
                    CircleShape
                ),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "顔写真",
                    modifier = Modifier.size(60.dp),
                    tint = Color(0xFF4CAF50)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "ユーザー",
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "ニックネーム",
                fontSize = 18.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = nickname,
            onValueChange = onNicknameChange,
            placeholder = { Text("ニックネームを入力") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("通知を受け取る", fontSize = 14.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = notificationsEnabled,
                onCheckedChange = { notificationsEnabled = it }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onEditClick,
            modifier = Modifier.align(Alignment.End),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
            enabled = nickname.isNotBlank()
        ) {
            Text("保存")
        }
    }
}