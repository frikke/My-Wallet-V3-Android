package piuk.blockchain.android.ui.auth.newlogin.presentation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import piuk.blockchain.android.ui.auth.newlogin.domain.model.SecureChannelBrowserMessage

@Parcelize
data class SecureChannelBrowserMessageArg(
    val type: String,
    val channelId: String,
    val timestamp: Long
) : Parcelable

fun SecureChannelBrowserMessageArg.toDomain() = SecureChannelBrowserMessage(
    type = type,
    channelId = channelId,
    timestamp = timestamp
)
