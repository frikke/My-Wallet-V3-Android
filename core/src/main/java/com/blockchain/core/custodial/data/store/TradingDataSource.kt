package com.blockchain.core.custodial.data.store

import com.blockchain.api.services.TradingBalance
import com.blockchain.store.StoreRequest
import com.blockchain.store.StoreResponse
import com.blockchain.storedatasource.FlushableDataSource
import kotlinx.coroutines.flow.Flow

interface TradingDataSource : FlushableDataSource {
    fun streamData(storeRequest: StoreRequest): Flow<StoreResponse<Throwable, List<TradingBalance>>>
}
