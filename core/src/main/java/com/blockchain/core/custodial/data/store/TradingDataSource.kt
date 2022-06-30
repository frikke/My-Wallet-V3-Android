package com.blockchain.core.custodial.data.store

import com.blockchain.api.services.TradingBalance
import com.blockchain.store.StoreResponse
import com.blockchain.storedatasource.FlushableDataSource
import kotlinx.coroutines.flow.Flow

interface TradingDataSource : FlushableDataSource {
    fun stream(refresh: Boolean): Flow<StoreResponse<Throwable, List<TradingBalance>>>
}
