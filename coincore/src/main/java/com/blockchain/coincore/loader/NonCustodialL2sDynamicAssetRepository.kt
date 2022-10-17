package com.blockchain.coincore.loader

import com.blockchain.api.coinnetworks.data.CoinNetwork
import com.blockchain.api.services.AssetDiscoveryApiService
import com.blockchain.api.services.DynamicAssetList
import com.blockchain.api.services.DynamicAssetProducts
import com.blockchain.core.chains.EvmNetwork
import com.blockchain.core.chains.EvmNetworksService
import com.blockchain.core.chains.ethereum.EthDataManager
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.outcome.getOrDefault
import com.blockchain.outcome.map
import com.blockchain.store.firstOutcome
import com.blockchain.utils.rxSingleOutcome
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.rx3.rxSingle

class NonCustodialL2sDynamicAssetRepository(
    private val discoveryService: AssetDiscoveryApiService,
    private val l2Store: NonCustodialL2sDynamicAssetStore,
    private val layerTwoFeatureFlag: Lazy<FeatureFlag>,
    private val coinNetworksFeatureFlag: Lazy<FeatureFlag>,
    private val evmNetworksService: Lazy<EvmNetworksService>
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
                discoveryService.otherEvmNetworks()
                    .flatMap { evmNetworks ->
                        getL1AssetsForNetworks(evmNetworks.mapNotNull { it.toEvmNetwork() })
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
                discoveryService.supportedEvmNetworks()
                    .flatMap { evmNetworks ->
                        getL1AssetsForNetworks(evmNetworks.mapNotNull { it.toEvmNetwork() })
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

    fun allEvmNetworks(): Single<List<EvmNetwork>> {
        return coinNetworksFeatureFlag.value.enabled.flatMap { isEnabled ->
            if (isEnabled) {
                discoveryService.supportedEvmNetworks().map { coinNetworks ->
                    coinNetworks.mapNotNull { network -> network.toEvmNetwork() }
                }
            } else {
                evmNetworksService.value.getSupportedNetworks().map {
                    it.plus(EthDataManager.ethChain)
                }
            }
        }
    }

    fun otherEvmNetworks(): Single<List<EvmNetwork>> {
        return coinNetworksFeatureFlag.value.enabled.flatMap { isEnabled ->
            if (isEnabled) {
                discoveryService.otherEvmNetworks().map { coinNetworks ->
                    coinNetworks.mapNotNull { network -> network.toEvmNetwork() }
                }
            } else {
                evmNetworksService.value.getSupportedNetworks()
            }
        }
    }

    private fun getL2sForSupportedL1s(): Single<DynamicAssetList> {
        return otherEvmAssets()
            .flatMap { evmAssets ->
                rxSingle {
                    l2Store.stream(
                        FreshnessStrategy.Cached(false)
                            .withKey(NonCustodialL2sDynamicAssetStore.Key(evmAssets.map { it.networkTicker }))
                    )
                        .firstOutcome()
                        .getOrDefault(emptyList())
                }
            }
    }

    private fun CoinNetwork.toEvmNetwork(): EvmNetwork? =
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
}
