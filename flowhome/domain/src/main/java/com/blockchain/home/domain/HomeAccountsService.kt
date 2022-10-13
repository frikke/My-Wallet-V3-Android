package com.blockchain.home.domain

import com.blockchain.coincore.SingleAccount
import com.blockchain.data.DataResource
import com.blockchain.home.model.AssetFilterStatus
import kotlinx.coroutines.flow.Flow

interface HomeAccountsService {
    fun accounts(): Flow<DataResource<List<SingleAccount>>>

    fun filters(): Flow<List<AssetFilterStatus>>
    fun updateFilters(filters: List<AssetFilterStatus>)
}
