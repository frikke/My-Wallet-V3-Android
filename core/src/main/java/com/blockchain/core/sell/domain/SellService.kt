package com.blockchain.core.sell.domain

import com.blockchain.data.DataResource
import kotlinx.coroutines.flow.Flow

interface SellService {
    fun loadSellAssets(): Flow<DataResource<SellEligibility>>
}
