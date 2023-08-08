package info.blockchain.balance

import java.math.BigDecimal
import java.math.BigInteger
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should be equal to`
import org.junit.Test

class CryptoValueTests {

    @Test
    fun `zero btc`() {
        CryptoValue.zero(CryptoCurrency.BTC) `should be equal to` CryptoValue(CryptoCurrency.BTC, BigInteger.ZERO)
    }

    @Test
    fun `zero bch`() {
        CryptoValue.zero(CryptoCurrency.BCH) `should be equal to` CryptoValue(CryptoCurrency.BCH, BigInteger.ZERO)
    }

    @Test
    fun `zero eth`() {
        CryptoValue.zero(CryptoCurrency.ETHER) `should be equal to` CryptoValue(CryptoCurrency.ETHER, BigInteger.ZERO)
    }

    @Test
    fun `toBigDecimal BTC`() {
        CryptoValue.fromMinor(
            CryptoCurrency.BTC,
            12345678901.toBigInteger()
        ).toBigDecimal() `should be equal to` BigDecimal("123.45678901")
    }

    @Test
    fun `toBigDecimal BCH`() {
        CryptoValue.fromMinor(
            CryptoCurrency.BCH,
            234.toBigInteger()
        ).toBigDecimal() `should be equal to` BigDecimal("0.00000234")
    }

    @Test
    fun `toBigDecimal ETH`() {
        CryptoValue(
            CryptoCurrency.ETHER,
            234L.toBigInteger()
        ).toBigDecimal() `should be equal to` BigDecimal("0.000000000000000234")
    }

    @Test
    fun `toBigDecimal keeps all trailing 0s`() {
        CryptoValue(
            CryptoCurrency.BTC,
            10000000000L.toBigInteger()
        ).toBigDecimal() `should be equal to` BigDecimal("100.00000000")
    }

    @Test
    fun `toMajorUnit Double`() {
        CryptoValue(CryptoCurrency.BTC, 12300001234L.toBigInteger())
            .toMajorUnitDouble() `should be equal to` 123.00001234
    }

    @Test
    fun `zero is not positive`() {
        CryptoValue.zero(CryptoCurrency.BTC).isPositive `should be` false
    }

    @Test
    fun `1 Satoshi is positive`() {
        CryptoValue.fromMinor(CryptoCurrency.BTC, 1.toBigInteger()).isPositive `should be` true
    }

    @Test
    fun `2 Satoshis is positive`() {
        CryptoValue.fromMinor(CryptoCurrency.BTC, 2.toBigInteger()).isPositive `should be` true
    }

    @Test
    fun `-1 Satoshi is not positive`() {
        CryptoValue.fromMinor(CryptoCurrency.BTC, (-1).toBigInteger()).isPositive `should be` false
    }

    @Test
    fun `zero isZero`() {
        CryptoValue.zero(CryptoCurrency.BTC).isZero `should be` true
    }

    @Test
    fun `1 satoshi is not isZero`() {
        CryptoValue.fromMinor(CryptoCurrency.BTC, 1.toBigInteger()).isZero `should be` false
    }

    @Test
    fun `1 wei is not isZero`() {
        CryptoValue.fromMinor(CryptoCurrency.ETHER, BigInteger.ONE).isZero `should be` false
    }

    @Test
    fun `0 wei is isZero`() {
        CryptoValue.fromMinor(CryptoCurrency.ETHER, BigInteger.ZERO).isZero `should be` true
    }

    @Test
    fun `amount is the minor part of the currency`() {
        CryptoValue(CryptoCurrency.BTC, 1234.toBigInteger()).toBigInteger() `should be equal to` 1234L.toBigInteger()
    }

    @Test
    fun `amount is the total minor part of the currency`() {
        CryptoValue.fromMajor(CryptoCurrency.ETHER, 2L.toBigDecimal())
            .toBigInteger() `should be equal to` 2e18.toBigDecimal().toBigIntegerExact()
    }

    @Test
    fun `amount when created from satoshis`() {
        CryptoValue.fromMinor(CryptoCurrency.BTC, 4567L.toBigInteger()).apply {
            currency `should be equal to` CryptoCurrency.BTC
            toBigInteger() `should be equal to` 4567.toBigInteger()
        }
    }

    @Test
    fun `amount when created from satoshis big integer`() {
        CryptoValue.fromMinor(CryptoCurrency.BTC, 4567.toBigInteger()).apply {
            currency `should be equal to` CryptoCurrency.BTC
            toBigInteger() `should be equal to` 4567.toBigInteger()
        }
    }

    @Test
    fun `amount of Cash when created from satoshis`() {
        CryptoValue.fromMinor(CryptoCurrency.BCH, 45678.toBigInteger()).apply {
            currency `should be equal to` CryptoCurrency.BCH
            toBigInteger() `should be equal to` 45678.toBigInteger()
        }
    }
}
