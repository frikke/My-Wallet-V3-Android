package com.blockchain.core.chains.erc20.data.store

import com.blockchain.api.ethereum.evm.BalancesResponse
import com.blockchain.store.StoreResponse
import com.blockchain.storedatasource.KeyedFlushableDataSource
import kotlinx.coroutines.flow.Flow

interface Erc20L2DataSource : KeyedFlushableDataSource<String> {
    fun stream(
        accountHash: String,
        networkTicker: String,
        refresh: Boolean
    ): Flow<StoreResponse<Throwable, BalancesResponse>>
}
