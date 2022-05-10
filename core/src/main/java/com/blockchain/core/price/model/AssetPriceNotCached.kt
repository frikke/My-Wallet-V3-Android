package com.blockchain.core.price.model

class AssetPriceNotCached(val base: String, val quote: String) :
    Throwable("No cached price available for $base to $quote")
