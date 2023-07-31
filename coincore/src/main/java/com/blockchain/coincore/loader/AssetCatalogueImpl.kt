package com.blockchain.coincore.loader

import com.blockchain.core.dynamicassets.DynamicAssetsDataManager
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.isCustodial
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

class AssetCatalogueImpl internal constructor(
    private val assetsService: DynamicAssetsService,
    private val assetsDataManager: DynamicAssetsDataManager
) : AssetCatalogue {

    private val fullAssetLookup: AtomicReference<Map<String, Currency>> = AtomicReference(emptyMap())

    fun initialise(): Single<Set<Currency>> =
        if (fullAssetLookup.get().isNotEmpty()) {
            Single.just(fullAssetLookup.get().values.toSet())
        } else {
            assetsService.availableCryptoAssets().zipWith(assetsDataManager.availableFiatAssets())
                .map { (cryptos, fiats) ->
                    cryptos.plus(fiats)
                }.doOnSuccess { enabledAssets ->
                    fullAssetLookup.set(
                        enabledAssets.associateBy { it.networkTicker.uppercase(Locale.ROOT) }
                    )
                }.map {
                    fullAssetLookup.get().values.toSet()
                }
        }

    // Brute force impl for now, but this will operate from a downloaded cache ultimately
    override fun fromNetworkTicker(symbol: String): Currency? =
        fullAssetLookup.get()[symbol.uppercase()]

    override fun fiatFromNetworkTicker(symbol: String): FiatCurrency? =
        fullAssetLookup.get()[symbol.uppercase()]?.asFiatCurrencyOrNull()

    override fun assetInfoFromNetworkTicker(symbol: String): AssetInfo? =
        fullAssetLookup.get()[symbol.uppercase()]?.asAssetInfoOrNull()

    override fun assetFromL1ChainByContractAddress(
        l1chain: String,
        contractAddress: String
    ): AssetInfo? = fullAssetLookup.get().values.filterIsInstance<AssetInfo>().firstOrNull { asset ->
        asset.coinNetwork?.networkTicker == l1chain &&
            asset.l2identifier?.equals(contractAddress, ignoreCase = true) == true
    }

    private fun Currency.asAssetInfoOrNull(): AssetInfo? {
        return (this as? AssetInfo)
    }

    private fun Currency?.asFiatCurrencyOrNull(): FiatCurrency? {
        return (this as? FiatCurrency)
    }

    override val supportedCryptoAssets: List<AssetInfo>
        get() = fullAssetLookup.get().values.filterIsInstance<AssetInfo>().toList()

    override val supportedCustodialAssets: List<AssetInfo>
        get() = supportedCryptoAssets.filter { it.isCustodial }

    override val supportedFiatAssets: List<FiatCurrency>
        get() = fullAssetLookup.get().values.filterIsInstance<FiatCurrency>().toList()

    override fun supportedL2Assets(chain: AssetInfo): List<AssetInfo> =
        supportedCryptoAssets.filter { it.coinNetwork?.networkTicker == chain.networkTicker }
}
