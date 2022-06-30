package com.blockchain.core.price.impl.assetpricestore

internal data class SupportedTickerGroup(
    val baseTickers: SupportedTickerList,
    val fiatQuoteTickers: SupportedTickerList
)
