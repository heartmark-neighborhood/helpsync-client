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
import coil.compose.AsyncImage
import com.example.helpsync.viewmodel.SupporterViewModel
import org.json.JSONObject
import org.koin.androidx.compose.koinViewModel

@Composable
fun RequestAcceptanceScreen(
    viewModel: SupporterViewModel = koinViewModel(),
    onDoneClick: () -> Unit
) {
    val helpRequestJson by viewModel.helpRequestJson.collectAsState()

    Log.d("RequestAcceptanceScreen", "🎨 Screen composing - helpRequestJson is null: ${helpRequestJson == null}")
    
    LaunchedEffect(helpRequestJson) {
        if (helpRequestJson != null) {
            Log.d("RequestAcceptanceScreen", "📥 Received help request data: $helpRequestJson")
            Log.d("RequestAcceptanceScreen", "📋 Data keys: ${helpRequestJson?.keys}")
            helpRequestJson?.forEach { (key, value) ->
                Log.d("RequestAcceptanceScreen", "  - $key: $value")
            }
        } else {
            Log.d("RequestAcceptanceScreen", "⏳ Waiting for help request data...")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // スマートキャストできないので、ローカル変数に代入
        val requestData = helpRequestJson
        if (requestData != null) {
            Log.d("RequestAcceptanceScreen", "=== Starting data parse ===")
            // dataフィールドからJSON文字列を取得してパース
            val dataString = requestData["data"]
            
            Log.d("RequestAcceptanceScreen", "📝 Data string: $dataString")
            Log.d("RequestAcceptanceScreen", "📝 Data string length: ${dataString?.length}")
            Log.d("RequestAcceptanceScreen", "📝 Type field: ${requestData["type"]}")
            
            // データをパースして値を取得
            data class RequesterData(
                val nickname: String,
                val iconUrl: String,
                val physicalDescription: String
            )
            
            val requesterData = if (dataString != null) {
                try {
                    Log.d("RequestAcceptanceScreen", "🔍 Attempting to parse JSON...")
                    val dataJson = JSONObject(dataString)
                    Log.d("RequestAcceptanceScreen", "✅ JSON parsed successfully")
                    Log.d("RequestAcceptanceScreen", "📋 JSON keys: ${dataJson.keys().asSequence().toList()}")
                    
                    val requester = dataJson.getJSONObject("requester")
                    Log.d("RequestAcceptanceScreen", "✅ Requester object extracted")
                    Log.d("RequestAcceptanceScreen", "📋 Requester keys: ${requester.keys().asSequence().toList()}")
                    
                    val nickname = requester.optString("nickname", "ニックネーム不明")
                    val iconUrl = requester.optString("iconUrl", "")
                    val physicalDescription = requester.optString("physicalDescription", "追加情報なし")
                    
                    Log.d("RequestAcceptanceScreen", "✅ Parsed values:")
                    Log.d("RequestAcceptanceScreen", "  - nickname: $nickname")
                    Log.d("RequestAcceptanceScreen", "  - iconUrl: ${iconUrl.take(50)}...")
                    Log.d("RequestAcceptanceScreen", "  - physicalDescription: $physicalDescription")
                    
                    RequesterData(nickname, iconUrl, physicalDescription)
                } catch (e: Exception) {
                    Log.e("RequestAcceptanceScreen", "❌ JSON parse error: ${e.message}")
                    e.printStackTrace()
                    RequesterData("ニックネーム不明", "", "追加情報なし")
                }
            } else {
                // フォールバック: 直接キーで取得を試みる
                Log.d("RequestAcceptanceScreen", "⚠️ Using fallback data structure")
                RequesterData(
                    requestData["requesterNickname"] ?: "ニックネーム不明",
                    requestData["requesterIconUrl"] ?: "",
                    requestData["requesterMessage"] ?: "追加情報なし"
                )
            }
            
            Log.d("RequestAcceptanceScreen", "=== Final data to display ===")
            Log.d("RequestAcceptanceScreen", "nickname: ${requesterData.nickname}")
            Log.d("RequestAcceptanceScreen", "iconUrl: ${requesterData.iconUrl}")
            Log.d("RequestAcceptanceScreen", "description: ${requesterData.physicalDescription}")
            
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
                    if (requesterData.iconUrl.isNotEmpty()) {
                        AsyncImage(
                            model = requesterData.iconUrl,
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
                    text = requesterData.nickname,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = requesterData.physicalDescription,
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