package com.blockchain.transactions.common

import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue

data class CombinedSourceNetworkFees(
    val amount: CryptoValue,
    val feeForAmount: CryptoValue,
    // Some crypto's fee change depending on the amount being sent, [feeForFullBalance] represents the maximum fee the
    // user would pay if they were to send their whole balance, ie. `spendableBalance = balance - feeForFullBalance`
    val feeForFullAvailable: CryptoValue,
) {
    companion object {
        fun zero(asset: AssetInfo): CombinedSourceNetworkFees = CombinedSourceNetworkFees(
            amount = CryptoValue.zero(asset),
            feeForAmount = CryptoValue.zero(asset),
            feeForFullAvailable = CryptoValue.zero(asset),
        )
    }
}
