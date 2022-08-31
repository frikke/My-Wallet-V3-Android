package piuk.blockchain.android.ui.transactionflow.accountsorting

import com.blockchain.api.services.AssetTag
import com.blockchain.coincore.Asset
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.core.custodial.domain.model.TradingAccountBalance
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.core.price.model.AssetPriceRecord
import com.blockchain.core.user.Watchlist
import com.blockchain.core.user.WatchlistDataManager
import com.blockchain.featureflag.FeatureFlag
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.math.BigDecimal
import java.math.BigInteger
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.transfer.DefaultAccountsSorting
import piuk.blockchain.android.ui.transfer.SwapTargetAccountsSorting

class SwapTargetAccountSortingTest {

    private val assetListOrderingFF: FeatureFlag = mock()
    private val defaultAccountsSorting: DefaultAccountsSorting = mock()
    private val coincore: Coincore = mock()
    private val exchangeRatesDataManager: ExchangeRatesDataManager = mock()
    private val watchlistDataManager: WatchlistDataManager = mock()

    private lateinit var subject: SwapTargetAccountsSorting

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

    private val ethAsset: Asset = mock {
        on { getPricesWith24hDeltaLegacy() }.thenReturn(
            Single.just(
                Prices24HrWithDelta(
                    delta24h = 1.0,
                    previousRate = ExchangeRate(
                        BigDecimal.ONE, ethMock, FiatCurrency.Dollars
                    ),
                    currentRate = ExchangeRate(
                        BigDecimal.ONE, ethMock, FiatCurrency.Dollars
                    )
                )
            )
        )
    }

    private val btcAsset: Asset = mock {
        on { getPricesWith24hDeltaLegacy() }.thenReturn(
            Single.just(
                Prices24HrWithDelta(
                    delta24h = 1.0,
                    previousRate = ExchangeRate(
                        BigDecimal.ONE, btcMock, FiatCurrency.Dollars
                    ),
                    currentRate = ExchangeRate(
                        BigDecimal.ONE, btcMock, FiatCurrency.Dollars
                    )
                )
            )
        )
    }

    private val xlmAsset: Asset = mock {
        on { getPricesWith24hDeltaLegacy() }.thenReturn(
            Single.just(
                Prices24HrWithDelta(
                    delta24h = 1.0,
                    previousRate = ExchangeRate(
                        BigDecimal.ONE, xlmMock, FiatCurrency.Dollars
                    ),
                    currentRate = ExchangeRate(
                        BigDecimal.ONE, xlmMock, FiatCurrency.Dollars
                    )
                )
            )
        )
    }

    companion object {
        private const val ONE_ETH = 1000000000000000000L
        private const val ONE_BTC = 100000000L
        private const val ONE_XLM = 10000000L
    }

    @Before
    fun setup() {
        subject = SwapTargetAccountsSorting(
            assetListOrderingFF = assetListOrderingFF,
            dashboardAccountsSorter = defaultAccountsSorting,
            coincore = coincore,
            exchangeRatesDataManager = exchangeRatesDataManager,
            watchlistDataManager = watchlistDataManager
        )
    }

    @Test
    fun `when flag is off then dashboard sorter is invoked`() {
        whenever(assetListOrderingFF.enabled).thenReturn(Single.just(false))
        whenever(defaultAccountsSorting.sorter()).thenReturn(mock())

        val list = listOf<SingleAccount>()
        subject.sorter().invoke(list).test()

        verify(defaultAccountsSorting).sorter()
        verifyNoMoreInteractions(defaultAccountsSorting)
    }

