package com.example.helpsync.help_mark_holder_matching_complete_screen

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.helpsync.SupporterInfo // ★Importが必要です
import com.example.helpsync.viewmodel.HelpMarkHolderViewModel
import com.example.helpsync.viewmodel.UserViewModel

@Composable
fun HelpMarkHolderMatchingCompleteScreen(
    requestId: String,
    navSupporterInfo: SupporterInfo?, // ★追加: MainActivityから渡される情報
    userViewModel: UserViewModel,
    helpMarkHolderViewModel: HelpMarkHolderViewModel,
    onHomeClick: () -> Unit = {}
) {
    // ViewModelからのデータ（バックアップ用）
    val vmSupporterProfile by userViewModel.supporterProfile.collectAsState()

    // ★修正: 「渡された情報」を優先し、なければViewModelの情報を使う
    val displayNickname = navSupporterInfo?.nickname ?: vmSupporterProfile?.nickname
    val displayIconUrl = navSupporterInfo?.iconUrl ?: vmSupporterProfile?.iconUrl

    val scaleAnimation = remember { Animatable(0f) }

    // 画面が表示されたときにアニメーション開始
    LaunchedEffect(Unit) {
        scaleAnimation.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    // 念のためバックグラウンドで詳細データも取りに行く（身体的特徴などのため）
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
            .background(Color(0xFFE8F5E8))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ★修正: ニックネームさえ分かれば表示する（nullチェックを変更）
        if (displayNickname == null) {
            // データ読み込み中はローディング表示
            CircularProgressIndicator()
            Text("サポーター情報を読み込み中...", modifier = Modifier.padding(top = 16.dp))
        } else {
            // データ読み込み完了後のUI
            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .scale(scaleAnimation.value),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "マッチング完了",
                    modifier = Modifier.size(60.dp),
                    tint = Color(0xFF4CAF50)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "マッチング完了！",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "支援者が見つかりました",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF757575),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0E0E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        // ★修正: displayIconUrlを使用
                        if (!displayIconUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = displayIconUrl,
                                contentDescription = "支援者プロフィール写真",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "支援者",
                                modifier = Modifier.size(30.dp),
                                tint = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ★修正: displayNicknameを使用
                    Text(
                        text = displayNickname,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )

                    // 身体的特徴（ViewModelから取得できた場合のみ表示）
                    if (vmSupporterProfile?.physicalFeatures?.isNotEmpty() == true) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "特徴: ${vmSupporterProfile!!.physicalFeatures}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = {
                    helpMarkHolderViewModel.callCompleteHelp(5, "thank you!");
                    onHomeClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    "ホームに戻る",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}