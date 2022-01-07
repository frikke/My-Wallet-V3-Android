package piuk.blockchain.android.ui.dashboard.model

import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.bitcoinCash
import com.blockchain.testutils.ether
import com.nhaarman.mockitokotlin2.mock
import info.blockchain.balance.CryptoCurrency
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import org.junit.Test

class BalanceUpdateTest {

    @Test(expected = IllegalStateException::class)
    fun `Updating a mismatched currency throws an exception`() {

        val initialState = DashboardState(
            activeAssets = mapOfAssets(
                CryptoCurrency.BTC to initialBtcState,
                CryptoCurrency.ETHER to initialEthState,
                CryptoCurrency.XLM to initialXlmState
            ),
            announcement = testAnnouncementCard_1
        )

        val subject = DashboardIntent.BalanceUpdate(
            CryptoCurrency.BTC,
            mock {
                on { total }.thenReturn(1.bitcoinCash())
                on { actionable }.thenReturn(1.bitcoinCash())
                on { pending }.thenReturn(1.bitcoinCash())
                on { exchangeRate }.thenReturn(mock())
            }
        )

        subject.reduce(initialState)
    }

    @Test
    fun `update changes effects correct asset`() {
        val initialState = DashboardState(
            activeAssets = mapOfAssets(
                CryptoCurrency.BTC to initialBtcState,
                CryptoCurrency.ETHER to initialEthState,
                CryptoCurrency.XLM to initialXlmState
            ),
            announcement = testAnnouncementCard_1
        )

        val subject = DashboardIntent.BalanceUpdate(
            CryptoCurrency.BTC,
            mock {
                on { total }.thenReturn(1.bitcoin())
                on { actionable }.thenReturn(1.bitcoin())
                on { pending }.thenReturn(1.bitcoin())
                on { exchangeRate }.thenReturn(mock())
            }
        )

        val result = subject.reduce(initialState)

        assertNotEquals(result.activeAssets, initialState.activeAssets)
        assertNotEquals(result[CryptoCurrency.BTC], initialState[CryptoCurrency.BTC])
        assertEquals(result[CryptoCurrency.ETHER], initialState[CryptoCurrency.ETHER])
        assertEquals(result[CryptoCurrency.XLM], initialState[CryptoCurrency.XLM])

        assertEquals(result.announcement, initialState.announcement)
    }

    @Test
    fun `receiving a valid balance update clears any balance errors`() {
        val initialState = DashboardState(
            activeAssets = mapOfAssets(
                CryptoCurrency.BTC to initialBtcState,
                CryptoCurrency.ETHER to initialEthState.copy(hasBalanceError = true),
                CryptoCurrency.XLM to initialXlmState
            ),
            announcement = testAnnouncementCard_1
        )

        val subject = DashboardIntent.BalanceUpdate(
            CryptoCurrency.ETHER,
            mock {
                on { total }.thenReturn(1.ether())
                on { actionable }.thenReturn(1.ether())
                on { pending }.thenReturn(1.ether())
                on { exchangeRate }.thenReturn(mock())
            }
        )

        val result = subject.reduce(initialState)

        assertFalse(result[CryptoCurrency.ETHER].hasBalanceError)

        assertNotEquals(result.activeAssets, initialState.activeAssets)
        assertEquals(result[CryptoCurrency.BTC], initialState[CryptoCurrency.BTC])
        assertEquals(result[CryptoCurrency.XLM], initialState[CryptoCurrency.XLM])

        assertEquals(result.announcement, initialState.announcement)
    }
}