    @Test
    fun `when ff is on, all accounts 0 balance, and all same trading volume, then watchlist order is maintained`() {
        whenever(assetListOrderingFF.enabled).thenReturn(Single.just(true))

        whenever(coincore[ethMock]).thenReturn(ethAsset)
        whenever(coincore[btcMock]).thenReturn(btcAsset)
        whenever(coincore[xlmMock]).thenReturn(xlmAsset)

        val ethCustodialAccount = generateCustodialAccount(ethMock, 0L)
        val btcCustodialAccount = generateCustodialAccount(btcMock, 0L)
        val xlmCustodialAccount = generateCustodialAccount(xlmMock, 0L)

        setTradingVolume(btcMock, 1.0)
        setTradingVolume(ethMock, 1.0)
        setTradingVolume(xlmMock, 1.0)

        whenever(watchlistDataManager.getWatchlist()).thenReturn(
            Single.just(
                Watchlist(
                    assetMap = mapOf(
                        ethMock to listOf(AssetTag.Favourite),
                        xlmMock to listOf(AssetTag.Favourite),
                        btcMock to listOf(AssetTag.Favourite)
                    )
                )
            )
        )

        val list = listOf(
            xlmCustodialAccount,
            btcCustodialAccount,
            ethCustodialAccount
        )

        val result = subject.sorter().invoke(list).test()

        result.assertValue {
            it[0] == ethCustodialAccount &&
                it[1] == xlmCustodialAccount &&
                it[2] == btcCustodialAccount
        }
    }

    @Test
    fun `when ff is on, btc, eth in watchlist, all accounts 0 balance, and all same trading volume, then order is btc, eth, xlm`() {
        whenever(assetListOrderingFF.enabled).thenReturn(Single.just(true))

        whenever(coincore[ethMock]).thenReturn(ethAsset)
        whenever(coincore[btcMock]).thenReturn(btcAsset)
        whenever(coincore[xlmMock]).thenReturn(xlmAsset)

        val ethCustodialAccount = generateCustodialAccount(ethMock, 0L)
        val btcCustodialAccount = generateCustodialAccount(btcMock, 0L)
        val xlmCustodialAccount = generateCustodialAccount(xlmMock, 0L)

        setTradingVolume(btcMock, 1.0)
        setTradingVolume(ethMock, 1.0)
        setTradingVolume(xlmMock, 1.0)

        whenever(watchlistDataManager.getWatchlist()).thenReturn(
            Single.just(
                Watchlist(
                    assetMap = mapOf(
                        btcMock to listOf(AssetTag.Favourite),
                        ethMock to listOf(AssetTag.Favourite)
                    )
                )
            )
        )

        val list = listOf(
            xlmCustodialAccount,
            btcCustodialAccount,
            ethCustodialAccount
        )

        val result = subject.sorter().invoke(list).test()

        result.assertValue {
            it[0] == btcCustodialAccount &&
                it[1] == ethCustodialAccount &&
                it[2] == xlmCustodialAccount
        }
    }

    @Test
    fun `when ff is on, btc, eth in watchlist, eth more balance than btc, and all same trading volume, then order is eth, btc, xlm`() {
        whenever(assetListOrderingFF.enabled).thenReturn(Single.just(true))

        whenever(coincore[ethMock]).thenReturn(ethAsset)
        whenever(coincore[btcMock]).thenReturn(btcAsset)
        whenever(coincore[xlmMock]).thenReturn(xlmAsset)

        val ethCustodialAccount = generateCustodialAccount(ethMock, ONE_ETH)
        val btcCustodialAccount = generateCustodialAccount(btcMock, 0L)
        val xlmCustodialAccount = generateCustodialAccount(xlmMock, ONE_XLM)

        setTradingVolume(btcMock, 1.0)
        setTradingVolume(ethMock, 1.0)
        setTradingVolume(xlmMock, 1.0)

        whenever(watchlistDataManager.getWatchlist()).thenReturn(
            Single.just(
                Watchlist(
                    assetMap = mapOf(
                        btcMock to listOf(AssetTag.Favourite),
                        ethMock to listOf(AssetTag.Favourite)
                    )
                )
            )
        )

        val list = listOf(
            xlmCustodialAccount,
            btcCustodialAccount,
            ethCustodialAccount
        )

        val result = subject.sorter().invoke(list).test()

        result.assertValue {
            it[0] == ethCustodialAccount &&
                it[1] == btcCustodialAccount &&
                it[2] == xlmCustodialAccount
        }
    }

