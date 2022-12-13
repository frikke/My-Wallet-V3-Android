package com.blockchain.prices.navigation

import androidx.compose.runtime.Stable
import info.blockchain.balance.AssetInfo

@Stable
interface PricesNavigation {
    fun coinview(asset: AssetInfo)
}
