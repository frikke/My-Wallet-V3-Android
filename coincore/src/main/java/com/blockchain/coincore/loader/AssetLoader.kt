package com.blockchain.coincore.loader

import com.blockchain.coincore.Asset
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.walletmode.WalletMode
import info.blockchain.balance.Currency
import io.reactivex.rxjava3.core.Completable
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow

internal interface AssetLoader {
    fun initAndPreload(): Completable

    // The assets which have balances and/or transaction history. This list is used for displaying content on the
    // Portfolio screen.
    fun activeAssets(
        walletMode: WalletMode,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES)
        )
    ): Flow<List<Asset>>

    fun activeAssets(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES)
        )
    ): Flow<List<Asset>>

    // The assets which have been loaded so far. On startup we load the L1s, the assets with Custodial support and
    // the active assets. This list is used for Swap targets.
    val loadedAssets: List<Asset>

    operator fun get(asset: Currency): Asset
}
