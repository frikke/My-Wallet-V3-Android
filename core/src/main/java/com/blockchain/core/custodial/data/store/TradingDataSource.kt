package com.blockchain.core.custodial.data.store

import com.blockchain.api.services.TradingBalance
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.storedatasource.FlushableDataSource
import kotlinx.coroutines.flow.Flow

interface TradingDataSource : FlushableDataSource {
    // todo(othman) check with andré about not using datasource interface, but allow mapping to/from store model
    fun streamData(request: FreshnessStrategy): Flow<DataResource<List<TradingBalance>>>
}
