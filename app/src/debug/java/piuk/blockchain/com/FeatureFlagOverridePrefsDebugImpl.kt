package piuk.blockchain.com

import android.content.SharedPreferences
import com.blockchain.preferences.FeatureFlagOverridePrefs
import com.blockchain.preferences.FeatureFlagState

class FeatureFlagOverridePrefsDebugImpl(private val store: SharedPreferences) : FeatureFlagOverridePrefs {

    override fun getFeatureState(featureFlagKey: String): String =
        store.getString(featureFlagKey, FeatureFlagState.REMOTE.toString()).orEmpty()

    override fun setFeatureState(featureFlagKey: String, featureFlagState: FeatureFlagState) {
        store.edit().putString(featureFlagKey, featureFlagState.toString()).apply()
    }
}
