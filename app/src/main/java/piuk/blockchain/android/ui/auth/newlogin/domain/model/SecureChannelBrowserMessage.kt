package piuk.blockchain.android.ui.auth.newlogin.domain.model

import piuk.blockchain.android.ui.auth.newlogin.presentation.SecureChannelBrowserMessageArg

data class SecureChannelBrowserMessage(
    val type: String,
    val channelId: String,
    val timestamp: Long
)

fun SecureChannelBrowserMessage.toArg(): SecureChannelBrowserMessageArg = this.run {
    SecureChannelBrowserMessageArg(
        type = type,
        channelId = channelId,
        timestamp = timestamp
    )
}
