package com.blockchain.core.price.impl.assetpricestore

import app.cash.turbine.test
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.core.price.model.AssetPriceNotFoundException
import com.blockchain.core.price.model.AssetPriceRecord
import com.blockchain.data.DataResource
import com.blockchain.data.KeyedFreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.outcome.Outcome
import com.blockchain.testutils.USD
import info.blockchain.balance.CryptoCurrency
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AssetPriceStoreTest {

    private val cacheFlow = MutableSharedFlow<DataResource<List<AssetPriceRecord>>>(replay = 1)
    private val cache: AssetPriceStoreCache = mockk {
        every { stream(any()) } returns cacheFlow
    }
    private val supportedTickersStore: SupportedTickersStore = mockk()

    private val subject = AssetPriceStore(
        cache = cache,
        supportedTickersStore = supportedTickersStore
    )

    @Test
    fun `warming supported tickers cache should request supported tickers from store and cache them locally`() =
        runTest {
            val supportedTickersStoreFlow = MutableSharedFlow<DataResource<SupportedTickerGroup>>(replay = 1)
            every { supportedTickersStore.stream(any()) } returns supportedTickersStoreFlow

            supportedTickersStoreFlow.emit(DataResource.Data(SUPPORTED_TICKERS_GROUP))
            assertEquals(subject.warmSupportedTickersCache(), Outcome.Success(Unit))
            assertEquals(subject.fiatQuoteTickers, SUPPORTED_FIAT_QUOTE_TICKERS)
        }

    @Test
    fun `getCurrentPriceForAsset should request asset price records from store and cache them locally`() = runTest {
        subject.getCurrentPriceForAsset(BTC, USD).test {
            verify {
                cache.stream(
                    KeyedFreshnessStrategy.Cached(
                        AssetPriceStoreCache.Key.GetAllCurrent(USD.networkTicker),
                        RefreshStrategy.RefreshIfStale
                    )
                )
            }
            cacheFlow.emit(DataResource.Loading)
            assertEquals(DataResource.Loading, awaitItem())
            cacheFlow.emit(DataResource.Data(RECORDS_LIST))
            assertEquals(DataResource.Data(RECORD_BTC_USD), awaitItem())
            expectNoEvents()

            assertEquals(subject.getCachedAssetPrice(BTC, USD), RECORD_BTC_USD)
            assertEquals(subject.getCachedAssetPrice(ETH, USD), RECORD_ETH_USD)
        }
    }

    @Test
    fun `getCurrentPriceForAsset should request cache and filter for the requested asset and emit Data if found`() =
        runTest {
            subject.getCurrentPriceForAsset(BTC, USD).test {
                cacheFlow.emit(DataResource.Loading)
                assertEquals(DataResource.Loading, awaitItem())
                cacheFlow.emit(DataResource.Data(RECORDS_LIST))
                assertEquals(DataResource.Data(RECORD_BTC_USD), awaitItem())
                expectNoEvents()
            }
        }

    @Test
    fun `getCurrentPriceForAsset should request cache and filter for the requested asset and emit Error if not found`() =
        runTest {
            subject.getCurrentPriceForAsset(BTC, USD).test {
                cacheFlow.emit(DataResource.Data(listOf(RECORD_ETH_USD)))
                assertEquals(DataResource.Error(AssetPriceNotFoundException(BTC, USD)), awaitItem())
                expectNoEvents()
            }
        }

    @Test
    fun `getCurrentPriceForAsset should filter out repeated equal emissions`() = runTest {
        subject.getCurrentPriceForAsset(BTC, USD).test {
            cacheFlow.emit(DataResource.Data(RECORDS_LIST))
            assertEquals(DataResource.Data(RECORD_BTC_USD), awaitItem())
            cacheFlow.emit(DataResource.Data(RECORDS_LIST))
            expectNoEvents()
        }
    }

    @Test
    fun `getYesterdayPriceForAsset should request cache and filter for the requested asset and emit Data if found`() =
        runTest {
            subject.getYesterdayPriceForAsset(BTC, USD).test {
                verify {
                    cache.stream(
                        KeyedFreshnessStrategy.Cached(
                            AssetPriceStoreCache.Key.GetAllYesterday(USD.networkTicker),
                            RefreshStrategy.RefreshIfStale
                        )
                    )
                }
                cacheFlow.emit(DataResource.Loading)
                assertEquals(DataResource.Loading, awaitItem())
                cacheFlow.emit(DataResource.Data(RECORDS_LIST))
                assertEquals(DataResource.Data(RECORD_BTC_USD), awaitItem())
                expectNoEvents()
            }
        }

    @Test
    fun `getYesterdayPriceForAsset should request cache and filter for the requested asset and emit Error if not found`() =
        runTest {
            subject.getYesterdayPriceForAsset(BTC, USD).test {
                cacheFlow.emit(DataResource.Data(listOf(RECORD_ETH_USD)))
                assertEquals(DataResource.Error(AssetPriceNotFoundException(BTC, USD)), awaitItem())
                expectNoEvents()
            }
        }

    @Test
    fun `getYesterdayPriceForAsset should filter out repeated equal emissions`() = runTest {
        subject.getYesterdayPriceForAsset(BTC, USD).test {
            cacheFlow.emit(DataResource.Data(RECORDS_LIST))
            assertEquals(DataResource.Data(RECORD_BTC_USD), awaitItem())
            cacheFlow.emit(DataResource.Data(RECORDS_LIST))
            expectNoEvents()
        }
    }

    @Test
    fun `getHistoricalPriceForAsset should request cache and return the first Success or Failure result`() = runTest {
        subject.getHistoricalPriceForAsset(BTC, USD, HistoricalTimeSpan.WEEK).test {
            cacheFlow.emit(DataResource.Loading)
            assertEquals(DataResource.Loading, awaitItem())
            cacheFlow.emit(DataResource.Data(RECORDS_HISTORICAL))
            assertEquals(DataResource.Data(RECORDS_HISTORICAL), awaitItem())
        }

        cacheFlow.resetReplayCache()
        subject.getHistoricalPriceForAsset(BTC, USD, HistoricalTimeSpan.WEEK).test {
            val error = RuntimeException("some message")
            cacheFlow.emit(DataResource.Error(error))
            assertEquals(DataResource.Error(error), awaitItem())
        }
    }

    companion object {
        private val SUPPORTED_BASE_TICKERS = listOf("BTC", "ETH")
        private val SUPPORTED_FIAT_QUOTE_TICKERS = listOf("USD", "EUR")
        private val SUPPORTED_TICKERS_GROUP = SupportedTickerGroup(
            fiatQuoteTickers = SUPPORTED_FIAT_QUOTE_TICKERS,
            baseTickers = SUPPORTED_BASE_TICKERS
        )

        private val BTC = CryptoCurrency.BTC
        private val ETH = CryptoCurrency.ETHER
        private val RECORD_BTC_USD = AssetPriceRecord(
            base = "BTC",
            quote = "USD",
            rate = 2.0.toBigDecimal(),
            fetchedAt = 1L,
            marketCap = 0.0
        )
        private val RECORD_ETH_USD = AssetPriceRecord(
            base = "ETH",
            quote = "USD",
            rate = 2.0.toBigDecimal(),
            fetchedAt = 1L,
            marketCap = 0.0
        )
        private val RECORD_BTC_EUR = AssetPriceRecord(
            base = "BTC",
            quote = "EUR",
            rate = 1.5.toBigDecimal(),
            fetchedAt = 2L,
            marketCap = 0.0
        )
        private val RECORD_ETH_EUR = AssetPriceRecord(
            base = "ETH",
            quote = "EUR",
            rate = 1.5.toBigDecimal(),
            fetchedAt = 2L,
            marketCap = 0.0
        )
        private val RECORDS_LIST = listOf(
            RECORD_ETH_EUR,
            RECORD_BTC_EUR,
            RECORD_ETH_USD,
            RECORD_BTC_USD
        )
        private val RECORDS_HISTORICAL = listOf(
            AssetPriceRecord(
                base = "BTC",
                quote = "USD",
                rate = 2.0.toBigDecimal(),
                fetchedAt = 10L,
                marketCap = 0.0
            ),
            AssetPriceRecord(
                base = "BTC",
                quote = "USD",
                rate = 1.8.toBigDecimal(),
                fetchedAt = 20L,
                marketCap = 0.0
            ),
            AssetPriceRecord(
                base = "BTC",
                quote = "USD",
                rate = 1.6.toBigDecimal(),
                fetchedAt = 30L,
                marketCap = 0.0
            )
        )
    }
}
