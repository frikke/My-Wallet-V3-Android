package com.blockchain.home.data.emptystate

import com.blockchain.home.emptystate.CustodialEmptyCardService
import com.blockchain.outcome.getOrDefault
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import java.math.BigDecimal

class CustodialEmptyCardRepository(
    private val emptyStateBuyAmountsRemoteConfig: EmptyStateBuyAmountsRemoteConfig
) : CustodialEmptyCardService {

    private val defaultEmptyStateAmounts = listOf("100", "200")

    override suspend fun getEmptyStateBuyAmounts(selectedTradingCurrency: FiatCurrency): List<Money> =
        emptyStateBuyAmountsRemoteConfig.getBuyAmounts()
            .getOrDefault(defaultEmptyStateAmounts)
            .map { amount ->
                Money.fromMajor(selectedTradingCurrency, BigDecimal(amount))
            }
}
