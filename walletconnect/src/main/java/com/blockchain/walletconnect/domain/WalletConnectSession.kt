package com.blockchain.walletconnect.domain

import com.trustwallet.walletconnect.models.WCPeerMeta
import com.trustwallet.walletconnect.models.session.WCSession
import java.io.Serializable

class WalletConnectSession(val url: String, val dAppInfo: DAppInfo, val walletInfo: WalletInfo) : Serializable {
    companion object {
        fun fromWCSession(wcSession: WCSession, peerMeta: WCPeerMeta, peerId: String) =
            WalletConnectSession(
                url = wcSession.toUri(),
                dAppInfo = DAppInfo(
                    peerId = peerId,
                    peerMeta = ClientMeta(
                        description = peerMeta.description.orEmpty(),
                        url = peerMeta.url,
                        icons = peerMeta.icons,
                        name = peerMeta.name
                    ),
                    chainId = DEFAULT_WALLET_CONNECT_CHAIN_ID,
                ),
                walletInfo = WalletInfo(
                    clientId = peerId,
                    sourcePlatform = "Android"
                )
            )

        const val DEFAULT_WALLET_CONNECT_CHAIN_ID = 1
    }

    override fun equals(other: Any?): Boolean {
        return (other as? WalletConnectSession)?.url == url
    }

    override fun hashCode(): Int {
        return url.hashCode()
    }
}

data class WalletInfo(val clientId: String, val sourcePlatform: String) : Serializable

data class DAppInfo(val peerId: String, val peerMeta: ClientMeta, val chainId: Int) : Serializable

data class ClientMeta(val description: String, val url: String, val icons: List<String>, val name: String) :
    Serializable {
    fun uiIcon(): String =
        icons.takeIf { it.isNotEmpty() }?.let { it[0] } ?: "https://www.blockchain.com/static/apple-touch-icon.png"
}
