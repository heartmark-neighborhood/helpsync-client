package com.example.helpsync.supporter_details_screen

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.helpsync.viewmodel.UserViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun SupporterDetailsScreen(
    requestId: String?,
    viewModel: UserViewModel = koinViewModel(),
    onDoneClick: () -> Unit
) {
    // UserViewModelからサポーター詳細情報をMap形式で受け取る
    val supporterDetailsMap by viewModel.supporterDetailsJson.collectAsState()

    Log.d("SupporterDetailsScreen", "🎨 Screen composing - requestId: $requestId")

    // requestIdが変更されたら、ViewModel経由で詳細を読み込む
    LaunchedEffect(requestId) {
        if (!requestId.isNullOrBlank()) {
            Log.d("SupporterDetailsScreen", "📥 Loading supporter details for requestId: $requestId")
            viewModel.loadSupporterDetails(requestId)
        }
    }

    // 画面から離れるときにデータをクリアする
    DisposableEffect(Unit) {
        onDispose {
            Log.d("SupporterDetailsScreen", "🧹 Clearing supporter details.")
            viewModel.clearSupporterDetails()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val dataMap = supporterDetailsMap
        if (dataMap != null) {
            Log.d("SupporterDetailsScreen", "=== Starting data extraction ===")
            
            // データクラスをSupporter用に定義
            data class SupporterData(
                val nickname: String,
                val iconUrl: String,
                val physicalDescription: String
            )
            
            // Mapから直接データを取得
            val supporterData = SupporterData(
                nickname = dataMap["nickname"] ?: "ニックネーム不明",
                iconUrl = dataMap["iconUrl"] ?: "",
                physicalDescription = dataMap["physicalFeatures"] ?: "追加情報なし"
            )
            
            Log.d("SupporterDetailsScreen", "=== Final data to display ===")
            Log.d("SupporterDetailsScreen", "nickname: ${supporterData.nickname}")
            Log.d("SupporterDetailsScreen", "iconUrl: ${supporterData.iconUrl}")
            Log.d("SupporterDetailsScreen", "description: ${supporterData.physicalDescription}")
            
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
                    if (supporterData.iconUrl.isNotEmpty()) {
                        AsyncImage(
                            model = supporterData.iconUrl,
                            contentDescription = "支援者のプロフィール写真",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
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
                    text = supporterData.nickname,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = supporterData.physicalDescription,
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
            Text("支援者の詳細を受信中...")
        }
    }
}
