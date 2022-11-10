package com.blockchain.coincore.loader

import com.blockchain.api.services.AssetDiscoveryApiService
import com.blockchain.api.services.DynamicAsset
import com.blockchain.api.services.DynamicAssetProducts
import com.blockchain.core.chains.EvmNetwork
import com.blockchain.domain.wallet.CoinNetwork
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.map
import com.blockchain.utils.rxSingleOutcome
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single

class UniversalDynamicAssetRepository(
    private val dominantL1Assets: Set<AssetInfo>,
    private val discoveryService: AssetDiscoveryApiService,
    private val l2sDynamicAssetRepository: NonCustodialL2sDynamicAssetRepository
) : DynamicAssetsService {
    override fun availableCryptoAssets(): Single<List<AssetInfo>> {
        return Single.zip(
            discoveryService.getErc20Assets(),
            discoveryService.getCustodialAssets(),
            l2sDynamicAssetRepository.allEvmAssets(),
            l2sDynamicAssetRepository.availableL2s()
        ) { erc20, custodial, evmCoins, evmList ->
            val cryptoAssets = erc20 + custodial + evmCoins + evmList
            cryptoAssets.asSequence().filterNot { it.isFiat }
                .toSet() // Remove dups
                .asSequence()
                .filter { it.supportsAnyCustodialOrNonCustodialProducts() }
                .mapNotNull { it.toAssetInfo(evmCoins.map { it.displayTicker }) }
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

    // Returns the AssetInfo for every Coin from coin definitions with the network type EVM except Ethereum
    override fun otherEvmAssets(): Single<List<AssetInfo>> {
        return l2sDynamicAssetRepository.otherEvmAssets()
            .map { it.mapNotNull { asset -> asset.toAssetInfo() } }
    }

    // Returns the list of EvmNetworks from the coin networks service including Ethereum
    override fun allEvmNetworks(): Single<List<EvmNetwork>> = l2sDynamicAssetRepository.allEvmNetworks()

    override fun getEvmNetworkForCurrency(currency: String): Maybe<EvmNetwork> =
        l2sDynamicAssetRepository.getEvmNetworkForCurrency(currency)

    // Returns the list of EvmNetworks from the coin networks service except Ethereum
    override fun otherEvmNetworks(): Single<List<EvmNetwork>> = l2sDynamicAssetRepository.otherEvmNetworks()

    override suspend fun allNetworks(): Outcome<Exception, List<CoinNetwork>> {
        return discoveryService.allNetworks().map { coinNetworks ->
            coinNetworks.map { coinNetworkDto ->
                CoinNetwork(
                    explorerUrl = coinNetworkDto.explorerUrl,
                    currency = coinNetworkDto.currency,
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
