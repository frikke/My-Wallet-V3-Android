package com.blockchain.deeplinking.navigation

sealed class Destination {
    data class AssetViewDestination(val networkTicker: String) : Destination()
    data class AssetBuyDestination(val code: String, val amount: String) : Destination()
    data class ActivityDestination(val filter: String? = null) : Destination()
}
