package piuk.blockchain.android.ui.auth

import kotlinx.serialization.Serializable

@Serializable
data class MobileNoticeDialog(
    val title: String = "",
    val body: String = "",
    val ctaText: String = "",
    val ctaLink: String = ""
)
