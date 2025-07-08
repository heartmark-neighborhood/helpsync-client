package com.example.helpsync.supporter_home_screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class SupportRequest(val id: Int, val nickname: String, val content: String)

val dummyRequests = listOf(
    SupportRequest(1, "ようかん", "席を譲ってほしい"),
    SupportRequest(2, "とちおとめ", "壁にもたれたい"),
    SupportRequest(3, "さんさん", "助けて")
)

@Composable
fun SupporterHomeScreen(
    onDoneClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text("支援依頼リスト", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(dummyRequests) { request ->
                Card(
                    onClick = { onDoneClick() }, // ← カードタップで遷移
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("依頼者: ${request.nickname}", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("内容: ${request.content}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
