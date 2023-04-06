package com.dex.domain

import com.blockchain.coincore.SingleAccount
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money

data class DexAccount(
    val account: SingleAccount,
    val currency: AssetInfo,
    val balance: Money,
    val fiatBalance: Money,
    val contractAddress: String,
    val chainId: Int
)

sealed class DexQuote {
    data class ExchangeQuote(
        val amount: Money,
        val price: Money,
        val outputAmount: OutputAmount,
        val networkFees: Money,
        val blockchainFees: Money
    ) : DexQuote()

    object InvalidQuote : DexQuote()
}

data class DexQuoteParams(
    val sourceAccount: DexAccount,
    val destinationAccount: DexAccount,
    val amount: Money,
    val slippage: Double
)
