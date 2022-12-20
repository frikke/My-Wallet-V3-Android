package piuk.blockchain.android.ui.dashboard.coinview

import com.blockchain.android.testutils.rxInit
import com.blockchain.api.services.DetailedAssetInformation
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.StateAwareAction
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.core.price.HistoricalRateList
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.data.DataResource
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.walletmode.WalletMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Before
import org.junit.Rule
import org.junit.Test

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
            remoteLogger = mock(),
            walletModeService = mock {
                on { enabledWalletMode() }.thenReturn(WalletMode.UNIVERSAL)
            }
        )
    }

    @Test
    fun `load asset details should fire other intents succeeds`() {
        val ticker = "BTC"
        val assetInfo: AssetInfo = mock {
            on { networkTicker }.thenReturn(ticker)
        }
        val asset: CryptoAsset = mock {
            on { this.currency }.thenReturn(assetInfo)
        }

        val selectedFiat: FiatCurrency = FiatCurrency.Dollars
        whenever(interactor.loadAssetDetails(ticker)).thenReturn(Single.just(Pair(asset, selectedFiat)))
        whenever(interactor.loadAccountDetails(asset)).thenReturn(Single.error(Exception()))
        whenever(interactor.loadRecurringBuys(asset.currency)).thenReturn(Single.error(Exception()))
        whenever(interactor.loadAssetInformation(asset.currency)).thenReturn(Single.error(Exception()))
        whenever(interactor.isBuyOptionAvailable(asset)).thenReturn(Single.just(true))

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
        }.assertValueAt(4) {
            it.viewState == CoinViewViewState.LoadingAssetDetails
        }
    }

    @Test
    fun `load asset details should not fire other intents when fails fetching asset`() {
        val ticker = "BTC"

        whenever(interactor.loadAssetDetails(ticker)).thenReturn(Single.error(Exception()))

        val test = subject.state.test()
        subject.process(CoinViewIntent.LoadAsset(ticker))

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.error == CoinViewError.UnknownAsset
        }
    }

    @Test
    fun `load asset details should not fire other intents when fails with unknown asset`() {
        val ticker = "BTC"

        val selectedFiat: FiatCurrency = FiatCurrency.Dollars
        whenever(interactor.loadAssetDetails(ticker)).thenReturn(Single.just(Pair(null, selectedFiat)))

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
            on { currency }.thenReturn(mock())
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
            remoteLogger = mock(),
            walletModeService = mock {
                on { enabledWalletMode() }.thenReturn(WalletMode.UNIVERSAL)
            }
        )

        val assetInfo = AssetInformation.NonTradeable(prices = prices, isAddedToWatchlist = true)
        whenever(interactor.loadAccountDetails(asset)).thenReturn(Single.just(assetInfo))
        whenever(interactor.loadHistoricPrices(eq(asset), any()))
            .thenReturn(Observable.just(DataResource.Error(Exception())))

        val test = localSubject.state.test()

        localSubject.process(CoinViewIntent.LoadAccounts(asset))

        test.assertValueAt(0) {
            it == state
        }.assertValueAt(1) {
            it.viewState == CoinViewViewState.LoadingWallets
        }.assertValueAt(2) {
            it.viewState is CoinViewViewState.ShowNonTradeableAccount &&
                it.assetPrices == assetInfo.prices && it.isAddedToWatchlist
        }.assertValueAt(3) {
            it.viewState == CoinViewViewState.LoadingChart
        }.assertValueAt(4) {
            it.error == CoinViewError.ChartLoadError
        }
    }

    @Test
    fun `load accounts should show account details and load quick actions`() {
        val asset: CryptoAsset = mock {
            on { currency }.thenReturn(mock())
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
            remoteLogger = mock(),
            walletModeService = mock {
                on { enabledWalletMode() }.thenReturn(WalletMode.UNIVERSAL)
            }
        )

        val list: List<AssetDisplayInfo> = listOf(
            AssetDisplayInfo.BrokerageDisplayInfo(
                account = mock<CustodialTradingAccount>(),
                filter = AssetFilter.Trading,
                amount = mock(),
                pendingAmount = mock(),
                fiatValue = mock(),
                actions = mock(),
                interestRate = 0.0,
                stakingRate = 0.0
            )
        )
        val assetInfo = AssetInformation.AccountsInfo(
            prices = prices,
            accountsList = list,
            totalCryptoBalance = mock(),
            totalFiatBalance = mock(),
            isAddedToWatchlist = true
        )

        whenever(interactor.loadAccountDetails(asset)).thenReturn(Single.just(assetInfo))
        whenever(interactor.loadHistoricPrices(eq(asset), any()))
            .thenReturn(Observable.just(DataResource.Error(Exception())))
        whenever(interactor.loadQuickActions(any(), any(), eq(asset))).thenReturn(Single.error(Exception()))
        val test = localSubject.state.test()

        localSubject.process(CoinViewIntent.LoadAccounts(asset))

        test.assertValueAt(0) {
            it == state
        }.assertValueAt(1) {
            it.viewState == CoinViewViewState.LoadingWallets
        }.assertValueAt(2) {
            it.viewState is CoinViewViewState.ShowAccountInfo &&
                it.assetPrices == assetInfo.prices && it.isAddedToWatchlist
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
            on { currency }.thenReturn(mock())
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
            on { currency }.thenReturn(mock())
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
            remoteLogger = mock(),
            walletModeService = mock {
                on { enabledWalletMode() }.thenReturn(WalletMode.UNIVERSAL)
            }
        )

        val assetInfo = AssetInformation.AccountsInfo(
            prices = prices,
            accountsList = listOf(mock()),
            totalCryptoBalance = mock(),
            totalFiatBalance = mock(),
            isAddedToWatchlist = true
        )

        val priceList: HistoricalRateList = listOf(mock(), mock())
        whenever(interactor.loadHistoricPrices(eq(asset), any()))
            .thenReturn(Observable.just(DataResource.Data(priceList)))
        whenever(interactor.loadQuickActions(any(), any(), eq(asset))).thenReturn(Single.error(Exception()))
        val test = localSubject.state.test()

        localSubject.process(
            CoinViewIntent.UpdateAccountDetails(
                CoinViewViewState.ShowAccountInfo(
                    totalCryptoBalance = assetInfo.totalCryptoBalance,
                    totalFiatBalance = assetInfo.totalFiatBalance,
                    assetDetails = listOf(mock()),
                    isAddedToWatchlist = true
                ),
                assetInfo, asset, true
            )
        )

        test.assertValueAt(0) {
            it == state
        }.assertValueAt(1) {
            it.viewState is CoinViewViewState.ShowAccountInfo &&
                it.assetPrices == assetInfo.prices && it.isAddedToWatchlist
        }.assertValueAt(2) {
            it.viewState == CoinViewViewState.LoadingChart
        }.assertValueAt(3) {
            it.viewState is CoinViewViewState.ShowAssetInfo
        }
    }

    @Test
    fun `update account details without selected fiat should fail`() {
        val asset: CryptoAsset = mock {
            on { currency }.thenReturn(mock())
        }
        val prices: Prices24HrWithDelta = mock()

        val assetInfo = AssetInformation.AccountsInfo(
            prices = prices,
            accountsList = emptyList(),
            totalCryptoBalance = mock(),
            totalFiatBalance = mock(),
            isAddedToWatchlist = true
        )

        val test = subject.state.test()

        subject.process(
            CoinViewIntent.UpdateAccountDetails(
                CoinViewViewState.ShowAccountInfo(
                    totalCryptoBalance = assetInfo.totalCryptoBalance,
                    totalFiatBalance = assetInfo.totalFiatBalance,
                    assetDetails = emptyList(),
                    isAddedToWatchlist = true
                ),
                assetInfo, asset, true
            )
        )

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.viewState is CoinViewViewState.ShowAccountInfo &&
                it.assetPrices == assetInfo.prices && it.isAddedToWatchlist
        }.assertValueAt(2) {
            it.error == CoinViewError.MissingSelectedFiat
        }
    }

    @Test
    fun `load asset chart succeeds when list is populated should send UI event`() {
        val asset: CryptoAsset = mock {
            on { currency }.thenReturn(mock())
        }
        val prices: Prices24HrWithDelta = mock()
        val priceList: HistoricalRateList = listOf(mock(), mock(), mock())
        whenever(interactor.loadHistoricPrices(eq(asset), any()))
            .thenReturn(Observable.just(DataResource.Data(priceList)))

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
    fun `load asset chart succeeds when list is not populated should send error event`() {
        val asset: CryptoAsset = mock {
            on { currency }.thenReturn(mock())
        }
        val prices: Prices24HrWithDelta = mock()
        val priceList: HistoricalRateList = emptyList()
        whenever(interactor.loadHistoricPrices(eq(asset), any()))
            .thenReturn(Observable.just(DataResource.Data(priceList)))

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
    fun `load asset chart fails`() {
        val asset: CryptoAsset = mock {
            on { currency }.thenReturn(mock())
        }
        val prices: Prices24HrWithDelta = mock()
        whenever(interactor.loadHistoricPrices(eq(asset), any()))
            .thenReturn(Observable.just(DataResource.Error(Exception())))

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
            on { currency }.thenReturn(mock())
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
            remoteLogger = mock(),
            walletModeService = mock {
                on { enabledWalletMode() }.thenReturn(WalletMode.UNIVERSAL)
            }
        )

        val priceList: HistoricalRateList = listOf(mock(), mock(), mock())
        whenever(interactor.loadHistoricPrices(eq(asset), eq(HistoricalTimeSpan.YEAR)))
            .thenReturn(Observable.just(DataResource.Data(priceList)))

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
    fun `load new chart period with asset succeeds but returns empty list`() {
        val asset: CryptoAsset = mock {
            on { currency }.thenReturn(mock())
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
            remoteLogger = mock(),
            walletModeService = mock {
                on { enabledWalletMode() }.thenReturn(WalletMode.UNIVERSAL)
            }
        )

        val priceList: HistoricalRateList = emptyList()
        whenever(interactor.loadHistoricPrices(eq(asset), eq(HistoricalTimeSpan.YEAR)))
            .thenReturn(Observable.just(DataResource.Data(priceList)))

        val test = localSubject.state.test()

        localSubject.process(
            CoinViewIntent.LoadNewChartPeriod(HistoricalTimeSpan.YEAR)
        )

        test.assertValueAt(0) {
            it == state
        }.assertValueAt(1) {
            it.viewState is CoinViewViewState.LoadingChart
        }.assertValueAt(2) {
            it.error == CoinViewError.ChartLoadError
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
            on { currency }.thenReturn(mock())
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
            remoteLogger = mock(),
            walletModeService = mock {
                on { enabledWalletMode() }.thenReturn(WalletMode.UNIVERSAL)
            }
        )

        val priceList: HistoricalRateList = emptyList()
        whenever(interactor.loadHistoricPrices(eq(asset), eq(HistoricalTimeSpan.YEAR)))
            .thenReturn(Observable.just(DataResource.Data(priceList)))

        val test = localSubject.state.test()

        localSubject.process(
            CoinViewIntent.LoadNewChartPeriod(HistoricalTimeSpan.YEAR)
        )

        test.assertValueAt(0) {
            it == state
        }.assertValueAt(1) {
            it.viewState == CoinViewViewState.LoadingChart
        }.assertValueAt(2) {
            it.error == CoinViewError.MissingAssetPrices
        }
    }

    @Test
    fun `load new chart period with no selected fiat should fail`() {
        val asset: CryptoAsset = mock {
            on { currency }.thenReturn(mock())
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
            remoteLogger = mock(),
            walletModeService = mock {
                on { enabledWalletMode() }.thenReturn(WalletMode.UNIVERSAL)
            }
        )

        val priceList: HistoricalRateList = emptyList()
        whenever(interactor.loadHistoricPrices(eq(asset), eq(HistoricalTimeSpan.YEAR)))
            .thenReturn(Observable.just(DataResource.Data(priceList)))

        val test = localSubject.state.test()

        localSubject.process(
            CoinViewIntent.LoadNewChartPeriod(HistoricalTimeSpan.YEAR)
        )

        test.assertValueAt(0) {
            it == state
        }
            .assertValueAt(1) {
                it.viewState == CoinViewViewState.LoadingChart
            }.assertValueAt(2) {
                it.error == CoinViewError.MissingSelectedFiat
            }
    }

    @Test
    fun `load recurring buys succeeds`() {
        val asset: CryptoAsset = mock {
            on { currency }.thenReturn(mock())
        }
        whenever(interactor.loadRecurringBuys(asset.currency)).thenReturn(Single.just(Pair(emptyList(), true)))

        val test = subject.state.test()
        subject.process(CoinViewIntent.LoadRecurringBuys(asset.currency))

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
            on { currency }.thenReturn(mock())
        }
        whenever(interactor.loadRecurringBuys(asset.currency)).thenReturn(Single.error(Exception()))

        val test = subject.state.test()
        subject.process(CoinViewIntent.LoadRecurringBuys(asset.currency))

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
            on { currency }.thenReturn(mock())
        }
        whenever(interactor.loadQuickActions(any(), any(), eq(asset))).thenReturn(
            Single.just(
                QuickActionData(mock(), mock(), mock(), mock())
            )
        )

        val test = subject.state.test()
        subject.process(CoinViewIntent.LoadQuickActions(mock(), mock(), asset))

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
            on { currency }.thenReturn(mock())
        }
        whenever(interactor.loadQuickActions(any(), any(), eq(asset))).thenReturn(Single.error(Exception()))

        val test = subject.state.test()
        subject.process(CoinViewIntent.LoadQuickActions(mock(), mock(), asset))

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.viewState == CoinViewViewState.LoadingQuickActions
        }.assertValueAt(2) {
            it.error == CoinViewError.QuickActionsFailed
        }
    }

    @Test
    fun `toggle watchlist should add successfully`() {
        val asset: CryptoAsset = mock {
            on { currency }.thenReturn(mock())
        }

        val state = CoinViewState(
            asset = asset,
            selectedFiat = FiatCurrency.Dollars,
            assetPrices = mock(),
            isAddedToWatchlist = false
        )

        val localSubject = CoinViewModel(
            initialState = state,
            mainScheduler = Schedulers.io(),
            interactor = interactor,
            environmentConfig = environmentConfig,
            remoteLogger = mock(),
            walletModeService = mock {
                on { enabledWalletMode() }.thenReturn(WalletMode.UNIVERSAL)
            }
        )

        whenever(interactor.addToWatchlist(asset.currency)).thenReturn(Single.just(mock()))

        val test = localSubject.state.test()
        localSubject.process(CoinViewIntent.ToggleWatchlist)
        test.assertValueAt(0) {
            it == state
        }.assertValueAt(1) {
            it.isAddedToWatchlist
        }
    }

    @Test
    fun `when buy is granted and asset is supported for trading, canBuy should be true`() {
        whenever(interactor.isBuyOptionAvailable(any()))
            .thenReturn(
                Single.just(true)
            )

        val asset: CryptoAsset = mock {
            on { currency }.thenReturn(mock())
        }

        val state = CoinViewState(
            asset = asset,
            canBuy = false
        )

        val localSubject = CoinViewModel(
            initialState = state,
            mainScheduler = Schedulers.io(),
            interactor = interactor,
            environmentConfig = environmentConfig,
            remoteLogger = mock(),
            walletModeService = mock {
                on { enabledWalletMode() }.thenReturn(WalletMode.UNIVERSAL)
            }
        )

        val testState = localSubject.state.test()
        localSubject.process(CoinViewIntent.CheckBuyStatus)

        testState.assertValueCount(2)
        testState.assertValueAt(1) {
            it.canBuy
        }
    }

    @Test
    fun `when buy is granted and asset is not supported for trading, canBuy should be false`() {
        whenever(interactor.isBuyOptionAvailable(any()))
            .thenReturn(
                Single.just(false)
            )

        val asset: CryptoAsset = mock {
            on { currency }.thenReturn(mock())
        }

        val state = CoinViewState(
            asset = asset,
            canBuy = true
        )

        val localSubject = CoinViewModel(
            initialState = state,
            mainScheduler = Schedulers.io(),
            interactor = interactor,
            environmentConfig = environmentConfig,
            remoteLogger = mock(),
            walletModeService = mock {
                on { enabledWalletMode() }.thenReturn(WalletMode.UNIVERSAL)
            }
        )

        val testState = localSubject.state.test()
        localSubject.process(CoinViewIntent.CheckBuyStatus)

        testState.assertValueCount(2)
        testState.assertValueAt(1) {
            it.canBuy.not()
        }
    }

    @Test
    fun `toggle watchlist should remove failure`() {
        val asset: CryptoAsset = mock {
            on { currency }.thenReturn(mock())
        }

        val state = CoinViewState(
            asset = asset,
            selectedFiat = FiatCurrency.Dollars,
            assetPrices = mock(),
            isAddedToWatchlist = true
        )

        val localSubject = CoinViewModel(
            initialState = state,
            mainScheduler = Schedulers.io(),
            interactor = interactor,
            environmentConfig = environmentConfig,
            remoteLogger = mock(),
            walletModeService = mock {
                on { enabledWalletMode() }.thenReturn(WalletMode.UNIVERSAL)
            }
        )

        whenever(interactor.removeFromWatchlist(asset.currency)).thenReturn(Completable.error(Exception()))

        val test = localSubject.state.test()
        localSubject.process(CoinViewIntent.ToggleWatchlist)
        test.assertValueAt(0) {
            it == state
        }.assertValueAt(1) {
            it.error == CoinViewError.WatchlistUpdateFailed
        }
    }

    @Test
    fun `toggle watchlist should remove`() {
        val asset: CryptoAsset = mock {
            on { currency }.thenReturn(mock())
        }

        val state = CoinViewState(
            asset = asset,
            selectedFiat = FiatCurrency.Dollars,
            assetPrices = mock(),
            isAddedToWatchlist = true
        )

        val localSubject = CoinViewModel(
            initialState = state,
            mainScheduler = Schedulers.io(),
            interactor = interactor,
            environmentConfig = environmentConfig,
            remoteLogger = mock(),
            walletModeService = mock {
                on { enabledWalletMode() }.thenReturn(WalletMode.UNIVERSAL)
            }
        )

        whenever(interactor.removeFromWatchlist(asset.currency)).thenReturn(Completable.complete())

        val test = localSubject.state.test()
        localSubject.process(CoinViewIntent.ToggleWatchlist)
        test.assertValueAt(0) {
            it == state
        }.assertValueAt(1) {
            !it.isAddedToWatchlist
        }
    }

    @Test
    fun `when CheckScreenToOpen returns ShowAccountActionSheet then state is updated`() {
        val asset: CryptoAsset = mock {
            on { currency }.thenReturn(mock())
        }
        val fiatCurrency: FiatCurrency = mock()

        val selectedAccount = mock<BlockchainAccount>()
        val assetDetailsItem: AssetDetailsItem.CryptoDetailsInfo = mock {
            on { account }.thenReturn(selectedAccount)
        }
        val actions = setOf<StateAwareAction>().toTypedArray()
        whenever(interactor.getAccountActions(asset, selectedAccount))
            .thenReturn(Single.just(CoinViewViewState.ShowAccountActionSheet(actions)))

        val testState = subject.state.test()
        subject.process(CoinViewIntent.AssetLoaded(asset, fiatCurrency))
        subject.process(CoinViewIntent.CheckScreenToOpen(assetDetailsItem))

        testState.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it == defaultState.copy(asset = asset, selectedFiat = fiatCurrency)
        }.assertValueAt(2) {
            it.selectedCryptoAccount == assetDetailsItem
        }.assertValueAt(3) {
            it.viewState is CoinViewViewState.ShowAccountActionSheet &&
                (it.viewState as CoinViewViewState.ShowAccountActionSheet).actions.contentEquals(actions)
        }
    }

    @Test
    fun `toggle watchlist add should fail`() {
        val asset: CryptoAsset = mock {
            on { currency }.thenReturn(mock())
        }

        val state = CoinViewState(
            asset = asset,
            selectedFiat = FiatCurrency.Dollars,
            assetPrices = mock(),
            isAddedToWatchlist = false
        )

        val localSubject = CoinViewModel(
            initialState = state,
            mainScheduler = Schedulers.io(),
            interactor = interactor,
            environmentConfig = environmentConfig,
            remoteLogger = mock(),
            walletModeService = mock {
                on { enabledWalletMode() }.thenReturn(WalletMode.UNIVERSAL)
            }
        )

        whenever(interactor.addToWatchlist(asset.currency)).thenReturn(Single.error(Exception()))

        val test = localSubject.state.test()
        localSubject.process(CoinViewIntent.ToggleWatchlist)
        test.assertValueAt(0) {
            it == state
        }.assertValueAt(1) {
            it.error == CoinViewError.WatchlistUpdateFailed
        }
    }

    @Test
    fun `when CheckScreenToOpen returns ShowAccountExplainerSheet then state is updated`() {
        val asset: CryptoAsset = mock {
            on { currency }.thenReturn(mock())
        }
        val fiatCurrency: FiatCurrency = mock()

        val selectedAccount = mock<BlockchainAccount>()
        val assetDetailsItem: AssetDetailsItem.CryptoDetailsInfo = mock {
            on { account }.thenReturn(selectedAccount)
        }
        val actions = setOf<StateAwareAction>().toTypedArray()
        whenever(interactor.getAccountActions(asset, selectedAccount))
            .thenReturn(Single.just(CoinViewViewState.ShowAccountExplainerSheet(actions)))

        val testState = subject.state.test()

        subject.process(CoinViewIntent.AssetLoaded(asset, fiatCurrency))
        subject.process(CoinViewIntent.CheckScreenToOpen(assetDetailsItem))

        testState.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it == defaultState.copy(asset = asset, selectedFiat = fiatCurrency)
        }.assertValueAt(2) {
            it.selectedCryptoAccount == assetDetailsItem
        }.assertValueAt(3) {
            it.viewState is CoinViewViewState.ShowAccountExplainerSheet &&
                (it.viewState as CoinViewViewState.ShowAccountExplainerSheet).actions.contentEquals(actions)
        }
    }

    @Test
    fun `load asset info details succeding should fire state update`() {
        val asset: CryptoAsset = mock {
            on { currency }.thenReturn(mock())
        }
        val assetDetails = DetailedAssetInformation("", "", "")
        whenever(interactor.loadAssetInformation(asset.currency)).thenReturn(Single.just(assetDetails))

        val test = subject.state.test()

        subject.process(CoinViewIntent.LoadAssetDetails(asset.currency))

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.viewState == CoinViewViewState.LoadingAssetDetails
        }
            .assertValueAt(2) {
                it.viewState is CoinViewViewState.ShowAssetDetails &&
                    (it.viewState as CoinViewViewState.ShowAssetDetails).details == assetDetails
            }
    }

    @Test
    fun `load asset info details failing should fire error update`() {
        val asset: CryptoAsset = mock {
            on { currency }.thenReturn(mock())
        }
        whenever(interactor.loadAssetInformation(asset.currency)).thenReturn(Single.error(Exception()))

        val test = subject.state.test()

        subject.process(CoinViewIntent.LoadAssetDetails(asset.currency))

        test.assertValueAt(0) {
            it == defaultState
        }.assertValueAt(1) {
            it.viewState == CoinViewViewState.LoadingAssetDetails
        }.assertValueAt(2) {
            it.error == CoinViewError.AssetDetailsLoadError
        }
    }
}
