package com.example.helpsync.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// ヘルプリクエストの状態を表すenum
enum class RequestStatus {
    PENDING,  // サポーターを探している最中
    MATCHED,  // サポーターが見つかった
    FAILED,
    EXPIRED,
    COMPLETED, // 支援完了
    CANCELED  // キャンセルされた
}

data class HelpRequest(
    val id: String = "", // FirestoreのドキュメントID
    val requesterId: String = "",
    val requesterNickname: String = "",
    val proximityUuid: String = "", // このリクエスト固有のUUID
    val status: RequestStatus = RequestStatus.PENDING,
    val matchedSupporterId: String? = null,
    @ServerTimestamp
    val createdAt: Date? = null
)