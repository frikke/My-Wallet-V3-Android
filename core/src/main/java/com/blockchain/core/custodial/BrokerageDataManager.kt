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
import com.blockchain.nabu.datamanagers.Product
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single
import java.time.ZonedDateTime

class BrokerageDataManager(
    private val brokerageService: BrokerageService
) {

    fun quoteForTransaction(
        pair: CurrencyPair,
        amount: Money,
        paymentMethodType: PaymentMethodType,
        paymentMethodId: String?,
        product: Product
    ): Single<BrokerageQuote> =
        brokerageService.fetchQuote(
            inputValue = amount.toBigInteger().toString(),
            paymentMethod = paymentMethodType.name,
            paymentMethodId = paymentMethodId,
            pair = listOf(pair.source.networkTicker, pair.destination.networkTicker).joinToString("-"),
            profile = product.toProfileRequestString()
        ).map { response ->
            response.toDomainModel(pair)
        }
}

private fun BrokerageQuoteResponse.toDomainModel(pair: CurrencyPair): BrokerageQuote =
    BrokerageQuote(
        id = quoteId,
        price = Money.fromMinor(pair.destination, price.toBigInteger()),
        createdAt = ZonedDateTime.parse(quoteCreatedAt),
        expiresAt = ZonedDateTime.parse(quoteExpiresAt),
        quoteMargin = quoteMarginPercent,
        settlementReason = settlementDetails?.reason?.toSettlementReason() ?: SettlementReason.NONE,
        availability = settlementDetails?.availability?.toAvailability() ?: Availability.UNAVAILABLE,
        feeDetails = QuoteFee(
            fee = Money.fromMinor(pair.source, feeDetails.fee.toBigInteger()),
            feeBeforePromo = Money.fromMinor(pair.source, feeDetails.feeWithoutPromo.toBigInteger()),
            promo = feeDetails.feeFlags.firstOrNull()?.toPromo() ?: Promo.NO_PROMO
        ),
        depositTerms = depositTerms?.toDepositTerms()
    )

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

private fun Product.toProfileRequestString(): String =
    when (this) {
        Product.BUY -> "SIMPLEBUY"
        else -> this.toString()
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
