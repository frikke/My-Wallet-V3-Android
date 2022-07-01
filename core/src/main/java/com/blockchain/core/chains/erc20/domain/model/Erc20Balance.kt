package com.blockchain.core.chains.erc20.domain.model

import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue

data class Erc20Balance(
    val balance: CryptoValue,
    val hasTransactions: Boolean
) {
    internal companion object {
        fun zero(assetInfo: AssetInfo): Erc20Balance =
            Erc20Balance(
                balance = CryptoValue.zero(assetInfo),
                hasTransactions = false
            )
    }
}