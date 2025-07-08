package com.example.helpsync.role_selection_screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.helpsync.R
import com.example.helpsync.ui.theme.HelpSyncTheme

@Composable
fun RoleSelectionScreen(
    onRoleSelected: (RoleType) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        // タイトル
        Text(
            text = "あなたの役割を\n選択してください",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp,
            color = Color(0xFF1976D2)
        )
        
        // サポーターカード
        ImprovedRoleCard(
            title = stringResource(R.string.supporter),
            icon = {
                Icon(
                    Icons.Default.Person,
                    contentDescription = stringResource(R.string.supporter_description),
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF4CAF50)
                )
            },
            onClick = { onRoleSelected(RoleType.SUPPORTER) }
        )
        
        // ヘルプマーク所持者カード
        ImprovedRoleCard(
            title = stringResource(R.string.help_mark_holder),
            icon = {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    // ピンクのハート
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = "ハート",
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFFE91E63)
                    )
                    // 白い十字
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "十字",
                        modifier = Modifier.size(28.dp),
                        tint = Color.White
                    )
                }
            },
            onClick = { onRoleSelected(RoleType.HELP_MARK_HOLDER) }
        )
    }
}

@Composable
private fun ImprovedRoleCard(
    title: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // アイコン背景
            Card(
                modifier = Modifier.size(100.dp),
                shape = RoundedCornerShape(50.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF8F9FA)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = Color(0xFF212121)
            )
        }
    }
}

enum class RoleType {
    SUPPORTER,
    HELP_MARK_HOLDER
}

@Preview(showBackground = true)
@Composable
fun RoleSelectionScreenPreview() {
    HelpSyncTheme {
        RoleSelectionScreen()
    }
}
