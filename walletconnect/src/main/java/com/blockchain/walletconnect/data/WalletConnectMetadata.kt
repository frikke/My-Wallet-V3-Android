package com.blockchain.walletconnect.data

import kotlinx.serialization.Serializable

@Serializable
data class WalletConnectMetadata(
    val sessions: WalletConnectSessions?
)

@Serializable
data class WalletConnectSessions(val v1: List<WalletConnectDapps>)

@Serializable
data class WalletConnectDapps(
    val url: String,
    val dAppInfo: DappInfo,
    val walletInfo: WalletInfoMetadata
)

@Serializable
data class WalletInfoMetadata(
    val sourcePlatform: String,
    val clientId: String
)

@Serializable
data class DappInfo(
    val chainId: Int?,
    val peerId: String,
    val peerMeta: PeerMeta
)

@Serializable
class PeerMeta(
    val name: String,
    val url: String,
    val icons: List<String>,
    val description: String
)
