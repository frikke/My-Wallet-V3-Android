package com.blockchain.remoteconfig

import com.blockchain.preferences.FeatureFlagOverridePrefs
import com.blockchain.preferences.FeatureFlagState
import io.reactivex.rxjava3.core.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class IntegratedFeatureFlag(private val remoteFlag: FeatureFlag) : FeatureFlag by remoteFlag, KoinComponent {

    private val prefs: FeatureFlagOverridePrefs by inject()

    override val enabled: Single<Boolean>
        get() = when (FeatureFlagState.valueOf(prefs.getFeatureState(key))) {
            FeatureFlagState.ENABLED -> Single.just(true)
            FeatureFlagState.DISABLED -> Single.just(false)
            FeatureFlagState.REMOTE -> remoteFlag.enabled.cache()
        }

    override val isEnabled: Boolean
        get() = remoteFlag.isEnabled
}
