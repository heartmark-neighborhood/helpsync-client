package com.example.helpsync.nickname_setting

import android.net.Uri // 一応残しておきますが、ダミーとして使います
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
// import androidx.compose.material.icons.filled.PhotoCamera // 不要になるのでコメントアウト
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
// import androidx.compose.ui.platform.LocalContext // imagePickerLauncher を使わないので不要
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// import androidx.activity.compose.rememberLauncherForActivityResult // 不要
// import androidx.activity.result.contract.ActivityResultContracts // 不要

@Composable
fun NicknameSetting(
    nickname: String,
    onNicknameChange: (String) -> Unit,
    onBackClick: () -> Unit = {},
    onDoneClick: () -> Unit = {}
) {
    var hasPhoto by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) } // ダミーとして保持
    // val context = LocalContext.current // 不要

    // val imagePickerLauncher = // 一時的にコメントアウト
    //     rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
    //         if (uri != null) {
    //             selectedImageUri = uri
    //             hasPhoto = true
    //         }
    //     }

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

        // 顔写真を追加セクション
        Text(
            text = "顔写真を追加",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 写真表示カード (タップで hasPhoto = true にする)
        Card(
            onClick = {
                // imagePickerLauncher.launch("image/*") // コメントアウト
                hasPhoto = true // タップしたら写真があるとみなす
                selectedImageUri = Uri.EMPTY // ダミーのUriを設定 (nullでも可)
            },
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.CenterHorizontally)
                .border(
                    2.dp,
                    if (hasPhoto) Color(0xFF4CAF50) else Color(0xFFE0E0E0), // hasPhotoに応じて枠線色変更
                    CircleShape
                ),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = if (hasPhoto) Color(0xFFE8F5E8) else Color(0xFFF5F5F5) // hasPhotoに応じて背景色変更
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // 常に写真アイコンを表示 (hasPhoto が true になった場合)
                // もし初期状態でカメラアイコンを表示したい場合は、この if 文は残してください
                // 今回は「タップしたら完了になるように」なので、タップ後の状態をデフォルトで表示するイメージ
                Icon(
                    imageVector = Icons.Default.Person, // 常に人物アイコン
                    contentDescription = "顔写真",
                    modifier = Modifier.size(60.dp),
                    tint = if (hasPhoto) Color(0xFF4CAF50) else Color(0xFF757575) // hasPhotoに応じて色変更
                )
                // 以下の Column は、hasPhoto が false の場合の表示なので、
                // タップしたら常に hasPhoto = true になる今回の変更では、
                // 初期表示以外では見えなくなります。初期表示でカメラアイコンが不要なら削除してもOKです。
                if (!hasPhoto) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Icon( // PhotoCamera アイコンは Person アイコンで代替するためコメントアウトしても良い
                        //     imageVector = Icons.Filled.PhotoCamera,
                        //     contentDescription = "カメラ",
                        //     modifier = Modifier.size(40.dp),
                        //     tint = Color(0xFF757575)
                        // )
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

        // ニックネーム入力
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

        // 完了ボタン
        Button(
            onClick = onDoneClick,
            enabled = hasPhoto && nickname.isNotBlank(), // hasPhoto が true になれば有効になる
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