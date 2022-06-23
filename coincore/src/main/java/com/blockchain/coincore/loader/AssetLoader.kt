package com.blockchain.coincore.loader

import com.blockchain.coincore.CryptoAsset
import com.blockchain.walletmode.WalletMode
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Completable

internal interface AssetLoader {
    fun initAndPreload(walletMode: WalletMode): Completable

    // The assets which have balances and/or transaction history. This list is used for displaying content on the
    // Portfolio screen.
    val activeAssets: List<CryptoAsset>

    // The assets which have been loaded so far. On startup we load the L1s, the assets with Custodial support and
    // the active assets. This list is used for Swap targets.
    val loadedAssets: List<CryptoAsset>

    operator fun get(asset: AssetInfo): CryptoAsset
}
