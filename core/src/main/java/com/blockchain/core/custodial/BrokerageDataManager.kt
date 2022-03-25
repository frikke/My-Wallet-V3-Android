package com.blockchain.core.custodial

import com.blockchain.api.brokerage.data.BrokerageQuoteResponse
import com.blockchain.api.brokerage.data.FeeDetailsResponse
import com.blockchain.api.brokerage.data.SettlementDetails
import com.blockchain.api.services.BrokerageService
import com.blockchain.core.custodial.models.Availability
import com.blockchain.core.custodial.models.BrokerageQuote
import com.blockchain.core.custodial.models.Promo
import com.blockchain.core.custodial.models.QuoteFee
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.datamanagers.CurrencyPair
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single

class BrokerageDataManager(
    private val authenticator: Authenticator,
    private val brokerageService: BrokerageService
) {
    fun quoteForTransaction(
        pair: CurrencyPair,
        amount: Money,
        paymentMethodType: PaymentMethodType,
        paymentMethodId: String?,
        product: Product
    ): Single<BrokerageQuote> =
        authenticator.authenticate { tokenResponse ->
            brokerageService.fetchQuote(
                authHeader = tokenResponse.authHeader,
                inputValue = amount.toBigInteger().toString(),
                paymentMethod = paymentMethodType.name,
                paymentMethodId = paymentMethodId,
                pair = listOf(pair.source.networkTicker, pair.destination.networkTicker).joinToString("-"),
                profile = product.toProfileRequestString()
            ).map { response ->
                response.toDomainModel(pair)
            }
        }
}

private fun BrokerageQuoteResponse.toDomainModel(pair: CurrencyPair): BrokerageQuote =
    BrokerageQuote(
        id = quoteId,
        price = Money.fromMinor(pair.destination, price.toBigInteger()),
        quoteMargin = quoteMarginPercent,
        availability = settlementDetails.availability?.toAvailability() ?: Availability.UNAVAILABLE,
        feeDetails = QuoteFee(
            fee = Money.fromMinor(pair.source, feeDetails.fee.toBigInteger()),
            feeBeforePromo = Money.fromMinor(pair.source, feeDetails.feeWithoutPromo.toBigInteger()),
            promo = feeDetails.feeFlags.firstOrNull()?.toPromo() ?: Promo.NO_PROMO
        )
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

private fun Product.toProfileRequestString(): String =
    when (this) {
        Product.BUY -> "SIMPLEBUY"
        else -> this.toString()
    }

// for feature reference add  SIMPLEBUY / SIMPLETRADE / SWAP_FROM_USERKEY / SWAP_INTERNAL / SWAP_ON_CHAIN
