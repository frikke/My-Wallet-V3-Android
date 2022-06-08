package com.blockchain.coincore.loader

import com.blockchain.api.services.AssetDiscoveryApiService
import com.blockchain.api.services.DynamicAsset
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single

class CustodialOnlyDynamicAssetsRepository(private val discoveryService: AssetDiscoveryApiService) :
    DynamicAssetsService {
    override fun availableCryptoAssets(): Single<List<AssetInfo>> =
        discoveryService.getCustodialAssets().map { cryptoAssets ->
            cryptoAssets.toSet() // Remove dups
                .mapNotNull { dynamicAsset ->
                    dynamicAsset.withOnlyCustodialSupport()
                }
                .map { it.toAssetInfo() }.toList()
        }

    private fun DynamicAsset.withOnlyCustodialSupport(): DynamicAsset? {
        return products.filter {
            custodialProducts.contains(it)
        }.takeIf { it.isNotEmpty() }?.toSet()?.let {
            this.copy(products = products.intersect(custodialProducts))
        }
    }
}
