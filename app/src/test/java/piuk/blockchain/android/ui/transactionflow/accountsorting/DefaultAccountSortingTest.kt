package piuk.blockchain.android.ui.transactionflow.accountsorting

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.Asset
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.logging.MomentEvent
import com.blockchain.logging.MomentLogger
import com.blockchain.preferences.DashboardPrefs
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Currency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.math.BigDecimal
import java.math.BigInteger
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.transfer.DefaultAccountsSorting

class DefaultAccountSortingTest {
    private val dashboardPrefs: DashboardPrefs = mock()
    private val coincore: Coincore = mock()
    private val walletModeService: WalletModeService = mock()
    private val assetCatalogue: AssetCatalogue = mock()
    private val momentLogger: MomentLogger = mock()

    private lateinit var subject: DefaultAccountsSorting

    private val ethMock: CryptoCurrency = object : CryptoCurrency(
        displayTicker = "ETH",
        networkTicker = "ETH",
        name = "Not a real thing",
        categories = setOf(AssetCategory.CUSTODIAL),
        precisionDp = 18,
        requiredConfirmations = 3,
        colour = "000000"
    ) {}

    private val btcMock: CryptoCurrency = object : CryptoCurrency(
        displayTicker = "BTC",
        networkTicker = "BTC",
        name = "Not a real thing",
        categories = setOf(AssetCategory.CUSTODIAL),
        precisionDp = 8,
        requiredConfirmations = 3,
        colour = "000000"
    ) {}

    private val xlmMock: CryptoCurrency = object : CryptoCurrency(
        displayTicker = "XLM",
        networkTicker = "XLM",
        name = "Not a real thing",
        categories = setOf(AssetCategory.CUSTODIAL),
        precisionDp = 7,
        requiredConfirmations = 3,
        colour = "000000"
    ) {}

    companion object {
        private const val ONE_ETH = 1000000000000000000L
        private const val ONE_BTC = 100000000L
        private const val ONE_XLM = 10000000L
    }

    @Before
    fun setup() {
        subject = DefaultAccountsSorting(
            dashboardPrefs = dashboardPrefs,
            assetCatalogue = assetCatalogue,
            walletModeService = walletModeService,
            coincore = coincore,
            momentLogger = momentLogger
        )

        doNothing().whenever(momentLogger).startEvent(any())
        doNothing().whenever(momentLogger).endEvent(any(), any())
    }

    @Test
    fun `given wallet mode !custodial only and prefs exist then ordering follows prefs ordering`() {
        whenever(walletModeService.walletModeSingle).thenReturn(Single.just(WalletMode.NON_CUSTODIAL))
        whenever(dashboardPrefs.dashboardAssetOrder).thenReturn(listOf("XLM", "BTC", "ETH"))
        whenever(assetCatalogue.assetInfoFromNetworkTicker("XLM")).thenReturn(xlmMock)
        whenever(assetCatalogue.assetInfoFromNetworkTicker("BTC")).thenReturn(btcMock)
        whenever(assetCatalogue.assetInfoFromNetworkTicker("ETH")).thenReturn(ethMock)

        val xlmAccount = setAccountForAssetWithBalance(xlmMock, 0L)
        val ethAccount = setAccountForAssetWithBalance(ethMock, 0L)
        val btcAccount = setAccountForAssetWithBalance(btcMock, 0L)

        val list = listOf(
            btcAccount,
            xlmAccount,
            ethAccount
        )

        val result = subject.sorter().invoke(list).test()

        result.assertValue {
            it[0] == xlmAccount &&
                it[1] == btcAccount &&
                it[2] == ethAccount
        }

        verify(dashboardPrefs).dashboardAssetOrder
        verify(assetCatalogue).assetInfoFromNetworkTicker("XLM")
        verify(assetCatalogue).assetInfoFromNetworkTicker("BTC")
        verify(assetCatalogue).assetInfoFromNetworkTicker("ETH")
        verifyNoMoreInteractions(dashboardPrefs)
        verifyNoMoreInteractions(assetCatalogue)
        verifyNoMoreInteractions(coincore)

        verifyMomentEvents(MomentEvent.DEFAULT_SORTING_NC_AND_UNIVERSAL)
    }

