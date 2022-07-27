package com.blockchain.core.chains.erc20.data.store

import com.blockchain.api.ethereum.evm.BalancesResponse
import com.blockchain.store.KeyedStoreRequest
import com.blockchain.store.StoreResponse
import com.blockchain.storedatasource.KeyedFlushableDataSource
import kotlinx.coroutines.flow.Flow

interface Erc20L2DataSource : KeyedFlushableDataSource<String> {
    fun streamData(
        request: KeyedStoreRequest<Erc20L2Store.Key>
    ): Flow<StoreResponse<BalancesResponse>>
}
