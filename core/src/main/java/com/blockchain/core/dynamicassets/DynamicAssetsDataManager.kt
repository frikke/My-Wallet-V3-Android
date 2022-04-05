package com.blockchain.core.dynamicassets

import com.blockchain.api.services.DetailedAssetInformation
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Single

typealias CryptoAssetList = List<AssetInfo>
typealias FiatAssetList = List<FiatCurrency>

interface DynamicAssetsDataManager {
    fun availableCryptoAssets(): Single<CryptoAssetList>
    fun availableFiatAssets(): Single<FiatAssetList>
    fun getAssetInformation(asset: AssetInfo): Single<DetailedAssetInformation>
}
