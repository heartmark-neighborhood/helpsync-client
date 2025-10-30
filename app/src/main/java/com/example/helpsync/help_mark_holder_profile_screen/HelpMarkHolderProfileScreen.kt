package com.example.helpsync.help_mark_holder_profile_screen

import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Warning
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
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi
import coil.compose.AsyncImage
import com.example.helpsync.location_worker.LocationWorker
import com.example.helpsync.viewmodel.UserViewModel
import org.koin.androidx.compose.koinViewModel
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpMarkHolderProfileScreen(
    nickname: String = "",
    onNicknameChange: (String) -> Unit = {},
    photoUri: Uri? = null,
    onPhotoChange: (Uri?) -> Unit = {},
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onCompleteClick: () -> Unit = {},
    onPhotoSave: (Uri) -> Unit = {},
    onSignOut: () -> Unit = {},
    userViewModel: UserViewModel = koinViewModel()
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
    val firebaseUser = userViewModel.getCurrentFirebaseUser()
    
    // Contextを取得（Composable関数内でのみ可能）
    val context = LocalContext.current
    
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
            Log.e("HelpMarkHolderProfileScreen", "Error calculating image hash: ${e.message}")
            null
        }
    }
    
    // 初回読み込み時にユーザー情報を設定
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            Log.d("HelpMarkHolderProfileScreen", "👤 Current user loaded: ${user.nickname}")
            Log.d("HelpMarkHolderProfileScreen", "📸 Existing iconUrl: ${user.iconUrl}")
            Log.d("HelpMarkHolderProfileScreen", "📝 Existing physicalFeatures: ${user.physicalFeatures}")
            
            // データが空の場合は既存のものを設定
            if (localNickname.isEmpty()) {
                localNickname = user.nickname
            }
            if (localPhysicalFeatures.isEmpty()) {
                localPhysicalFeatures = user.physicalFeatures
            }
            
            // 新しく選択された画像がない場合は、既存のiconUrlを使用
            if (localPhotoUri == null && user.iconUrl.isNotEmpty()) {
                Log.d("HelpMarkHolderProfileScreen", "🔄 Using existing iconUrl for display")
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
    // 初期設定画面なので、フォームが有効なら常にボタンを有効にする
    val isButtonEnabled = isFormValid && !isLoading
    
    // デバッグログを追加
    LaunchedEffect(localNickname, localPhysicalFeatures, localPhotoUri, currentUser) {
        Log.d("HelpMarkHolderProfileScreen", "=== Button State Debug ===")
        Log.d("HelpMarkHolderProfileScreen", "localNickname: '$localNickname'")
        Log.d("HelpMarkHolderProfileScreen", "localPhysicalFeatures: '$localPhysicalFeatures'")
        Log.d("HelpMarkHolderProfileScreen", "localPhotoUri: $localPhotoUri")
        Log.d("HelpMarkHolderProfileScreen", "hasExistingPhoto: $hasExistingPhoto")
        Log.d("HelpMarkHolderProfileScreen", "isFormValid: $isFormValid")
        Log.d("HelpMarkHolderProfileScreen", "isInitialSetup: $isInitialSetup")
        Log.d("HelpMarkHolderProfileScreen", "hasAnyChanges: $hasAnyChanges")
        Log.d("HelpMarkHolderProfileScreen", "isButtonEnabled: $isButtonEnabled")
        Log.d("HelpMarkHolderProfileScreen", "Current user nickname: '${currentUser?.nickname}'")
        Log.d("HelpMarkHolderProfileScreen", "Current user physicalFeatures: '${currentUser?.physicalFeatures}'")
        Log.d("HelpMarkHolderProfileScreen", "Current user iconUrl: '${currentUser?.iconUrl}'")
    }
    
    // 画像選択のランチャー
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        Log.d("HelpMarkHolderProfileScreen", "=== 画像選択結果 ===")
        Log.d("HelpMarkHolderProfileScreen", "Selected URI: $uri")
        uri?.let { selectedUri ->
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(selectedUri)
                Log.d("HelpMarkHolderProfileScreen", "📄 MIME type: $mimeType")
                
                // ファイルサイズ取得
                val inputStream = contentResolver.openInputStream(selectedUri)
                val fileSize = inputStream?.available() ?: 0
                inputStream?.close()
                Log.d("HelpMarkHolderProfileScreen", "📏 File size: $fileSize bytes")
                
                // ファイル内容の最初の部分を読んで分析
                val previewStream = contentResolver.openInputStream(selectedUri)
                val buffer = ByteArray(64)
                val bytesRead = previewStream?.read(buffer) ?: 0
                previewStream?.close()
                
                val hexString = buffer.take(bytesRead).joinToString(" ") { 
                    String.format("%02X", it) 
                }
                Log.d("HelpMarkHolderProfileScreen", "🔍 File header (first $bytesRead bytes): $hexString")
                
                // ファイル種別を内容から推測
                val fileTypeFromContent = when {
                    buffer.size >= 2 && buffer[0] == 0xFF.toByte() && buffer[1] == 0xD8.toByte() -> "JPEG"
                    buffer.size >= 8 && buffer[1] == 'P'.code.toByte() && buffer[2] == 'N'.code.toByte() && buffer[3] == 'G'.code.toByte() -> "PNG"
                    buffer.size >= 12 && buffer[8] == 'W'.code.toByte() && buffer[9] == 'E'.code.toByte() && buffer[10] == 'B'.code.toByte() && buffer[11] == 'P'.code.toByte() -> "WEBP"
                    else -> "UNKNOWN"
                }
                Log.d("HelpMarkHolderProfileScreen", "🎯 Content-based file type: $fileTypeFromContent")
                
                // 画像ファイルの妥当性チェック
                val isValidImageMime = mimeType?.startsWith("image/") == true
                val isValidImageContent = fileTypeFromContent != "UNKNOWN"
                
                if (!isValidImageMime && !isValidImageContent) {
                    Log.e("HelpMarkHolderProfileScreen", "❌ ERROR: Selected file is not a valid image!")
                } else {
                    Log.d("HelpMarkHolderProfileScreen", "✅ Valid image file selected")
                    localPhotoUri = selectedUri
                    
                    // 画像のハッシュ値を計算（重複チェック用）
                    Log.d("HelpMarkHolderProfileScreen", "🔍 Calculating image hash for duplicate detection...")
                    localPhotoHash = calculateImageHash(selectedUri)
                    Log.d("HelpMarkHolderProfileScreen", "📝 Image hash: $localPhotoHash")
                    
                    onPhotoChange(selectedUri)
                }
            } catch (e: Exception) {
                Log.e("HelpMarkHolderProfileScreen", "❌ Error analyzing selected file: ${e.message}", e)
            }
        } ?: run {
            Log.d("HelpMarkHolderProfileScreen", "❌ 画像選択がキャンセルされました")
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
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 認証状態のチェック
            if (firebaseUser == null) {
                // 認証されていない場合の警告表示
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚠️ 認証エラー",
                            color = Color(0xFFD32F2F),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ユーザーが認証されていません。サインインし直してください。",
                            color = Color(0xFFD32F2F),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "プロフィール写真",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "写真をタップして変更できます",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // 画像選択エリア - 直接タップで選択
            Card(
                onClick = {
                    Log.d("HelpMarkHolderProfileScreen", "🖼️ Image area clicked!")
                    imagePickerLauncher.launch("image/*")
                },
                shape = CircleShape,
                modifier = Modifier
                    .size(160.dp)
                    .border(
                        4.dp,
                        when {
                            hasPhotoChanges -> Color(0xFF4CAF50) // 新しい画像が選択された
                            hasExistingPhoto -> Color(0xFF2196F3) // 既存の画像がある
                            else -> Color(0xFFE0E0E0) // 画像がない
                        },
                        CircleShape
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        hasPhotoChanges -> Color(0xFFE8F5E8) // 薄い緑
                        hasExistingPhoto -> Color(0xFFF3F8FF) // 薄い青
                        else -> Color(0xFFFAFAFA) // 薄いグレー
                    }
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // 表示する画像を決定
                    val imageToDisplay = localPhotoUri ?: currentUser?.iconUrl

                    if (hasExistingPhoto && imageToDisplay != null) {
                        AsyncImage(
                            model = imageToDisplay,
                            contentDescription = "プロフィール写真",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // 画像がない場合のUI
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "プロフィール写真なし",
                                modifier = Modifier.size(64.dp),
                                tint = Color(0xFFBDBDBD)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "写真を追加",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF2196F3),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "タップして選択",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF757575),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "プロフィール写真を設定すると\n信頼性が向上します",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9E9E9E),
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 写真の状態表示
            if (hasExistingPhoto) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "写真状態",
                            tint = Color(0xFF4CAF50)
                        )
                        Text(
                            text = when {
                                localPhotoUri != null && hasPhotoChanges -> "新しい写真が選択されました（既存の画像は削除されます）"
                                localPhotoUri != null && !hasPhotoChanges -> "同じ写真が再選択されました（変更なし）"
                                !currentUser?.iconUrl.isNullOrEmpty() -> "既存の写真が設定済みです"
                                else -> "写真が設定済みです"
                            },
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
                            imageVector = Icons.Default.Person,
                            contentDescription = "写真なし",
                            tint = Color(0xFFFF9800)
                        )
                        Column(
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "プロフィール写真が未設定です",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "写真を追加すると信頼性が向上します",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ニックネーム入力セクション
            Text(
                text = "ニックネーム",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = localNickname,
                onValueChange = { localNickname = it },
                placeholder = { Text("例: たろう、さくら") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (hasNicknameChanges) Color(0xFF4CAF50) else Color(
                        0xFF2196F3
                    ),
                    unfocusedBorderColor = if (hasNicknameChanges) Color(0xFF81C784) else MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 支援内容セクション
            Text(
                text = "身体的特徴",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = localPhysicalFeatures,
                onValueChange = { localPhysicalFeatures = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = {
                    Text(
                        text = "例：車椅子を使用\n例：白杖を使用\n例：聴覚に障害あり",
                        color = Color(0xFF757575)
                    )
                },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (hasPhysicalFeaturesChanges) Color(0xFF4CAF50) else Color(
                        0xFF2196F3
                    ),
                    unfocusedBorderColor = if (hasPhysicalFeaturesChanges) Color(0xFF81C784) else Color(
                        0xFFE0E0E0
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "身体的特徴を記入することで、適切な支援者とマッチングしやすくなります",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF757575),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(48.dp))

            var backgroundLocationEnabled by remember { mutableStateOf(false) }
            val fineLocationPermission = Manifest.permission.ACCESS_FINE_LOCATION
            val backgroundLocationPermission = Manifest.permission.ACCESS_BACKGROUND_LOCATION

            fun checkPermissions() {
                val fineLocationGranted = ContextCompat.checkSelfPermission(
                    context,
                    fineLocationPermission
                ) == PackageManager.PERMISSION_GRANTED
                val backgroundLocationGranted = ContextCompat.checkSelfPermission(
                    context,
                    backgroundLocationPermission
                ) == PackageManager.PERMISSION_GRANTED
                backgroundLocationEnabled = fineLocationGranted && backgroundLocationGranted
                Log.d(
                    "HelpMarkHolderProfileScreen",
                    "Permissions checked: fineLocation=$fineLocationGranted, backgroundLocation=$backgroundLocationGranted"
                )
            }

            LaunchedEffect(Unit) {
                checkPermissions()
            }

            val backgroundLocationLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { isGranted ->
                    backgroundLocationEnabled = isGranted
                    if (isGranted) {
                        Log.d(
                            "HelpMarkHolderProfileScreen",
                            "Background location permission GRANTED"
                        )
                    } else {
                        Log.e(
                            "HelpMarkHolderProfileScreen",
                            "Background location permission DENIED"
                        )
                    }
                }
            )

            val fineLocationLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { isGranted ->
                    if (isGranted) {
                        Log.d(
                            "HelpMarkHolderProfileScreen",
                            "Fine location permission GRANTED, requesting background location..."
                        )
                        backgroundLocationLauncher.launch(backgroundLocationPermission)
                    } else {
                        Log.e("HelpMarkHolderProfileScreen", "Fine location permission DENIED")
                        backgroundLocationEnabled = false
                    }
                }
            )

            LaunchedEffect(backgroundLocationEnabled) {
                val workManager = WorkManager.getInstance(context)
                if (backgroundLocationEnabled) {
                    Log.d(
                        "HelpMarkHolderProfileScreen",
                        "バックグラウンドでの位置情報が有効になりました。WorkManagerのタスクを開始します。"
                    )
                    val constraints = Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()

                    Log.d(
                        "HelpMarkHolderProfileScreen",
                        "Building PeriodicWorkRequest for LocationWorker."
                    )
                    val periodicWorkRequest =
                        PeriodicWorkRequestBuilder<LocationWorker>(15, TimeUnit.MINUTES)
                            .setConstraints(constraints)
                            .build()
                    workManager.enqueueUniquePeriodicWork(
                        LocationWorker.WORK_NAME,
                        ExistingPeriodicWorkPolicy.UPDATE, // 既存のタスクがあれば何もしない
                        periodicWorkRequest
                    )
                    Log.d(
                        "HelpMarkHolderProfileScreen",
                        "WorkManager task (${LocationWorker.WORK_NAME}) enqueued with policy UPDATE."
                    )
                } else {
                    Log.d(
                        "HelpMarkHolderProfileScreen",
                        "Background location disabled. Cancelling WorkManager task."
                    )
                    workManager.cancelUniqueWork(LocationWorker.WORK_NAME)
                    Log.d(
                        "HelpMarkHolderProfileScreen",
                        "WorkManager task (${LocationWorker.WORK_NAME}) cancelled."
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "バックグラウンドでの位置情報",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Status Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusIcon =
                        if (backgroundLocationEnabled) Icons.Default.Check else Icons.Default.Warning
                    val iconTint =
                        if (backgroundLocationEnabled) Color(0xFF4CAF50) else Color(0xFFFFA000)
                    Icon(imageVector = statusIcon, contentDescription = "Status", tint = iconTint)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (backgroundLocationEnabled) "許可されています" else "許可されていません",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "アプリがバックグラウンドにあるときでも、助けを必要としている人を見つけるために使用されます。この機能を有効にするには、位置情報の権限を「常に許可」に設定する必要があります。",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Button
                Button(
                    onClick = {
                        if (backgroundLocationEnabled) {
                            // Open App Settings
                            Log.d("HelpMarkHolderProfileScreen", "Opening app settings...")
                            val intent =
                                android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", context.packageName, null)
                            intent.data = uri
                            context.startActivity(intent)
                        } else {
                            // Request Permissions
                            Log.d(
                                "HelpMarkHolderProfileScreen",
                                "Requesting background location permission..."
                            )
                            val fineLocationGranted = ContextCompat.checkSelfPermission(
                                context,
                                fineLocationPermission
                            ) == PackageManager.PERMISSION_GRANTED
                            if (fineLocationGranted) {
                                backgroundLocationLauncher.launch(backgroundLocationPermission)
                            } else {
                                fineLocationLauncher.launch(fineLocationPermission)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (backgroundLocationEnabled) "設定を開く" else "権限を許可する")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
              // 完了ボタン
            Button(
                onClick = { 
                    if (isLoading) return@Button

                    Log.d("HelpMarkHolderProfileScreen", "Complete button clicked")
                    userViewModel.saveProfileChanges(
                        nickname = localNickname.trim(),
                        physicalFeatures = localPhysicalFeatures.trim(),
                        imageUri = localPhotoUri
                    ) {
                        // This callback is executed after the save operation is complete.
                        Log.d("HelpMarkHolderProfileScreen", "Save operation completed, navigating back.")
                        onCompleteClick()
                    }
                },
                enabled = isButtonEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFormValid) Color(0xFF4CAF50) else Color(0xFFE0E0E0),
                    disabledContainerColor = Color(0xFFE0E0E0)
                )
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
                            else -> "完了"
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
            
            // エラーメッセージの表示
            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Text(
                        text = error,
                        color = Color(0xFFD32F2F),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            
            if (firebaseUser != null) {
                Spacer(modifier = Modifier.height(32.dp))

                OutlinedButton(
                    onClick = {
                        Log.d("HelpMarkHolderProfileScreen", "Sign out button clicked")
                        userViewModel.signOut()
                        onSignOut()
                    },
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFD32F2F)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "サインアウト",
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
