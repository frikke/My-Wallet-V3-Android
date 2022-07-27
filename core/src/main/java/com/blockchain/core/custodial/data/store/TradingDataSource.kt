package com.blockchain.core.custodial.data.store

import com.blockchain.api.services.TradingBalance
import com.blockchain.store.StoreRequest
import com.blockchain.store.StoreResponse
import com.blockchain.storedatasource.FlushableDataSource
import kotlinx.coroutines.flow.Flow

interface TradingDataSource : FlushableDataSource {
    // todo(othman) check with andré about not using datasource interface, but allow mapping to/from store model
    fun streamData(request: StoreRequest): Flow<StoreResponse<List<TradingBalance>>>
}
