package com.dex.data

import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.dataOrElse
import com.blockchain.data.filterNotLoading
import com.blockchain.preferences.DexPrefs
import com.blockchain.unifiedcryptowallet.domain.balances.CoinNetworksService
import com.dex.data.stores.DexChainDataStorage
import com.dex.domain.DexNetworkService
import info.blockchain.balance.CoinNetwork
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

class DexNetworkRepository(
    private val dexPrefs: DexPrefs,
    private val dexChainDataStorage: DexChainDataStorage,
    private val coinNetworksService: CoinNetworksService
) : DexNetworkService {
    private val freshness = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)

    private val _chainId = MutableStateFlow(dexPrefs.selectedChainId)
    override val chainId: StateFlow<Int>
        get() = _chainId

    override fun selectedChainId(): Int {
        return _chainId.value
    }

    override fun updateSelectedNetwork(chainId: Int) {
        dexPrefs.selectedChainId = chainId
        _chainId.value = chainId
    }

    override suspend fun supportedNetworks(): List<CoinNetwork> {
        val allNetworks = coinNetworksService.allCoinNetworks().first().dataOrElse(emptyList())
        val dexNetworks = dexChainDataStorage.stream(freshness).filterNotLoading().first().dataOrElse(emptyList())
        return allNetworks.filter { it.chainId in dexNetworks.map { dxNetwork -> dxNetwork.chainId } }
    }
}
