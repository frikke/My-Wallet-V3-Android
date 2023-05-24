package com.dex.domain

import com.blockchain.coincore.SingleAccount
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money

data class DexAccount(
    val account: SingleAccount,
    val currency: DexCurrency,
    val balance: Money,
    val fiatBalance: Money
)

sealed class DexQuote {
    data class ExchangeQuote(
        val amount: Money,
        val price: Money,
        val value: String,
        val data: String,
        val gasLimit: String,
        val quoteTtl: Long,
        val destinationContractAddress: String,
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
    val slippage: Double,
    val sourceHasBeenAllowed: Boolean
)

data class DexCurrency(
    private val currency: AssetInfo,
    val chainId: Int,
    override val index: Int = currency.index,
    val contractAddress: String?
) : AssetInfo by currency
