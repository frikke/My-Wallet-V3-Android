package com.blockchain.coincore.loader

import com.blockchain.api.services.AssetDiscoveryApiService
import com.blockchain.api.services.DynamicAsset
import com.blockchain.core.dynamicassets.CryptoAssetList
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single

class NonCustodialDynamicAssetRepository(
    private val discoveryService: AssetDiscoveryApiService,
    private val fixedAssets: Set<AssetInfo>,
    private val l2sDynamicAssetRepository: NonCustodialL2sDynamicAssetRepository
) : DynamicAssetsService {
    override fun availableCryptoAssets(): Single<CryptoAssetList> =
        Single.zip(
            discoveryService.getErc20Assets(),
            l2sDynamicAssetRepository.availableL2s()
        ) { erc20, evmList ->
            val cryptoAssets = erc20 + evmList
            cryptoAssets.asSequence().filterNot { it.isFiat }
                .toSet() // Remove dups
                .mapNotNull { dynamicAsset ->
                    dynamicAsset.withOnlyNonCustodialSupport()
                }
                .map { it.toAssetInfo() }.toList()
        }.map { dynamic ->
            dynamic.filterNot { fixedAssets.contains(it) }.plus(fixedAssets)
        }
}

private fun DynamicAsset.withOnlyNonCustodialSupport(): DynamicAsset? {
    return products.filter {
        nonCustodialProducts.contains(it)
    }.takeIf { it.isNotEmpty() }?.toSet()?.let {
        this.copy(products = products.intersect(nonCustodialProducts))
    }
}
