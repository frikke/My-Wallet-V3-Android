package com.blockchain.home.emptystate

import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money

interface CustodialEmptyCardService {
    suspend fun getEmptyStateBuyAmounts(selectedTradingCurrency: FiatCurrency): List<Money>
}
