package com.blockchain.home.actions

import com.blockchain.coincore.StateAwareAction
import com.blockchain.walletmode.WalletMode
import kotlinx.coroutines.flow.Flow

interface QuickActionsService {
    fun availableQuickActionsForWalletMode(walletMode: WalletMode): Flow<List<StateAwareAction>>
    fun moreActions(walletMode: WalletMode): Flow<List<StateAwareAction>>
}
