package com.blockchain.core.price.impl.assetpricestore

import kotlinx.serialization.Serializable

@Serializable
internal data class SupportedTickerGroup(
    val baseTickers: SupportedTickerList,
    val fiatQuoteTickers: SupportedTickerList
)
