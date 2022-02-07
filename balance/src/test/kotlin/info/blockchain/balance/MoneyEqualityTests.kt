package info.blockchain.balance

import com.blockchain.testutils.CAD
import com.blockchain.testutils.GBP
import com.blockchain.testutils.USD
import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.bitcoinCash
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should not be`
import org.amshove.kluent.`should not be equal to`
import org.junit.Test

class MoneyEqualityTests {

    @Test
    fun `c2c equality`() {
        val m1: Money = 1.bitcoin()
        val m2: Money = 1.bitcoin()

        m1 `should not be` m2
        m1 `should be equal to` m2
    }

    @Test
    fun `c2c inequality by value`() {
        val m1: Money = 1.bitcoin()
        val m2: Money = 2.bitcoin()

        m1 `should not be` m2
        m1 `should not be equal to` m2
    }

    @Test
    fun `c2c inequality by currency`() {
        val m1: Money = 1.bitcoin()
        val m2: Money = 1.bitcoinCash()

        m1 `should not be` m2
        m1 `should not be equal to` m2
    }

    @Test
    fun `c2f inequality`() {
        val m1: Money = 1.bitcoin()
        val m2: Money = FiatValue.fromMinor(USD, 1.toBigInteger())

        m1 `should not be` m2
        m1 `should not be equal to` m2
    }

    @Test
    fun `f2f inequality by currency`() {
        val m1: Money = FiatValue.fromMinor(GBP, 1.toBigInteger())
        val m2: Money = FiatValue.fromMinor(USD, 1.toBigInteger())

        m1 `should not be` m2
        m1 `should not be equal to` m2
    }

    @Test
    fun `f2f inequality by value`() {
        val m1: Money = FiatValue.fromMinor(CAD, 1.toBigInteger())
        val m2: Money = FiatValue.fromMinor(CAD, 2.toBigInteger())

        m1 `should not be` m2
        m1 `should not be equal to` m2
    }

    @Test
    fun `f2f equality`() {
        val m1: Money = FiatValue.fromMinor(CAD, 2.toBigInteger())
        val m2: Money = FiatValue.fromMinor(CAD, 2.toBigInteger())

        m1 `should not be` m2
        m1 `should be equal to` m2
    }
}
