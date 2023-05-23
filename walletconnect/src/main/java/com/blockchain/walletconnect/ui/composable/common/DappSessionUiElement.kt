package com.blockchain.walletconnect.ui.composable.common

import com.blockchain.walletconnect.domain.WalletConnectSession
import info.blockchain.balance.CryptoCurrency

data class DappSessionUiElement(
    val dappName: String,
    val dappDescription: String,
    val dappUrl: String,
    val dappLogoUrl: String,
    val chainName: String,
    val chainLogo: String,
    val sessionId: String,
    val isV2: Boolean
)

fun WalletConnectSession.toDappSessionUiElement(): DappSessionUiElement {
    return DappSessionUiElement(
        dappName = this.dAppInfo.peerMeta.name,
        dappDescription = this.dAppInfo.peerMeta.description,
        dappUrl = this.dAppInfo.peerMeta.url.substringAfter("https://"),
        dappLogoUrl = this.dAppInfo.peerMeta.icons.firstOrNull().orEmpty(),
        chainName = CryptoCurrency.ETHER.name, // TODO support other ERC20 chains
        chainLogo = CryptoCurrency.ETHER.logo, // TODO support other ERC20 chains
        sessionId = this.walletInfo.clientId,
        isV2 = this.isV2
    )
}
