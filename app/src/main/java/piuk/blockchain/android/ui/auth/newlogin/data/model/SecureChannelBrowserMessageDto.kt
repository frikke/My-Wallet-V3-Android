package piuk.blockchain.android.ui.auth.newlogin.data.model

import kotlinx.serialization.Serializable
import piuk.blockchain.android.ui.auth.newlogin.domain.model.SecureChannelBrowserMessage

@Serializable
internal data class SecureChannelBrowserMessageDto(
    val type: String,
    val channelId: String,
    val timestamp: Long
)

internal fun SecureChannelBrowserMessageDto.toDomain(): SecureChannelBrowserMessage = this.run {
    SecureChannelBrowserMessage(
        type = type,
        channelId = channelId,
        timestamp = timestamp
    )
}
