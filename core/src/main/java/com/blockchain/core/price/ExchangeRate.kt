package com.blockchain.core.price

import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import info.blockchain.balance.UnknownValue
import info.blockchain.balance.ValueTypeMismatchException
import java.lang.IllegalStateException
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency

sealed class ExchangeRate {
    abstract val rate: BigDecimal?

    abstract fun convert(value: Money, round: Boolean = true): Money
    abstract fun price(): Money
    abstract fun inverse(roundingMode: RoundingMode = RoundingMode.HALF_UP, scale: Int = -1): ExchangeRate

    class CryptoToCrypto(
        val from: AssetInfo,
        val to: AssetInfo,
        override val rate: BigDecimal?
    ) : ExchangeRate() {
        internal fun applyRate(cryptoValue: CryptoValue): Money {
            validateCurrency(from, cryptoValue.currency)
            return rate?.let {
                CryptoValue.fromMajor(
                    to,
                    it.multiply(cryptoValue.toBigDecimal())
                )
            } ?: UnknownValue.unknownCryptoValue(to)
        }

        override fun convert(value: Money, round: Boolean): Money =
            applyRate(value as CryptoValue)

        override fun price(): Money =
            rate?.let { CryptoValue.fromMajor(to, it) } ?: UnknownValue.unknownCryptoValue(to)

        override fun inverse(roundingMode: RoundingMode, scale: Int) =
            rate?.let {
                CryptoToCrypto(
                    to,
                    from,
                    BigDecimal.ONE.divide(
                        rate,
                        if (scale == -1) from.precisionDp else scale,
                        roundingMode
                    ).stripTrailingZeros()
                )
            } ?: CryptoToCrypto(
                to,
                from,
                rate
            )
    }

    data class CryptoToFiat(
        val from: AssetInfo,
        val to: String,
        override val rate: BigDecimal?
    ) : ExchangeRate() {
        internal fun applyRate(cryptoValue: CryptoValue, round: Boolean = false): Money {
            validateCurrency(from, cryptoValue.currency)
            return rate?.let {
                FiatValue.fromMajor(
                    currencyCode = to,
                    major = it.multiply(cryptoValue.toBigDecimal()),
                    round = round
                )
            } ?: UnknownValue.unknownFiatValue(to)
        }

        override fun convert(value: Money, round: Boolean): Money =
            applyRate(value as CryptoValue, round)

        override fun price(): Money =
            rate?.let { FiatValue.fromMajor(to, it) } ?: UnknownValue.unknownFiatValue(to)

        override fun inverse(roundingMode: RoundingMode, scale: Int) =
            rate?.let {
                FiatToCrypto(
                    to,
                    from,
                    BigDecimal.ONE.divide(
                        rate,
                        if (scale == -1) from.precisionDp else scale,
                        roundingMode
                    ).stripTrailingZeros()
                )
            } ?: FiatToCrypto(
                to,
                from,
                rate
            )
    }

    class FiatToCrypto(
        val from: String,
        val to: AssetInfo,
        override val rate: BigDecimal?
    ) : ExchangeRate() {
        internal fun applyRate(fiatValue: FiatValue): Money {
            validateCurrency(from, fiatValue.currencyCode)
            return rate?.let {
                CryptoValue.fromMajor(
                    to,
                    rate.multiply(fiatValue.toBigDecimal())
                )
            } ?: UnknownValue.unknownCryptoValue(to)
        }

        override fun convert(value: Money, round: Boolean): Money =
            applyRate(value as FiatValue)

        override fun price(): Money =
            rate?.let { CryptoValue.fromMajor(to, it) } ?: UnknownValue.unknownCryptoValue(to)

        override fun inverse(roundingMode: RoundingMode, scale: Int) =
            rate?.let {
                CryptoToFiat(
                    to,
                    from,
                    BigDecimal.ONE.divide(
                        rate,
                        if (scale == -1) {
                            Currency.getInstance(from).defaultFractionDigits
                        } else {
                            scale
                        },
                        roundingMode
                    ).stripTrailingZeros()
                )
            } ?: CryptoToFiat(
                to,
                from,
                rate
            )
    }

