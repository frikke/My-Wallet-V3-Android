package com.blockchain.core.interest.data.store

import com.blockchain.api.services.InterestBalanceDetails
import com.blockchain.store.StoreResponse
import com.blockchain.storedatasource.FlushableDataSource
import kotlinx.coroutines.flow.Flow

interface InterestDataSource : FlushableDataSource {
    fun stream(refresh: Boolean): Flow<StoreResponse<Throwable, List<InterestBalanceDetails>>>
}
