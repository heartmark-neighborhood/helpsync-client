package com.example.helpsync.nickname_setting

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import com.example.helpsync.viewmodel.UserViewModel

@Composable
fun NicknameSetting(
    nickname: String,
    onNicknameChange: (String) -> Unit,
    photoUri: Uri?,
    onPhotoChange: (Uri?) -> Unit,
    userViewModel: UserViewModel,
    onBackClick: () -> Unit = {},
    onDoneClick: (String) -> Unit = {}
) {
    // ローカルでニックネームの状態を管理
    var localNickname by remember(nickname) { mutableStateOf(nickname) }
    var localPhotoUri by remember(photoUri) { mutableStateOf(photoUri) }
    
    // UserViewModelから現在のユーザー情報を取得
    val currentUser = userViewModel.currentUser
    
    // 既存の画像があるかどうかを判定（localPhotoUri または currentUser.iconUrl）
    val hasPhoto = localPhotoUri != null || !currentUser?.iconUrl.isNullOrEmpty()
    
    // 初回読み込み時に既存のユーザー情報を設定
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            android.util.Log.d("NicknameSetting", "👤 Current user loaded: ${user.nickname}")
            android.util.Log.d("NicknameSetting", "📸 Existing iconUrl: ${user.iconUrl}")
            
            // ニックネームが空の場合は既存のものを設定
            if (localNickname.isEmpty()) {
                localNickname = user.nickname
            }
            
            // 新しく選択された画像がない場合は、既存のiconUrlを使用
            if (localPhotoUri == null && !user.iconUrl.isNullOrEmpty()) {
                android.util.Log.d("NicknameSetting", "🔄 Using existing iconUrl for display")
            }
        }
    }

    // デバッグログを追加
    LaunchedEffect(localNickname, localPhotoUri, currentUser?.iconUrl) {
        android.util.Log.d("NicknameSetting", "=== Photo State Debug ===")
        android.util.Log.d("NicknameSetting", "Local nickname: '$localNickname'")
        android.util.Log.d("NicknameSetting", "Local photo URI: $localPhotoUri")
        android.util.Log.d("NicknameSetting", "Current user iconUrl: '${currentUser?.iconUrl}'")
        android.util.Log.d("NicknameSetting", "Has photo: $hasPhoto")
        val imageToDisplay = localPhotoUri ?: currentUser?.iconUrl
        android.util.Log.d("NicknameSetting", "Image to display: $imageToDisplay")
    }
    
    // 画像選択のランチャー
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            android.util.Log.d("NicknameSetting", "=== 画像選択結果 ===")
            android.util.Log.d("NicknameSetting", "Selected URI: $it")
            android.util.Log.d("NicknameSetting", "URI scheme: ${it.scheme}")
            android.util.Log.d("NicknameSetting", "URI path: ${it.path}")
            android.util.Log.d("NicknameSetting", "URI toString: ${it.toString()}")
            
            localPhotoUri = it
            onPhotoChange(it)
        } ?: run {
            android.util.Log.d("NicknameSetting", "画像選択がキャンセルされました")
        }
    }

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

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "支援者が本人確認のために使用します。\n下の写真エリアをタップして追加してください。",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF666666),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 写真選択エリア
        Card(
            onClick = { 
                // 画像ファイルを選択（すべての画像形式を許可）
                imagePickerLauncher.launch("image/*") 
            },
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.CenterHorizontally)
                .border(
                    3.dp, 
                    if (hasPhoto) Color(0xFF4CAF50) else Color(0xFF2196F3), 
                    CircleShape
                ),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = if (hasPhoto) Color(0xFFE8F5E8) else Color(0xFFF3F8FF)
            )
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // 表示する画像を決定（新しく選択された画像が優先、なければ既存のiconUrl）
                val imageToDisplay = localPhotoUri ?: currentUser?.iconUrl
                
                if (hasPhoto && imageToDisplay != null) {
                    AsyncImage(
                        model = imageToDisplay,
                        contentDescription = "選択された写真",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        onError = { error ->
                            android.util.Log.e("NicknameSetting", "❌ 画像読み込みエラー: ${error.result.throwable.message}")
                        },
                        onSuccess = {
                            android.util.Log.d("NicknameSetting", "✅ 画像読み込み成功: $imageToDisplay")
                        }
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "写真を追加",
                            modifier = Modifier.size(40.dp),
                            tint = Color(0xFF2196F3)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "写真を選択",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF2196F3),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "タップして追加",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 写真の状態表示
        if (hasPhoto) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "完了",
                        tint = Color(0xFF4CAF50)
                    )
                    Text(
                        text = "写真が選択されました！",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "必要",
                        tint = Color(0xFFFF9800)
                    )
                    Text(
                        text = "写真の追加が必要です",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE65100),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "ニックネーム",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "支援時に表示される名前です",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF666666),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = localNickname,
            onValueChange = { localNickname = it },
            placeholder = { Text("例: たろう、さくら") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4CAF50),
                focusedLabelColor = Color(0xFF4CAF50)
            )
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 変更の有無を判定
        val hasNicknameChanged = localNickname != nickname
        // NicknameSettingでは画像選択があれば常に「変更あり」とする（問答無用で更新）
        val hasPhotoChanged = localPhotoUri != null
        val hasAnyChanges = hasNicknameChanged || hasPhotoChanged
        
        // フォームの有効性チェック：写真（新規選択または既存）があり、ニックネームが入力されている
        val isFormValid = hasPhoto && localNickname.isNotBlank()
        
        // 完了ボタンの有効化条件：写真が存在（新規選択または既存のiconUrl）してニックネームが入力されている場合
        val isCompletionReady = hasPhoto && localNickname.isNotBlank()
        
        // デバッグログを追加
        LaunchedEffect(isCompletionReady, hasPhoto, localNickname, hasPhotoChanged) {
            android.util.Log.d("NicknameSetting", "=== Completion Ready Check ===")
            android.util.Log.d("NicknameSetting", "Has photo (new or existing): $hasPhoto")
            android.util.Log.d("NicknameSetting", "Local nickname not blank: ${localNickname.isNotBlank()}")
            android.util.Log.d("NicknameSetting", "Has photo changed (new image selected): $hasPhotoChanged")
            android.util.Log.d("NicknameSetting", "Is completion ready: $isCompletionReady")
            android.util.Log.d("NicknameSetting", "Local photo URI: $localPhotoUri")
            android.util.Log.d("NicknameSetting", "Existing iconUrl: ${currentUser?.iconUrl}")
            android.util.Log.d("NicknameSetting", "Original photo URI parameter: $photoUri")
        }
        
        // 保存ボタン（統一版）
        Button(
            onClick = {
                android.util.Log.d("NicknameSetting", "=== 保存ボタン clicked ===")
                android.util.Log.d("NicknameSetting", "hasAnyChanges: $hasAnyChanges")
                android.util.Log.d("NicknameSetting", "isFormValid: $isFormValid")
                android.util.Log.d("NicknameSetting", "isCompletionReady: $isCompletionReady")
                android.util.Log.d("NicknameSetting", "hasNicknameChanged: $hasNicknameChanged")
                android.util.Log.d("NicknameSetting", "hasPhotoChanged: $hasPhotoChanged")
                
                if (!isCompletionReady) {
                    android.util.Log.d("NicknameSetting", "準備ができていないため、保存をスキップ")
                    return@Button
                }
                
                // ニックネーム変更の保存（変更があった場合のみ）
                if (hasNicknameChanged) {
                    android.util.Log.d("NicknameSetting", "ニックネームを更新中: $localNickname")
                    userViewModel.updateNickname(localNickname)
                    onNicknameChange(localNickname)
                }
                
                // 写真変更の保存（画像がある場合は必ずアップロード）
                if (hasPhoto) {
                    // 新しく選択された画像を優先、なければ既存の画像でも処理継続
                    val imageToUpload = localPhotoUri
                    if (imageToUpload != null) {
                        android.util.Log.d("NicknameSetting", "新しく選択された画像をアップロード中: $imageToUpload")
                        // 画像をアップロードしてiconUrlを取得・保存
                        userViewModel.uploadProfileImage(imageToUpload) { downloadUrl ->
                            android.util.Log.d("NicknameSetting", "✅ 画像アップロード完了: $downloadUrl")
                            if (downloadUrl.isNotEmpty()) {
                                android.util.Log.d("NicknameSetting", "💾 iconUrlをデータベースに保存中...")
                                userViewModel.updateUserIconUrl(downloadUrl)
                            } else {
                                android.util.Log.e("NicknameSetting", "❌ ダウンロードURLが空です")
                            }
                        }
                        onPhotoChange(imageToUpload)
                    } else {
                        android.util.Log.d("NicknameSetting", "既存の画像を使用します: ${currentUser?.iconUrl}")
                        // 既存の画像がある場合は、そのまま処理を継続
                    }
                } else {
                    android.util.Log.d("NicknameSetting", "画像がありません（これは通常発生しないはずです）")
                }
                
                onDoneClick(localNickname)
            },
            enabled = isCompletionReady && !userViewModel.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isCompletionReady) Color(0xFF4CAF50) else Color(0xFFE0E0E0),
                disabledContainerColor = Color(0xFFE0E0E0)
            )
        ) {
            if (userViewModel.isLoading) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "保存中...",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Text(
                    text = if (isCompletionReady) "完了" else "入力が必要",
                    color = if (isCompletionReady) Color.White else Color(0xFF757575),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // エラーメッセージの表示
        userViewModel.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFC62828),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!isFormValid) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Text(
                    text = if (!hasPhoto && localNickname.isBlank()) {
                        "写真とニックネームの入力が必要です"
                    } else if (!hasPhoto) {
                        "写真の追加が必要です"
                    } else {
                        "ニックネームの入力が必要です"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFC62828),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                )
            }
        }
    }
}