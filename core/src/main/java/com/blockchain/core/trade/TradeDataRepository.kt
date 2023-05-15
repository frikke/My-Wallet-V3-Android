package com.blockchain.core.trade

import com.blockchain.api.services.TradeService
import com.blockchain.api.trade.data.AccumulatedInPeriod
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.trade.TradeDataService
import com.blockchain.domain.trade.model.QuotePrice
import com.blockchain.domain.transactions.TransferDirection
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.map
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.data.toDomain

class TradeDataRepository(
    private val tradeService: TradeService,
    private val assetCatalogue: AssetCatalogue
) : TradeDataService {

    override fun isFirstTimeBuyer(): Single<Boolean> =
        tradeService.isFirstTimeBuyer()
            .map { accumulatedInPeriod ->
                accumulatedInPeriod.tradesAccumulated
                    .first { it.termType == AccumulatedInPeriod.ALL }.amount.value.toDouble() == 0.0
            }

    override suspend fun getBuyQuotePrice(
        currencyPair: CurrencyPair,
        amount: Money,
        paymentMethod: PaymentMethodType
    ): Outcome<Exception, QuotePrice> =
        tradeService.getQuotePrice(
            currencyPair = currencyPair.rawValue,
            amount = amount.toBigInteger().toString(),
            paymentMethod = paymentMethod.name,
            orderProfileName = "SIMPLEBUY"
        ).map { response ->
            response.toDomain(assetCatalogue)
        }

    override suspend fun getSellQuotePrice(
        currencyPair: CurrencyPair,
        amount: Money,
        direction: TransferDirection
    ): Outcome<Exception, QuotePrice> =
        tradeService.getQuotePrice(
            currencyPair = currencyPair.rawValue,
            amount = amount.toBigInteger().toString(),
            paymentMethod = direction.getQuotePaymentMethod(),
            orderProfileName = direction.getQuoteOrderProfileName()
        ).map { response ->
            response.toDomain(assetCatalogue)
        }

    override suspend fun getSwapQuotePrice(
        currencyPair: CurrencyPair,
        amount: Money,
        direction: TransferDirection
    ): Outcome<Exception, QuotePrice> =
        tradeService.getQuotePrice(
            currencyPair = currencyPair.rawValue,
            amount = amount.toBigInteger().toString(),
            paymentMethod = direction.getQuotePaymentMethod(),
            orderProfileName = direction.getQuoteOrderProfileName()
        ).map { response ->
            response.toDomain(assetCatalogue)
        }

    private fun TransferDirection.getQuotePaymentMethod(): String =
        if (this == TransferDirection.INTERNAL) "FUNDS" else "DEPOSIT"

    private fun TransferDirection.getQuoteOrderProfileName(): String = when (this) {
        TransferDirection.ON_CHAIN -> "SWAP_ON_CHAIN"
        TransferDirection.FROM_USERKEY -> "SWAP_FROM_USERKEY"
        TransferDirection.TO_USERKEY -> throw UnsupportedOperationException()
        TransferDirection.INTERNAL -> "SWAP_INTERNAL"
    }
}
