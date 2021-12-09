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
    NEW_PRICING_BROKERAGE_QUOTE("New pricing quote api", true),
    FAB_SHEET_CTAS("Show Buy on RHS and Sell on LHS in the FAB bottom sheet", true),
    REDESIGN_PT2("Enable Redesign part 2", false)
}
