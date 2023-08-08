package com.blockchain.presentation.balance

import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.data.DataResource
import info.blockchain.balance.Money
import info.blockchain.balance.percentageDelta

data class WalletBalance(
    val balance: DataResource<Money>,
    private val cryptoBalanceDifference24h: DataResource<Money>,
    private val cryptoBalanceNow: DataResource<Money>
) {

    val balanceDifference: BalanceDifferenceConfig?
        get() {
            return when {
                cryptoBalanceNow is DataResource.Data && cryptoBalanceDifference24h is DataResource.Data -> {
                    val now = cryptoBalanceNow.data
                    val yesterday = cryptoBalanceDifference24h.data

                    val difference = now.minus(yesterday)
                    if (now.isZero && difference.isZero) {
                        null
                    } else
                        ValueChange.fromValue(now.percentageDelta(yesterday)).takeIf { !it.value.isNaN() }
                            ?.let { valueChange ->
                                BalanceDifferenceConfig(
                                    differenceSymbol = difference.symbol,
                                    differenceAmount = difference.toStringWithoutSymbol(),
                                    valueChange = valueChange
                                )
                            }
                }

                else -> {
                    null
                }
            }
        }
}

data class BalanceDifferenceConfig(
    val differenceSymbol: String,
    val differenceAmount: String,
    val valueChange: ValueChange
)
