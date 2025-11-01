package com.example.helpsync.supporter_setting_screen

import android.annotation.SuppressLint
import android.net.Uri
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.compose.AsyncImage
import com.example.helpsync.location_worker.LocationWorker
import com.example.helpsync.viewmodel.UserViewModel
import com.example.helpsync.viewmodel.DeviceManagementVewModel
import org.koin.androidx.compose.koinViewModel
import java.security.MessageDigest
import java.util.concurrent.TimeUnit


@Composable
fun SupporterSettingScreen(
    nickname: String = "",
    onNicknameChange: (String) -> Unit = {},
    photoUri: Uri? = null,
    onPhotoChange: (Uri?) -> Unit = {},
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    onEditClick: (String) -> Unit = {},
    onPhotoSave: (Uri) -> Unit = {},
    userViewModel: UserViewModel, // デフォルト値を削除して必須パラメータに
    deviceViewModel: DeviceManagementVewModel = koinViewModel(),
    onSignOut: () -> Unit = {}
) {
    // ローカルでニックネームと写真の状態を管理
    var localNickname by remember(nickname) { mutableStateOf(nickname) }
    var localPhotoUri by remember(photoUri) { mutableStateOf(photoUri) }
    // 画像の内容ハッシュを保存（重複アップロード防止用）
    var localPhotoHash by remember { mutableStateOf<String?>(null) }
    var currentPhotoHash by remember { mutableStateOf<String?>(null) }
    // 初期状態の写真URIを保持（変更検出用）
    val initialPhotoUri by remember(photoUri) { mutableStateOf(photoUri) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    
    // UserViewModelから現在のユーザー情報を取得
    val currentUser = userViewModel.currentUser
    val isLoading = userViewModel.isLoading
    val errorMessage = userViewModel.errorMessage
    
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
            Log.e("SupporterSettingScreen", "Error calculating image hash: ${e.message}")
            null
        }
    }
    
    // 初回読み込み時にユーザー情報を設定
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            Log.d("SupporterSettingScreen", "👤 Current user loaded: ${user.nickname}")
            Log.d("SupporterSettingScreen", "📸 Existing iconUrl: ${user.iconUrl}")
            
            // ニックネームが空の場合は既存のものを設定
            if (localNickname.isEmpty()) {
                localNickname = user.nickname
            }
            
            // 新しく選択された画像がない場合は、既存のiconUrlを使用
            if (localPhotoUri == null && user.iconUrl.isNotEmpty()) {
                Log.d("SupporterSettingScreen", "🔄 Using existing iconUrl for display")
            }
            
            // 既存画像のハッシュ値を計算（UserViewModelに保存されているか確認）
            // 注意: Firebase URLから直接ハッシュを計算するのは困難なので、
            // 別途ハッシュ値をデータベースに保存する仕組みが必要
            if (user.iconUrl.isNotEmpty() && currentPhotoHash == null) {
                // 現在は既存画像のハッシュ値を取得する方法がないため、
                // 新しい画像が選択された場合のみハッシュ比較を行う
                Log.d("SupporterSettingScreen", "既存画像のハッシュ値取得はスキップ（Firebase URLから直接計算不可）")
            }
        }
    }
    
    // 変更があったかどうかを判定（修正版）
    val originalNickname = currentUser?.nickname ?: ""
    val hasNicknameChanges = localNickname.trim() != originalNickname.trim()
    // 画像の変更判定：ハッシュ値による重複チェック
    val hasPhotoChanges = localPhotoUri != null && (localPhotoHash != currentPhotoHash)
    // 既存の画像があるかどうかを判定（localPhotoUri または currentUser.iconUrl）
    val hasExistingPhoto = localPhotoUri != null || !currentUser?.iconUrl.isNullOrEmpty()
    val hasAnyChanges = hasNicknameChanges || hasPhotoChanges
    
    // 完了ボタンの有効化条件：変更がある場合のみ（修正版）
    val isButtonEnabled = hasAnyChanges && !isLoading
    
    // デバッグログを追加
    LaunchedEffect(localNickname, localPhotoUri, currentUser?.iconUrl) {
        Log.d("SupporterSettingScreen", "=== Change Detection Debug ===")
        Log.d("SupporterSettingScreen", "Parameter nickname: '$nickname'")
        Log.d("SupporterSettingScreen", "Current user nickname: '$originalNickname'")
        Log.d("SupporterSettingScreen", "Local nickname: '$localNickname'")
        Log.d("SupporterSettingScreen", "Local nickname trimmed: '${localNickname.trim()}'")
        Log.d("SupporterSettingScreen", "Initial photo URI (parameter): $photoUri")
        Log.d("SupporterSettingScreen", "Local photo URI (selected): $localPhotoUri")
        Log.d("SupporterSettingScreen", "Current user iconUrl: '${currentUser?.iconUrl}'")
        Log.d("SupporterSettingScreen", "Local photo hash: $localPhotoHash")
        Log.d("SupporterSettingScreen", "Current photo hash: $currentPhotoHash")
        Log.d("SupporterSettingScreen", "Hash comparison: ${localPhotoHash != currentPhotoHash}")
        Log.d("SupporterSettingScreen", "Photo selected: ${localPhotoUri != null}")
        Log.d("SupporterSettingScreen", "IconUrl is empty: ${currentUser?.iconUrl.isNullOrEmpty()}")
        Log.d("SupporterSettingScreen", "Has nickname changes: $hasNicknameChanges")
        Log.d("SupporterSettingScreen", "Has photo changes: $hasPhotoChanges")
        Log.d("SupporterSettingScreen", "Has existing photo: $hasExistingPhoto")
        Log.d("SupporterSettingScreen", "Has any changes: $hasAnyChanges")
        Log.d("SupporterSettingScreen", "Button enabled: $isButtonEnabled")
        val imageToDisplay = localPhotoUri ?: currentUser?.iconUrl
        Log.d("SupporterSettingScreen", "Image to display: $imageToDisplay")
    }
    
    // 画像選択のランチャー
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        Log.d("SupporterSettingScreen", "=== 画像選択結果 ===")
        Log.d("SupporterSettingScreen", "Selected URI: $uri")
        uri?.let { selectedUri ->
            Log.d("SupporterSettingScreen", "URI scheme: ${selectedUri.scheme}")
            Log.d("SupporterSettingScreen", "URI path: ${selectedUri.path}")
            Log.d("SupporterSettingScreen", "URI toString: ${selectedUri.toString()}")
            Log.d("SupporterSettingScreen", "URI authority: ${selectedUri.authority}")
            
            try {
                // ファイルの詳細分析
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(selectedUri)
                Log.d("SupporterSettingScreen", "📄 MIME type: $mimeType")
                
                // ファイルサイズ取得
                val inputStream = contentResolver.openInputStream(selectedUri)
                val fileSize = inputStream?.available() ?: 0
                inputStream?.close()
                Log.d("SupporterSettingScreen", "📏 File size: $fileSize bytes")
                
                // ファイル内容の最初の部分を読んで分析
                val previewStream = contentResolver.openInputStream(selectedUri)
                val buffer = ByteArray(64)
                val bytesRead = previewStream?.read(buffer) ?: 0
                previewStream?.close()
                
                val hexString = buffer.take(bytesRead).joinToString(" ") { 
                    String.format("%02X", it) 
                }
                Log.d("SupporterSettingScreen", "🔍 File header (first $bytesRead bytes): $hexString")
                
                // ファイル種別を内容から推測
                val fileTypeFromContent = when {
                    buffer.size >= 2 && buffer[0] == 0xFF.toByte() && buffer[1] == 0xD8.toByte() -> "JPEG"
                    buffer.size >= 8 && buffer[1] == 'P'.code.toByte() && buffer[2] == 'N'.code.toByte() && buffer[3] == 'G'.code.toByte() -> "PNG"
                    buffer.size >= 12 && buffer[8] == 'W'.code.toByte() && buffer[9] == 'E'.code.toByte() && buffer[10] == 'B'.code.toByte() && buffer[11] == 'P'.code.toByte() -> "WEBP"
                    else -> "UNKNOWN"
                }
                Log.d("SupporterSettingScreen", "🎯 Content-based file type: $fileTypeFromContent")
                
                // テキストファイルかどうかの判定
                val isLikelyText = buffer.take(bytesRead).all { byte ->
                    byte in 0x09..0x0D || byte in 0x20..0x7E || byte >= 0 // ASCII範囲内
                }
                Log.d("SupporterSettingScreen", "📝 Appears to be text file: $isLikelyText")
                
                if (isLikelyText && bytesRead > 0) {
                    val textContent = String(buffer, 0, bytesRead)
                    Log.w("SupporterSettingScreen", "⚠️ WARNING: Selected file appears to be text: '$textContent'")
                }
                
                // 画像ファイルの妥当性チェック
                val isValidImageMime = mimeType?.startsWith("image/") == true
                val isValidImageContent = fileTypeFromContent != "UNKNOWN"
                
                Log.d("SupporterSettingScreen", "✅ Validation results:")
                Log.d("SupporterSettingScreen", "   Valid MIME type: $isValidImageMime")
                Log.d("SupporterSettingScreen", "   Valid content: $isValidImageContent")
                
                if (!isValidImageMime && !isValidImageContent) {
                    Log.e("SupporterSettingScreen", "❌ ERROR: Selected file is not a valid image!")
                } else {
                    Log.d("SupporterSettingScreen", "✅ Valid image file selected")
                }
                
            } catch (e: Exception) {
                Log.e("SupporterSettingScreen", "❌ Error analyzing selected file: ${e.message}", e)
            }
            
            Log.d("SupporterSettingScreen", "✅ Setting local photo URI")
            localPhotoUri = selectedUri
            
            // 画像のハッシュ値を計算（重複チェック用）
            Log.d("SupporterSettingScreen", "🔍 Calculating image hash for duplicate detection...")
            localPhotoHash = calculateImageHash(selectedUri)
            Log.d("SupporterSettingScreen", "📝 Image hash: $localPhotoHash")
            
            onPhotoChange(selectedUri)
        } ?: run {
            Log.d("SupporterSettingScreen", "❌ 画像選択がキャンセルされました")
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 認証状態のチェック
        val firebaseUser = userViewModel.getCurrentFirebaseUser()
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
                Log.d("SupporterSettingScreen", "🖼️ Image area clicked!")
                // 画像ファイルを選択（すべての画像形式を許可）
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
                // 表示する画像を決定（新しく選択された画像が優先、なければ既存のiconUrl）
                val imageToDisplay = localPhotoUri ?: currentUser?.iconUrl
                
                if (hasExistingPhoto && imageToDisplay != null) {
                    AsyncImage(
                        model = imageToDisplay,
                        contentDescription = "プロフィール写真",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        onError = { error ->
                            Log.e("SupporterSettingScreen", "❌ 画像読み込みエラー: ${error.result.throwable.message}")
                        },
                        onSuccess = {
                            Log.d("SupporterSettingScreen", "✅ 画像読み込み成功: $imageToDisplay")
                        }
                    )
                } else {
                    // 画像がない場合の分かりやすいUI
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // 大きなカメラアイコン
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "プロフィール写真なし",
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFFBDBDBD)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // メインメッセージ
                        Text(
                            text = "写真を追加",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF2196F3),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // サブメッセージ
                        Text(
                            text = "タップして選択",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF757575),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 追加の説明
                        Text(
                            text = "プロフィール写真を設定すると\n信頼性が向上します",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9E9E9E),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
            // 画像がない場合の案内
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

        Text(
            text = "ニックネーム",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = localNickname,
            onValueChange = { localNickname = it },
            placeholder = { Text("例: たろう、さくら") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (hasNicknameChanges) Color(0xFF4CAF50) else Color(0xFF2196F3),
                unfocusedBorderColor = if (hasNicknameChanges) Color(0xFF81C784) else MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        var backgroundLocationEnabled by remember { mutableStateOf(false) }
        val fineLocationPermission = android.Manifest.permission.ACCESS_FINE_LOCATION
        val backgroundLocationPermission = android.Manifest.permission.ACCESS_BACKGROUND_LOCATION

        fun checkPermissions() {
            val fineLocationGranted = ContextCompat.checkSelfPermission(context, fineLocationPermission) == PackageManager.PERMISSION_GRANTED
            val backgroundLocationGranted = ContextCompat.checkSelfPermission(context, backgroundLocationPermission) == PackageManager.PERMISSION_GRANTED
            backgroundLocationEnabled = fineLocationGranted && backgroundLocationGranted
            Log.d("SupporterSettingScreen", "Permissions checked: fineLocation=$fineLocationGranted, backgroundLocation=$backgroundLocationGranted")
        }

        LaunchedEffect(Unit) {
            checkPermissions()
        }

        val backgroundLocationLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                backgroundLocationEnabled = isGranted
                if (isGranted) {
                    Log.d("SupporterSettingScreen", "Background location permission GRANTED")
                } else {
                    Log.e("SupporterSettingScreen", "Background location permission DENIED")
                }
            }
        )

        val fineLocationLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                if (isGranted) {
                    Log.d("SupporterSettingScreen", "Fine location permission GRANTED, requesting background location...")
                    backgroundLocationLauncher.launch(backgroundLocationPermission)
                } else {
                    Log.e("SupporterSettingScreen", "Fine location permission DENIED")
                    backgroundLocationEnabled = false
                }
            }
        )

        LaunchedEffect(backgroundLocationEnabled) {
            val workManager = WorkManager.getInstance(context)
            if (backgroundLocationEnabled) {
                Log.d("SupporterSettingScreen", "バックグラウンドでの位置情報が有効になりました。WorkManagerのタスクを開始します。")
                val constraints = Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val periodicWorkRequest =
                    PeriodicWorkRequestBuilder<LocationWorker>(15, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .build()
                workManager.enqueueUniquePeriodicWork(
                    LocationWorker.WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE, // 既存のタスクがあれば置き換える
                    periodicWorkRequest
                )
                Log.d("SupporterSettingScreen", "WorkManagerのタスク (${LocationWorker.WORK_NAME}) をキューに追加しました。")
            } else {
                Log.d("SupporterSettingScreen", "バックグラウンドでの位置情報が無効になりました。WorkManagerのタスクをキャンセルします。")
                workManager.cancelUniqueWork(LocationWorker.WORK_NAME)
                Log.d("SupporterSettingScreen", "WorkManagerのタスク (${LocationWorker.WORK_NAME}) をキャンセルしました。")
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
                val statusIcon = if (backgroundLocationEnabled) Icons.Default.Check else Icons.Default.Warning
                val iconTint = if (backgroundLocationEnabled) Color(0xFF4CAF50) else Color(0xFFFFA000)
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
                        Log.d("SupporterSettingScreen", "Opening app settings...")
                        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", context.packageName, null)
                        intent.data = uri
                        context.startActivity(intent)
                    } else {
                        // Request Permissions
                        Log.d("SupporterSettingScreen", "Requesting background location permission...")
                        val fineLocationGranted = ContextCompat.checkSelfPermission(context, fineLocationPermission) == PackageManager.PERMISSION_GRANTED
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

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "通知を受け取る",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = notificationsEnabled,
                onCheckedChange = { notificationsEnabled = it }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { 
                // アップロード中の場合は処理をスキップ
                if (isLoading) {
                    Log.d("SupporterSettingScreen", "⚠️ Upload in progress, ignoring button click")
                    return@Button
                }
                
                Log.d("SupporterSettingScreen", "=== 保存ボタン clicked ===")
                Log.d("SupporterSettingScreen", "Original nickname (from currentUser): '$originalNickname'")
                Log.d("SupporterSettingScreen", "Current local nickname: '$localNickname'")
                Log.d("SupporterSettingScreen", "Has nickname changes: $hasNicknameChanges")
                Log.d("SupporterSettingScreen", "Has photo changes: $hasPhotoChanges")
                Log.d("SupporterSettingScreen", "Local photo URI: $localPhotoUri")
                Log.d("SupporterSettingScreen", "Current user: ${currentUser?.email}")
                
                // 変更があるかどうかをチェック
                if (!hasAnyChanges) {
                    Log.d("SupporterSettingScreen", "⚠️ No changes to save")
                    return@Button
                }
                
                Log.d("SupporterSettingScreen", "💾 Starting save process...")
                
                // ニックネームの変更があった場合は保存
                if (hasNicknameChanges) {
                    Log.d("SupporterSettingScreen", "📝 Saving nickname: '$localNickname'")
                    userViewModel.updateNickname(localNickname)
                    onEditClick(localNickname)
                }
                
                // 写真の変更があった場合、または画像が未アップロードの場合は保存
                if (localPhotoUri != null && hasPhotoChanges) {
                    Log.d("SupporterSettingScreen", "📸 Saving profile image...")
                    Log.d("SupporterSettingScreen", "Photo URI to upload: $localPhotoUri")
                    Log.d("SupporterSettingScreen", "Photo hash: $localPhotoHash")
                    Log.d("SupporterSettingScreen", "Current user iconUrl: ${currentUser?.iconUrl}")
                    Log.d("SupporterSettingScreen", "Reason: New/different image detected by hash comparison")
                    try {
                        // 画像をアップロードしてiconUrlを取得
                        Log.d("SupporterSettingScreen", "🔄 Starting image upload...")
                        Log.d("SupporterSettingScreen", "Will replace existing image: ${currentUser?.iconUrl}")
                        userViewModel.uploadProfileImage(localPhotoUri!!) { downloadUrl ->
                            Log.d("SupporterSettingScreen", "✅ Image uploaded successfully: $downloadUrl")
                            // アップロード成功後、iconUrlをデータベースに保存
                            if (downloadUrl.isNotEmpty()) {
                                Log.d("SupporterSettingScreen", "💾 Updating user with new iconUrl...")
                                Log.d("SupporterSettingScreen", "Old iconUrl was: ${currentUser?.iconUrl}")
                                userViewModel.updateUserIconUrl(downloadUrl)
                                // 現在のハッシュ値を更新（次回の比較用）
                                currentPhotoHash = localPhotoHash
                                Log.d("SupporterSettingScreen", "🔄 Updated current photo hash for future comparisons")
                            } else {
                                Log.e("SupporterSettingScreen", "❌ Download URL is empty")
                            }
                        }
                        Log.d("SupporterSettingScreen", "✅ Image upload initiated")
                        onPhotoSave(localPhotoUri!!)
                    } catch (e: Exception) {
                        Log.e("SupporterSettingScreen", "❌ Error uploading image: ${e.message}", e)
                    }
                } else {
                    if (localPhotoUri != null) {
                        Log.d("SupporterSettingScreen", "📸 Same image detected - skipping upload")
                        Log.d("SupporterSettingScreen", "localPhotoUri: $localPhotoUri")
                        Log.d("SupporterSettingScreen", "localPhotoHash: $localPhotoHash")
                        Log.d("SupporterSettingScreen", "currentPhotoHash: $currentPhotoHash")
                        Log.d("SupporterSettingScreen", "Duplicate upload prevented")
                    } else {
                        Log.d("SupporterSettingScreen", "📸 No photo to save")
                        Log.d("SupporterSettingScreen", "localPhotoUri: $localPhotoUri")
                        Log.d("SupporterSettingScreen", "hasPhotoChanges: $hasPhotoChanges")
                    }
                }
                
                Log.d("SupporterSettingScreen", "✅ Save process completed")
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (hasAnyChanges) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                disabledContainerColor = Color(0xFFE0E0E0)
            ),
            enabled = hasAnyChanges && !isLoading, // 変更がない場合は無効化
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
                        !hasAnyChanges -> "変更なし"
                        hasAnyChanges -> "変更を保存"
                        else -> "保存"
                    },
                    color = if (hasAnyChanges) Color.White else Color(0xFF757575),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
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
        
        // サインアウトボタン
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(
            onClick = {
                Log.d("SupporterSettingScreen", "Sign out button clicked")
                // デバイス削除を先に実行してから、完了後にサインアウト
                deviceViewModel.callDeleteDevice {
                    Log.d("SupporterSettingScreen", "Device deletion completed")
                    userViewModel.signOut()
                    onSignOut()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFD32F2F)
            ),
            border = BorderStroke(1.dp, Color(0xFFD32F2F)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "サインアウト",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}