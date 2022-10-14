package com.blockchain.home.domain

import com.blockchain.coincore.SingleAccount
import com.blockchain.data.DataResource
import kotlinx.coroutines.flow.Flow

interface HomeAccountsService {
    fun accounts(): Flow<DataResource<List<SingleAccount>>>
}
