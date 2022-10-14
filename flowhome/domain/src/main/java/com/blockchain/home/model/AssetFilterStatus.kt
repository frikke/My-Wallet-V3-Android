package com.blockchain.home.model

sealed interface AssetFilter {
    object ShowSmallBalances : AssetFilter {
        /**
         * This will represent 1USD/1EUR..
         * depending on the current fiat selected
         */
        val MinimumBalance = 1.toBigDecimal()
    }
}

data class AssetFilterStatus(
    val filter: AssetFilter,
    val isEnabled: Boolean
)
