package piuk.blockchain.android.ui.coinview.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.blockchain.charts.ChartEntry
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CryptoAsset
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.core.asset.domain.AssetService
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.price.HistoricalRate
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.core.recurringbuy.domain.RecurringBuyService
import com.blockchain.core.watchlist.domain.WatchlistService
import com.blockchain.data.DataResource
import com.blockchain.image.LogoValue
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.news.NewsService
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.testutils.CoroutineTestRule
import com.blockchain.wallet.DefaultLabels
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.coinview.domain.GetAccountActionsUseCase
import piuk.blockchain.android.ui.coinview.domain.GetAssetPriceUseCase
import piuk.blockchain.android.ui.coinview.domain.LoadAssetAccountsUseCase
import piuk.blockchain.android.ui.coinview.domain.LoadAssetRecurringBuysUseCase
import piuk.blockchain.android.ui.coinview.domain.LoadQuickActionsUseCase
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccount
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAccounts
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetDetail
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetPrice
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetPriceHistory
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewAssetTotalBalance
import piuk.blockchain.android.ui.coinview.domain.model.CoinviewQuickActions

class CoinviewViewModelTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @get:Rule
    var coroutineTestRule = CoroutineTestRule()

    private val walletModeService: WalletModeService = mockk()
    private val coincore: Coincore = mockk()
    private val assetCatalogue: AssetCatalogue = mockk()
    private val currencyPrefs: CurrencyPrefs = mockk()
    private val labels: DefaultLabels = mockk()
    private val getAssetPriceUseCase: GetAssetPriceUseCase = mockk()
    private val watchlistService: WatchlistService = mockk()
    private val loadAssetAccountsUseCase: LoadAssetAccountsUseCase = mockk()
    private val getAccountActionsUseCase: GetAccountActionsUseCase = mockk()
    private val loadAssetRecurringBuysUseCase: LoadAssetRecurringBuysUseCase = mockk()
    private val loadQuickActionsUseCase: LoadQuickActionsUseCase = mockk()
    private val assetService: AssetService = mockk()
    private val custodialWalletManager: CustodialWalletManager = mockk()

    private lateinit var viewModel: CoinviewViewModel

    private val networkTicker = "SOMETICKER"
    private val recurringBuyId = "recurringBuyId"
    private val coinviewArgs: CoinviewArgs = CoinviewArgs(networkTicker, recurringBuyId)

    private val cryptoAsset: CryptoAsset = mockk()
    private val currency: AssetInfo = mockk()
    private val logo = "logo"
    private val color = "color"

    private val fiatCurrency: FiatCurrency = mockk()
    private val fiatCurrencySymbol: String = "fiatCurrencySymbol"

    private val money: Money = mockk()
    private val balanceFormatted = "balanceFormatted"
    private val percentChange = ValueChange.fromValue(1.1)
    private val timeSpan = HistoricalTimeSpan.DAY
    private val totalBalance: CoinviewAssetTotalBalance = mockk()
    private val totalCryptoBalance: Map<AssetFilter, Money> = mapOf(AssetFilter.All to money)
    private val totalFiatBalance: Money = money

    private val coinviewAccount: CoinviewAccount.Custodial.Trading = mockk()
    private val blockchainAccount: CryptoAccount = mockk()
    private val coinviewCustodialAccounts = CoinviewAccounts.Custodial(listOf(coinviewAccount))

    private val coinviewAssetPrice: CoinviewAssetPrice = mockk()

    private val recurringBuyService: RecurringBuyService = mockk()

    private val newsService: NewsService = mockk()
    private val kycService: KycService = mockk()

    private val tradingWalletLabel = "TradingWalletLabel"

    @Before
    fun setUp() {
        every { walletModeService.walletMode } returns flowOf(WalletMode.CUSTODIAL)

        viewModel = CoinviewViewModel(
            walletModeService = walletModeService,
            coincore = coincore,
            assetCatalogue = assetCatalogue,
            currencyPrefs = currencyPrefs,
            labels = labels,
            getAssetPriceUseCase = getAssetPriceUseCase,
            watchlistService = watchlistService,
            loadAssetAccountsUseCase = loadAssetAccountsUseCase,
            getAccountActionsUseCase = getAccountActionsUseCase,
            loadAssetRecurringBuysUseCase = loadAssetRecurringBuysUseCase,
            loadQuickActionsUseCase = loadQuickActionsUseCase,
            assetService = assetService,
            custodialWalletManager = custodialWalletManager,
            recurringBuyService = recurringBuyService,
            newsService = newsService,
            kycService = kycService
        )

        every { cryptoAsset.currency } returns currency
        every { cryptoAsset.currency.name } returns networkTicker
        every { cryptoAsset.currency.displayTicker } returns networkTicker
        every { cryptoAsset.currency.logo } returns logo
        every { cryptoAsset.currency.colour } returns color
        every { cryptoAsset.currency.networkTicker } returns networkTicker
        every { coincore[coinviewArgs.networkTicker] } returns cryptoAsset

        every { totalBalance.totalCryptoBalance } returns totalCryptoBalance
        every { totalBalance.totalFiatBalance } returns totalFiatBalance
        every { money.toStringWithSymbol() } returns balanceFormatted

        every { coinviewAccount.account } returns blockchainAccount
        every { coinviewAccount.isEnabled } returns true
        every { coinviewAccount.fiatBalance } returns DataResource.Data(money)
        every { coinviewAccount.cryptoBalance } returns DataResource.Data(money)

        every { fiatCurrency.symbol } returns fiatCurrencySymbol
        every { currencyPrefs.selectedFiatCurrency } returns fiatCurrency

        every { coinviewAssetPrice.price } returns money
        every { coinviewAssetPrice.changeDifference } returns money
        every { coinviewAssetPrice.percentChange } returns percentChange.value
        every { coinviewAssetPrice.timeSpan } returns HistoricalTimeSpan.DAY

        every { labels.getDefaultTradingWalletLabel() } returns tradingWalletLabel
    }

    // asset
    @Test
    fun `GIVEN asset not found, WHEN viewCreated is called, THEN asset state should be Error`() = runTest {
        every { coincore[coinviewArgs.networkTicker] } returns null

        viewModel.viewState.test {
            viewModel.viewCreated(coinviewArgs)
            awaitItem().run {
                assertEquals(DataResource.Error(Exception()), asset)
            }
        }
    }

    @Test
    fun `GIVEN asset found, WHEN viewCreated is called, THEN asset state should be Data`() = runTest {
        viewModel.viewState.test {
            expectMostRecentItem()
            viewModel.viewCreated(coinviewArgs)
            awaitItem().run {
                assertEquals(
                    DataResource.Data(CoinviewAssetState(asset = cryptoAsset.currency, l1Network = null)),
                    asset
                )
            }
        }
    }

    // tradeable/non
    @Test
    fun `GIVEN asset non tradeable, THEN tradeable state should be NonTradeable`() = runTest {
        val dataResource = MutableSharedFlow<DataResource<CoinviewAssetDetail>>()
        coEvery { loadAssetAccountsUseCase(cryptoAsset) } returns dataResource

        viewModel.viewState.test {
            viewModel.viewCreated(coinviewArgs)
            expectMostRecentItem()

            viewModel.onIntent(CoinviewIntent.LoadAccountsData)
            dataResource.emit(DataResource.Data(CoinviewAssetDetail.NonTradeable(totalBalance)))
            awaitItem().run {
                assertEquals(CoinviewAssetTradeableState.NonTradeable(networkTicker, networkTicker), tradeable)
            }
        }
    }

    @Test
    fun `GIVEN asset tradeable, THEN tradeable state should be Tradeable`() = runTest {
        val dataResource = MutableSharedFlow<DataResource<CoinviewAssetDetail>>()
        coEvery { loadAssetAccountsUseCase(cryptoAsset) } returns dataResource
        coEvery { loadAssetRecurringBuysUseCase(any(), any()) } returns MutableSharedFlow()
        val dataResourceQuickActionUnused = MutableSharedFlow<DataResource<CoinviewQuickActions>>()
        coEvery { loadQuickActionsUseCase(any(), any(), any()) } returns dataResourceQuickActionUnused

        viewModel.viewState.test {
            viewModel.viewCreated(coinviewArgs)
            expectMostRecentItem()

            viewModel.onIntent(CoinviewIntent.LoadAccountsData)
            dataResource.emit(DataResource.Data(CoinviewAssetDetail.Tradeable(coinviewCustodialAccounts, totalBalance)))
            awaitItem().run {
                assertEquals(CoinviewAssetTradeableState.Tradeable, tradeable)
            }
        }
    }

    // price
    @Test
    fun `GIVEN valid prices, WHEN LoadPriceData is called, THEN assetPrice state should be Data`() = runTest {
        val dataResource = MutableSharedFlow<DataResource<CoinviewAssetPriceHistory>>()
        coEvery { getAssetPriceUseCase(cryptoAsset, timeSpan, fiatCurrency) } returns dataResource

        viewModel.viewState.test {
            viewModel.viewCreated(coinviewArgs)
            expectMostRecentItem()

            viewModel.onIntent(CoinviewIntent.LoadPriceData)
            dataResource.emit(
                DataResource.Data(CoinviewAssetPriceHistory(listOf(HistoricalRate(1L, 1.1)), coinviewAssetPrice))
            )
            awaitItem().run {
                val expected = DataResource.Data(
                    CoinviewPriceState(
                        assetName = networkTicker,
                        assetLogo = logo,
                        fiatSymbol = fiatCurrencySymbol,
                        price = balanceFormatted,
                        priceChange = balanceFormatted,
                        valueChange = percentChange,
                        intervalText = PriceIntervalText.TimeSpanText(HistoricalTimeSpan.DAY),
                        chartData = CoinviewPriceState.CoinviewChartState.Data(listOf(ChartEntry(1.0F, 1.1F))),
                        selectedTimeSpan = timeSpan
                    )
                )
                assertEquals(expected, assetPrice)
            }
        }
    }

    @Test
    fun `GIVEN empty prices, WHEN LoadPriceData is called, THEN assetPrice state should be Error`() = runTest {
        val dataResource = MutableSharedFlow<DataResource<CoinviewAssetPriceHistory>>()
        coEvery { getAssetPriceUseCase(cryptoAsset, timeSpan, fiatCurrency) } returns dataResource

        viewModel.viewState.test {
            viewModel.viewCreated(coinviewArgs)
            expectMostRecentItem()

            viewModel.onIntent(CoinviewIntent.LoadPriceData)
            dataResource.emit(DataResource.Data(CoinviewAssetPriceHistory(listOf(), coinviewAssetPrice)))
            awaitItem().run {
                assert(assetPrice is DataResource.Error)
            }
        }
    }

    // watchlist
    @Test
    fun `GIVEN asset in watchlist, WHEN LoadWatchlistData is called, sTHEN } state should be Data true`() = runTest {
        val dataResource = MutableSharedFlow<DataResource<Boolean>>()
        every { watchlistService.isAssetInWatchlist(cryptoAsset.currency) } returns dataResource

        viewModel.viewState.test {
            viewModel.viewCreated(coinviewArgs)
            expectMostRecentItem()

            viewModel.onIntent(CoinviewIntent.LoadWatchlistData)
            dataResource.emit(DataResource.Data(true))
            awaitItem().run {
                assertEquals(DataResource.Data(true), watchlist)
            }
        }
    }

    @Test
    fun `GIVEN asset not in watchlist, WHEN LoadWatchlistData is called, sTHEN } state should be Data false`() =
        runTest {
            val dataResource = MutableSharedFlow<DataResource<Boolean>>()
            every { watchlistService.isAssetInWatchlist(cryptoAsset.currency) } returns dataResource

            viewModel.viewState.test {
                viewModel.viewCreated(coinviewArgs)
                expectMostRecentItem()

                viewModel.onIntent(CoinviewIntent.LoadWatchlistData)
                dataResource.emit(DataResource.Data(false))
                awaitItem().run {
                    assertEquals(DataResource.Data(false), watchlist)
                }
            }
        }

    // accounts
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `GIVEN valid accounts, WHEN LoadAccountsData is called, THEN totalBalance state should be Data`() =
        runTest {
            val dataResource = MutableSharedFlow<DataResource<CoinviewAssetDetail>>()
            coEvery { loadAssetAccountsUseCase(cryptoAsset) } returns dataResource
            coEvery {
                loadAssetRecurringBuysUseCase(
                    any(),
                    any()
                )
            } returns MutableSharedFlow()
            val dataResourceQuickActionUnused = MutableSharedFlow<DataResource<CoinviewQuickActions>>()
            coEvery { loadQuickActionsUseCase(any(), any(), any()) } returns dataResourceQuickActionUnused

            viewModel.viewState.test {
                viewModel.viewCreated(coinviewArgs)
                expectMostRecentItem()

                viewModel.onIntent(CoinviewIntent.LoadAccountsData)
                dataResource.emit(
                    DataResource.Data(CoinviewAssetDetail.Tradeable(coinviewCustodialAccounts, totalBalance))
                )
                awaitItem().run {
                    val expected = DataResource.Data(
                        CoinviewAccountsState(
                            assetName = networkTicker,
                            totalBalance = balanceFormatted,
                            accounts = listOf(
                                CoinviewAccountsState.CoinviewAccountState.Available(
                                    cvAccount = coinviewAccount,
                                    title = "SOMETICKER",
                                    subtitle = null,
                                    cryptoBalance = balanceFormatted,
                                    fiatBalance = balanceFormatted,
                                    logo = LogoValue.SingleIcon(logo),
                                    assetColor = color
                                )
                            )
                        )
                    )

                    assertEquals(expected, accounts)
                }
            }
        }
}
