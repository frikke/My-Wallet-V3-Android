package com.blockchain.featureflags

interface InternalFeatureFlagApi {
    fun isFeatureEnabled(gatedFeature: GatedFeature): Boolean
    fun enable(gatedFeature: GatedFeature)
    fun disable(gatedFeature: GatedFeature)
    fun disableAll()
    fun getAll(): Map<GatedFeature, Boolean>
}

enum class GatedFeature(
    val readableName: String,
    val enabledForCompanyInternalBuild: Boolean = false
) {
    ADD_SUB_WALLET_ADDRESSES("Create BTC sub-wallets"),
    SEAMLESS_LIMITS("New transaction flow errors and Limits API", true),
    SETTINGS_FEATURE_LIMITS("Enable Features & Limits in Settings", true),
    ENABLE_DYNAMIC_ASSETS("Enable dynamic assets and split dashboard", true),
    WALLET_REDESIGN("Enable wallet redesign", true),
    STRIPE_CHECKOUT_PAYMENTS("Enable Stripe and Checkout", false)
}
