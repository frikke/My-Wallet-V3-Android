package com.blockchain.remoteconfig

import com.blockchain.domain.experiments.RemoteConfigService
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.outcome.getOrDefault
import com.blockchain.utils.awaitOutcome
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.rx3.await

fun RemoteConfigService.featureFlag(key: String, name: String): FeatureFlag = object : FeatureFlag {
    override val key: String = key
    override val readableName: String = name
    override val enabled: Single<Boolean> get() = getIfFeatureEnabled(key)

    override suspend fun coEnabled(): Boolean = enabled.awaitOutcome().getOrDefault(false)
}
