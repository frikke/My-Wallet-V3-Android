package com.blockchain.core.custodial.data.store

import com.blockchain.api.services.TradingBalance
import com.blockchain.store.StoreResponse
import kotlinx.coroutines.flow.Flow

internal interface TradingDataSource {
    fun stream(refresh: Boolean): Flow<StoreResponse<Throwable, List<TradingBalance>>>
    fun invalidate()
}
