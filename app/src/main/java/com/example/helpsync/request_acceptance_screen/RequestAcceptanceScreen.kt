package com.example.helpsync.request_acceptance_screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.helpsync.R
import com.example.helpsync.viewmodel.SupporterViewModel

@Composable
fun RequestAcceptanceScreen(
    viewModel: SupporterViewModel = viewModel(),
    onDoneClick: () -> Unit
) {
    val helpRequestJson by viewModel.helpRequestJson.collectAsState()
    val profileData = helpRequestJson

    LaunchedEffect(profileData) {
        if (profileData != null) {
            Log.d("RequestAcceptanceScreen", "📄 Displaying profile data: $profileData")
        } else {
            Log.d("RequestAcceptanceScreen", "⏳ Waiting for profile data...")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (profileData != null) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "マッチングが成立しました！",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(32.dp))
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    val iconUrl = profileData["requesterIconUrl"]
                    if (!iconUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = iconUrl,
                            contentDescription = "要請者のプロフィール写真",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            fallback = painterResource(id = android.R.drawable.ic_menu_gallery)
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "プロフィール写真なし",
                            modifier = Modifier.size(90.dp),
                            tint = Color.Gray
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = profileData["requesterNickname"] ?: "ニックネーム不明",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = profileData["requesterMessage"] ?: "追加情報なし",
                    fontSize = 16.sp
                )
            }
            OutlinedButton(
                onClick = onDoneClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("完了", fontSize = 16.sp)
            }
        } else {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("ヘルプ要求の詳細を受信中...")
        }
    }
}