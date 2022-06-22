package com.blockchain.core.interest.data.store

import com.blockchain.api.services.InterestBalanceDetails
import com.blockchain.store.StoreResponse
import kotlinx.coroutines.flow.Flow

internal interface InterestDataSource {
    fun stream(refresh: Boolean): Flow<StoreResponse<Throwable, List<InterestBalanceDetails>>>
    fun invalidate()
}
