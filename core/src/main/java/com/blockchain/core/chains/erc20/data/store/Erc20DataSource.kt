package com.blockchain.core.chains.erc20.data.store

import com.blockchain.api.services.Erc20TokenBalance
import com.blockchain.store.StoreRequest
import com.blockchain.store.StoreResponse
import com.blockchain.storedatasource.FlushableDataSource
import kotlinx.coroutines.flow.Flow

interface Erc20DataSource : FlushableDataSource {
    fun streamData(request: StoreRequest): Flow<StoreResponse<Throwable, List<Erc20TokenBalance>>>
}
