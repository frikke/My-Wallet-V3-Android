package com.blockchain.core.custodial

import com.blockchain.api.brokerage.data.BrokerageQuoteResponse
import com.blockchain.api.brokerage.data.DepositTermsResponse
import com.blockchain.api.brokerage.data.FeeDetailsResponse
import com.blockchain.api.brokerage.data.SettlementDetails
import com.blockchain.api.services.BrokerageService
import com.blockchain.core.custodial.models.Availability
import com.blockchain.core.custodial.models.BrokerageQuote
import com.blockchain.core.custodial.models.Promo
import com.blockchain.core.custodial.models.QuoteFee
import com.blockchain.domain.paymentmethods.model.DepositTerms
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.paymentmethods.model.SettlementReason
import com.blockchain.domain.paymentmethods.model.SettlementType
import com.blockchain.domain.transactions.TransferDirection
import com.blockchain.utils.CurrentTimeProvider
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single
import java.time.ZonedDateTime
import piuk.blockchain.android.data.calcSourceToOutputRateFromInputAmountAndResultAmount

class BrokerageDataManager(
    private val brokerageService: BrokerageService,
) {

    fun getBuyQuote(
        pair: CurrencyPair,
        amount: Money,
        paymentMethodType: PaymentMethodType,
        paymentMethodId: String?,
    ): Single<BrokerageQuote> =
        brokerageService.fetchQuote(
            inputValue = amount.toBigInteger().toString(),
            paymentMethod = paymentMethodType.name,
            paymentMethodId = paymentMethodId,
            pair = pair.rawValue,
            profile = "SIMPLEBUY"
        ).map { response ->
            response.toDomainModel(pair, amount)
        }

    fun getSellQuote(
        pair: CurrencyPair,
        amount: Money,
        direction: TransferDirection,
    ): Single<BrokerageQuote> =
        brokerageService.fetchQuote(
            inputValue = amount.toBigInteger().toString(),
            paymentMethod = direction.getQuotePaymentMethod(),
            paymentMethodId = null,
            pair = pair.rawValue,
            profile = direction.getQuoteOrderProfileName(),
        ).map { response ->
            response.toDomainModel(pair, amount)
        }

    fun getSwapQuote(
        pair: CurrencyPair,
        amount: Money,
        direction: TransferDirection,
    ): Single<BrokerageQuote> =
        brokerageService.fetchQuote(
            inputValue = amount.toBigInteger().toString(),
            paymentMethod = direction.getQuotePaymentMethod(),
            paymentMethodId = null,
            pair = pair.rawValue,
            profile = direction.getQuoteOrderProfileName(),
        ).map { response ->
            response.toDomainModel(pair, amount)
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

private fun BrokerageQuoteResponse.toDomainModel(pair: CurrencyPair, inputAmount: Money): BrokerageQuote {
    val serverCreatedAt = ZonedDateTime.parse(quoteCreatedAt).toInstant().toEpochMilli()
    val serverExpiresAt = ZonedDateTime.parse(quoteExpiresAt).toInstant().toEpochMilli()
    val quoteTtl = serverExpiresAt - serverCreatedAt
    // We're manually "overriding" the serverCreatedAt because the user's phone clock might have been changed
    val createdAt = CurrentTimeProvider.currentTimeMillis()
    val expiresAt = createdAt + quoteTtl
    return BrokerageQuote(
        id = quoteId,
        currencyPair = pair,
        inputAmount = inputAmount,
        sourceToDestinationRate = calcSourceToOutputRateFromInputAmountAndResultAmount(
            pair,
            inputAmount,
            price.toBigInteger(),
            resultAmount.toBigInteger()
        ),
        rawPrice = Money.fromMinor(
            currency = pair.destination,
            value = price.toBigInteger(),
        ),
        resultAmount = Money.fromMinor(pair.destination, resultAmount.toBigInteger()),
        createdAt = createdAt,
        expiresAt = expiresAt,
        quoteMargin = quoteMarginPercent,
        settlementReason = settlementDetails?.reason?.toSettlementReason() ?: SettlementReason.NONE,
        availability = settlementDetails?.availability?.toAvailability() ?: Availability.UNAVAILABLE,
        networkFee = Money.fromMinor(pair.destination, networkFee.toBigInteger()),
        staticFee = Money.fromMinor(pair.source, staticFee.toBigInteger()),
        feeDetails = QuoteFee(
            fee = Money.fromMinor(pair.source, feeDetails.fee.toBigInteger()),
            feeBeforePromo = Money.fromMinor(pair.source, feeDetails.feeWithoutPromo.toBigInteger()),
            promo = feeDetails.feeFlags.firstOrNull()?.toPromo() ?: Promo.NO_PROMO
        ),
        depositTerms = depositTerms?.toDepositTerms()
    )
}

private fun String.toPromo(): Promo =
    when (this) {
        FeeDetailsResponse.NEW_USER_WAIVER -> Promo.NEW_USER
        else -> Promo.NO_PROMO
    }

private fun String.toAvailability(): Availability =
    when (this) {
        SettlementDetails.INSTANT -> Availability.INSTANT
        SettlementDetails.REGULAR -> Availability.REGULAR
        SettlementDetails.UNAVAILABLE -> Availability.UNAVAILABLE
        else -> Availability.UNAVAILABLE
    }

private fun String.toSettlementReason(): SettlementReason = try {
    SettlementReason.valueOf(this)
} catch (ex: Exception) {
    SettlementReason.UNKNOWN
}

private fun String.toSettlementType(): SettlementType = try {
    SettlementType.valueOf(this)
} catch (ex: Exception) {
    SettlementType.UNKNOWN
}

private fun DepositTermsResponse.toDepositTerms(): DepositTerms =
    DepositTerms(
        creditCurrency = this.creditCurrency,
        availableToTradeMinutesMin = this.availableToTradeMinutesMin,
        availableToTradeMinutesMax = this.availableToTradeMinutesMax,
        availableToTradeDisplayMode = this.availableToTradeDisplayMode.toDisplayMode(),
        availableToWithdrawMinutesMin = this.availableToWithdrawMinutesMin,
        availableToWithdrawMinutesMax = this.availableToWithdrawMinutesMax,
        availableToWithdrawDisplayMode = this.availableToWithdrawDisplayMode.toDisplayMode(),
        settlementType = this.settlementType?.toSettlementType(),
        settlementReason = this.settlementReason?.toSettlementReason()
    )

private fun String.toDisplayMode(): DepositTerms.DisplayMode = try {
    DepositTerms.DisplayMode.valueOf(this)
} catch (ex: Exception) {
    DepositTerms.DisplayMode.NONE
}

// for feature reference add  SIMPLEBUY / SIMPLETRADE / SWAP_FROM_USERKEY / SWAP_INTERNAL / SWAP_ON_CHAIN