    class FiatToFiat(
        val from: String,
        val to: String,
        override val rate: BigDecimal?
    ) : ExchangeRate() {
        private fun applyRate(fiatValue: FiatValue): Money {
            validateCurrency(from, fiatValue.currencyCode)
            return rate?.let {
                FiatValue.fromMajor(
                    to,
                    it.multiply(fiatValue.toBigDecimal())
                )
            } ?: UnknownValue.unknownFiatValue(to)
        }

        override fun convert(value: Money, round: Boolean): Money =
            applyRate(value as FiatValue)

        override fun price(): Money =
            rate?.let { FiatValue.fromMajor(to, it) } ?: UnknownValue.unknownFiatValue(to)

        override fun inverse(roundingMode: RoundingMode, scale: Int) =
            rate?.let {
                FiatToFiat(
                    to,
                    from,
                    BigDecimal.ONE.divide(
                        rate,
                        if (scale == -1) {
                            Currency.getInstance(from).defaultFractionDigits
                        } else {
                            scale
                        },
                        roundingMode
                    ).stripTrailingZeros()
                )
            } ?: FiatToFiat(
                to,
                from,
                rate
            )
    }

    object InvalidRate : ExchangeRate() {
        override val rate: BigDecimal
            get() = BigDecimal.ZERO

        override fun convert(value: Money, round: Boolean): Money {
            throw IllegalStateException("Convert called on Invalid Exchange Rate")
        }

        override fun price(): Money {
            throw IllegalStateException("Convert called on Invalid Exchange Rate")
        }

        override fun inverse(roundingMode: RoundingMode, scale: Int): ExchangeRate {
            return this
        }
    }

    companion object {
        private fun validateCurrency(expected: AssetInfo, got: AssetInfo) {
            if (expected != got)
                throw ValueTypeMismatchException("exchange", expected.networkTicker, got.networkTicker)
        }

        private fun validateCurrency(expected: String, got: String) {
            if (expected != got)
                throw ValueTypeMismatchException("exchange", expected, got)
        }
    }
}

operator fun CryptoValue?.times(rate: ExchangeRate.CryptoToCrypto?) =
    this?.let { rate?.applyRate(it) }

operator fun CryptoValue?.div(rate: ExchangeRate.CryptoToCrypto?) =
    this?.let { rate?.inverse()?.applyRate(it) }

operator fun FiatValue?.times(rate: ExchangeRate.FiatToCrypto?) =
    this?.let { rate?.applyRate(it) }

operator fun CryptoValue?.times(exchangeRate: ExchangeRate.CryptoToFiat?) =
    this?.let { exchangeRate?.applyRate(it) }

operator fun CryptoValue?.div(exchangeRate: ExchangeRate.FiatToCrypto?) =
    this?.let { exchangeRate?.inverse()?.applyRate(it) }

operator fun FiatValue?.div(exchangeRate: ExchangeRate.CryptoToFiat?) =
    this?.let { exchangeRate?.inverse()?.applyRate(it) }

fun ExchangeRate.hasSameSourceAndTarget(other: ExchangeRate): Boolean =
    when (this) {
        is ExchangeRate.CryptoToFiat -> (other as? ExchangeRate.CryptoToFiat)?.from == from && other.to == to
        is ExchangeRate.FiatToCrypto -> (other as? ExchangeRate.FiatToCrypto)?.from == from && other.to == to
        is ExchangeRate.FiatToFiat -> (other as? ExchangeRate.FiatToFiat)?.from == from && other.to == to
        is ExchangeRate.CryptoToCrypto -> (other as? ExchangeRate.CryptoToCrypto)?.from == from && other.to == to
        is ExchangeRate.InvalidRate -> throw IllegalStateException("Use of Invalid Rate")
    }

fun ExchangeRate.hasOppositeSourceAndTarget(other: ExchangeRate): Boolean =
    this.hasSameSourceAndTarget(other.inverse())

fun ExchangeRate.canConvert(value: Money): Boolean =
    when (this) {
        is ExchangeRate.FiatToCrypto -> value.currencyCode == this.from
        is ExchangeRate.CryptoToFiat -> (value is CryptoValue && value.currency == this.from)
        is ExchangeRate.FiatToFiat -> (value is FiatValue && value.currencyCode == this.from)
        is ExchangeRate.CryptoToCrypto -> (value is CryptoValue && value.currency == this.from)
        is ExchangeRate.InvalidRate -> false
    }