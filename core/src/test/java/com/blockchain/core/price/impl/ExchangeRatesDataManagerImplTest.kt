package com.blockchain.core.price.impl

import app.cash.turbine.test
import com.blockchain.api.services.AssetPrice
import com.blockchain.api.services.AssetPriceService
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.core.price.impl.assetpricestore.AssetPriceStore
import com.blockchain.core.price.model.AssetPriceRecord
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.USD
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.testutils.rxInit
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.core.Single
import java.util.Calendar
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ExchangeRatesDataManagerImplTest {

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        computationTrampoline()
        ioTrampoline()
    }

    private val priceService: AssetPriceService = mock {
        on {
            getHistoricPriceSeriesSince(
                base = any(),
                quote = any(),
                start = any(),
                scale = any()
            )
        }.thenReturn(Single.just(PRICE_DATA_OLD))
    }

    private val currencyPrefs: CurrencyPrefs = mock {
        on { selectedFiatCurrency }.thenReturn(SELECTED_FIAT)
    }

    private val calendar = Calendar.getInstance().apply {
        timeInMillis = DATE_NOW_MILLIS
    }

    private val priceStore: AssetPriceStore = mock() {
        on {
            getHistoricalPriceForAsset(
                base = any(),
                quote = any(),
                timeSpan = any(),
                freshnessStrategy = any()
            )
        }.thenReturn(flowOf(DataResource.Data(PRICE_DATA)))
    }
    private val newAssetPriceStoreFF: FeatureFlag = mock() {
        on { enabled }.thenReturn(Single.just(true))
    }

    private val subject = ExchangeRatesDataManagerImpl(
        priceStore = priceStore,
        assetCatalogue = mock(),
        assetPriceService = priceService,
        currencyPrefs = currencyPrefs
    )

    @Test
    fun `get All Time Price`() = runTest {
        subject.getHistoricPriceSeries(
            asset = OLD_ASSET,
            span = HistoricalTimeSpan.ALL_TIME,
            freshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
        ).test {
            awaitEvent()
            verify(priceStore)
                .getHistoricalPriceForAsset(
                    OLD_ASSET, SELECTED_FIAT, HistoricalTimeSpan.ALL_TIME, FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
                )
            awaitComplete()
        }
    }

    @Test
    fun getYearPrice() = runTest {
        subject.getHistoricPriceSeries(
            asset = OLD_ASSET,
            span = HistoricalTimeSpan.YEAR,
            now = calendar,
            freshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
        )
            .test {
                awaitEvent()
                verify(priceStore)
                    .getHistoricalPriceForAsset(
                        OLD_ASSET,
                        SELECTED_FIAT,
                        HistoricalTimeSpan.YEAR,
                        FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
                    )
                awaitComplete()
            }
    }

    @Test
    fun getMonthPrice() = runTest {
        subject.getHistoricPriceSeries(
            asset = OLD_ASSET,
            span = HistoricalTimeSpan.MONTH,
            now = calendar,
            freshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
        )
            .test {
                awaitEvent()
                verify(priceStore)
                    .getHistoricalPriceForAsset(
                        OLD_ASSET,
                        SELECTED_FIAT,
                        HistoricalTimeSpan.MONTH,
                        FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
                    )
                awaitComplete()
            }
    }

    @Test
    fun getWeekPrice() = runTest {
        subject.getHistoricPriceSeries(
            asset = OLD_ASSET,
            span = HistoricalTimeSpan.WEEK,
            now = calendar,
            freshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
        ).test {
            awaitEvent()
            verify(priceStore)
                .getHistoricalPriceForAsset(
                    OLD_ASSET,
                    SELECTED_FIAT,
                    HistoricalTimeSpan.WEEK,
                    FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
                )
            awaitComplete()
        }
    }

    @Test
    fun getDayPrice() = runTest {
        subject.getHistoricPriceSeries(
            asset = OLD_ASSET,
            span = HistoricalTimeSpan.DAY,
            now = calendar,
            freshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
        ).test {
            awaitEvent()
            verify(priceStore)
                .getHistoricalPriceForAsset(
                    OLD_ASSET,
                    SELECTED_FIAT,
                    HistoricalTimeSpan.DAY,
                    FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
                )
            awaitComplete()
        }
    }

    @Test
    fun `get year price on new asset`() = runTest {
        subject.getHistoricPriceSeries(
            asset = NEW_ASSET,
            span = HistoricalTimeSpan.WEEK,
            now = calendar,
            freshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
        ).test {
            awaitEvent()
            verify(priceStore)
                .getHistoricalPriceForAsset(
                    OLD_ASSET,
                    SELECTED_FIAT,
                    HistoricalTimeSpan.WEEK,
                    FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
                )
            awaitComplete()
        }
    }

    companion object {
        private const val DATE_NOW_MILLIS = 1626972500000L
        private const val DATE_ONE_YEAR_AGO_SECS = 1595436500L
        private const val DATE_ONE_MONTH_AGO_SECS = 1624380500L
        private const val DATE_ONE_WEEK_AGO_SECS = 1626367700L
        private const val DATE_ONE_DAY_AGO_SECS = 1626886100L

        private val SELECTED_FIAT = USD

        private val OLD_ASSET = object : CryptoCurrency(
            displayTicker = "DUMMY",
            networkTicker = "DUMMY",
            name = "Dummies",
            startDate = 1000000001,
            categories = emptySet(),
            precisionDp = 8,
            requiredConfirmations = 5,
            colour = "#123456"
        ) {}

        private val NEW_ASSET = object : CryptoCurrency(
            displayTicker = "DUMMY",
            networkTicker = "DUMMY",
            name = "Dummies",
            startDate = DATE_ONE_WEEK_AGO_SECS,
            categories = emptySet(),
            precisionDp = 8,
            requiredConfirmations = 5,
            colour = "#123456"
        ) {}

        private val PRICE_DATA_OLD = listOf(
            AssetPrice(
                base = OLD_ASSET.networkTicker,
                quote = SELECTED_FIAT.networkTicker,
                price = 100.toDouble(),
                timestampSeconds = 200000,
                marketCap = 0.0,
                tradingVolume24h = 0.0
            )
        )
        private val PRICE_DATA = listOf(
            AssetPriceRecord(
                base = OLD_ASSET.networkTicker,
                quote = SELECTED_FIAT.networkTicker,
                rate = 100.toBigDecimal(),
                fetchedAt = 200000,
                marketCap = 0.0,
            )
        )
    }
}
