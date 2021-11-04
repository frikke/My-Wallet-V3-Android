package com.blockchain.coincore.loader

import com.blockchain.coincore.CryptoAsset
import com.blockchain.remoteconfig.FeatureFlag
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

// TODO this will change to support both fiat and crypto, when we have a common interface/class for both
interface AssetLoader {
    fun initAndPreload(): Completable
    val loadedAssets: List<CryptoAsset>

    operator fun get(asset: AssetInfo): CryptoAsset
}

internal class AssetLoaderSwitcher(
    private val featureFlag: FeatureFlag,
    private val dynamicLoader: DynamicAssetLoader,
    staticLoader: StaticAssetLoader
) : AssetLoader {

    private var useLoader: AssetLoader = staticLoader

    private val useDynamicLoader: Single<Boolean> by lazy {
        featureFlag.enabled
    }

    override fun initAndPreload(): Completable =
        useDynamicLoader.flatMapCompletable { enabled ->
            if (enabled) {
                useLoader = dynamicLoader
            }
            useLoader.initAndPreload()
        }

    override val loadedAssets: List<CryptoAsset> by lazy {
        useLoader.loadedAssets
    }

    override fun get(asset: AssetInfo): CryptoAsset =
        useLoader[asset]
}
