package com.blockchain.core.asset.domain

import com.blockchain.api.services.DetailedAssetInformation
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import info.blockchain.balance.AssetInfo
import kotlinx.coroutines.flow.Flow

interface AssetService {

    fun getAssetInformation(
        asset: AssetInfo,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.ForceRefresh)
    ): Flow<DataResource<DetailedAssetInformation>>
}
