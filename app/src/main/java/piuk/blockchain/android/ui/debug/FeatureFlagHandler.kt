package piuk.blockchain.android.ui.debug

import com.blockchain.preferences.FeatureFlagOverridePrefs
import com.blockchain.preferences.FeatureFlagState
import com.blockchain.remoteconfig.FeatureFlag

class FeatureFlagHandler(
    private val featureFlags: List<FeatureFlag>,
    private val prefs: FeatureFlagOverridePrefs
) {
    fun getAllFeatureFlags(): Map<FeatureFlag, FeatureFlagState> {
        return featureFlags.associateWith { flag -> FeatureFlagState.valueOf(prefs.getFeatureState(flag.key)) }
    }

    fun setFeatureFlagState(featureFlag: FeatureFlag, state: FeatureFlagState) {
        prefs.setFeatureState(featureFlag.key, state)
    }
}
