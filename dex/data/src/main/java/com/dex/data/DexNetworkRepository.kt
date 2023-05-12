package com.dex.data

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.combineDataResourceFlows
import com.blockchain.preferences.DexPrefs
import com.blockchain.unifiedcryptowallet.domain.balances.CoinNetworksService
import com.blockchain.utils.toException
import com.dex.data.stores.DexChainDataStorage
import com.dex.domain.DexChainService
import info.blockchain.balance.CoinNetwork
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch

class DexNetworkRepository(
    private val dexPrefs: DexPrefs,
    private val dexChainDataStorage: DexChainDataStorage,
    private val coinNetworksService: CoinNetworksService
) : DexChainService {
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

    override fun supportedNetworks(): Flow<DataResource<List<CoinNetwork>>> {
        return combineDataResourceFlows(
            coinNetworksService.allCoinNetworks(),
            dexChainDataStorage.stream(freshness)
        ) { coinNetworks, dexChains ->
            coinNetworks.filter { coinNetwork ->
                dexChains.any { dexChain ->
                    coinNetwork.chainId == dexChain.chainId || coinNetwork.networkTicker == "MATIC"
                }
            }
        }.catch { emit(DataResource.Error(it.toException())) }
    }
}
