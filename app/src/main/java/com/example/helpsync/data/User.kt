package com.example.helpsync.data

import java.util.Date

data class User(
    var email: String = "",
    var role: String = "",
    var nickname: String = "",
    var iconUrl: String = "",
    var physicalFeatures: String = "",
    var createdAt: Date = Date(),
    var updatedAt: Date = Date()
)

enum class UserRole(val value: String) {
    SUPPORTER("supporter"),
    REQUESTER("requester");
    
    companion object {
        fun fromString(value: String): UserRole? {
            return values().find { it.value == value }
        }
    }
}
