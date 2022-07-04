package piuk.blockchain.android.simplebuy.yodlee

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FastLinkMessage(val type: String?, val data: MessageData?)

@Serializable
data class MessageData(
    val fnToCall: String?,
    val title: String?,
    val code: String?,
    val message: String?,
    val providerName: String?,
    val requestId: String?,
    val isMFAError: Boolean?,
    val reason: String?,
    val status: String?,
    val action: String?,
    val providerAccountId: String?,
    val providerId: String?,
    val sites: List<SiteData>?,
    @SerialName("url")
    val externalUrl: String?
)

@Serializable
data class SiteData(
    val status: String?,
    val providerId: String?,
    val requestId: String?,
    val providerName: String?,
    val providerAccountId: String?,
    val accountId: String?
)
