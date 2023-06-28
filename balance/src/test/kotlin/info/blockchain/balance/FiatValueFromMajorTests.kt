package info.blockchain.balance

import com.blockchain.testutils.GBP
import com.blockchain.testutils.JPY
import com.blockchain.testutils.gbp
import com.blockchain.testutils.jpy
import org.amshove.kluent.`should be equal to`
import org.junit.Test

class FiatValueFromMajorTests {

    @Test
    fun `from major GBP`() {
        FiatValue.fromMajor(
            GBP,
            1.23.toBigDecimal()
        ) `should be equal to` 1.23.gbp()
    }

    @Test
    fun `from major JPY`() {
        FiatValue.fromMajor(
            JPY,
            500.toBigDecimal()
        ) `should be equal to` 500.jpy()
    }

    @Test
    fun `from major JPY has scale 0`() {
        FiatValue.fromMajor(
            JPY,
            1.toBigDecimal()
        ).toBigDecimal().scale() `should be equal to` 0
    }
}
