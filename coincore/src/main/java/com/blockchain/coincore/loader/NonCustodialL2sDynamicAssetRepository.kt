package com.blockchain.coincore.loader

import com.blockchain.api.coinnetworks.data.CoinNetworkDto
import com.blockchain.api.services.AssetDiscoveryApiService
import com.blockchain.api.services.DynamicAssetList
import com.blockchain.api.services.DynamicAssetProducts
import com.blockchain.core.chains.EvmNetwork
import com.blockchain.core.chains.EvmNetworksService
import com.blockchain.core.chains.ethereum.EthDataManager
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.data.RefreshStrategy
import com.blockchain.domain.wallet.NetworkType
import com.blockchain.featureflag.FeatureFlag
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
    private val layerTwoFeatureFlag: Lazy<FeatureFlag>,
    private val coinNetworksFeatureFlag: Lazy<FeatureFlag>,
    private val evmNetworksService: Lazy<EvmNetworksService>,
    private val coinNetworksStore: CoinNetworksStore
) {
    fun availableL2s(): Single<DynamicAssetList> {
        return layerTwoFeatureFlag.value.enabled.flatMap {
            if (it) {
                getL2sForSupportedL1s()
            } else {
                Single.just(emptyList())
            }
        }
    }

    fun otherEvmAssets(): Single<DynamicAssetList> {
        return coinNetworksFeatureFlag.value.enabled.flatMap { isEnabled ->
            if (isEnabled) {
                coinNetworksStore.stream(
                    FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
                )
                    .asSingle()
                    .flatMap { evmNetworks ->
                        getL1AssetsForNetworks(
                            evmNetworks.filter { network ->
                                network.type == NetworkType.EVM &&
                                    network.currency != CryptoCurrency.ETHER.networkTicker
                            }
                                .mapNotNull { it.toEvmNetwork() }
                        )
                    }
            } else {
                evmNetworksService.value.getSupportedNetworks()
                    .flatMap { evmNetworks ->
                        getL1AssetsForNetworks(evmNetworks)
                    }
            }
        }
    }

    fun allEvmAssets(): Single<DynamicAssetList> {
        return coinNetworksFeatureFlag.value.enabled.flatMap { isEnabled ->
            if (isEnabled) {
                coinNetworksStore.stream(
                    FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
                )
                    .asSingle()
                    .flatMap { evmNetworks ->
                        getL1AssetsForNetworks(
                            evmNetworks.filter { network -> network.type == NetworkType.EVM }
                                .mapNotNull { it.toEvmNetwork() }
                        )
                    }
            } else {
                evmNetworksService.value.getSupportedNetworks()
                    .flatMap { evmNetworks ->
                        getL1AssetsForNetworks(evmNetworks.plus(EthDataManager.ethChain))
                    }
            }
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
        } else coinNetworksFeatureFlag.value.enabled.flatMap { isEnabled ->
            if (isEnabled) {
                coinNetworksStore.stream(
                    FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
                )
                    .asSingle()
                    .map { evmNetworks ->
                        evmNetworks.filter { network -> network.type == NetworkType.EVM }
                            .mapNotNull { it.toEvmNetwork() }
                    }
            } else {
                evmNetworksService.value.getSupportedNetworks().map {
                    it.plus(EthDataManager.ethChain)
                }
            }
        }.doOnSuccess {
            evmNetworksCache = it
        }
    }

    fun getEvmNetworkForCurrency(currency: String): Maybe<EvmNetwork> {
        return coinNetworksFeatureFlag.value.enabled.flatMapMaybe { isEnabled ->
            if (isEnabled) {
                coinNetworksStore.stream(
                    FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
                )
                    .asSingle()
                    .flatMapMaybe { evmNetworks ->
                        evmNetworks.first { network ->
                            network.type == NetworkType.EVM && network.currency == currency
                        }
                            .toEvmNetwork()?.let { evmNetwork ->
                                Maybe.just(evmNetwork)
                            } ?: Maybe.empty()
                    }
            } else {
                evmNetworksService.value.getSupportedNetworkForCurrency(currency)
            }
        }
    }

    fun otherEvmNetworks(): Single<List<EvmNetwork>> {
        return coinNetworksFeatureFlag.value.enabled.flatMap { isEnabled ->
            if (isEnabled) {
                coinNetworksStore.stream(
                    FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
                )
                    .asSingle()
                    .map { evmNetworks ->
                        evmNetworks.filter { network ->
                            network.type == NetworkType.EVM && network.currency != CryptoCurrency.ETHER.networkTicker
                        }
                            .mapNotNull { it.toEvmNetwork() }
                    }
            } else {
                evmNetworksService.value.getSupportedNetworks()
            }
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
                        chainId = chainId,
                        nodeUrl = nodeUrls.first(),
                        explorerUrl = explorerUrl
                    )
                }
            }
        } else null
}
