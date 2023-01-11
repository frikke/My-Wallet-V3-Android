package com.blockchain.walletmode

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import info.blockchain.balance.Money
import kotlinx.coroutines.flow.Flow

interface WalletModeBalanceService {
    fun balanceFor(walletMode: WalletMode, freshnessStrategy: FreshnessStrategy): Flow<DataResource<Money>>
    fun totalBalance(freshnessStrategy: FreshnessStrategy): Flow<DataResource<Money>>
    fun getBalanceWithFailureState(
        walletMode: WalletMode,
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<Pair<Money, Boolean>>>
}
