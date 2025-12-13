package com.example.helpsync.help_request_detail_screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.helpsync.data.HelpRequesterInfo

@Composable
fun HelpRequestDetailScreen(
    info: HelpRequesterInfo?,
    onBack: () -> Unit
) {
    if (info == null) {
        Text("情報を読み込めませんでした")
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ヘルプ要請詳細") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!info.iconUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = info.iconUrl,
                    contentDescription = "ヘルプマーク所持者のアイコン",
                    modifier = Modifier.size(96.dp)
                )
            } else {
                Text("アイコンなし")
            }

            Text("ニックネーム: ${info.nickname}", style = MaterialTheme.typography.titleMedium)
            Text("要請ID: ${info.requestId}", style = MaterialTheme.typography.bodyMedium)
            info.detail?.let { Text("要請内容: $it", style = MaterialTheme.typography.bodyMedium) }
            

            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onBack) { Text("閉じる") }
        }
    }
}