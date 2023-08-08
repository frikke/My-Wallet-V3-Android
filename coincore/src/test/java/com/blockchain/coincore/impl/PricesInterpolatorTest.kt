package com.blockchain.coincore.impl

import com.blockchain.coincore.testutil.CoincoreTestBase
import com.blockchain.coincore.testutil.USD
import com.blockchain.nabu.datamanagers.PriceTier
import com.blockchain.nabu.datamanagers.repositories.Interpolator
import com.blockchain.nabu.datamanagers.repositories.LinearInterpolator
import com.blockchain.nabu.datamanagers.repositories.PricesInterpolator
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.Money
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.math.BigDecimal
import java.math.BigInteger
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class PricesInterpolatorTest : CoincoreTestBase() {

    private val interpolator: Interpolator = mockk()

    private val subject = PricesInterpolator(
        interpolator = interpolator,
        pair = PAIR,
        prices = PRICES
    )

    @Test
    fun `amount below min volume should use the min volume price`() {
        val amount = Money.fromMinor(SOURCE, BigInteger.valueOf(0))
        val actual = subject.getRate(amount)
        actual shouldBeEqualTo PRICE_1.price
    }

    @Test
    fun `amount above max volume should use the max volume price`() {
        val amount = Money.fromMinor(SOURCE, BigInteger.valueOf(10_000))
        val actual = subject.getRate(amount)
        actual shouldBeEqualTo PRICE_3.price
    }

    @Test
    fun `amount between volumes should interpolate between the 2 volumes prices`() {
        val amount = Money.fromMinor(SOURCE, BigInteger.valueOf(1_500))
        val expected = Money.fromMajor(DESTINATION, BigDecimal(1_337))

        every { interpolator.interpolate(any(), any(), any(), any()) } returns BigDecimal(1_337)
        val actual = subject.getRate(amount)
        actual shouldBeEqualTo expected

        val cenas = LinearInterpolator().interpolate(
            x = listOf(PRICE_1.volume.toBigDecimal(), PRICE_2.volume.toBigDecimal()),
            y = listOf(PRICE_1.price.toBigDecimal(), PRICE_2.price.toBigDecimal()),
            xi = amount.toBigDecimal(),
            scale = DESTINATION.precisionDp
        )

        verify {
            interpolator.interpolate(
                x = listOf(PRICE_1.volume.toBigDecimal(), PRICE_2.volume.toBigDecimal()),
                y = listOf(PRICE_1.price.toBigDecimal(), PRICE_2.price.toBigDecimal()),
                xi = amount.toBigDecimal(),
                scale = DESTINATION.precisionDp
            )
        }
    }

    companion object {
        private val SOURCE = CryptoCurrency.BTC
        private val DESTINATION = USD

        private val PAIR = CurrencyPair(SOURCE, DESTINATION)
        private val PRICE_1 = PriceTier(
            volume = Money.fromMinor(SOURCE, BigInteger.valueOf(1_000)),
            price = Money.fromMinor(DESTINATION, BigInteger.valueOf(100))
        )
        private val PRICE_2 = PriceTier(
            volume = Money.fromMinor(SOURCE, BigInteger.valueOf(2_000)),
            price = Money.fromMinor(DESTINATION, BigInteger.valueOf(200))
        )
        private val PRICE_3 = PriceTier(
            volume = Money.fromMinor(SOURCE, BigInteger.valueOf(3_000)),
            price = Money.fromMinor(DESTINATION, BigInteger.valueOf(300))
        )

        private val PRICES = listOf(PRICE_1, PRICE_2, PRICE_3)
    }
}
