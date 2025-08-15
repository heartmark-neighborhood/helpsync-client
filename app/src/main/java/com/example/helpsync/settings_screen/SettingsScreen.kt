package com.example.helpsync.settings_screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.helpsync.viewmodel.UserViewModel
import java.security.MessageDigest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    nickname: String = "",
    onNicknameChange: (String) -> Unit = {},
    photoUri: Uri? = null,
    onPhotoChange: (Uri?) -> Unit = {},
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onCompleteClick: () -> Unit = {},
    onPhotoSave: (Uri) -> Unit = {},
    userViewModel: UserViewModel = viewModel()
) {
    // ローカルで状態を管理
    var localNickname by remember(nickname) { mutableStateOf(nickname) }
    var localPhotoUri by remember(photoUri) { mutableStateOf(photoUri) }
    var localPhysicalFeatures by remember { mutableStateOf("") }
    // 画像の内容ハッシュを保存（重複アップロード防止用）
    var localPhotoHash by remember { mutableStateOf<String?>(null) }
    var currentPhotoHash by remember { mutableStateOf<String?>(null) }
    
    // UserViewModelから現在のユーザー情報を取得
    val currentUser = userViewModel.currentUser
    val isLoading = userViewModel.isLoading
    val errorMessage = userViewModel.errorMessage
    
    // Contextを取得（Composable関数内でのみ可能）
    val context = LocalContext.current
    
    // ユーザーデータを初期化時に読み込む
    LaunchedEffect(Unit) {
        val firebaseUser = userViewModel.getCurrentFirebaseUser()
        if (firebaseUser != null && userViewModel.currentUser == null) {
            android.util.Log.d("SettingsScreen", "🔄 Loading user data for UID: ${firebaseUser.uid}")
            // UserViewModelの内部メソッドを使ってユーザーデータを読み込む
            // 直接的な方法がない場合は、UserRepositoryから読み込む
        }
    }
    
    // 画像のハッシュ値を計算する関数
    fun calculateImageHash(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            
            if (bytes != null) {
                val digest = MessageDigest.getInstance("SHA-256")
                val hashBytes = digest.digest(bytes)
                hashBytes.joinToString("") { "%02x".format(it) }
            } else null
        } catch (e: Exception) {
            android.util.Log.e("SettingsScreen", "Error calculating image hash: ${e.message}")
            null
        }
    }
    
    // 初回読み込み時にユーザー情報を設定
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            android.util.Log.d("SettingsScreen", "👤 Current user loaded: ${user.nickname}")
            android.util.Log.d("SettingsScreen", "📸 Existing iconUrl: ${user.iconUrl}")
            android.util.Log.d("SettingsScreen", "📝 Existing physicalFeatures: ${user.physicalFeatures}")
            
            // データが空の場合は既存のものを設定
            if (localNickname.isEmpty()) {
                localNickname = user.nickname
            }
            if (localPhysicalFeatures.isEmpty()) {
                localPhysicalFeatures = user.physicalFeatures
            }
            
            // 新しく選択された画像がない場合は、既存のiconUrlを使用
            if (localPhotoUri == null && !user.iconUrl.isNullOrEmpty()) {
                android.util.Log.d("SettingsScreen", "🔄 Using existing iconUrl for display")
            }
        }
    }
    
    // 変更があったかどうかを判定
    val originalNickname = currentUser?.nickname ?: ""
    val originalPhysicalFeatures = currentUser?.physicalFeatures ?: ""
    val hasNicknameChanges = localNickname.trim() != originalNickname.trim()
    val hasPhysicalFeaturesChanges = localPhysicalFeatures.trim() != originalPhysicalFeatures.trim()
    // 画像の変更判定：ハッシュ値による重複チェック
    val hasPhotoChanges = localPhotoUri != null && (localPhotoHash != currentPhotoHash)
    // 既存の画像があるかどうかを判定
    val hasExistingPhoto = localPhotoUri != null || !currentUser?.iconUrl.isNullOrEmpty()
    val hasAnyChanges = hasNicknameChanges || hasPhysicalFeaturesChanges || hasPhotoChanges
    
    // 完了ボタンの有効化条件：必須項目が入力されている場合（初回設定を考慮）
    val isFormValid = localNickname.trim().isNotEmpty() && 
                     localPhysicalFeatures.trim().isNotEmpty() && 
                     (hasExistingPhoto || localPhotoUri != null)
    
    // 初回設定の場合は変更がなくても完了可能、既存ユーザーの場合は変更が必要
    val isInitialSetup = currentUser?.nickname.isNullOrEmpty() || 
                        currentUser?.physicalFeatures.isNullOrEmpty() || 
                        currentUser?.iconUrl.isNullOrEmpty()
    val isButtonEnabled = isFormValid && (isInitialSetup || hasAnyChanges) && !isLoading
    
    // 画像選択のランチャー
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        android.util.Log.d("SettingsScreen", "=== 画像選択結果 ===")
        android.util.Log.d("SettingsScreen", "Selected URI: $uri")
        uri?.let { selectedUri ->
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(selectedUri)
                android.util.Log.d("SettingsScreen", "📄 MIME type: $mimeType")
                
                // ファイルサイズ取得
                val inputStream = contentResolver.openInputStream(selectedUri)
                val fileSize = inputStream?.available() ?: 0
                inputStream?.close()
                android.util.Log.d("SettingsScreen", "📏 File size: $fileSize bytes")
                
                // 画像ファイルの妥当性チェック
                val isValidImageMime = mimeType?.startsWith("image/") == true
                
                if (!isValidImageMime) {
                    android.util.Log.e("SettingsScreen", "❌ ERROR: Selected file is not a valid image!")
                } else {
                    android.util.Log.d("SettingsScreen", "✅ Valid image selected")
                    
                    // ハッシュ値を計算
                    val newHash = calculateImageHash(selectedUri)
                    localPhotoHash = newHash
                    android.util.Log.d("SettingsScreen", "🔒 Calculated hash: $newHash")
                    
                    // URIとハッシュを更新
                    localPhotoUri = selectedUri
                    onPhotoChange(selectedUri)
                    android.util.Log.d("SettingsScreen", "📸 Photo URI updated successfully")
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsScreen", "❌ Error analyzing selected file: ${e.message}", e)
            }
        } ?: run {
            android.util.Log.d("SettingsScreen", "❌ 画像選択がキャンセルされました")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        // トップバー
        TopAppBar(
            title = {
                Text(
                    text = "プロフィール設定",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "戻る",
                        tint = Color(0xFF2196F3)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White,
                titleContentColor = Color(0xFF212121),
                navigationIconContentColor = Color(0xFF2196F3)
            )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顔写真アップロード
            Text(
                text = "顔写真",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Card(
                onClick = { imagePickerLauncher.launch("image/*") },
                shape = CircleShape,
                modifier = Modifier
                    .size(120.dp)
                    .border(
                        2.dp,
                        Color(0xFF2196F3),
                        CircleShape
                    ),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F8FF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        localPhotoUri != null -> {
                            AsyncImage(
                                model = localPhotoUri,
                                contentDescription = "選択された写真",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        !currentUser?.iconUrl.isNullOrEmpty() -> {
                            AsyncImage(
                                model = currentUser?.iconUrl,
                                contentDescription = "既存の写真",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "写真を追加",
                                modifier = Modifier.size(48.dp),
                                tint = Color(0xFF2196F3)
                            )
                        }
                    }
                    
                    // プラスアイコンを右下に表示
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Card(
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3)),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "写真を変更",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // ニックネーム入力
            Text(
                text = "ニックネーム",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            
            OutlinedTextField(
                value = localNickname,
                onValueChange = { newValue ->
                    localNickname = newValue
                    onNicknameChange(newValue)
                },
                placeholder = { 
                    Text(
                        "ニックネームを入力してください",
                        color = Color(0xFF9E9E9E)
                    ) 
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2196F3),
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedTextColor = Color(0xFF212121),
                    unfocusedTextColor = Color(0xFF212121)
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // サポート内容入力
            Text(
                text = "身体的特徴",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            
            OutlinedTextField(
                value = localPhysicalFeatures,
                onValueChange = { localPhysicalFeatures = it },
                placeholder = { 
                    Text(
                        "身体的特徴（車椅子、白杖など）を入力してください",
                        color = Color(0xFF9E9E9E)
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2196F3),
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedTextColor = Color(0xFF212121),
                    unfocusedTextColor = Color(0xFF212121)
                ),
                maxLines = 5
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // エラーメッセージ表示
            errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = error,
                        color = Color(0xFFD32F2F),
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // 完了ボタン
            Button(
                onClick = {
                    if (isLoading) {
                        android.util.Log.d("SettingsScreen", "⚠️ Upload in progress, ignoring button click")
                        return@Button
                    }
                    
                    android.util.Log.d("SettingsScreen", "=== Complete button clicked ===")
                    android.util.Log.d("SettingsScreen", "localNickname: '$localNickname'")
                    android.util.Log.d("SettingsScreen", "localPhysicalFeatures: '$localPhysicalFeatures'")
                    android.util.Log.d("SettingsScreen", "localPhotoUri: $localPhotoUri")
                    android.util.Log.d("SettingsScreen", "Is initial setup: $isInitialSetup")
                    android.util.Log.d("SettingsScreen", "Has any changes: $hasAnyChanges")
                    
                    // 初回設定でない場合かつ変更がない場合のみスキップ
                    if (!isInitialSetup && !hasAnyChanges) {
                        android.util.Log.d("SettingsScreen", "⚠️ No changes to save")
                        return@Button
                    }
                    
                    // ニックネームの変更または初回設定の場合は保存
                    if (hasNicknameChanges || (isInitialSetup && localNickname.trim().isNotEmpty())) {
                        android.util.Log.d("SettingsScreen", "📝 Saving nickname: '$localNickname'")
                        userViewModel.updateNickname(localNickname.trim())
                        onNicknameChange(localNickname.trim())
                    }
                    
                    // 支援内容の変更または初回設定の場合は保存
                    if (hasPhysicalFeaturesChanges || (isInitialSetup && localPhysicalFeatures.trim().isNotEmpty())) {
                        android.util.Log.d("SettingsScreen", "📝 Saving physical features: '$localPhysicalFeatures'")
                        userViewModel.updatePhysicalFeatures(localPhysicalFeatures.trim())
                    }
                    
                    // 写真の変更または初回設定の場合は保存
                    if ((localPhotoUri != null && hasPhotoChanges) || (isInitialSetup && localPhotoUri != null)) {
                        android.util.Log.d("SettingsScreen", "📸 Saving profile image...")
                        try {
                            userViewModel.uploadProfileImage(localPhotoUri!!) { downloadUrl ->
                                android.util.Log.d("SettingsScreen", "✅ Image uploaded successfully: $downloadUrl")
                                if (downloadUrl.isNotEmpty()) {
                                    userViewModel.updateUserIconUrl(downloadUrl)
                                    // ハッシュを更新（重複アップロード防止）
                                    currentPhotoHash = localPhotoHash
                                }
                            }
                            onPhotoSave(localPhotoUri!!)
                        } catch (e: Exception) {
                            android.util.Log.e("SettingsScreen", "❌ Error uploading image: ${e.message}", e)
                        }
                    }
                    
                    android.util.Log.d("SettingsScreen", "✅ Settings saved successfully, staying on current screen")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isButtonEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                    disabledContainerColor = Color(0xFFE0E0E0)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
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
                        text = when {
                            !isFormValid -> "すべての項目を入力してください"
                            isInitialSetup -> "保存"
                            !hasAnyChanges -> "変更なし"
                            else -> "変更を保存"
                        },
                        color = if (isFormValid) Color.White else Color(0xFF757575),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (!isFormValid) {
                Text(
                    text = "ニックネーム、写真、身体的特徴の入力が必要です",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF5722),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
