package com.blockchain.api.selfcustody

import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BalancesResponse(
    @SerialName("currencies")
    val balances: List<BalanceEntry>,
    @SerialName("networks")
    val networksStatus: List<NetworkStatus> = emptyList()
)

@Serializable
data class BalanceEntry(
    @SerialName("ticker")
    val currency: String,
    @SerialName("account")
    val account: AccountInfo,
    @SerialName("amount")
    val balance: BalanceInfo?,
    @SerialName("unconfirmed")
    val pending: BalanceInfo?,
    @SerialName("price")
    val price: @Contextual BigDecimal?
)

@Serializable
data class BalanceInfo(
    @SerialName("amount")
    val amount: @Contextual BigInteger,
    @SerialName("precision")
    val precision: Int
)

@Serializable
data class NetworkStatus(
    @SerialName("ticker")
    val ticker: String,
    @SerialName("errorLoadingBalances")
    val hasFailedToLoad: Boolean
)

