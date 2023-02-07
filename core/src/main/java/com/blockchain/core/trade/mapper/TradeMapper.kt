package piuk.blockchain.android.data

import com.blockchain.api.trade.data.NextPaymentRecurringBuy
import com.blockchain.api.trade.data.NextPaymentRecurringBuyResponse
import com.blockchain.api.trade.data.QuoteResponse
import com.blockchain.api.trade.data.RecurringBuyResponse
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.trade.model.EligibleAndNextPaymentRecurringBuy
import com.blockchain.domain.trade.model.QuotePrice
import com.blockchain.domain.trade.model.RecurringBuy
import com.blockchain.domain.trade.model.RecurringBuyState
import com.blockchain.nabu.datamanagers.custodialwalletimpl.toPaymentMethodType
import com.blockchain.nabu.models.responses.simplebuy.toRecurringBuyFrequency
import com.blockchain.utils.fromIso8601ToUtc
import com.blockchain.utils.toLocalTime
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.Money
import java.util.Date

fun NextPaymentRecurringBuyResponse.toDomain(): List<EligibleAndNextPaymentRecurringBuy> = this.nextPayments.map {
    EligibleAndNextPaymentRecurringBuy(
        frequency = it.period.toRecurringBuyFrequency(),
        nextPaymentDate = it.nextPayment,
        eligibleMethods = it.eligibleMethods.map(String::toPaymentMethodTypeDomain)
    )
}

private fun String.toPaymentMethodTypeDomain(): PaymentMethodType = when (this) {
    NextPaymentRecurringBuy.BANK_TRANSFER -> PaymentMethodType.BANK_TRANSFER
    NextPaymentRecurringBuy.PAYMENT_CARD -> PaymentMethodType.PAYMENT_CARD
    NextPaymentRecurringBuy.FUNDS -> PaymentMethodType.FUNDS
    else -> PaymentMethodType.UNKNOWN
}

fun RecurringBuyResponse.toDomain(assetCatalogue: AssetCatalogue): RecurringBuy? {
    val asset = assetCatalogue.assetInfoFromNetworkTicker(destinationCurrency) ?: return null
    val fiatCurrency = assetCatalogue.fiatFromNetworkTicker(inputCurrency) ?: return null
    return RecurringBuy(
        id = id,
        state = state.toRecurringBuyStateDomain(),
        recurringBuyFrequency = period.toRecurringBuyFrequency(),
        nextPaymentDate = nextPayment.fromIso8601ToUtc()?.toLocalTime() ?: Date(),
        paymentMethodType = paymentMethod.toPaymentMethodType(),
        amount = Money.fromMinor(fiatCurrency, inputValue.toBigInteger()),
        asset = asset,
        createDate = insertedAt.fromIso8601ToUtc()?.toLocalTime() ?: Date(),
        paymentMethodId = paymentMethodId
    )
}

private fun String.toRecurringBuyStateDomain() =
    when (this) {
        RecurringBuyResponse.ACTIVE -> RecurringBuyState.ACTIVE
        RecurringBuyResponse.INACTIVE -> RecurringBuyState.INACTIVE
        else -> throw IllegalStateException("Unsupported recurring state")
    }

private fun String.toPaymentMethodType(): PaymentMethodType = when (this) {
    QuoteResponse.PAYMENT_CARD -> PaymentMethodType.PAYMENT_CARD
    QuoteResponse.FUNDS -> PaymentMethodType.FUNDS
    QuoteResponse.BANK_TRANSFER -> PaymentMethodType.BANK_TRANSFER
    QuoteResponse.BANK_ACCOUNT -> PaymentMethodType.BANK_ACCOUNT
    else -> PaymentMethodType.UNKNOWN
}

fun QuoteResponse.toDomain(assetCatalogue: AssetCatalogue): QuotePrice {
    val currencyPair = CurrencyPair.fromRawPair(
        rawValue = currencyPair,
        assetCatalogue = assetCatalogue
    )
    require(currencyPair != null) { "CurrencyPair in GetQuote is null" }
    return QuotePrice(
        currencyPair = currencyPair,
        amount = Money.fromMinor(
            currency = currencyPair.source,
            value = amount.toBigInteger()
        ),
        price = Money.fromMinor(
            currency = currencyPair.destination,
            value = price.toBigInteger()
        ),
        resultAmount = Money.fromMinor(
            currency = currencyPair.destination,
            value = resultAmount.toBigInteger()
        ),
        dynamicFee = Money.fromMinor(
            currency = currencyPair.source,
            value = dynamicFee.toBigInteger()
        ),
        networkFee = networkFee?.let {
            Money.fromMinor(
                currency = currencyPair.source,
                value = it.toBigInteger()
            )
        },
        paymentMethod = paymentMethod.toPaymentMethodType(),
        orderProfileName = orderProfileName
    )
}
