package com.blockchain.home.presentation.navigation

import com.blockchain.coincore.CryptoTarget
import java.security.KeyPair

sealed class ScanResult(
    val isDeeplinked: Boolean
) {
    class HttpUri(
        val uri: String,
        isDeeplinked: Boolean
    ) : ScanResult(isDeeplinked)

    class TxTarget(
        val targets: Set<CryptoTarget>,
        isDeeplinked: Boolean
    ) : ScanResult(isDeeplinked)

    class ImportedWallet(
        val keyPair: KeyPair
    ) : ScanResult(false)

    class SecuredChannelLogin(
        val handshake: String
    ) : ScanResult(false)

    class WalletConnectRequest(
        val data: String
    ) : ScanResult(false)

    class WalletConnectV2Request(
        val data: String
    ) : ScanResult(false)
}
