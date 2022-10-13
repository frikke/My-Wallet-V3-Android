package com.blockchain.home.model

enum class AssetFilter {
    ShowSmallBalances;

    companion object {
        val MinimumBalance = 1.toBigDecimal()
    }
}

data class AssetFilterStatus(
    val filter: AssetFilter,
    val isEnabled: Boolean
)
