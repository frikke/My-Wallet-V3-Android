package com.dex.domain

import com.blockchain.data.DataResource
import info.blockchain.balance.CoinNetwork
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface DexChainService {
    val chainId: StateFlow<Int>
    fun selectedChainId(): Int
    fun supportedNetworks(): Flow<DataResource<List<CoinNetwork>>>
    fun updateSelectedNetwork(chainId: Int)
}
