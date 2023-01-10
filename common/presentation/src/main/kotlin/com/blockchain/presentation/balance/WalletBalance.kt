package com.blockchain.presentation.balance

import androidx.compose.ui.graphics.Color
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.data.DataResource
import com.blockchain.data.combineDataResources
import com.blockchain.data.dataOrElse
import info.blockchain.balance.Money
import info.blockchain.balance.percentageDelta

data class WalletBalance(
    val balance: DataResource<Money>,
    private val cryptoBalanceDifference24h: DataResource<Money>,
    private val cryptoBalanceNow: DataResource<Money>,
) {

    val balanceDifference: BalanceDifferenceConfig
        get() = combineDataResources(cryptoBalanceNow, cryptoBalanceDifference24h) { now, yesterday ->
            val difference = now.minus(yesterday)
            if (now.isZero && difference.isZero)
                BalanceDifferenceConfig()
            else
                ValueChange.fromValue(now.percentageDelta(yesterday)).takeIf { !it.value.isNaN() }
                    ?.let { valueChange ->
                        BalanceDifferenceConfig(
                            "${valueChange.indicator} " +
                                difference.toStringWithSymbol() +
                                " (${valueChange.value}%)",
                            valueChange.color
                        )
                    } ?: BalanceDifferenceConfig()
        }.dataOrElse(BalanceDifferenceConfig())
}

data class BalanceDifferenceConfig(val text: String = "", val color: Color = Color.Transparent)
