package com.example.helpsync.support_details_confirmation_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun SupportDetailsConfirmationScreen(
    requestId: String,
    viewModel: UserViewModel,
    onDoneClick: () -> Unit = {}
) {
    val requesterProfile by viewModel.requesterProfile.collectAsState()

    // 画面が表示されたときに、指定されたrequestIdの詳細を読み込む
    LaunchedEffect(requestId) {
        viewModel.loadMatchedRequestDetails(requestId)
    }

    // 画面から離れるときにViewModelのデータをクリアする
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearMatchedDetails()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (requesterProfile == null) {
            CircularProgressIndicator()
        } else {
            // メインコンテンツ
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "以下の要請を承認しました",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 顔写真表示エリア
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    if (requesterProfile?.iconUrl?.isNotEmpty() == true) {
                        AsyncImage(
                            model = requesterProfile!!.iconUrl,
                            contentDescription = "要請者のプロフィール写真",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "プロフィール写真",
                            modifier = Modifier.size(90.dp),
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ニックネーム
                Text(
                    text = requesterProfile!!.nickname,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 身体的特徴など
                Text(
                    text = requesterProfile!!.physicalFeatures,
                    fontSize = 16.sp
                )
            }

            // 閉じるボタン
            OutlinedButton(
                onClick = onDoneClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("× 閉じる", fontSize = 16.sp)
            }
        }
    }
}