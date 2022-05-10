package com.blockchain.core.price.impl.assetpricestore

import com.blockchain.core.price.impl.SupportedTickerList

internal data class SupportedTickerGroup(
    val baseTickers: SupportedTickerList,
    val fiatQuoteTickers: SupportedTickerList
)
