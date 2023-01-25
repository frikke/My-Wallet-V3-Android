package com.blockchain.coincore.loader

import com.blockchain.api.services.AssetDiscoveryApiService
import com.blockchain.api.services.DynamicAsset
import com.blockchain.api.services.DynamicAssetProducts
import com.blockchain.core.chains.EvmNetwork
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.domain.wallet.CoinNetwork
import com.blockchain.outcome.map
import com.blockchain.store.mapData
import com.blockchain.utils.rxSingleOutcome
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow

class UniversalDynamicAssetRepository(
    private val dominantL1Assets: Set<AssetInfo>,
    private val discoveryService: AssetDiscoveryApiService,
    private val l2sDynamicAssetRepository: NonCustodialL2sDynamicAssetRepository,
    private val coinNetworksStore: CoinNetworksStore
) : DynamicAssetsService {
    override fun availableCryptoAssets(): Single<List<AssetInfo>> {
        return Single.zip(
            discoveryService.getEthErc20s(),
            discoveryService.getCustodialAssets(),
            l2sDynamicAssetRepository.allEvmAssets(),
            l2sDynamicAssetRepository.getOtherEvmsErc20s(),
            l2sDynamicAssetRepository.allEvmNetworks()
        ) { erc20, custodial, evmCoins, evmList, evmNetworks ->
            val cryptoAssets = erc20 + custodial + evmCoins + evmList
            cryptoAssets.asSequence().filterNot { it.isFiat }
                .toSet() // Remove dups
                .asSequence()
                .filter { it.supportsAnyCustodialOrNonCustodialProducts() }
                .mapNotNull { it.toAssetInfo(evmNetworks) }
                .filterNot { it.networkTicker in dominantL1Assets.map { l1 -> l1.networkTicker } }
                .plus(dominantL1Assets)
                .toList()
        }
    }

    override fun availableL1Assets(): Single<List<AssetInfo>> = rxSingleOutcome {
        discoveryService.getL1Coins()
            .map { assets ->
                assets.map { asset ->
                    asset.copy(
                        products = asset.products.plus(DynamicAssetProducts.PrivateKey)
                    )
                }
            }
            .map { assets -> assets.mapNotNull { it.toAssetInfo() } }
    }

    override fun allEvmAssets(): Single<List<AssetInfo>> {
        return l2sDynamicAssetRepository.allEvmAssets()
            .map { it.map { asset -> asset.toAssetInfo() } }
    }

    // Returns the list of EvmNetworks from the coin networks service including Ethereum
    override fun allEvmNetworks(): Single<List<EvmNetwork>> = l2sDynamicAssetRepository.allEvmNetworks()

    override fun getEvmNetworkForCurrency(currency: String): Maybe<EvmNetwork> =
        l2sDynamicAssetRepository.getEvmNetworkForCurrency(currency)

    override fun allNetworks(): Flow<DataResource<List<CoinNetwork>>> {
        return coinNetworksStore.stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
            .mapData { coinNetworks ->
                coinNetworks.map { coinNetworkDto ->
                    CoinNetwork(
                        explorerUrl = coinNetworkDto.explorerUrl,
                        currency = coinNetworkDto.nativeAsset,
                        network = coinNetworkDto.network,
                        name = coinNetworkDto.name,
                        type = coinNetworkDto.type,
                        chainId = coinNetworkDto.identifiers.chainId,
                        nodeUrls = coinNetworkDto.nodeUrls,
                        feeCurrencies = coinNetworkDto.feeCurrencies,
                        isMemoSupported = coinNetworkDto.isMemoSupported
                    )
                }
            }
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
