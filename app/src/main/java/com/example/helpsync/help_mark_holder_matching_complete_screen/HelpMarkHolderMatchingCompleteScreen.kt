package com.example.helpsync.help_mark_holder_matching_complete_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.helpsync.viewmodel.HelpMarkHolderViewModel
import com.example.helpsync.viewmodel.UserViewModel

@Composable
fun HelpMarkHolderMatchingCompleteScreen(
    requestId: String,
    userViewModel: UserViewModel,
    helpMarkHolderViewModel: HelpMarkHolderViewModel,
    onHomeClick: () -> Unit = {}
) {
    val supporterProfile by userViewModel.supporterProfile.collectAsState()
    val matchedRequestDetails by userViewModel.matchedRequestDetails.collectAsState()
    val errorMessage = userViewModel.errorMessage

    LaunchedEffect(requestId) {
        if (requestId.isNotBlank()) {
            userViewModel.loadMatchedRequestDetails(requestId)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            userViewModel.clearMatchedDetails()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "マッチング完了",
            modifier = Modifier.size(100.dp),
            tint = Color(0xFF4CAF50)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "マッチングが成立しました！",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "支援してくれるサポーターが見つかりました",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        when {
            errorMessage != null -> {
                Text(
                    text = "サポーター情報の取得に失敗しました",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            matchedRequestDetails == null || supporterProfile == null -> {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("サポーター情報を取得中...")
                }
            }

            else -> {
                val profile = supporterProfile!!

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(CircleShape)
                                    .background(Color.LightGray),
                                contentAlignment = Alignment.Center
                            ) {
                                if (profile.iconUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = profile.iconUrl,
                                        contentDescription = "サポータープロフィール画像",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "プロフィール画像なし",
                                        modifier = Modifier.size(72.dp),
                                        tint = Color.Gray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = if (profile.nickname.isNotBlank()) profile.nickname else "ニックネーム未設定",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (profile.physicalFeatures.isNotBlank()) {
                                    profile.physicalFeatures
                                } else {
                                    "身体的特徴の登録はありません"
                                },
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = {
                helpMarkHolderViewModel.callCompleteHelp(5, "thank you!")
                onHomeClick()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("ホームに戻る", fontSize = 16.sp)
        }
    }
}