    @Test
    fun `when ff is on, btc, eth in watchlist, same balances btc more trading volume then order is btc, eth, xlm`() {
        whenever(assetListOrderingFF.enabled).thenReturn(Single.just(true))

        whenever(coincore[ethMock]).thenReturn(ethAsset)
        whenever(coincore[btcMock]).thenReturn(btcAsset)
        whenever(coincore[xlmMock]).thenReturn(xlmAsset)

        val ethCustodialAccount = generateCustodialAccount(ethMock, 0L)
        val btcCustodialAccount = generateCustodialAccount(btcMock, 0L)
        val xlmCustodialAccount = generateCustodialAccount(xlmMock, ONE_XLM)

        setTradingVolume(btcMock, 5.0)
        setTradingVolume(ethMock, 1.0)
        setTradingVolume(xlmMock, 1.0)

        whenever(watchlistDataManager.getWatchlist()).thenReturn(
            Single.just(
                Watchlist(
                    assetMap = mapOf(
                        ethMock to listOf(AssetTag.Favourite),
                        btcMock to listOf(AssetTag.Favourite)
                    )
                )
            )
        )

        val list = listOf(
            xlmCustodialAccount,
            btcCustodialAccount,
            ethCustodialAccount
        )

        val result = subject.sorter().invoke(list).test()

        result.assertValue {
            it[0] == btcCustodialAccount &&
                it[1] == ethCustodialAccount &&
                it[2] == xlmCustodialAccount
        }
    }

    @Test
    fun `when ff is on, xlm in watchlist, btc more balance than eth, then ordering is xlm, btc, etc`() {
        whenever(assetListOrderingFF.enabled).thenReturn(Single.just(true))

        whenever(coincore[ethMock]).thenReturn(ethAsset)
        whenever(coincore[btcMock]).thenReturn(btcAsset)
        whenever(coincore[xlmMock]).thenReturn(xlmAsset)

        val ethCustodialAccount = generateCustodialAccount(ethMock, 0L)
        val btcCustodialAccount = generateCustodialAccount(btcMock, 100 * ONE_BTC)
        val xlmCustodialAccount = generateCustodialAccount(xlmMock, 0L)

        setTradingVolume(btcMock, 1.0)
        setTradingVolume(ethMock, 1.0)
        setTradingVolume(xlmMock, 1.0)

        whenever(watchlistDataManager.getWatchlist()).thenReturn(
            Single.just(
                Watchlist(
                    assetMap = mapOf(
                        xlmMock to listOf(AssetTag.Favourite)
                    )
                )
            )
        )

        val list = listOf(
            ethCustodialAccount,
            btcCustodialAccount,
            xlmCustodialAccount
        )

        val result = subject.sorter().invoke(list).test()

        result.assertValue {
            it[0] == xlmCustodialAccount &&
                it[1] == btcCustodialAccount &&
                it[2] == ethCustodialAccount
        }
    }

    @Test
    fun `when ff is on, none in watchlist, btc more balance than eth & xlm, with same balance, but xlm more trading volume then ordering is btc, xlm, eth`() {
        whenever(assetListOrderingFF.enabled).thenReturn(Single.just(true))

        whenever(coincore[ethMock]).thenReturn(ethAsset)
        whenever(coincore[btcMock]).thenReturn(btcAsset)
        whenever(coincore[xlmMock]).thenReturn(xlmAsset)

        val ethCustodialAccount = generateCustodialAccount(ethMock, ONE_ETH)
        val btcCustodialAccount = generateCustodialAccount(btcMock, 2 * ONE_BTC)
        val xlmCustodialAccount = generateCustodialAccount(xlmMock, ONE_XLM)

        setTradingVolume(btcMock, 1.0)
        setTradingVolume(ethMock, 2.0)
        setTradingVolume(xlmMock, 5.0)

        whenever(watchlistDataManager.getWatchlist()).thenReturn(
            Single.just(
                Watchlist(
                    assetMap = mapOf()
                )
            )
        )

        val list = listOf(
            ethCustodialAccount,
            btcCustodialAccount,
            xlmCustodialAccount
        )

        val result = subject.sorter().invoke(list).test()

        result.assertValue {
            it[0] == btcCustodialAccount &&
                it[1] == xlmCustodialAccount &&
                it[2] == ethCustodialAccount
        }
    }

