package com.blockchain.core.asset.data

import com.blockchain.api.assetdiscovery.data.AssetInformationDto
import com.blockchain.api.services.DetailedAssetInformation
import com.blockchain.core.asset.data.dataresources.AssetInformationStore
import com.blockchain.core.asset.domain.AssetService
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.mapData
import info.blockchain.balance.AssetInfo
import kotlinx.coroutines.flow.Flow

class AssetRepository(
    private val assetInformationStore: AssetInformationStore
) : AssetService {

    override fun getAssetInformation(
        asset: AssetInfo
    ): Flow<DataResource<DetailedAssetInformation>> {
        return assetInformationStore
            .stream(
                FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
                    .withKey(AssetInformationStore.Key(asset.networkTicker))
            )
            .mapData {
                it.toAssetInfo()
            }
    }

    private fun AssetInformationDto.toAssetInfo(): DetailedAssetInformation =
        DetailedAssetInformation(
            description = description.orEmpty(),
            website = website.orEmpty(),
            whitepaper = whitepaper.orEmpty()
        )
}
