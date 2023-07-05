package com.blockchain.home.domain

import com.blockchain.coincore.SingleAccount
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.walletmode.WalletMode
import info.blockchain.balance.Currency
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

interface HomeAccountsService {
    fun accounts(
        walletMode: WalletMode,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES)
        )
    ): Flow<DataResource<List<SingleAccount>>>

    fun failedNetworks(
        walletMode: WalletMode,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES)
        )
    ): Flow<DataResource<List<Currency>>>
}
