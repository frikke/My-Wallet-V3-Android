package piuk.blockchain.android.ui.dashboard.model

import info.blockchain.balance.CryptoCurrency
import kotlin.test.assertEquals
import org.junit.Test

class ShowAnnouncementTest {

    val subject = DashboardIntent.ShowAnnouncement(testAnnouncementCard_1)

    @Test
    fun `showing an announcement, sets announcement and leaves other fields unchanged`() {

        val initialState = DashboardState(
            activeAssets = mapOfAssets(
                CryptoCurrency.BTC to initialBtcState,
                CryptoCurrency.ETHER to initialEthState,
                CryptoCurrency.XLM to initialXlmState
            ),
            announcement = null
        )

        val result = subject.reduce(initialState)

        assertEquals(result.activeAssets, initialState.activeAssets)
        assertEquals(result.announcement, testAnnouncementCard_1)
    }

    @Test
    fun `replacing an announcement, sets announcement and leaves other fields unchanged`() {

        val initialState = DashboardState(
            activeAssets = mapOfAssets(
                CryptoCurrency.BTC to initialBtcState,
                CryptoCurrency.ETHER to initialEthState,
                CryptoCurrency.XLM to initialXlmState
            ),
            announcement = testAnnouncementCard_2
        )

        val result = subject.reduce(initialState)

        assertEquals(result.activeAssets, initialState.activeAssets)
        assertEquals(result.announcement, testAnnouncementCard_1)
    }
}
