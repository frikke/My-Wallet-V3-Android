package com.blockchain.coincore.loader

import com.blockchain.core.dynamicassets.DynamicAssetsDataManager
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.isCustodial
import info.blockchain.balance.l1chain
import io.reactivex.rxjava3.core.Single
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

class AssetCatalogueImpl internal constructor(
    private val fixedAssets: Set<AssetInfo>,
    private val assetsDataManager: DynamicAssetsDataManager
) : AssetCatalogue {

    private val fullAssetLookup: AtomicReference<Map<String, AssetInfo>> = AtomicReference(emptyMap())

    fun initialise(): Single<Set<AssetInfo>> =
        if (fullAssetLookup.get().isNotEmpty()) {
            Single.just(fullAssetLookup.get().values.toSet())
        } else {
            assetsDataManager.availableCryptoAssets()
                .map { list ->
                    // Remove any fixed assets that also appear in the dynamic set
                    list.filterNot { fixedAssets.contains(it) }
                }.doOnSuccess { enabledAssets ->
                    val allEnabledAssets = fixedAssets + enabledAssets
                    fullAssetLookup.set(
                        allEnabledAssets.associateBy { it.networkTicker.uppercase(Locale.ROOT) }
                    )
                }.map {
                    fullAssetLookup.get().values.toSet()
                }
        }

    // Brute force impl for now, but this will operate from a downloaded cache ultimately
    override fun fromNetworkTicker(symbol: String): AssetInfo? =
        fullAssetLookup.get()[symbol.uppercase()]

    override fun fromNetworkTickerWithL2Id(
        symbol: String,
        l2chain: AssetInfo,
        l2Id: String
    ): AssetInfo? =
        fromNetworkTicker(symbol)?.let { found ->
            found.takeIf {
                it.l1chain(this) == l2chain &&
                    it.l2identifier?.equals(l2Id, ignoreCase = true) == true
            }
        }

    override fun isFiatTicker(symbol: String): Boolean =
        supportedFiatAssets.contains(symbol)

    override val supportedCryptoAssets: List<AssetInfo> by lazy {
        fullAssetLookup.get().values.toList()
    }

    override val supportedCustodialAssets: List<AssetInfo> by lazy {
        fullAssetLookup.get().values.filter { it.isCustodial }
    }

    // TEMP: This will come from the /fiat BE supported assets call
    override val supportedFiatAssets: List<String> = listOf("EUR", "USD", "GBP")

    override fun supportedL2Assets(chain: AssetInfo): List<AssetInfo> =
        supportedCryptoAssets.filter { it.l1chainTicker == chain.networkTicker }
}
