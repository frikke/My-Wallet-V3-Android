package com.blockchain.core.asset.domain

import com.blockchain.api.services.DetailedAssetInformation
import com.blockchain.data.DataResource
import info.blockchain.balance.AssetInfo
import kotlinx.coroutines.flow.Flow

interface AssetService {

    fun getAssetInformation(
        asset: AssetInfo
    ): Flow<DataResource<DetailedAssetInformation>>
}
