package com.blockchain.coincore.loader

import com.blockchain.api.coinnetworks.data.CoinNetworkDto
import com.blockchain.api.services.AssetDiscoveryApiService
import com.blockchain.api.services.DynamicAssetList
import com.blockchain.api.services.DynamicAssetProducts
import com.blockchain.core.chains.EvmNetwork
import com.blockchain.core.chains.EvmNetworksService
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.data.RefreshStrategy
import com.blockchain.domain.wallet.NetworkType
import com.blockchain.outcome.getOrDefault
import com.blockchain.outcome.map
import com.blockchain.store.asSingle
import com.blockchain.store.firstOutcome
import com.blockchain.utils.rxSingleOutcome
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.rx3.rxSingle

class NonCustodialL2sDynamicAssetRepository(
    private val discoveryService: AssetDiscoveryApiService,
    private val l2Store: NonCustodialL2sDynamicAssetStore,
    private val evmNetworksService: Lazy<EvmNetworksService>,
    private val coinNetworksStore: CoinNetworksStore
) {
    fun availableL2s(): Single<DynamicAssetList> {
        return getL2sForSupportedL1s()
    }

    fun allEvmAssets(): Single<DynamicAssetList> {
        return coinNetworksStore.stream(
            FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
        )
            .asSingle()
            .flatMap { evmNetworks ->
                getL1AssetsForNetworks(
                    evmNetworks.filter { network -> network.type == NetworkType.EVM }
                        .mapNotNull { it.toEvmNetwork() }
                )
            }
    }

    private fun getL1AssetsForNetworks(networks: List<EvmNetwork>) =
        rxSingleOutcome {
            discoveryService.getL1Coins().map { l1Coins ->
                l1Coins.mapNotNull { coin ->
                    networks.find {
                        it.networkTicker == coin.networkTicker || it.networkTicker == coin.displayTicker
                    }?.let { network ->
                        // TODO(dtverdota) remove once PK product is added to coin definitions
                        coin.copy(
                            products = coin.products.plus(DynamicAssetProducts.PrivateKey),
                            explorerUrl = network.explorerUrl
                        )
                    }
                }
            }
        }

    private var evmNetworksCache = listOf<EvmNetwork>()

    fun allEvmNetworks(): Single<List<EvmNetwork>> {
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

    fun getEvmNetworkForCurrency(currency: String): Maybe<EvmNetwork> {
        return coinNetworksStore.stream(
            FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
        )
            .asSingle()
            .flatMapMaybe { evmNetworks ->
                evmNetworks.first { network ->
                    network.type == NetworkType.EVM && network.nativeAsset == currency
                }
                    .toEvmNetwork()?.let { evmNetwork ->
                        Maybe.just(evmNetwork)
                    } ?: Maybe.empty()
            }
    }

    fun otherEvmNetworks(): Single<List<EvmNetwork>> {
        return coinNetworksStore.stream(
            FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
        )
            .asSingle()
            .map { evmNetworks ->
                evmNetworks.filter { network ->
                    network.type == NetworkType.EVM && network.nativeAsset != CryptoCurrency.ETHER.networkTicker
                }
                    .mapNotNull { it.toEvmNetwork() }
            }
    }

    private fun getL2sForSupportedL1s(): Single<DynamicAssetList> {
        return otherEvmNetworks()
            .flatMap { evmAssets ->
                rxSingle {
                    l2Store.stream(
                        FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
                            .withKey(NonCustodialL2sDynamicAssetStore.Key(evmAssets.map { it.networkTicker }))
                    )
                        .firstOutcome()
                        .getOrDefault(emptyList())
                }
            }
    }

    private fun CoinNetworkDto.toEvmNetwork(): EvmNetwork? =
        if (type == NetworkType.EVM) {
            identifiers.chainId?.let { chainId ->
                network?.let {
                    EvmNetwork(
                        networkTicker = it,
                        networkName = name,
                        nativeAsset = this.nativeAsset,
                        chainId = chainId,
                        nodeUrl = nodeUrls.first(),
                        explorerUrl = explorerUrl
                    )
                }
            }
        } else null
}
