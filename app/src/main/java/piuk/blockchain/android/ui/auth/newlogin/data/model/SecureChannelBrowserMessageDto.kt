package piuk.blockchain.android.ui.auth.newlogin.data.model

import com.blockchain.domain.auth.SecureChannelBrowserMessage
import kotlinx.serialization.Serializable

@Serializable
internal data class SecureChannelBrowserMessageDto(
    val type: String,
    val channelId: String,
    val timestamp: Long
)

internal fun SecureChannelBrowserMessageDto.toDomain() = SecureChannelBrowserMessage(
    type = type,
    channelId = channelId,
    timestamp = timestamp
)
