package com.blockchain.preferences

enum class FeatureFlagState {
    ENABLED,
    DISABLED,
    REMOTE
}

interface FeatureFlagOverridePrefs {
    fun getFeatureState(featureFlagKey: String): String
    fun setFeatureState(featureFlagKey: String, featureFlagState: FeatureFlagState)
}
