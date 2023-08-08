package piuk.blockchain.android.data

import com.blockchain.api.trade.data.QuoteResponse
import com.blockchain.domain.trade.model.QuotePrice
import com.blockchain.nabu.datamanagers.custodialwalletimpl.toPaymentMethodType
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import java.lang.Integer.max
import java.math.BigInteger
import java.math.RoundingMode

fun QuoteResponse.toDomain(assetCatalogue: AssetCatalogue): QuotePrice {
    val currencyPair = CurrencyPair.fromRawPair(
        rawValue = currencyPair,
        assetCatalogue = assetCatalogue
    )
    require(currencyPair != null) { "CurrencyPair in GetQuote is null" }
    val amountMoney = Money.fromMinor(
        currency = currencyPair.source,
        value = amount.toBigInteger()
    )
    return QuotePrice(
        currencyPair = currencyPair,
        amount = amountMoney,
        sourceToDestinationRate = calcSourceToOutputRateFromInputAmountAndResultAmount(
            currencyPair,
            amountMoney,
            price.toBigInteger(),
            resultAmount.toBigInteger()
        ),
        rawPrice = Money.fromMinor(
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
                currency = currencyPair.destination,
                value = it.toBigInteger()
            )
        },
        paymentMethod = paymentMethod.toPaymentMethodType(),
        orderProfileName = orderProfileName
    )
}

// The price that comes from the BE doesn't really seem to make any sense, so we're deriving a new price from
// the resultAmount, which is the real value the user will get, this price will then be used as an ExchangeRate
internal fun calcSourceToOutputRateFromInputAmountAndResultAmount(
    currencyPair: CurrencyPair,
    amountMoney: Money,
    quotePrice: BigInteger,
    resultAmount: BigInteger
): ExchangeRate {
    // There can be some cases where the resultAmount is very low, for example when selling 100minor XLM to EUR, 100minor XLM
    // isn't even 0.01EUR and thus resultAmount will be 0, this will skew the "price" rate massively depending on the resultAmount value.
    // So if resultAmount is less than 100minor we'll just use the quotePrice, even tho this quotePrice doesn't take into
    // account fees and spread, it's better then providing a rate that's potentially wrong
    val rate = if (resultAmount >= BigInteger.valueOf(100L)) {
        val resultAmountDecimal = resultAmount.toBigDecimal()
        val amountMajor = amountMoney.toBigDecimal()
        val scale = max(resultAmountDecimal.scale(), amountMajor.scale())
        val result = (resultAmountDecimal.divide(amountMajor, scale, RoundingMode.CEILING))
        result.movePointLeft(currencyPair.destination.precisionDp)
    } else {
        quotePrice.toBigDecimal(currencyPair.destination.precisionDp)
    }
    return ExchangeRate(
        rate = rate,
        from = currencyPair.source,
        to = currencyPair.destination
    )
}
