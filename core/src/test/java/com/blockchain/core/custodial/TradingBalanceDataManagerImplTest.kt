package com.blockchain.core.custodial

import com.blockchain.android.testutils.rxInit
import com.blockchain.nabu.GBP
import com.blockchain.nabu.USD
import com.blockchain.testutils.waitForCompletionWithoutErrors
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Single
import org.junit.Rule
import org.junit.Test

class TradingBalanceDataManagerImplTest {

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        computationTrampoline()
        ioTrampoline()
    }

    private val tradingBalanceCallCache: TradingBalanceCallCache = mock()
    private val subject = TradingBalanceDataManagerImpl(
        balanceCallCache = tradingBalanceCallCache
    )

    @Test
    fun `will get balance for a given asset`() {
        val cacheResult = buildCacheResult(
            listOf(CRYPTO_ASSET_1, CRYPTO_ASSET_2, USD, GBP)
        )

        whenever(tradingBalanceCallCache.getTradingBalances())
            .thenReturn(Single.just(cacheResult))

        subject.getBalanceForCurrency(CRYPTO_ASSET_1)
            .test()
            .waitForCompletionWithoutErrors()
            .assertValue {
                val total = it.total
                total is CryptoValue &&
                    total.currency == CRYPTO_ASSET_1 &&
                    total.isPositive
            }
    }

    @Test
    fun `balance not found for a given asset`() {
        val cacheResult = buildCacheResult(
            listOf(CRYPTO_ASSET_1),
        )

        whenever(tradingBalanceCallCache.getTradingBalances())
            .thenReturn(Single.just(cacheResult))

        subject.getBalanceForCurrency(CRYPTO_ASSET_2)
            .test()
            .waitForCompletionWithoutErrors()
            .assertValue {
                val total = it.total
                total is CryptoValue &&
                    total.currency == CRYPTO_ASSET_2 &&
                    total.isZero
            }
    }

    @Test
    fun `will get balance for a given fiat`() {
        val cacheResult = buildCacheResult(
            listOf(CRYPTO_ASSET_1, CRYPTO_ASSET_2, USD, GBP),
        )

        whenever(tradingBalanceCallCache.getTradingBalances())
            .thenReturn(Single.just(cacheResult))

        subject.getBalanceForCurrency(USD)
            .test()
            .await()
            .assertValue {
                val total = it.total as? FiatValue
                total is FiatValue &&
                    total.currencyCode == USD.networkTicker &&
                    total.isPositive
            }
    }

    @Test
    fun `balance not found for a given fiat`() {
        val cacheResult = buildCacheResult(
            listOf(CRYPTO_ASSET_1, GBP)
        )

        whenever(tradingBalanceCallCache.getTradingBalances())
            .thenReturn(Single.just(cacheResult))

        subject.getBalanceForCurrency(USD)
            .test()
            .await()
            .assertValue {
                val total = it.total
                total is FiatValue &&
                    total.currencyCode == USD.networkTicker &&
                    total.isZero
            }
    }

    @Test
    fun `get active assets`() {
        val cacheResult = buildCacheResult(
            listOf(CRYPTO_ASSET_1, CRYPTO_ASSET_2, GBP)
        )

        whenever(tradingBalanceCallCache.getTradingBalances())
            .thenReturn(Single.just(cacheResult))

        subject.getActiveAssets()
            .test()
            .waitForCompletionWithoutErrors()
            .assertValue {
                it.size == 3 &&
                    it.contains(CRYPTO_ASSET_1) &&
                    it.contains(CRYPTO_ASSET_2) &&
                    it.contains(GBP)
            }
    }

    @Test
    fun `there are no active assets`() {
        val cacheResult = buildCacheResult(
            emptyList()
        )

        whenever(tradingBalanceCallCache.getTradingBalances())
            .thenReturn(Single.just(cacheResult))

        subject.getActiveAssets()
            .test()
            .waitForCompletionWithoutErrors()
            .assertValue {
                it.isEmpty()
            }
    }

    @Test
    fun `there are no active fiats`() {
        val cacheResult = buildCacheResult(
            emptyList()
        )

        whenever(tradingBalanceCallCache.getTradingBalances())
            .thenReturn(Single.just(cacheResult))

        subject.getActiveAssets()
            .test()
            .waitForCompletionWithoutErrors()
            .assertValue {
                it.isEmpty()
            }
    }

    private fun anyBalanceForAsset(asset: Currency): TradingAccountBalance =
        TradingAccountBalance(
            total = Money.fromMinor(asset, 1.toBigInteger()),
            withdrawable = Money.fromMinor(asset, 2.toBigInteger()),
            pending = Money.fromMinor(asset, 3.toBigInteger())
        )

    private fun buildCacheResult(currencies: List<Currency>): TradingBalanceRecord =
        TradingBalanceRecord(
            balances = currencies.map { it to anyBalanceForAsset(it) }.toMap(),
        )

    companion object {
        private const val CRYPTO_TICKER_1 = "CRYPTO1"
        private const val CRYPTO_TICKER_2 = "CRYPTO2"

        private val CRYPTO_ASSET_1 = object : CryptoCurrency(
            displayTicker = CRYPTO_TICKER_1,
            networkTicker = CRYPTO_TICKER_1,
            name = "Crypto_1",
            categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
            precisionDp = 8,
            requiredConfirmations = 5,
            colour = "#123456"
        ) {}

        private val CRYPTO_ASSET_2 = object : CryptoCurrency(
            displayTicker = CRYPTO_TICKER_2,
            networkTicker = CRYPTO_TICKER_2,
            name = "Crypto_2",
            categories = setOf(AssetCategory.CUSTODIAL),
            precisionDp = 8,
            requiredConfirmations = 5,
            colour = "#123456"
        ) {}
    }
}
