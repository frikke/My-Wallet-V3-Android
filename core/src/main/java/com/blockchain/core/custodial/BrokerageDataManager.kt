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
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyQuoteResponse
import com.blockchain.nabu.service.NabuService
import com.blockchain.remoteconfig.IntegratedFeatureFlag
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single

class BrokerageDataManager(
    private val featureFlag: IntegratedFeatureFlag,
    private val authenticator: Authenticator,
    private val nabuService: NabuService,
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
            featureFlag.enabled.flatMap { enabled ->
                val brokerageQuote = brokerageService.fetchQuote(
                    authHeader = tokenResponse.authHeader,
                    inputValue = amount.toBigInteger().toString(),
                    paymentMethod = paymentMethodType.name,
                    paymentMethodId = paymentMethodId,
                    pair = listOf(pair.source.networkTicker, pair.destination.networkTicker).joinToString("-"),
                    profile = product.toProfileRequestString()
                ).map { response ->
                    response.toDomainModel(pair)
                }

                val simpleBuyQuote = nabuService.getSimpleBuyQuote(
                    sessionToken = tokenResponse,
                    action = product.name,
                    currencyPair = "${pair.destination.networkTicker}-${pair.source.networkTicker}",
                    amount = amount.toBigInteger().toString(),
                    currency = amount.currencyCode
                ).map { response ->
                    response.toDomainModel(pair.source, pair.destination, amount)
                }

                if (enabled)
                    brokerageQuote
                else
                    simpleBuyQuote
            }
        }
}

private fun SimpleBuyQuoteResponse.toDomainModel(
    fiatCurrency: Currency,
    asset: Currency,
    amount: Money
): BrokerageQuote {
    return BrokerageQuote(
        id = null,
        price = Money.fromMinor(fiatCurrency, rate.toBigInteger()),
        quoteMargin = null,
        availability = null,
        feeDetails = QuoteFee(
            fee = Money.zero(fiatCurrency),
            feeBeforePromo = Money.zero(fiatCurrency),
            promo = Promo.NO_PROMO
        )
    )
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
