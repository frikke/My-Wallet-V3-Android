package com.blockchain.home.domain

import com.blockchain.coincore.SingleAccount
import com.blockchain.data.DataResource
import com.blockchain.home.model.AssetFilters
import kotlinx.coroutines.flow.Flow

interface HomeAccountsService {
    fun accounts(): Flow<DataResource<List<SingleAccount>>>

    fun filters(): Flow<AssetFilters>
    fun updateFilters(filters: Boolean)
}