    @Test
    fun `when ff is on, btc in watchlist, eth and xlm same balance but xlm more trading volume then ordering is btc, xlm, eth`() {
        whenever(assetListOrderingFF.enabled).thenReturn(Single.just(true))

        whenever(coincore[ethMock]).thenReturn(ethAsset)
        whenever(coincore[btcMock]).thenReturn(btcAsset)
        whenever(coincore[xlmMock]).thenReturn(xlmAsset)

        val ethCustodialAccount = generateCustodialAccount(ethMock, ONE_ETH)
        val btcCustodialAccount = generateCustodialAccount(btcMock, 0L)
        val xlmCustodialAccount = generateCustodialAccount(xlmMock, ONE_XLM)

        setTradingVolume(btcMock, 1.0)
        setTradingVolume(ethMock, 2.0)
        setTradingVolume(xlmMock, 5.0)

        whenever(watchlistDataManager.getWatchlist()).thenReturn(
            Single.just(
                Watchlist(
                    assetMap = mapOf(
                        btcMock to listOf(AssetTag.Favourite)
                    )
                )
            )
        )

        val list = listOf(
            ethCustodialAccount,
            btcCustodialAccount,
            xlmCustodialAccount
        )

        val result = subject.sorter().invoke(list).test()

        result.assertValue {
            it[0] == btcCustodialAccount &&
                it[1] == xlmCustodialAccount &&
                it[2] == ethCustodialAccount
        }
    }

    @Test
    fun `when ff is on, all in watchlist, btc highest balance, eth & xlm same balances, xlm more trading volume the order is btc, xlm, eth`() {
        whenever(assetListOrderingFF.enabled).thenReturn(Single.just(true))

        whenever(coincore[ethMock]).thenReturn(ethAsset)
        whenever(coincore[btcMock]).thenReturn(btcAsset)
        whenever(coincore[xlmMock]).thenReturn(xlmAsset)

        val ethCustodialAccount = generateCustodialAccount(ethMock, ONE_ETH)
        val btcCustodialAccount = generateCustodialAccount(btcMock, 2 * ONE_BTC)
        val xlmCustodialAccount = generateCustodialAccount(xlmMock, ONE_XLM)

        setTradingVolume(btcMock, 1.0)
        setTradingVolume(ethMock, 2.0)
        setTradingVolume(xlmMock, 5.0)

        whenever(watchlistDataManager.getWatchlist()).thenReturn(
            Single.just(
                Watchlist(
                    assetMap = mapOf(
                        xlmMock to listOf(AssetTag.Favourite),
                        ethMock to listOf(AssetTag.Favourite),
                        btcMock to listOf(AssetTag.Favourite)
                    )
                )
            )
        )

        val list = listOf(
            ethCustodialAccount,
            btcCustodialAccount,
            xlmCustodialAccount
        )

        val result = subject.sorter().invoke(list).test()

        result.assertValue {
            it[0] == btcCustodialAccount &&
                it[1] == xlmCustodialAccount &&
                it[2] == ethCustodialAccount
        }
    }

    private fun generateCustodialAccount(mock: CryptoCurrency, balance: Long) =
        CustodialTradingAccount(
            currency = mock,
            label = "${mock}CustodialAccount",
            exchangeRates = mock {
                on { exchangeRateToUserFiat(mock) }.thenReturn(
                    Observable.just(ExchangeRate(BigDecimal.ONE, mock, FiatCurrency.Dollars))
                )
            },
            custodialWalletManager = mock(),
            tradingService = mock {
                on { getBalanceFor(mock) }.thenReturn(
                    Observable.just(
                        TradingAccountBalance(
                            total = Money.fromMinor(mock, BigInteger.valueOf(balance)),
                            withdrawable = mock(),
                            pending = mock(),
                            hasTransactions = true
                        )
                    )
                )
            },
            identity = mock(),
            walletModeService = mock(),
            kycService = mock()
        )

    private fun setTradingVolume(asset: CryptoCurrency, volume: Double) {
        val priceRecord: AssetPriceRecord = mock {
            on { tradingVolume24h }.thenReturn(volume)
        }
        whenever(exchangeRatesDataManager.getCurrentAssetPrice(asset, FiatCurrency.Dollars))
            .thenReturn(
                Single.just(priceRecord)
            )
    }
}
