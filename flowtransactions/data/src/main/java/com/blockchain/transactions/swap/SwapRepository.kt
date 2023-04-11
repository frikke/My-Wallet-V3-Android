package com.blockchain.transactions.swap

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatCurrency

internal class SwapRepository : SwapService {
    override suspend fun fiatToCrypto(
        fiatValue: String,
        fiatCurrency: FiatCurrency,
        cryptoCurrency: CryptoCurrency
    ): String {
        return (fiatValue.toDouble() / 2).toString()
    }

    override suspend fun cryptoToFiat(
        cryptoValue: String,
        fiatCurrency: FiatCurrency,
        cryptoCurrency: CryptoCurrency
    ): String {
        return (cryptoValue.toDouble() * 2).toString()
    }
}