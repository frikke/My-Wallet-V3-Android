package com.blockchain.core.dynamicassets

import com.blockchain.api.services.DetailedAssetInformation
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Single

typealias CryptoAssetList = List<AssetInfo>
typealias FiatAssetList = List<FiatCurrency>

interface DynamicAssetsDataManager {
    fun availableFiatAssets(): Single<FiatAssetList>

    @Deprecated("use flow AssetService:getAssetInformation")
    fun getAssetInformation(asset: AssetInfo): Single<DetailedAssetInformation>
}
