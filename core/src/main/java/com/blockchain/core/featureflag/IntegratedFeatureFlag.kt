package com.blockchain.core.featureflag

import com.blockchain.featureflag.FeatureFlag
import com.blockchain.preferences.FeatureFlagOverridePrefs
import com.blockchain.preferences.FeatureFlagState
import io.reactivex.rxjava3.core.Single
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

    override val isEnabled: Boolean
        get() = when (FeatureFlagState.valueOf(prefs.getFeatureState(key))) {
            FeatureFlagState.ENABLED -> true
            FeatureFlagState.DISABLED -> false
            FeatureFlagState.REMOTE -> remoteFlag.isEnabled
        }

    override suspend fun coEnabled(): Boolean = enabled.await()
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
    override val isEnabled: Boolean
        get() = when (FeatureFlagState.valueOf(prefs.getFeatureState(key))) {
            FeatureFlagState.ENABLED -> true
            FeatureFlagState.DISABLED -> false
            else -> defaultValue
        }
    override suspend fun coEnabled(): Boolean = isEnabled
}
