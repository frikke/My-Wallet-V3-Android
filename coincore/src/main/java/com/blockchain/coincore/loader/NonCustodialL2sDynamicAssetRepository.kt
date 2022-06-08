package com.blockchain.coincore.loader

import com.blockchain.api.services.AssetDiscoveryApiService
import com.blockchain.api.services.DynamicAssetList
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.outcome.fold
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.rx3.rxSingle

class NonCustodialL2sDynamicAssetRepository(
    private val l1EvmAssets: Set<CryptoCurrency>,
    private val discoveryService: AssetDiscoveryApiService,
    private val layerTwoFeatureFlag: Lazy<FeatureFlag>
) {
    fun availableL2s(): Single<DynamicAssetList> {
        return layerTwoFeatureFlag.value.enabled.flatMap {
            if (it) {
                getL2sForSupportedL1s()
            } else Single.just(emptyList())
        }
    }

    private fun getL2sForSupportedL1s(): Single<DynamicAssetList> {
        return rxSingle {
            l1EvmAssets.map { asset ->
                discoveryService.getL2AssetsForL1(asset.displayTicker.lowercase())
                    .fold(
                        onFailure = { throw it.throwable },
                        onSuccess = { it }
                    )
            }.flatten()
        }
    }
}
