package com.blockchain.core.price.model

import info.blockchain.balance.Currency

data class AssetPriceNotFoundException(val base: String, val quote: String) :
    RuntimeException("No cached price available for $base to $quote") {
    constructor(base: Currency, quote: Currency) : this(base.networkTicker, quote.networkTicker)
}
