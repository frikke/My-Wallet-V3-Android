package piuk.blockchain.android.ui.dashboard.model

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class DashboardStateTest {

    @Test
    fun `if assets are zero, balance is zero`() {
        val subject = DashboardState(
            activeAssets = mapOfAssets(
                CryptoCurrency.BTC to initialBtcState,
                CryptoCurrency.ETHER to initialEthState,
                CryptoCurrency.XLM to initialXlmState
            ),
            announcement = null
        )

        assertEquals(subject.dashboardBalance!!.fiatBalance, FiatValue.zero(FIAT_CURRENCY))
    }

    @Test
    fun `if only one asset loaded, and is zero, then total is zero`() {
        val subject = DashboardState(
            activeAssets = mapOfAssets(
                CryptoCurrency.BTC to initialBtcState,
                CryptoCurrency.ETHER to BrokerageCryptoAsset(CryptoCurrency.ETHER),
                CryptoCurrency.XLM to BrokerageCryptoAsset(CryptoCurrency.XLM)
            ),
            announcement = null
        )

        assertEquals(subject.dashboardBalance!!.fiatBalance, FiatValue.zero(FIAT_CURRENCY))
    }

    @Test
    fun `if no assets are loaded, total balance is null`() {
        val subject = DashboardState(
            activeAssets = mapOfAssets(
                CryptoCurrency.BTC to BrokerageCryptoAsset(CryptoCurrency.BTC),
                CryptoCurrency.ETHER to BrokerageCryptoAsset(CryptoCurrency.ETHER),
                CryptoCurrency.XLM to BrokerageCryptoAsset(CryptoCurrency.XLM)
            ),
            announcement = null
        )

        assertNull(subject.dashboardBalance!!.fiatBalance)
    }

    @Test
    fun `if bitcoin asset is loaded then delta should be -25`() {
        val subject = DashboardState(
            activeAssets = mapOfAssets(
                CryptoCurrency.BTC to testBtcState,
                CryptoCurrency.ETHER to initialEthState,
                CryptoCurrency.XLM to initialXlmState
            ),
            announcement = null
        )

        val expectedResult = Pair(
            FiatValue.fromMajor(FIAT_CURRENCY, (-1000).toBigDecimal()),
            -25.0
        )

        val result = (subject.dashboardBalance as BrokerageBalanceState).delta

        assertEquals(expectedResult, result)
    }

    @Test
    fun `if  assets is loaded then delta should be -25`() {
        val subject = DashboardState(
            activeAssets = mapOfAssets(
                CryptoCurrency.BTC to testBtcState,
                CryptoCurrency.ETHER to initialEthState,
                CryptoCurrency.XLM to initialXlmState
            ),
            announcement = null
        )

        assertEquals(
            Pair(
                FiatValue.fromMajor(
                    FIAT_CURRENCY,
                    (-1000).toBigDecimal()
                ),
                -25.0
            ),
            (subject.dashboardBalance as BrokerageBalanceState).delta
        )
    }
}
