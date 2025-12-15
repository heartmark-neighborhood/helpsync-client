package com.example.helpsync

import kotlinx.serialization.Serializable

@Serializable
data class SupporterInfo(
    val id: String,
    val nickname: String,
    val iconUrl: String?
)

@Serializable
data class SupporterNavInfo(
    val requestId: String,
    val supporterInfo: SupporterInfo
)