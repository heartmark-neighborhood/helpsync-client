package com.example.helpsync.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import com.example.helpsync.viewmodel.UserViewModel
import com.example.helpsync.viewmodel.DeviceManagementVewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    onNavigateToSignUp: () -> Unit,
    onSignInSuccess: () -> Unit,
    userViewModel: UserViewModel = koinViewModel(),
    deviceViewModel: DeviceManagementVewModel = koinViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isFormValid by remember { mutableStateOf(false) }

    // フォームのバリデーション
    LaunchedEffect(email, password) {
        isFormValid = email.isNotBlank() && password.isNotBlank()
    }

    // サインイン成功時の処理: デバイス登録を呼び出す
    LaunchedEffect(userViewModel.isSignedIn) {
        if (userViewModel.isSignedIn) {
            // 最初はプレースホルダ緯度経度で登録（後で位置情報更新処理が上書きします）
            deviceViewModel.callRegisterNewDevice(0.0, 0.0)
            onSignInSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "サインイン",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // メールアドレス入力
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("メールアドレス") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // パスワード入力
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("パスワード") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // サインインボタン
        Button(
            onClick = {
                userViewModel.signIn(email, password)
            },
            enabled = isFormValid && !userViewModel.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (userViewModel.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("サインイン")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // サインアップページへのリンク
        TextButton(onClick = onNavigateToSignUp) {
            Text("アカウントをお持ちでない方はこちら")
        }

        // エラーメッセージ表示
        userViewModel.errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
