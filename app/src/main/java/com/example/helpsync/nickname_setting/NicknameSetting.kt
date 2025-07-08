package com.example.helpsync.nickname_setting

import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NicknameSetting(
    nickname: String,
    onNicknameChange: (String) -> Unit,
    photoUri: Uri?,
    onPhotoChange: (Uri?) -> Unit,
    onBackClick: () -> Unit = {},
    onDoneClick: () -> Unit = {}
) {
    val hasPhoto = photoUri != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "戻る"
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "顔写真を追加",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            onClick = {
                onPhotoChange(Uri.EMPTY)
            },
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.CenterHorizontally)
                .border(2.dp, if (hasPhoto) Color(0xFF4CAF50) else Color(0xFFE0E0E0), CircleShape),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = if (hasPhoto) Color(0xFFE8F5E8) else Color(0xFFF5F5F5)
            )
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "顔写真",
                    modifier = Modifier.size(60.dp),
                    tint = if (hasPhoto) Color(0xFF4CAF50) else Color(0xFF757575)
                )
                if (!hasPhoto) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "タップして\n写真を追加",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF757575),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "支援者が本人確認のために使用します",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF757575),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

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

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onDoneClick,
            enabled = hasPhoto && nickname.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50),
                disabledContainerColor = Color(0xFFE0E0E0)
            )
        ) {
            Text(
                text = "完了",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!hasPhoto || nickname.isBlank()) {
            Text(
                text = "写真とニックネームの入力が必要です",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFF5722),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}