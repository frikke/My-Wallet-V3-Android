package piuk.blockchain.android.ui.dashboard.model

import info.blockchain.balance.CryptoCurrency
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test
import piuk.blockchain.android.ui.dashboard.navigation.DashboardNavigationAction

class HideAssetDetailsTest {

    val subject = DashboardIntent.ClearBottomSheet

    @Test
    fun `clearing empty asset sheet no effect`() {
        val initialState = DashboardState(
            activeAssets = mapOfAssets(
                CryptoCurrency.BTC to initialBtcState,
                CryptoCurrency.ETHER to initialEthState,
                CryptoCurrency.XLM to initialXlmState
            ),
            dashboardNavigationAction = null,
            announcement = testAnnouncementCard_1
        )

        val result = DashboardIntent.ClearBottomSheet.reduce(initialState)
        assertEquals(result, initialState)
    }

    @Test
    fun `clearing asset sheet, clears the asset and leaves other fields unchanged`() {

        val initialState = DashboardState(
            activeAssets = mapOfAssets(
                CryptoCurrency.BTC to initialBtcState,
                CryptoCurrency.ETHER to initialEthState,
                CryptoCurrency.XLM to initialXlmState
            ),
            dashboardNavigationAction = null,
            announcement = testAnnouncementCard_1
        )

        val result = DashboardIntent.ClearBottomSheet.reduce(initialState)

        assertEquals(result.activeAssets, initialState.activeAssets)
        assertNull(result.dashboardNavigationAction)
        assertEquals(result.announcement, initialState.announcement)
    }

    @Test
    fun `clearing promo sheet, clears the sheet and leaves other fields unchanged`() {

        val initialState = DashboardState(
            activeAssets = mapOfAssets(
                CryptoCurrency.BTC to initialBtcState,
                CryptoCurrency.ETHER to initialEthState,
                CryptoCurrency.XLM to initialXlmState
            ),
            dashboardNavigationAction = DashboardNavigationAction.StxAirdropComplete,
            announcement = testAnnouncementCard_1
        )

        val result = DashboardIntent.ClearBottomSheet.reduce(initialState)

        assertEquals(result.activeAssets, initialState.activeAssets)
        assertNull(result.dashboardNavigationAction)
        assertEquals(result.announcement, initialState.announcement)
    }
}
