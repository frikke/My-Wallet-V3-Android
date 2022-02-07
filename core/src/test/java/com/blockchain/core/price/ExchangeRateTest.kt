package com.blockchain.core.price

import com.blockchain.nabu.GBP
import com.blockchain.nabu.USD
import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.bitcoinCash
import com.blockchain.testutils.ether
import com.blockchain.testutils.usd
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.ValueTypeMismatchException
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should be equal to`
import org.junit.Test

class ExchangeRateTest {

    @Test
    fun `crypto to crypto`() {
        ExchangeRate(from = CryptoCurrency.BTC, to = CryptoCurrency.BCH, rate = 20.toBigDecimal())
            .convert(10.bitcoin()) `should be equal to` 200.bitcoinCash()
    }

    @Test
    fun `crypto to fiat`() {
        ExchangeRate(from = CryptoCurrency.BTC, to = USD, rate = 20.toBigDecimal())
            .convert(10.bitcoin()) `should be equal to` 200.usd()
    }

    @Test
    fun `fiat to crypto`() {
        ExchangeRate(from = USD, to = CryptoCurrency.BTC, rate = 20.toBigDecimal())
            .convert(10.usd()) `should be equal to` 200.bitcoin()
    }

    @Test(expected = ValueTypeMismatchException::class)
    fun `crypto to crypto - from miss match`() {
        ExchangeRate(from = CryptoCurrency.BCH, to = CryptoCurrency.BCH, rate = 20.toBigDecimal())
            .convert(10.bitcoin())
    }

    @Test(expected = ValueTypeMismatchException::class)
    fun `crypto to fiat - from miss match`() {
        ExchangeRate(from = CryptoCurrency.BTC, to = USD, rate = 20.toBigDecimal())
            .convert(10.ether())
    }

    @Test(expected = ValueTypeMismatchException::class)
    fun `fiat to crypto - from miss match`() {
        ExchangeRate(from = GBP, to = CryptoCurrency.BTC, rate = 20.toBigDecimal())
            .convert(10.usd())
    }

    @Test
    fun `crypto to fiat - inverse`() {
        ExchangeRate(from = CryptoCurrency.BTC, to = USD, rate = 20.toBigDecimal()).inverse()
            .convert(200.usd()) `should be equal to` 10.bitcoin()
    }

    @Test
    fun `fiat to crypto - inverse`() {
        ExchangeRate(from = USD, to = CryptoCurrency.BTC, rate = 20.toBigDecimal()).inverse()
            .convert(200.bitcoin()) `should be equal to` 10.usd()
    }

    @Test
    fun `crypto to crypto - inverse`() {
        ExchangeRate(from = CryptoCurrency.BTC, to = CryptoCurrency.BCH, rate = 20.toBigDecimal()).inverse()
            .apply {
                from `should be` CryptoCurrency.BCH
                to `should be` CryptoCurrency.BTC
                price.toBigDecimal().stripTrailingZeros() `should be equal to` 0.05.toBigDecimal()
            }
    }
}
