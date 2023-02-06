package com.blockchain.coincore.loader

import com.blockchain.api.coinnetworks.data.CoinNetworkDto
import com.blockchain.core.chains.ethereum.EvmNetworksService
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.outcome.map
import com.blockchain.store.asSingle
import info.blockchain.balance.CoinNetwork
import info.blockchain.balance.NetworkType
import io.reactivex.rxjava3.core.Single

class NonCustodialL2sDynamicAssetRepository(
    private val coinNetworksStore: CoinNetworksStore
) : EvmNetworksService {

    private var evmNetworksCache = listOf<CoinNetwork>()

    override fun allEvmNetworks(): Single<List<CoinNetwork>> {
        return if (evmNetworksCache.isNotEmpty()) {
            Single.just(evmNetworksCache)
        } else
            coinNetworksStore.stream(
                FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
            )
                .asSingle()
                .map { evmNetworks ->
                    evmNetworks.filter { network -> network.type == NetworkType.EVM }
                        .mapNotNull { it.toEvmNetwork() }
                }
                .doOnSuccess {
                    evmNetworksCache = it
                }
    }

    private fun CoinNetworkDto.toEvmNetwork(): CoinNetwork? =
        if (type == NetworkType.EVM) {
            require(identifiers.chainId != null)
            CoinNetwork(
                networkTicker = networkTicker,
                name = name,
                shortName = shortName,
                nativeAssetTicker = this.nativeAsset,
                chainId = identifiers.chainId!!,
                nodeUrls = nodeUrls,
                type = NetworkType.EVM,
                feeCurrencies = feeCurrencies,
                isMemoSupported = isMemoSupported,
                explorerUrl = explorerUrl
            )
        } else null
}
