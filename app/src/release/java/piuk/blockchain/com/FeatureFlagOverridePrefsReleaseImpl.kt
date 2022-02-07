package piuk.blockchain.com

import com.blockchain.preferences.FeatureFlagOverridePrefs
import com.blockchain.preferences.FeatureFlagState

class FeatureFlagOverridePrefsReleaseImpl : FeatureFlagOverridePrefs {

    override fun getFeatureState(featureFlagKey: String): String = FeatureFlagState.REMOTE.toString()

    override fun setFeatureState(featureFlagKey: String, featureFlagState: FeatureFlagState) {
        // Do nothing, the state is always retrieved from Firebase/remote config
    }
}
