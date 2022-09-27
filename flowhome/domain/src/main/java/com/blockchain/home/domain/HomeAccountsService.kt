package com.blockchain.home.domain

import com.blockchain.coincore.BlockchainAccount
import com.blockchain.data.DataResource
import kotlinx.coroutines.flow.Flow

interface HomeAccountsService {
    fun accounts(): Flow<DataResource<List<BlockchainAccount>>>
}
