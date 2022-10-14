package com.blockchain.coincore.loader

import com.blockchain.api.services.DynamicAssetList
import com.blockchain.data.FreshnessStrategy
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.outcome.getOrDefault
import com.blockchain.store.firstOutcome
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.rx3.rxSingle

class NonCustodialL2sDynamicAssetRepository(
    private val l2Store: NonCustodialL2sDynamicAssetStore,
    private val layerTwoFeatureFlag: Lazy<FeatureFlag>
) {
    fun availableL2s(): Single<DynamicAssetList> {
        return layerTwoFeatureFlag.value.enabled.flatMap {
            rxSingle {
                if (it) {
                    getL2sForSupportedL1s()
                } else {
                    emptyList()
                }
            }
        }
    }

    private suspend fun getL2sForSupportedL1s(): DynamicAssetList =
        l2Store.stream(FreshnessStrategy.Cached(false))
            .firstOutcome()
            .getOrDefault(emptyList())
}
