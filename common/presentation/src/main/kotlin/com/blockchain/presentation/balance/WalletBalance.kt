package com.blockchain.presentation.balance

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.data.DataResource
import info.blockchain.balance.Money
import info.blockchain.balance.percentageDelta

data class WalletBalance(
    val balance: DataResource<Money>,
    private val cryptoBalanceDifference24h: DataResource<Money>,
    private val cryptoBalanceNow: DataResource<Money>
) {

    val balanceDifference: BalanceDifferenceConfig
        @Composable get() {
            return when {
                cryptoBalanceNow is DataResource.Data && cryptoBalanceDifference24h is DataResource.Data -> {
                    val now = cryptoBalanceNow.data
                    val yesterday = cryptoBalanceDifference24h.data

                    val difference = now.minus(yesterday)
                    if (now.isZero && difference.isZero) {
                        BalanceDifferenceConfig()
                    } else
                        ValueChange.fromValue(now.percentageDelta(yesterday)).takeIf { !it.value.isNaN() }
                            ?.let { valueChange ->
                                BalanceDifferenceConfig(
                                    text = "${valueChange.indicator} " +
                                        difference.toStringWithSymbol() +
                                        " (${valueChange.value}%)",
                                    color = valueChange.color
                                )
                            } ?: BalanceDifferenceConfig()
                }

                else -> {
                    BalanceDifferenceConfig()
                }
            }
        }
}

data class BalanceDifferenceConfig(
    val text: String = "",
    val color: Color = Color.Transparent
)
