package com.blockchain.core.dynamicassets.impl

import assets.CachedAssetInfo
import com.blockchain.core.Database
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency

internal class AssetInfoCache(
    private val database: Database
) {
    fun fetch(): List<AssetInfo> =
        database.cachedAssetInfoQueries
            .selectCrypto()
            .executeAsList()
            .map { it.toAssetInfo() }

    fun put(list: List<AssetInfo>, timestamp: Long) {
        list.forEach { database.cachedAssetInfoQueries.upsert(it.toCachedAssetInfo()) }
    }
}

private fun AssetInfo.toCachedAssetInfo(): CachedAssetInfo =
    CachedAssetInfo(
        networkTicker = networkTicker,
        displayTicker = displayTicker,
        name = name,
        isFiat = true,
        categories = "categories",
        precisionDp = precisionDp,
        requiredConfirmations = requiredConfirmations,
        l1chain = l1chainTicker,
        l2identifier = l2identifier,
        colour = colour,
        logo = logo,
        txExplorerUrlBase = txExplorerUrlBase
    )

private fun CachedAssetInfo.toAssetInfo(): AssetInfo {
    require(!isFiat)

    return CryptoCurrency(
        networkTicker = networkTicker,
        displayTicker = displayTicker,
        name = name,
        categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
        precisionDp = precisionDp,
        requiredConfirmations = requiredConfirmations,
        l1chainTicker = l1chain,
        l2identifier = l2identifier,
        colour = colour,
        logo = logo,
        txExplorerUrlBase = txExplorerUrlBase
    )
}