    @Test
    fun `given wallet mode !custodial only and prefs don't exist then ordering is alphabetical`() {
        whenever(walletModeService.walletModeSingle).thenReturn(Single.just(WalletMode.NON_CUSTODIAL))

        whenever(dashboardPrefs.dashboardAssetOrder).thenReturn(emptyList())
        whenever(assetCatalogue.supportedCryptoAssets).thenReturn(listOf(xlmMock, btcMock, ethMock))

        val xlmAccount = setAccountForAssetWithBalance(xlmMock, 0L)
        val ethAccount = setAccountForAssetWithBalance(ethMock, 0L)
        val btcAccount = setAccountForAssetWithBalance(btcMock, 0L)

        val list = listOf(
            btcAccount,
            xlmAccount,
            ethAccount
        )

        val result = subject.sorter().invoke(list).test()

        result.assertValue {
            it[0] == btcAccount &&
                it[1] == ethAccount &&
                it[2] == xlmAccount
        }

        verify(dashboardPrefs).dashboardAssetOrder
        verify(assetCatalogue).supportedCryptoAssets
        verifyNoMoreInteractions(dashboardPrefs)
        verifyNoMoreInteractions(assetCatalogue)
        verifyNoMoreInteractions(coincore)

        verifyMomentEvents(MomentEvent.DEFAULT_SORTING_NC_AND_UNIVERSAL)
    }

    @Test
    fun `given wallet mode custodial only then ordering follows balances`() {
        whenever(walletModeService.walletModeSingle).thenReturn(Single.just(WalletMode.CUSTODIAL))
        val xlmAccount = setAccountForAssetWithBalance(xlmMock, ONE_XLM)
        val ethAccount = setAccountForAssetWithBalance(ethMock, 2 * ONE_ETH)
        val btcAccount = setAccountForAssetWithBalance(btcMock, 5 * ONE_BTC)

        val list = listOf(
            xlmAccount,
            ethAccount,
            btcAccount
        )

        val result = subject.sorter().invoke(list).test()

        result.assertValue {
            it[0] == btcAccount &&
                it[1] == ethAccount &&
                it[2] == xlmAccount
        }

        verifyNoMoreInteractions(dashboardPrefs)
        verifyNoMoreInteractions(assetCatalogue)
        verifyMomentEvents(MomentEvent.DEFAULT_SORTING_CUSTODIAL_ONLY)
    }

    private fun setAccountForAssetWithBalance(currency: Currency, accountBalance: Long): CryptoAccount {
        val mockBalance = mock<AccountBalance> {
            on { total }.thenReturn(Money.fromMinor(currency, BigInteger.valueOf(accountBalance)))
            on { withdrawable }.thenReturn(Money.fromMinor(currency, BigInteger.valueOf(accountBalance)))
            on { pending }.thenReturn(Money.fromMinor(currency, BigInteger.valueOf(accountBalance)))
            on { exchangeRate }.thenReturn(
                ExchangeRate(
                    BigDecimal.ONE, currency, FiatCurrency.Dollars
                )
            )
        }
        val singleAccount: CryptoAccount = mock {
            on { balanceRx }.thenReturn(
                Observable.just(
                    mockBalance
                )
            )
            on { this.currency }.thenReturn(currency as AssetInfo)
        }
        val prices24HrWithDelta = Prices24HrWithDelta(
            1.0,
            ExchangeRate(
                BigDecimal.ONE, currency, FiatCurrency.Dollars
            ),
            ExchangeRate(
                BigDecimal.ONE, currency, FiatCurrency.Dollars
            )
        )

        val assetMock = mock<Asset> {
            on { getPricesWith24hDeltaLegacy() }.thenReturn(Single.just(prices24HrWithDelta))
        }

        whenever(coincore[currency]).thenReturn(assetMock)

        return singleAccount
    }

    private fun verifyMomentEvents(event: MomentEvent) {
        verify(momentLogger).startEvent(event)
        verify(momentLogger).endEvent(event)
        verifyNoMoreInteractions(momentLogger)
    }
}
