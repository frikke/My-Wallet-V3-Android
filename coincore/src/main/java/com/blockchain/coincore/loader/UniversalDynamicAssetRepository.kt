package com.blockchain.coincore.loader

import com.blockchain.api.services.AssetDiscoveryApiService
import com.blockchain.api.services.DynamicAsset
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single

class UniversalDynamicAssetRepository(
    private val discoveryService: AssetDiscoveryApiService,
    private val l2sDynamicAssetRepository: NonCustodialL2sDynamicAssetRepository
) : DynamicAssetsService {
    override fun availableCryptoAssets(): Single<List<AssetInfo>> {
        return Single.zip(
            discoveryService.getErc20Assets(),
            discoveryService.getCustodialAssets(),
            l2sDynamicAssetRepository.availableL2s()
        ) { erc20, custodial, evmList ->
            val cryptoAssets = erc20 + custodial + evmList
            cryptoAssets.filterNot { it.isFiat }
                .toSet() // Remove dups
                .filter { it.supportsAnyCustodialOrNonCustodialProducts() }
                .map { it.toAssetInfo() }
        }
    }
}

private fun DynamicAsset.supportsAnyCustodialOrNonCustodialProducts(): Boolean {
    return this.products.intersect(nonCustodialProducts + custodialProducts).isNotEmpty()
}
