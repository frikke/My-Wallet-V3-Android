package com.blockchain.core.featureflag

import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.api.getuser.data.GetUserStore
import com.blockchain.outcome.getOrDefault
import com.blockchain.preferences.FeatureFlagOverridePrefs
import com.blockchain.preferences.FeatureFlagState
import com.blockchain.store.asSingle
import com.blockchain.utils.awaitOutcome
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx3.await
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class IntegratedFeatureFlag(private val remoteFlag: FeatureFlag) : FeatureFlag by remoteFlag, KoinComponent {

    private val prefs: FeatureFlagOverridePrefs by inject()

    override val enabled: Single<Boolean>
        get() = when (FeatureFlagState.valueOf(prefs.getFeatureState(key))) {
            FeatureFlagState.ENABLED -> Single.just(true)
            FeatureFlagState.DISABLED -> Single.just(false)
            FeatureFlagState.REMOTE -> remoteFlag.enabled
        }.onErrorReturnItem(false)

    override suspend fun coEnabled(): Boolean = enabled.awaitOutcome().getOrDefault(false)
}

// TODO(aromano): CASSY Remove when cassy goes live
class CassyAlphaTesterUserTagFeatureFlag(
    private val integratedFeatureFlag: IntegratedFeatureFlag
) : FeatureFlag by integratedFeatureFlag, KoinComponent {

    private val environmentConfig: EnvironmentConfig by inject()
    private val getUserStore: GetUserStore by scopedInject()

    override val enabled: Single<Boolean>
        get() = integratedFeatureFlag.enabled.flatMap { isEnabled ->
            when {
                isEnabled -> Single.just(true)
                environmentConfig.isAlphaBuild() ->
                    getUserStore.stream(FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale))
                        .asSingle()
                        .map { user -> user.isCassyAlphaTester }
                else -> Single.just(false)
            }
        }
}

class LocalOnlyFeatureFlag(
    private val prefs: FeatureFlagOverridePrefs,
    override val key: String,
    override val readableName: String,
    val defaultValue: Boolean
) : FeatureFlag {
    override val enabled: Single<Boolean>
        get() = when (FeatureFlagState.valueOf(prefs.getFeatureState(key))) {
            FeatureFlagState.ENABLED -> Single.just(true)
            FeatureFlagState.DISABLED -> Single.just(false)
            else -> Single.just(defaultValue)
        }

    override suspend fun coEnabled(): Boolean = enabled.await()
}
