package piuk.blockchain.android.ui.transactionflow.accountsorting

import com.blockchain.api.services.AssetTag
import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.Asset
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.price.Prices24HrWithDelta
import com.blockchain.core.price.model.AssetPriceRecord
import com.blockchain.core.user.Watchlist
import com.blockchain.core.user.WatchlistDataManager
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.logging.MomentEvent
import com.blockchain.logging.MomentLogger
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.math.BigDecimal
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.transfer.BuyListAccountSorting

class BuyAssetOrderingTest {

    private lateinit var subject: BuyListAccountSorting
    private val assetListOrderingFF: FeatureFlag = mock()
    private val coincore: Coincore = mock()
    private val exchangeRatesDataManager: ExchangeRatesDataManager = mock()
    private val watchlistDataManager: WatchlistDataManager = mock()
    private val momentLogger: MomentLogger = mock()

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

    private val initialAssetList = listOf(
        xlmMock, ethMock, btcMock
    )

    @Before
    fun setup() {
        subject = BuyListAccountSorting(
            assetListOrderingFF = assetListOrderingFF,
            coincore = coincore,
            exchangeRatesDataManager = exchangeRatesDataManager,
            watchlistDataManager = watchlistDataManager,
            momentLogger = momentLogger
        )

        doNothing().whenever(momentLogger).startEvent(any())
        doNothing().whenever(momentLogger).endEvent(any(), any())
    }

    @Test
    fun `given ff off then ordering is maintained`() {
        whenever(assetListOrderingFF.enabled).thenReturn(Single.just(false))

        val prices24HrWithDelta = mock<Prices24HrWithDelta> {
            on { currentRate }.thenReturn(mock())
            on { delta24h }.thenReturn(0.0)
        }

        val asset = mock<Asset>()
        whenever(asset.getPricesWith24hDeltaLegacy()).thenReturn(Single.just(prices24HrWithDelta))
        whenever(coincore[any<Currency>()]).thenReturn(asset)

        val result = subject.sort(initialAssetList).test()
        result.assertValue {
            it[0].asset == initialAssetList[0] &&
                it[1].asset == initialAssetList[1] &&
                it[2].asset == initialAssetList[2]
        }

        verifyNoMoreInteractions(exchangeRatesDataManager)
        verifyNoMoreInteractions(watchlistDataManager)

        verifyMomentEvents(MomentEvent.BUY_LIST_ORDERING_FF_OFF)
    }

    @Test
    fun `given ff active, all accounts have 0 balance and equal trading volume, then ordering follows watchlist`() {
        whenever(assetListOrderingFF.enabled).thenReturn(Single.just(true))

        setAccountForAssetWithBalanceAndTradingVolume(ethMock, 0L, 1.0)
        setAccountForAssetWithBalanceAndTradingVolume(xlmMock, 0L, 1.0)
        setAccountForAssetWithBalanceAndTradingVolume(btcMock, 0L, 1.0)

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

        val result = subject.sort(initialAssetList).test()

        result.assertValue {
            it[0].asset == xlmMock &&
                it[1].asset == ethMock &&
                it[2].asset == btcMock
        }

        verifyMomentEvents(MomentEvent.BUY_LIST_ORDERING_FF_ON)
    }

    @Test
    fun `given ff active, xlm in watchlist, btc has more balance than eth and all equal trading volume, then ordering is xlm, btc, eth`() {
        whenever(assetListOrderingFF.enabled).thenReturn(Single.just(true))

        setAccountForAssetWithBalanceAndTradingVolume(ethMock, 0L, 1.0)
        setAccountForAssetWithBalanceAndTradingVolume(xlmMock, 0L, 1.0)
        setAccountForAssetWithBalanceAndTradingVolume(btcMock, ONE_BTC, 1.0)

        whenever(watchlistDataManager.getWatchlist()).thenReturn(
            Single.just(
                Watchlist(
                    assetMap = mapOf(
                        xlmMock to listOf(AssetTag.Favourite)
                    )
                )
            )
        )

        val result = subject.sort(initialAssetList).test()

        result.assertValue {
            it[0].asset == xlmMock &&
                it[1].asset == btcMock &&
                it[2].asset == ethMock
        }

        verifyMomentEvents(MomentEvent.BUY_LIST_ORDERING_FF_ON)
    }

    @Test
    fun `given a set asset list when the ff active, btc and eth have same balance but eth has more trading volume then ordering follows xlm, eth, btc`() {
        whenever(assetListOrderingFF.enabled).thenReturn(Single.just(true))

        setAccountForAssetWithBalanceAndTradingVolume(ethMock, ONE_ETH, 1.0)
        setAccountForAssetWithBalanceAndTradingVolume(xlmMock, 0L, 1.0)
        setAccountForAssetWithBalanceAndTradingVolume(btcMock, ONE_BTC, 1.0)

        whenever(watchlistDataManager.getWatchlist()).thenReturn(
            Single.just(
                Watchlist(
                    assetMap = mapOf(
                        xlmMock to listOf(AssetTag.Favourite)
                    )
                )
            )
        )

        val result = subject.sort(initialAssetList).test()

        result.assertValue {
            it[0].asset == xlmMock &&
                it[1].asset == ethMock &&
                it[2].asset == btcMock
        }

        verifyMomentEvents(MomentEvent.BUY_LIST_ORDERING_FF_ON)
    }

