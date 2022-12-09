package com.blockchain.prices.navigation

import androidx.compose.runtime.Stable
import com.blockchain.chrome.navigation.AppNavigation
import info.blockchain.balance.AssetInfo

@Stable
interface PricesNavigation : AppNavigation {
    fun coinview(asset: AssetInfo)
}
