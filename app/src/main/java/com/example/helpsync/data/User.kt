package com.example.helpsync.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class User(
    @DocumentId
    var uid: String = "",
    
    @get:PropertyName("email") @set:PropertyName("email")
    var email: String = "",
    
    @get:PropertyName("roles") @set:PropertyName("roles")
    var roles: List<String> = emptyList(),
    
    @get:PropertyName("nickname") @set:PropertyName("nickname")
    var nickname: String = "",
    
    @get:PropertyName("iconUrl") @set:PropertyName("iconUrl")
    var iconUrl: String = "",
    
    @get:PropertyName("physicalFeatures") @set:PropertyName("physicalFeatures")
    var physicalFeatures: String = "",
    
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Long = System.currentTimeMillis(),
    
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAt: Long = System.currentTimeMillis()
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
