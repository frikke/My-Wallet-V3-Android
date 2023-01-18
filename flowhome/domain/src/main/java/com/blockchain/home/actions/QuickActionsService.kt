package com.blockchain.home.actions

import com.blockchain.coincore.StateAwareAction
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.walletmode.WalletMode
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

interface QuickActionsService {
    fun availableQuickActionsForWalletMode(
        walletMode: WalletMode,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES)
        )
    ): Flow<List<StateAwareAction>>

    fun moreActions(
        walletMode: WalletMode,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES)
        )
    ): Flow<List<StateAwareAction>>
}
