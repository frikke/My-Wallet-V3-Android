package piuk.blockchain.android.ui.dashboard.coinview

import com.blockchain.android.testutils.rxInit
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.FeatureAccess
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.dashboard.assetdetails.CheckBuyStatus

class CoinViewModelTest {

    private val defaultState = CoinViewState()

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() }.thenReturn(false)
    }

    private val interactor: CoinViewInteractor = mock()

    private lateinit var subject: CoinViewModel

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        subject = CoinViewModel(
            initialState = defaultState,
            mainScheduler = Schedulers.io(),
            interactor = interactor,
            environmentConfig = environmentConfig,
            crashLogger = mock()
        )
    }

    @Test
    fun `load asset details should fire other intents succeeds`() {
        val ticker = "BTC"
        val asset: CryptoAsset = mock {
            on { assetInfo }.thenReturn(mock())
        }
        val selectedFiat: FiatCurrency = FiatCurrency.Dollars
        whenever(interactor.loadAssetDetails(ticker)).thenReturn(Pair(asset, selectedFiat))
        whenever(interactor.loadAccountDetails(asset)).thenReturn(Single.error(Exception()))
        whenever(interactor.loadRecurringBuys(asset.assetInfo)).thenReturn(Single.error(Exception()))

        val test = subject.state.test()
        subject.process(CoinViewIntent.LoadAsset(ticker))

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.asset == asset && it.selectedFiat == selectedFiat
        }.assertValueAt(2) {
            it.viewState == CoinViewViewState.LoadingWallets
        }.assertValueAt(3) {
            it.viewState == CoinViewViewState.LoadingRecurringBuys
        }
    }

    @Test
    fun `load asset details should fire other intents fails with unknown asset`() {
        val ticker = "BTC"

        val selectedFiat: FiatCurrency = FiatCurrency.Dollars
        whenever(interactor.loadAssetDetails(ticker)).thenReturn(Pair(null, selectedFiat))

        val test = subject.state.test()
        subject.process(CoinViewIntent.LoadAsset(ticker))

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.error == CoinViewError.UnknownAsset
        }
    }

    @Test
    fun `load accounts should show account details non tradeable succeeds`() {
        val asset: CryptoAsset = mock {
            on { assetInfo }.thenReturn(mock())
        }
        val prices: Prices24HrWithDelta = mock()

        val state = CoinViewState(
            selectedFiat = FiatCurrency.Dollars
        )

        val localSubject = CoinViewModel(
            initialState = state,
            mainScheduler = Schedulers.io(),
            interactor = interactor,
            environmentConfig = environmentConfig,
            crashLogger = mock()
        )

        val assetInfo = AssetInformation.NonTradeable(prices = prices)
        whenever(interactor.loadAccountDetails(asset)).thenReturn(Single.just(assetInfo))
        whenever(interactor.loadHistoricPrices(eq(asset), any())).thenReturn(Single.error(Exception()))

        val test = localSubject.state.test()

        localSubject.process(CoinViewIntent.LoadAccounts(asset))

        test.assertValueAt(0) {
            it == state
        }.assertValueAt(1) {
            it.viewState == CoinViewViewState.LoadingWallets
        }.assertValueAt(2) {
            it.viewState == CoinViewViewState.NonTradeableAccount &&
                it.assetPrices == assetInfo.prices
        }.assertValueAt(3) {
            it.viewState == CoinViewViewState.LoadingChart
        }.assertValueAt(4) {
            it.error == CoinViewError.ChartLoadError
        }
    }

    @Test
    fun `load accounts should show account details and load quick actions`() {
        val asset: CryptoAsset = mock {
            on { assetInfo }.thenReturn(mock())
        }
        val prices: Prices24HrWithDelta = mock()

        val state = CoinViewState(
            selectedFiat = FiatCurrency.Dollars
        )

        val localSubject = CoinViewModel(
            initialState = state,
            mainScheduler = Schedulers.io(),
            interactor = interactor,
            environmentConfig = environmentConfig,
            crashLogger = mock()
        )

        val list: List<AssetDisplayInfo> = listOf(
            AssetDisplayInfo(
                account = mock<CustodialTradingAccount>(),
                filter = AssetFilter.Custodial,
                amount = mock(),
                pendingAmount = mock(),
                fiatValue = mock(),
                actions = mock(),
                interestRate = 0.0
            )
        )
        val assetInfo = AssetInformation.AccountsInfo(
            prices = prices,
            accountsList = list,
            totalCryptoBalance = mock(),
            totalFiatBalance = mock()
        )

        whenever(interactor.loadAccountDetails(asset)).thenReturn(Single.just(assetInfo))
        whenever(interactor.loadHistoricPrices(eq(asset), any())).thenReturn(Single.error(Exception()))
        whenever(interactor.loadQuickActions(any(), any())).thenReturn(Single.error(Exception()))
        val test = localSubject.state.test()

        localSubject.process(CoinViewIntent.LoadAccounts(asset))

        test.assertValueAt(0) {
            it == state
        }.assertValueAt(1) {
            it.viewState == CoinViewViewState.LoadingWallets
        }.assertValueAt(2) {
            it.viewState is CoinViewViewState.ShowAccountInfo &&
                it.assetPrices == assetInfo.prices
        }.assertValueAt(3) {
            it.viewState == CoinViewViewState.LoadingQuickActions
        }.assertValueAt(4) {
            it.viewState == CoinViewViewState.LoadingChart
        }.assertValueAt(5) {
            it.error == CoinViewError.QuickActionsFailed
        }.assertValueAt(6) {
            it.error == CoinViewError.ChartLoadError
        }
    }

    @Test
    fun `load accounts should show account details fails`() {
        val asset: CryptoAsset = mock {
            on { assetInfo }.thenReturn(mock())
        }
        whenever(interactor.loadAccountDetails(asset)).thenReturn(Single.error(Exception()))

        val test = subject.state.test()
        subject.process(CoinViewIntent.LoadAccounts(asset))
        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.viewState == CoinViewViewState.LoadingWallets
        }.assertValueAt(2) {
            it.error == CoinViewError.WalletLoadError
        }
    }

    @Test
    fun `update account details with selected fiat should load chart`() {
        val asset: CryptoAsset = mock {
            on { assetInfo }.thenReturn(mock())
        }
        val prices: Prices24HrWithDelta = mock()

        val state = CoinViewState(
            selectedFiat = FiatCurrency.Dollars
        )

        val localSubject = CoinViewModel(
            initialState = state,
            mainScheduler = Schedulers.io(),
            interactor = interactor,
            environmentConfig = environmentConfig,
            crashLogger = mock()
        )

        val list: List<AssetDisplayInfo> = listOf(
            AssetDisplayInfo(
                account = mock<CustodialTradingAccount>(),
                filter = AssetFilter.Custodial,
                amount = mock(),
                pendingAmount = mock(),
                fiatValue = mock(),
                actions = mock(),
                interestRate = 0.0
            )
        )
        val assetInfo = AssetInformation.AccountsInfo(
            prices = prices,
            accountsList = list,
            totalCryptoBalance = mock(),
            totalFiatBalance = mock()
        )

        val priceList: HistoricalRateList = emptyList()
        whenever(interactor.loadHistoricPrices(eq(asset), any())).thenReturn(Single.just(priceList))
        whenever(interactor.loadQuickActions(any(), any())).thenReturn(Single.error(Exception()))
        val test = localSubject.state.test()

        localSubject.process(
            CoinViewIntent.UpdateAccountDetails(CoinViewViewState.ShowAccountInfo(assetInfo), assetInfo, asset)
        )

        test.assertValueAt(0) {
            it == state
        }.assertValueAt(1) {
            it.viewState is CoinViewViewState.ShowAccountInfo &&
                it.assetPrices == assetInfo.prices
        }.assertValueAt(2) {
            it.viewState == CoinViewViewState.LoadingChart
        }.assertValueAt(3) {
            it.viewState is CoinViewViewState.ShowAssetInfo
        }
    }

    @Test
    fun `update account details without selected fiat should fail`() {
        val asset: CryptoAsset = mock {
            on { assetInfo }.thenReturn(mock())
        }
        val prices: Prices24HrWithDelta = mock()

        val assetInfo = AssetInformation.AccountsInfo(
            prices = prices,
            accountsList = emptyList(),
            totalCryptoBalance = mock(),
            totalFiatBalance = mock()
        )

        val test = subject.state.test()

        subject.process(
            CoinViewIntent.UpdateAccountDetails(CoinViewViewState.ShowAccountInfo(assetInfo), assetInfo, asset)
        )

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.viewState is CoinViewViewState.ShowAccountInfo &&
                it.assetPrices == assetInfo.prices
        }.assertValueAt(2) {
            it.error == CoinViewError.MissingSelectedFiat
        }
    }

    @Test
    fun `load asset chart succeeds`() {
        val asset: CryptoAsset = mock {
            on { assetInfo }.thenReturn(mock())
        }
        val prices: Prices24HrWithDelta = mock()
        val priceList: HistoricalRateList = emptyList()
        whenever(interactor.loadHistoricPrices(eq(asset), any())).thenReturn(Single.just(priceList))

        val test = subject.state.test()

        subject.process(
            CoinViewIntent.LoadAssetChart(asset, prices, FiatCurrency.Dollars)
        )

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.viewState is CoinViewViewState.LoadingChart
        }.assertValueAt(2) {
            it.viewState is CoinViewViewState.ShowAssetInfo
        }
    }

    @Test
    fun `load asset chart fails`() {
        val asset: CryptoAsset = mock {
            on { assetInfo }.thenReturn(mock())
        }
        val prices: Prices24HrWithDelta = mock()
        val priceList: HistoricalRateList = emptyList()
        whenever(interactor.loadHistoricPrices(eq(asset), any())).thenReturn(Single.error(Exception()))

        val test = subject.state.test()

        subject.process(
            CoinViewIntent.LoadAssetChart(asset, prices, FiatCurrency.Dollars)
        )

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.viewState is CoinViewViewState.LoadingChart
        }.assertValueAt(2) {
            it.error == CoinViewError.ChartLoadError
        }
    }

    @Test
    fun `load new chart period with asset succeeds`() {
        val asset: CryptoAsset = mock {
            on { assetInfo }.thenReturn(mock())
        }
        val prices: Prices24HrWithDelta = mock()

        val state = CoinViewState(
            asset = asset,
            selectedFiat = FiatCurrency.Dollars,
            assetPrices = prices
        )

        val localSubject = CoinViewModel(
            initialState = state,
            mainScheduler = Schedulers.io(),
            interactor = interactor,
            environmentConfig = environmentConfig,
            crashLogger = mock()
        )

        val priceList: HistoricalRateList = emptyList()
        whenever(interactor.loadHistoricPrices(eq(asset), eq(HistoricalTimeSpan.YEAR))).thenReturn(
            Single.just(priceList)
        )

        val test = localSubject.state.test()

        localSubject.process(
            CoinViewIntent.LoadNewChartPeriod(HistoricalTimeSpan.YEAR)
        )

        test.assertValueAt(0) {
            it == state
        }.assertValueAt(1) {
            it.viewState is CoinViewViewState.LoadingChart
        }.assertValueAt(2) {
            it.viewState is CoinViewViewState.ShowAssetInfo
        }
    }

    @Test
    fun `load new chart period with unknown asset should fail`() {
        val test = subject.state.test()

        subject.process(
            CoinViewIntent.LoadNewChartPeriod(HistoricalTimeSpan.YEAR)
        )

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.viewState is CoinViewViewState.LoadingChart
        }.assertValueAt(2) {
            it.error == CoinViewError.UnknownAsset
        }
    }

    @Test
    fun `load new chart period with no asset prices should fail`() {
        val asset: CryptoAsset = mock {
            on { assetInfo }.thenReturn(mock())
        }

        val state = CoinViewState(
            asset = asset,
            selectedFiat = FiatCurrency.Dollars,
            assetPrices = null
        )

        val localSubject = CoinViewModel(
            initialState = state,
            mainScheduler = Schedulers.io(),
            interactor = interactor,
            environmentConfig = environmentConfig,
            crashLogger = mock()
        )

        val priceList: HistoricalRateList = emptyList()
        whenever(interactor.loadHistoricPrices(eq(asset), eq(HistoricalTimeSpan.YEAR))).thenReturn(
            Single.just(priceList)
        )

        // for some reason using @Test(expected = IllegalStateException::class) isn't working in this case,
        // so this is a workaround to test for exceptions
        try {
            localSubject.process(
                CoinViewIntent.LoadNewChartPeriod(HistoricalTimeSpan.YEAR)
            )
        } catch (e: Exception) {
            Assert.assertTrue(e is java.lang.IllegalStateException)
        }
    }

    @Test
    fun `load new chart period with no selected fiat should fail`() {
        val asset: CryptoAsset = mock {
            on { assetInfo }.thenReturn(mock())
        }

        val state = CoinViewState(
            asset = asset,
            selectedFiat = null,
            assetPrices = mock()
        )

        val localSubject = CoinViewModel(
            initialState = state,
            mainScheduler = Schedulers.io(),
            interactor = interactor,
            environmentConfig = environmentConfig,
            crashLogger = mock()
        )

        val priceList: HistoricalRateList = emptyList()
        whenever(interactor.loadHistoricPrices(eq(asset), eq(HistoricalTimeSpan.YEAR))).thenReturn(
            Single.just(priceList)
        )

        // for some reason using @Test(expected = IllegalStateException::class) isn't working in this case,
        // so this is a workaround to test for exceptions
        try {
            localSubject.process(
                CoinViewIntent.LoadNewChartPeriod(HistoricalTimeSpan.YEAR)
            )
        } catch (e: Exception) {
            Assert.assertTrue(e is java.lang.IllegalStateException)
        }
    }

    @Test
    fun `load recurring buys succeeds`() {
        val asset: CryptoAsset = mock {
            on { assetInfo }.thenReturn(mock())
        }
        whenever(interactor.loadRecurringBuys(asset.assetInfo)).thenReturn(Single.just(Pair(emptyList(), true)))

        val test = subject.state.test()
        subject.process(CoinViewIntent.LoadRecurringBuys(asset.assetInfo))

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.viewState == CoinViewViewState.LoadingRecurringBuys
        }.assertValueAt(2) {
            it.viewState is CoinViewViewState.ShowRecurringBuys
        }
    }

    @Test
    fun `load recurring buys fails`() {
        val asset: CryptoAsset = mock {
            on { assetInfo }.thenReturn(mock())
        }
        whenever(interactor.loadRecurringBuys(asset.assetInfo)).thenReturn(Single.error(Exception()))

        val test = subject.state.test()
        subject.process(CoinViewIntent.LoadRecurringBuys(asset.assetInfo))

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.viewState == CoinViewViewState.LoadingRecurringBuys
        }.assertValueAt(2) {
            it.error == CoinViewError.RecurringBuysLoadError
        }
    }

    @Test
    fun `load quick actions succeeds`() {
        val asset: CryptoAsset = mock {
            on { assetInfo }.thenReturn(mock())
        }
        whenever(interactor.loadQuickActions(any(), any())).thenReturn(
            Single.just(
                QuickActionData(mock(), mock(), mock())
            )
        )

        val test = subject.state.test()
        subject.process(CoinViewIntent.LoadQuickActions(mock(), mock()))

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.viewState == CoinViewViewState.LoadingQuickActions
        }.assertValueAt(2) {
            it.viewState is CoinViewViewState.QuickActionsLoaded
        }
    }

    @Test
    fun `load quick actions fails`() {
        val asset: CryptoAsset = mock {
            on { assetInfo }.thenReturn(mock())
        }
        whenever(interactor.loadQuickActions(any(), any())).thenReturn(Single.error(Exception()))

        val test = subject.state.test()
        subject.process(CoinViewIntent.LoadQuickActions(mock(), mock()))

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.viewState == CoinViewViewState.LoadingQuickActions
        }.assertValueAt(2) {
            it.error == CoinViewError.QuickActionsFailed
        }
    }

    @Test
    fun `when user can buy and there is no buy warning nothing should happen`() {
        whenever(interactor.userCanBuy()).thenReturn(Single.just(FeatureAccess.Granted()))

        val testState = subject.state.test()
        subject.process(CoinViewIntent.CheckBuyStatus)

        testState.assertValueAt(0) {
            it == defaultState
        }
    }

    @Test
    fun `when userCanBuy fails nothing should happen`() {
        whenever(interactor.userCanBuy()).thenReturn(Single.error(Throwable()))

        val testState = subject.state.test()
        subject.process(CoinViewIntent.CheckBuyStatus)

        testState.assertValueAt(0) {
            it == defaultState
        }
    }

    @Test
    fun `when userCanBuy and hasActionBuyWarning warning then state reflects update`() {
        whenever(interactor.userCanBuy())
            .thenReturn(
                Single.just(
                    FeatureAccess.Blocked(
                        BlockedReason.TooManyInFlightTransactions(3)
                    )
                )
            )

        val testState = subject.state.test()
        subject.process(CoinViewIntent.CheckBuyStatus)

        testState.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it == defaultState.copy(hasActionBuyWarning = true)
        }
    }

    @Test
    fun `when CheckScreenToOpen returns ShowAccountActionSheet then state is updated`() {
        val selectedAccount = mock<BlockchainAccount>()
        val assetDetailsItemNew: AssetDetailsItemNew.CryptoDetailsInfo = mock {
            on { account }.thenReturn(selectedAccount)
        }
        whenever(interactor.checkPreferencesAndNavigateTo(selectedAccount))
            .thenReturn(CoinViewViewState.ShowAccountActionSheet)

        val testState = subject.state.test()
        subject.process(CoinViewIntent.CheckScreenToOpen(assetDetailsItemNew))

        testState.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it == defaultState.copy(selectedCryptoAccount = assetDetailsItemNew)
        }.assertValueAt(2) {
            it == defaultState.copy(
                selectedCryptoAccount = assetDetailsItemNew,
                viewState = CoinViewViewState.ShowAccountActionSheet
            )
        }
    }

    @Test
    fun `whenCheckScreenToOpen returns ShowAccountExplainerSheet then state is updated`() {
        val selectedAccount = mock<BlockchainAccount>()
        val assetDetailsItemNew: AssetDetailsItemNew.CryptoDetailsInfo = mock {
            on { account }.thenReturn(selectedAccount)
        }
        whenever(interactor.checkPreferencesAndNavigateTo(selectedAccount))
            .thenReturn(CoinViewViewState.ShowAccountExplainerSheet)

        val testState = subject.state.test()
        subject.process(CoinViewIntent.CheckScreenToOpen(assetDetailsItemNew))

        testState.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it == defaultState.copy(selectedCryptoAccount = assetDetailsItemNew)
        }.assertValueAt(2) {
            it == defaultState.copy(
                selectedCryptoAccount = assetDetailsItemNew,
                viewState = CoinViewViewState.ShowAccountExplainerSheet
            )
        }
    }
}
