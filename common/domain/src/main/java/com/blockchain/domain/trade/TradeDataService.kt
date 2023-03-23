package com.blockchain.domain.trade

import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.trade.model.QuotePrice
import com.blockchain.domain.transactions.TransferDirection
import com.blockchain.outcome.Outcome
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single

interface TradeDataService {

    fun isFirstTimeBuyer(): Single<Boolean>

    suspend fun getBuyQuotePrice(
        currencyPair: CurrencyPair,
        amount: Money,
        paymentMethod: PaymentMethodType,
    ): Outcome<Exception, QuotePrice>

    suspend fun getSellQuotePrice(
        currencyPair: CurrencyPair,
        amount: Money,
        direction: TransferDirection,
    ): Outcome<Exception, QuotePrice>

    suspend fun getSwapQuotePrice(
        currencyPair: CurrencyPair,
        amount: Money,
        direction: TransferDirection,
    ): Outcome<Exception, QuotePrice>
}
