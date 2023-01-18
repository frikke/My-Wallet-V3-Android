package com.blockchain.home.actions

import com.blockchain.coincore.StateAwareAction
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.walletmode.WalletMode
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow

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
