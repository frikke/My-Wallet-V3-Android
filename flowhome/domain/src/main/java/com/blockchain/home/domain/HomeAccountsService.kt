package com.blockchain.home.domain

import com.blockchain.coincore.SingleAccount
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.walletmode.WalletMode
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

interface HomeAccountsService {
    fun accounts(
        walletMode: WalletMode,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES)
        )
    ): Flow<DataResource<List<SingleAccount>>>
}
