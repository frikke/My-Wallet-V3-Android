package com.blockchain.home.domain

import com.blockchain.coincore.SingleAccount
import com.blockchain.data.DataResource
import com.blockchain.walletmode.WalletMode
import kotlinx.coroutines.flow.Flow

interface HomeAccountsService {
    fun accounts(walletMode: WalletMode): Flow<DataResource<List<SingleAccount>>>
}
