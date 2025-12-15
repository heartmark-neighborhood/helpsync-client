import kotlinx.serialization.Serializable

@Serializable
data class HelpRequesterInfo(
    val requestId: String,
    val nickname: String,
    val iconUrl: String? = null,
    val detail: String? = null,  
    
)