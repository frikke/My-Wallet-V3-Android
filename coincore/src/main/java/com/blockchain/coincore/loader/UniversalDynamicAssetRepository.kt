package com.blockchain.coincore.loader

import com.blockchain.api.services.AssetDiscoveryApiService
import com.blockchain.api.services.DynamicAsset
import com.blockchain.api.services.DynamicAssetProducts
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.outcome.map
import com.blockchain.store.asSingle
import com.blockchain.store.mapData
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CoinNetwork
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow

class UniversalDynamicAssetRepository(
    private val discoveryService: AssetDiscoveryApiService,
    private val l2sDynamicAssetRepository: NonCustodialL2sDynamicAssetRepository,
    private val coinNetworksStore: CoinNetworksStore
) : DynamicAssetsService {
    override fun availableCryptoAssets(): Single<List<AssetInfo>> {
        return allNetworks().asSingle().flatMap { networks ->
            nativeAssetsForNetworks(networks)
                .flatMap { nativeAssets ->
                    discoveryService.getErc20s(networks).map { erc20s ->
                        nativeAssets + erc20s
                    }
                }.flatMap { nativeAndErc20s ->
                    discoveryService.getCustodialAssets().map { custodialAssets ->
                        nativeAndErc20s.plusOrMerge(custodialAssets)
                    }
                }.map { cryptoAssets ->
                    cryptoAssets.asSequence().filterNot { it.isFiat }
                        .toSet() // Remove dups
                        .asSequence()
                        .filter { it.supportsAnyCustodialOrNonCustodialProducts() }
                        .mapNotNull { it.toAssetInfo(networks) }
                        .toList()
                }
        }
    }

    private fun nativeAssetsForNetworks(networks: List<CoinNetwork>): Single<List<DynamicAsset>> {
        return discoveryService.getL1Coins().map {
            it.filter { dynamicAsset ->
                dynamicAsset.networkTicker in networks.map { network -> network.nativeAssetTicker }
            }.map { asset ->
                asset.copy(products = asset.products.plus(DynamicAssetProducts.PrivateKey))
            }
        }
    }

    override fun allEvmNetworks(): Single<List<CoinNetwork>> = l2sDynamicAssetRepository.allEvmNetworks()

    override fun allNetworks(): Flow<DataResource<List<CoinNetwork>>> {
        return coinNetworksStore.stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
            .mapData { coinNetworks ->
                coinNetworks.map { coinNetworkDto ->
                    CoinNetwork(
                        explorerUrl = coinNetworkDto.explorerUrl,
                        nativeAssetTicker = coinNetworkDto.nativeAsset,
                        networkTicker = coinNetworkDto.networkTicker,
                        name = coinNetworkDto.name,
                        shortName = coinNetworkDto.shortName,
                        type = coinNetworkDto.type,
                        chainId = coinNetworkDto.identifiers.chainId,
                        nodeUrls = coinNetworkDto.nodeUrls,
                        feeCurrencies = coinNetworkDto.feeCurrencies,
                        isMemoSupported = coinNetworkDto.isMemoSupported
                    )
                }
            }
    }

    private fun List<DynamicAsset>.plusOrMerge(assets: List<DynamicAsset>): List<DynamicAsset> {
        val list = this.toMutableList()
        assets.filter { it !in list }.forEach { asset ->
            if (asset.networkTicker in list.map { it.networkTicker }) {
                val assetToBeMerged = list.first { it.networkTicker == asset.networkTicker }
                list.apply {
                    remove(assetToBeMerged)
                    add(
                        assetToBeMerged.copy(
                            products = assetToBeMerged.products.plus(asset.products)
                        )
                    )
                }
            } else {
                list.add(asset)
            }
        }
        return list
    }
}

private fun DynamicAsset.supportsAnyCustodialOrNonCustodialProducts(): Boolean {
    return this.products.intersect(
        setOf(
            DynamicAssetProducts.PrivateKey,
            DynamicAssetProducts.DynamicSelfCustody,
            DynamicAssetProducts.CustodialWalletBalance,
            DynamicAssetProducts.InterestBalance
        )
    ).isNotEmpty()
}
