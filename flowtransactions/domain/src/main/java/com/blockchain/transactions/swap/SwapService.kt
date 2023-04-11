package com.blockchain.transactions.swap

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatCurrency

interface SwapService {
    suspend fun fiatToCrypto(
        fiatValue: String,
        fiatCurrency: FiatCurrency,
        cryptoCurrency: CryptoCurrency
    ): String

    suspend fun cryptoToFiat(
        cryptoValue: String,
        fiatCurrency: FiatCurrency,
        cryptoCurrency: CryptoCurrency
    ): String
}