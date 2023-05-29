package com.blockchain.transactions.upsell.viewmodel

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import com.blockchain.prices.domain.AssetPriceInfo

data class UpsellAnotherAssetModelState(
    val assetsToUpSell: DataResource<List<AssetPriceInfo>> = DataResource.Loading,
    val isLoading: Boolean = true
) : ModelState
