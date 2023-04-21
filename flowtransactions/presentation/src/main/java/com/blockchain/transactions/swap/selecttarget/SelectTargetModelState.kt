package com.blockchain.transactions.swap.selecttarget

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import com.blockchain.prices.domain.AssetPriceInfo

data class SelectTargetModelState(
    val prices: DataResource<List<AssetPriceInfo>> = DataResource.Loading
) : ModelState
