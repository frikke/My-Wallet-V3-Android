package com.dex.domain

import info.blockchain.balance.CoinNetwork
import kotlinx.coroutines.flow.StateFlow

interface DexNetworkService {
    val chainId: StateFlow<Int>
    fun selectedChainId(): Int
    suspend fun supportedNetworks(): List<CoinNetwork>
    fun updateSelectedNetwork(chainId: Int)
}
