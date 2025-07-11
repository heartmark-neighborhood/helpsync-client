package com.example.helpsync.support_content_input_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportContentInputScreen(
    onBackClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    var nickname by remember { mutableStateOf("") }
    var supportContent by remember { mutableStateOf("") }
    var hasPhoto by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // トップバー
        TopAppBar(
            title = {
                Text(
                    text = "プロフィール設定",
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                }
            },
            actions = {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "設定",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF6200EE),
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // ニックネーム入力セクション
            Text(
                text = "ニックネーム",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { 
                    Text(
                        text = "あなたのニックネームを入力してください",
                        color = Color(0xFF757575)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6200EE),
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 顔写真セクション
            Text(
                text = "顔写真を追加",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 写真アップロード領域
            Card(
                onClick = { hasPhoto = !hasPhoto },
                modifier = Modifier
                    .size(120.dp)
                    .border(
                        2.dp, 
                        if (hasPhoto) Color(0xFF4CAF50) else Color(0xFFE0E0E0),
                        CircleShape
                    ),
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = if (hasPhoto) Color(0xFFE8F5E8) else Color(0xFFF5F5F5)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasPhoto) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "顔写真",
                            modifier = Modifier.size(60.dp),
                            tint = Color(0xFF4CAF50)
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "カメラ",
                                modifier = Modifier.size(40.dp),
                                tint = Color(0xFF757575)
                            )
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
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 支援内容セクション
            Text(
                text = "必要な支援内容",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 支援内容入力フィールド
            OutlinedTextField(
                value = supportContent,
                onValueChange = { supportContent = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = {
                    Text(
                        text = "例：電車で席を譲ってほしい\n例：重い荷物を持ってほしい\n例：道案内をしてほしい",
                        color = Color(0xFF757575)
                    )
                },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6200EE),
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "具体的に記入することで、適切な支援者とマッチングしやすくなります",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF757575),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // 保存ボタン
            Button(
                onClick = onSaveClick,
                enabled = hasPhoto && supportContent.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6200EE),
                    disabledContainerColor = Color(0xFFE0E0E0)
                )
            ) {
                Text(
                    text = "保存",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (!hasPhoto || supportContent.isBlank()) {
                Text(
                    text = "写真と支援内容の入力が必要です",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF5722),
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
