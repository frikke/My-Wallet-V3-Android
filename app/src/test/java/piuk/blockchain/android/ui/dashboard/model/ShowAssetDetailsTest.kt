package piuk.blockchain.android.ui.dashboard.model

import info.blockchain.balance.CryptoCurrency
import kotlin.test.assertEquals
import org.junit.Test
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsFlow

class ShowAssetDetailsTest {

    private val flow = AssetDetailsFlow(CryptoCurrency.ETHER)
    private val subject = DashboardIntent.UpdateLaunchDialogFlow(flow)

    @Test
    fun `showing asset details, sets asset type and leaves other fields unchanged`() {

        val initialState = DashboardState(
            activeAssets = mapOfAssets(
                CryptoCurrency.BTC to initialBtcState,
                CryptoCurrency.ETHER to initialEthState,
                CryptoCurrency.XLM to initialXlmState
            ),
            activeFlow = null,
            announcement = testAnnouncementCard_1
        )

        val result = subject.reduce(initialState)

        assertEquals(result.activeAssets, initialState.activeAssets)
        assertEquals(result.activeFlow, flow)
        assertEquals(result.announcement, testAnnouncementCard_1)
    }

    @Test
    fun `replacing asset details type, sets asset and leaves other fields unchanged`() {

        val initialState = DashboardState(
            activeAssets = mapOfAssets(
                CryptoCurrency.BTC to initialBtcState,
                CryptoCurrency.ETHER to initialEthState,
                CryptoCurrency.XLM to initialXlmState
            ),
            activeFlow = null,
            announcement = testAnnouncementCard_1
        )

        val result = subject.reduce(initialState)

        assertEquals(result.activeAssets, initialState.activeAssets)
        assertEquals(result.activeFlow, flow)
        assertEquals(result.announcement, testAnnouncementCard_1)
    }

    @Test
    fun `replacing an asset details type with the same type has no effect`() {
        val initialState = DashboardState(
            activeAssets = mapOfAssets(
                CryptoCurrency.BTC to initialBtcState,
                CryptoCurrency.ETHER to initialEthState,
                CryptoCurrency.XLM to initialXlmState
            ),
            activeFlow = flow,
            announcement = testAnnouncementCard_1
        )

        val result = subject.reduce(initialState)

        assertEquals(result, initialState)
    }
}
