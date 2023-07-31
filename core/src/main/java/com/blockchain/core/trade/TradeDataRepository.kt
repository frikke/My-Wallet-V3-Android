package com.blockchain.core.trade

import com.blockchain.api.services.TradeService
import com.blockchain.api.trade.data.AccumulatedInPeriod
import com.blockchain.domain.experiments.RemoteConfigService
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.trade.TradeDataService
import com.blockchain.domain.trade.model.QuickFillRoundingData
import com.blockchain.domain.trade.model.QuotePrice
import com.blockchain.domain.transactions.TransferDirection
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.map
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.builtins.ListSerializer
import piuk.blockchain.android.data.toDomain

class TradeDataRepository(
    private val tradeService: TradeService,
    private val assetCatalogue: AssetCatalogue,
    private val remoteConfigService: RemoteConfigService,
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

    override suspend fun getQuickFillRoundingForBuy(): Outcome<
        Exception,
        List<QuickFillRoundingData.BuyRoundingData>
        > = remoteConfigService.getParsedJsonValue(
        CONFIG_BUY_ID,
        ListSerializer(QuickFillRoundingData.BuyRoundingData.serializer())
    )

    override suspend fun getQuickFillRoundingForSell(): Outcome<
        Exception,
        List<QuickFillRoundingData.SellSwapRoundingData>
        > =
        remoteConfigService.getParsedJsonValue(
            CONFIG_SELL_ID,
            ListSerializer(QuickFillRoundingData.SellSwapRoundingData.serializer())
        )

    override suspend fun getQuickFillRoundingForSwap(): Outcome<
        Exception,
        List<QuickFillRoundingData.SellSwapRoundingData>
        > = remoteConfigService.getParsedJsonValue(
        CONFIG_SWAP_ID,
        ListSerializer(QuickFillRoundingData.SellSwapRoundingData.serializer())
    )

    private fun TransferDirection.getQuotePaymentMethod(): String =
        if (this == TransferDirection.INTERNAL) "FUNDS" else "DEPOSIT"

    private fun TransferDirection.getQuoteOrderProfileName(): String = when (this) {
        TransferDirection.ON_CHAIN -> "SWAP_ON_CHAIN"
        TransferDirection.FROM_USERKEY -> "SWAP_FROM_USERKEY"
        TransferDirection.TO_USERKEY -> throw UnsupportedOperationException()
        TransferDirection.INTERNAL -> "SWAP_INTERNAL"
    }

    companion object {
        private const val BASE_PATH = "blockchain_app_configuration_transaction"
        private const val END_PATH = "quickfill_configuration"
        private const val CONFIG_SWAP_ID = "${BASE_PATH}_swap_$END_PATH"
        private const val CONFIG_SELL_ID = "${BASE_PATH}_sell_$END_PATH"
        private const val CONFIG_BUY_ID = "${BASE_PATH}_buy_$END_PATH"
    }
}
