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
    ENABLE_DYNAMIC_ASSETS("Enable dynamic assets and split dashboard", true),
    AUTOCOMPLETE_ADDRESS("Enable autocomplete address kyc flow"),
    WALLET_REDESIGN("Enable wallet redesign", false)
}
