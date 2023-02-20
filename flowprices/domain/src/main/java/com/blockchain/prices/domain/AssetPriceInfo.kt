package com.blockchain.prices.domain

import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.data.DataResource
import info.blockchain.balance.AssetInfo

data class AssetPriceInfo(
    val price: DataResource<Prices24HrWithDelta>,
    val assetInfo: AssetInfo,
    val isTradable: Boolean,
    val isInWatchlist: Boolean
)

object MostPopularTickers : ArrayList<String>()