    @Test
    fun `given ff on, eth & xlm in watchlist with less balance than btc then ordering follows eth, xlm, btc`() {
        whenever(assetListOrderingFF.enabled).thenReturn(Single.just(true))

        setAccountForAssetWithBalanceAndTradingVolume(ethMock, 0L, 2.0)
        setAccountForAssetWithBalanceAndTradingVolume(xlmMock, 0L, 1.0)
        setAccountForAssetWithBalanceAndTradingVolume(btcMock, ONE_BTC, 1.0)

        whenever(watchlistDataManager.getWatchlist()).thenReturn(
            Single.just(
                Watchlist(
                    assetMap = mapOf(
                        ethMock to listOf(AssetTag.Favourite),
                        xlmMock to listOf(AssetTag.Favourite)
                    )
                )
            )
        )

        val result = subject.sort(initialAssetList).test()

        result.assertValue {
            it[0].asset == ethMock &&
                it[1].asset == xlmMock &&
                it[2].asset == btcMock
        }

        verifyMomentEvents(MomentEvent.BUY_LIST_ORDERING_FF_ON)
    }

    @Test
    fun `given ff on, nothing in watchlist, all 0 balance, then ordering follows trading volume`() {
        whenever(assetListOrderingFF.enabled).thenReturn(Single.just(true))

        setAccountForAssetWithBalanceAndTradingVolume(ethMock, 0L, 2.0)
        setAccountForAssetWithBalanceAndTradingVolume(xlmMock, 0L, 10.0)
        setAccountForAssetWithBalanceAndTradingVolume(btcMock, 0L, 5.0)

        whenever(watchlistDataManager.getWatchlist()).thenReturn(
            Single.just(
                Watchlist(assetMap = mapOf())
            )
        )

        val result = subject.sort(initialAssetList).test()

        result.assertValue {
            it[0].asset == xlmMock &&
                it[1].asset == btcMock &&
                it[2].asset == ethMock
        }

        verifyMomentEvents(MomentEvent.BUY_LIST_ORDERING_FF_ON)
    }

    @Test
    fun `given ff on, all in watchlist, different balances, same trading volume balance then order is btc, xlm, eth`() {
        whenever(assetListOrderingFF.enabled).thenReturn(Single.just(true))

        setAccountForAssetWithBalanceAndTradingVolume(ethMock, ONE_ETH, 1.0)
        setAccountForAssetWithBalanceAndTradingVolume(xlmMock, 2 * ONE_XLM, 1.0)
        setAccountForAssetWithBalanceAndTradingVolume(btcMock, 5 * ONE_BTC, 1.0)

        whenever(watchlistDataManager.getWatchlist()).thenReturn(
            Single.just(
                Watchlist(
                    assetMap = mapOf(
                        btcMock to listOf(AssetTag.Favourite),
                        ethMock to listOf(AssetTag.Favourite),
                        xlmMock to listOf(AssetTag.Favourite)
                    )
                )
            )
        )

        val result = subject.sort(initialAssetList).test()

        result.assertValue {
            it[0].asset == btcMock &&
                it[1].asset == xlmMock &&
                it[2].asset == ethMock
        }

        verifyMomentEvents(MomentEvent.BUY_LIST_ORDERING_FF_ON)
    }

    private fun setAccountForAssetWithBalanceAndTradingVolume(
        currency: Currency,
        accountBalance: Long,
        tradingVolume: Double
    ) {
        val accountGroup: AccountGroup = mock {
            on { balanceRx }.thenReturn(Observable.just(AccountBalance.testBalance(currency, accountBalance)))
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
            on { accountGroup(AssetFilter.All) }.thenReturn(Maybe.just(accountGroup))
            on { getPricesWith24hDeltaLegacy() }.thenReturn(Single.just(prices24HrWithDelta))
        }
        whenever(coincore[currency]).thenReturn(assetMock)

        val priceRecord: AssetPriceRecord = mock {
            on { tradingVolume24h }.thenReturn(tradingVolume)
        }
        whenever(exchangeRatesDataManager.getCurrentAssetPrice(currency, FiatCurrency.Dollars))
            .thenReturn(Single.just(priceRecord))
    }

    companion object {
        private const val ONE_ETH = 1000000000000000000L
        private const val ONE_BTC = 100000000L
        private const val ONE_XLM = 10000000L
    }

    private fun verifyMomentEvents(event: MomentEvent) {
        verify(momentLogger).startEvent(event)
        verify(momentLogger).endEvent(event)
        verifyNoMoreInteractions(momentLogger)
    }
}
