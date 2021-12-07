package com.blockchain.core.custodial

import com.blockchain.featureflags.GatedFeature
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.remoteconfig.FeatureFlag
import com.blockchain.remoteconfig.IntegratedFeatureFlag
import io.reactivex.rxjava3.core.Single

class BrokerageQuoteFeatureFlag(
    private val localApi: InternalFeatureFlagApi,
    private val remoteConfig: FeatureFlag
) : IntegratedFeatureFlag() {

    override fun isLocalEnabled(): Boolean = localApi.isFeatureEnabled(GatedFeature.NEW_PRICING_BROKERAGE_QUOTE)

    override fun isRemoteEnabled(): Single<Boolean> = remoteConfig.enabled
}
