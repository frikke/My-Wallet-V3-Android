package com.blockchain.coincore

import com.blockchain.core.price.ExchangeRates
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import info.blockchain.balance.asFiatCurrencyOrThrow
import java.lang.IllegalStateException
import java.math.RoundingMode

@Deprecated("Use the reactive from ExchangeRateDataManager")
fun Money.toUserFiat(exchangeRates: ExchangeRates): Money =
    when (this) {
        is CryptoValue -> exchangeRates.getLastCryptoToUserFiatRate(this.currency).convert(this)
        is FiatValue -> exchangeRates.getLastFiatToUserFiatRate(this.currency).convert(this)
        else -> throw IllegalStateException("Unknown money type")
    }

@Deprecated("Use the reactive from ExchangeRateDataManager")
fun Money.toFiat(targetFiat: FiatCurrency, exchangeRates: ExchangeRates): Money =
    when (this) {
        is CryptoValue -> exchangeRates.getLastCryptoToFiatRate(this.currency, targetFiat).convert(this)
        is FiatValue -> exchangeRates.getLastFiatToFiatRate(this.currency, targetFiat).convert(this)
        else -> throw IllegalStateException("Unknown money type")
    }

@Deprecated("Use the reactive from ExchangeRateDataManager")
fun Money.toCrypto(exchangeRates: ExchangeRates, cryptoCurrency: AssetInfo) =
    if (isZero) {
        CryptoValue.zero(cryptoCurrency)
    } else {
        val rate = exchangeRates.getLastCryptoToFiatRate(cryptoCurrency, this.currency.asFiatCurrencyOrThrow())
        rate.inverse(RoundingMode.HALF_UP, cryptoCurrency.precisionDp).convert(this)
    }
