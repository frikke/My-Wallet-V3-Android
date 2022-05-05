package piuk.blockchain.android.ui.auth.newlogin.data.model

import kotlinx.serialization.Serializable

@Serializable
internal sealed class SecureChannelMessageDto {

    @Serializable
    object Empty : SecureChannelMessageDto()

    @Serializable
    data class PairingHandshake(
        val guid: String,
        val pubkey: String,
        val type: String = "handshake"
    ) : SecureChannelMessageDto()

    @Serializable
    data class Login(
        val guid: String,
        val password: String,
        val sharedKey: String,
        val remember: Boolean,
        val type: String = "login_wallet"
    ) : SecureChannelMessageDto()
}