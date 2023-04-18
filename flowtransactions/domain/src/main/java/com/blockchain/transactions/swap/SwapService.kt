package com.blockchain.transactions.swap

import com.blockchain.coincore.CryptoAccount
import com.blockchain.data.DataResource
import kotlinx.coroutines.flow.Flow

interface SwapService {
    fun sourceAccounts(): Flow<DataResource<List<CryptoAccount>>>

    fun custodialSourceAccountsWithBalances(): Flow<DataResource<List<CryptoAccountWithBalance>>>
}
