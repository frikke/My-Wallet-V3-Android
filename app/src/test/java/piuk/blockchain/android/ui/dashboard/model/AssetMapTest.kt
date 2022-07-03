package piuk.blockchain.android.ui.dashboard.model

import com.blockchain.coincore.AccountBalance
import com.blockchain.core.price.ExchangeRate
import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.ether
import com.nhaarman.mockitokotlin2.mock
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import org.junit.Test

class AssetMapTest {

    private val subject = AssetMap(
        map = mapOfAssets(
            CryptoCurrency.BTC to initialBtcState,
            CryptoCurrency.ETHER to initialEthState,
            CryptoCurrency.XLM to initialXlmState
        )
    )

    @Test(expected = IllegalArgumentException::class)
    fun `Exception thrown if unknown asset requested from get()`() {
        val invalidAsset: AssetInfo = mock()

        subject[invalidAsset]
    }

    @Test
    fun `copy with patchAsset works as expected`() {
        val newAsset = BrokerageAsset(
            currency = CryptoCurrency.BTC,
            accountBalance = mock {
                on { total }.thenReturn(20.bitcoin())
                on { withdrawable }.thenReturn(20.bitcoin())
                on { pending }.thenReturn(20.bitcoin())
                on { exchangeRate }.thenReturn(
                    ExchangeRate(300.toBigDecimal(), FIAT_CURRENCY, CryptoCurrency.BTC)
                )
            },
            prices24HrWithDelta = mock(),
            priceTrend = emptyList()
        )

        val copy = subject.copy(patchAsset = newAsset)

        assertNotEquals(copy[CryptoCurrency.BTC], subject[CryptoCurrency.BTC])
        assertEquals(copy[CryptoCurrency.BTC], newAsset)
        assertEquals(copy[CryptoCurrency.ETHER], subject[CryptoCurrency.ETHER])
        assertEquals(copy[CryptoCurrency.XLM], subject[CryptoCurrency.XLM])
    }

    @Test
    fun `copy with patchBalance works as expected`() {
        val newBalance = 20.ether()
        val newAccountBalance = mock<AccountBalance> {
            on { total }.thenReturn(newBalance)
            on { withdrawable }.thenReturn(newBalance)
            on { pending }.thenReturn(newBalance)
            on { exchangeRate }.thenReturn(mock())
        }

        val copy = subject.copy(patchBalance = newAccountBalance)

        assertEquals(copy[CryptoCurrency.BTC], subject[CryptoCurrency.BTC])
        assertNotEquals(copy[CryptoCurrency.ETHER], subject[CryptoCurrency.ETHER])
        assertEquals(copy[CryptoCurrency.ETHER].accountBalance, newAccountBalance)
        assertEquals(copy[CryptoCurrency.XLM], subject[CryptoCurrency.XLM])
    }

    @Test
    fun `reset() replaces all assets`() {
        val result = subject.reset()

        assertEquals(result.size, subject.size)
        subject.keys.forEach {
            assertNotSame(result[it], subject[it])
        }
    }
}
