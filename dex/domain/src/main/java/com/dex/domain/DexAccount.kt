package com.dex.domain

import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money

data class DexAccount(
    val account: CryptoNonCustodialAccount,
    val currency: DexCurrency,
    val balance: Money,
    val fiatBalance: Money?
)

sealed class DexQuote {
    data class ExchangeQuote(
        val sellAmount: ExchangeAmount,
        val buyAmount: ExchangeAmount,
        val price: Money,
        val value: String,
        val data: String,
        val gasLimit: String,
        val quoteTtl: Long,
        val destinationContractAddress: String,
        val networkFees: Money,
        val gasPrice: String,
        val blockchainFees: Money
    ) : DexQuote()

    object InvalidQuote : DexQuote()
}

data class DexQuoteParams(
    val sourceAccount: DexAccount,
    val destinationAccount: DexAccount,
    val inputAmount: ExchangeAmount,
    val slippage: Double,
    val sourceHasBeenAllowed: Boolean
)

data class DexCurrency(
    private val currency: AssetInfo,
    val chainId: Int,
    override val index: Int = currency.index,
    val contractAddress: String?
) : AssetInfo by currency
