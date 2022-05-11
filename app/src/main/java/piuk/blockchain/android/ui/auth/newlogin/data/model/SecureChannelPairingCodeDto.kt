package piuk.blockchain.android.ui.auth.newlogin.data.model

import kotlinx.serialization.Serializable

@Serializable
internal data class SecureChannelPairingCodeDto(
    val pubkey: String,
    val channelId: String
)
