package com.blockchain.core.dynamicassets

import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single

data class FiatInfo(
    val displayTicker: String,
    val networkTicker: String,
    val name: String
)

typealias CryptoAssetList = List<AssetInfo>
typealias FiatAssetList = List<FiatInfo>

interface DynamicAssetsDataManager {
    fun availableCryptoAssets(): Single<CryptoAssetList>
    fun availableFiatAssets(): Single<FiatAssetList>
}
