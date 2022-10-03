package com.blockchain.core.chains.erc20.data.store

import com.blockchain.api.services.Erc20TokenBalance
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.storedatasource.FlushableDataSource
import kotlinx.coroutines.flow.Flow

interface Erc20DataSource : FlushableDataSource {
    // todo(othman) check with andré about not using datasource interface, but allow mapping to/from store model
    fun streamData(request: FreshnessStrategy): Flow<DataResource<List<Erc20TokenBalance>>>
}
