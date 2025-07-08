package com.example.helpsync.supporter_setting_screen

import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.example.helpsync.nickname_setting.NicknameSetting

@Composable
fun MainSettingsScreen() {
    var nickname by rememberSaveable { mutableStateOf("") }
    var photoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var isEditing by remember { mutableStateOf(true) }

    if (isEditing) {
        NicknameSetting(
            nickname = nickname,
            onNicknameChange = { nickname = it },
            photoUri = photoUri,
            onPhotoChange = { photoUri = it },
            onBackClick = { isEditing = false },
            onDoneClick = { isEditing = false }
        )
    } else {
        SupporterSettingsScreen(
            nickname = nickname,
            onNicknameChange = { nickname = it },
            photoUri = photoUri,
            onPhotoChange = { photoUri = it },
            modifier = Modifier,
            onEditClick = { isEditing = true }
        )
    }
}