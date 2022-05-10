package com.blockchain.core.price.model

import info.blockchain.balance.Currency

sealed class AssetPriceError {
    data class RequestFailed(val message: String?) : AssetPriceError()
    data class PricePairNotFound(val base: Currency, val quote: Currency) : AssetPriceError()
}
