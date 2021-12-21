package com.blockchain.testutils

import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import java.math.BigDecimal
import java.math.BigInteger

fun Number.gbp() = FiatValue.fromMajor(GBP, numberToBigDecimal())

fun Number.usd() = FiatValue.fromMajor(USD, numberToBigDecimal())

fun Number.jpy() = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("JPY"), numberToBigDecimal())

fun Number.eur() = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("EUR"), numberToBigDecimal())

fun Number.cad() = FiatValue.fromMajor(FiatCurrency.fromCurrencyCode("CAD"), numberToBigDecimal())

val USD = FiatCurrency.fromCurrencyCode("USD")
val EUR = FiatCurrency.fromCurrencyCode("EUR")
val GBP = FiatCurrency.fromCurrencyCode("GBP")
val CAD = FiatCurrency.fromCurrencyCode("CAD")
val JPY = FiatCurrency.fromCurrencyCode("JPY")
val PLN = FiatCurrency.fromCurrencyCode("PLN")

fun Number.numberToBigDecimal(): BigDecimal =
    when (this) {
        is Double -> toBigDecimal()
        is Int -> toBigDecimal()
        is Long -> toBigDecimal()
        is BigDecimal -> this
        else -> throw NotImplementedError(this.javaClass.name)
    }

fun Number.numberToBigInteger(): BigInteger =
    when (this) {
        is BigInteger -> this
        is Int -> toBigInteger()
        is Long -> toBigInteger()
        else -> throw NotImplementedError(this.javaClass.name)
    }

fun Number.bitcoin() = CryptoValue.fromMajor(CryptoCurrency.BTC, numberToBigDecimal())
fun Number.satoshi() = CryptoValue.fromMinor(CryptoCurrency.BTC, numberToBigInteger())
fun Number.ether() = CryptoValue.fromMajor(CryptoCurrency.ETHER, numberToBigDecimal())
fun Number.gwei() = CryptoValue.fromMinor(CryptoCurrency.ETHER, numberToBigDecimal() * 1000000000.toBigDecimal())
fun Number.bitcoinCash() = CryptoValue.fromMajor(CryptoCurrency.BCH, numberToBigDecimal())
fun Number.satoshiCash() = CryptoValue.fromMinor(CryptoCurrency.BCH, numberToBigDecimal())
fun Number.lumens() = CryptoValue.fromMajor(CryptoCurrency.XLM, numberToBigDecimal())
fun Number.stroops() = CryptoValue.fromMinor(CryptoCurrency.XLM, numberToBigInteger())

fun Number.testValue(asset: AssetInfo) = CryptoValue.fromMajor(asset, numberToBigDecimal())