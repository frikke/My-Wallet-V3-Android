package com.blockchain.walletmode

import com.blockchain.data.DataResource
import info.blockchain.balance.Money
import kotlinx.coroutines.flow.Flow

interface WalletModeBalanceService {
    fun balanceFor(walletMode: WalletMode): Flow<DataResource<Money>>
    fun totalBalance(): Flow<DataResource<Money>>
}
