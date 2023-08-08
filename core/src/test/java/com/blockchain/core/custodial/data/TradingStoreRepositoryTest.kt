package com.blockchain.core.custodial.data

import com.blockchain.api.custodial.data.TradingBalanceResponseDto
import com.blockchain.core.custodial.data.store.TradingStore
import com.blockchain.core.custodial.domain.TradingService
import com.blockchain.core.custodial.domain.model.TradingAccountBalance
import com.blockchain.data.DataResource
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class TradingStoreRepositoryTest {

    private val assetCatalogue = mockk<AssetCatalogue>()
    private val tradingStore = mockk<TradingStore>()

    private val tradingService: TradingService = TradingRepository(
        assetCatalogue = assetCatalogue,
        tradingStore = tradingStore
    )

    private val cryptoAsset1 = object : CryptoCurrency(
        displayTicker = "CRYPTO1",
        networkTicker = "CRYPTO1",
        name = "Crypto_1",
        categories = setOf(AssetCategory.TRADING, AssetCategory.NON_CUSTODIAL),
        precisionDp = 8,
        requiredConfirmations = 5,
        colour = "#123456"
    ) {}

    private val cryptoAsset2 = object : CryptoCurrency(
        displayTicker = "CRYPTO2",
        networkTicker = "CRYPTO2",
        name = "Crypto_2",
        categories = setOf(AssetCategory.TRADING),
        precisionDp = 8,
        requiredConfirmations = 5,
        colour = "#123456"
    ) {}

    private val data = listOf(cryptoAsset1, cryptoAsset2).associateWith { anyBalanceForAsset(it) }

    private val cacheResult: Map<String, TradingBalanceResponseDto> = listOf(cryptoAsset1, cryptoAsset2).associate {
        Pair(
            it.displayTicker,
            TradingBalanceResponseDto(
                pending = "3",
                total = "1",
                withdrawable = "2",
                mainBalanceToDisplay = "4"
            )
        )
    }

    @Before
    fun setUp() {
        every { assetCatalogue.fromNetworkTicker(cryptoAsset1.displayTicker) } returns
            cryptoAsset1
        every { assetCatalogue.fromNetworkTicker(cryptoAsset2.displayTicker) } returns
            cryptoAsset2

        every { tradingStore.stream(any()) } returns
            flowOf(DataResource.Data(cacheResult))
    }

    @Test
    fun `WHEN getBalances is called, THEN data should be returned`() {
        tradingService.getBalances()
            .test()
            .await()
            .assertValue {
                it == data
            }
    }

    @Test
    fun `GIVEN asset included, WHEN getBalanceFor is called, THEN Balance should be returned`() {
        tradingService.getBalanceFor(asset = cryptoAsset1)
            .test()
            .await()
            .assertValue {
                it == anyBalanceForAsset(cryptoAsset1)
            }
    }

    @Test
    fun `GIVEN asset not included, WHEN getBalanceFor is called, THEN zeroBalance should be returned`() {
        val asset = CryptoCurrency.BTC

        tradingService.getBalanceFor(asset = asset)
            .test()
            .await()
            .assertValue {
                it == zeroBalanceForAsset(asset)
            }
    }

    @Test
    fun `WHEN getActiveAssets is called, THEN data-keys should be returned`() = runTest {
        val result = tradingService.getActiveAssets().last()
        assertEquals(data.keys, result)
    }

    private fun anyBalanceForAsset(asset: Currency): TradingAccountBalance =
        TradingAccountBalance(
            total = Money.fromMinor(asset, 1.toBigInteger()),
            withdrawable = Money.fromMinor(asset, 2.toBigInteger()),
            pending = Money.fromMinor(asset, 3.toBigInteger()),
            hasTransactions = true
        )

    private fun zeroBalanceForAsset(asset: Currency): TradingAccountBalance =
        TradingAccountBalance(
            total = Money.zero(asset),
            withdrawable = Money.zero(asset),
            pending = Money.zero(asset),
        )
}
