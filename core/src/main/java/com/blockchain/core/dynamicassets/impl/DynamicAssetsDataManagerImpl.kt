package com.blockchain.core.dynamicassets.impl

import com.blockchain.api.assetdiscovery.data.AssetInformationDto
import com.blockchain.api.services.AssetDiscoveryApiService
import com.blockchain.api.services.DetailedAssetInformation
import com.blockchain.api.services.DynamicAsset
import com.blockchain.core.dynamicassets.DynamicAssetsDataManager
import com.blockchain.core.dynamicassets.FiatAssetList
import com.blockchain.outcome.map
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.androidcore.utils.extensions.rxSingleOutcome

internal class DynamicAssetsDataManagerImpl(
    private val discoveryService: AssetDiscoveryApiService
) : DynamicAssetsDataManager {

    override fun availableFiatAssets(): Single<FiatAssetList> =
        discoveryService.getFiatAssets()
            .map { list -> list.map { it.toFiatCurrency() } }

    override fun getAssetInformation(asset: AssetInfo): Single<DetailedAssetInformation> =
        rxSingleOutcome {
            discoveryService.getAssetInformation(assetTicker = asset.networkTicker)
                .map { it.toAssetInfo() }
                .map { info ->
                    DetailedAssetInformation(
                        description = info?.description.orEmpty(),
                        website = info?.website.orEmpty(),
                        whitepaper = info?.whitepaper.orEmpty()
                    )
                }
        }
}

private fun AssetInformationDto.toAssetInfo(): DetailedAssetInformation? =
    if (description != null && website != null) {
        DetailedAssetInformation(
            description = description!!,
            website = website!!,
            whitepaper = whitepaper.orEmpty()
        )
    } else {
        null
    }

private fun DynamicAsset.toFiatCurrency(): FiatCurrency =
    FiatCurrency.fromCurrencyCode(this.networkTicker)
