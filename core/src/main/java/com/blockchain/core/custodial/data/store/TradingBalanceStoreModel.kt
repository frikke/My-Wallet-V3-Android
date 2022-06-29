package com.blockchain.core.custodial.data.store

import com.blockchain.api.services.TradingBalance
import java.math.BigInteger
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
internal data class TradingBalanceStoreModel(
    val assetTicker: String,
    val pending: @Contextual BigInteger,
    val total: @Contextual BigInteger,
    val withdrawable: @Contextual BigInteger,
)

internal fun TradingBalance.toStore() = TradingBalanceStoreModel(
    assetTicker = assetTicker,
    pending = pending,
    total = total,
    withdrawable = withdrawable
)

internal fun TradingBalanceStoreModel.toDomain() = TradingBalance(
    assetTicker = assetTicker,
    pending = pending,
    total = total,
    withdrawable = withdrawable
)
