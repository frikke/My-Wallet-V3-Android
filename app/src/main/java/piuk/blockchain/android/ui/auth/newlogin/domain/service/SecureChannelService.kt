package piuk.blockchain.android.ui.auth.newlogin.domain.service

import piuk.blockchain.android.ui.auth.newlogin.domain.model.SecureChannelBrowserMessage

interface SecureChannelService {
    fun sendErrorMessage(channelId: String, pubKeyHash: String)
    fun sendHandshake(json: String)
    fun sendLoginMessage(channelId: String, pubKeyHash: String)
    fun decryptMessage(pubKeyHash: String, messageEncrypted: String): SecureChannelBrowserMessage?
}

