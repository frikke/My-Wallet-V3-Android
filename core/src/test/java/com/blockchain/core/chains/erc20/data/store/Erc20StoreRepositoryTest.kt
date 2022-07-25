package com.blockchain.core.chains.erc20.data.store

import com.blockchain.api.services.Erc20TokenBalance
import com.blockchain.core.chains.erc20.data.Erc20StoreRepository
import com.blockchain.core.chains.erc20.domain.Erc20StoreService
import com.blockchain.core.chains.erc20.domain.model.Erc20Balance
import com.blockchain.store.StoreResponse
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class Erc20StoreRepositoryTest {
    private val assetCatalogue = mockk<AssetCatalogue>()
    private val erc20DataSource = mockk<Erc20DataSource>()

    private val erc20StoreService: Erc20StoreService = Erc20StoreRepository(
        assetCatalogue = assetCatalogue,
        erc20DataSource = erc20DataSource
    )

    private val cryptoCurrency = object : CryptoCurrency(
        displayTicker = "CRYPTO1",
        networkTicker = "CRYPTO1",
        name = "Crypto_1",
        categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
        precisionDp = 8,
        requiredConfirmations = 5,
        colour = "#123456"
    ) {}

    private val erc20TokenBalance = Erc20TokenBalance(
        ticker = "CRYPTO1",
        contractAddress = "contractAddress",
        balance = 1.toBigInteger(),
        precisionDp = 1,
        transferCount = 2
    )

    private val erc20Balance = Erc20Balance(
        balance = CryptoValue.fromMinor(cryptoCurrency, 1.toBigInteger()),
        hasTransactions = true
    )

    private val data = mapOf(cryptoCurrency to erc20Balance)

    @Before
    fun setUp() {
        every { erc20DataSource.streamData(any()) } returns flowOf(StoreResponse.Data(listOf(erc20TokenBalance)))
        every { erc20DataSource.invalidate() } just Runs
        every { assetCatalogue.assetFromL1ChainByContractAddress(any(), any()) } returns cryptoCurrency
    }

    @Test
    fun `WHEN getBalances is called, THEN data should be returned`() {
        erc20StoreService.getBalances()
            .test()
            .await()
            .assertValue {
                it == data
            }

        verify(exactly = 1) {
            assetCatalogue.assetFromL1ChainByContractAddress(
                CryptoCurrency.ETHER.networkTicker,
                "contractAddress"
            )
        }
    }

    @Test
    fun `GIVEN asset included, WHEN getBalanceFor is called, THEN erc20Balance should be returned`() {
        erc20StoreService.getBalanceFor(asset = cryptoCurrency)
            .test()
            .await()
            .assertValue {
                it == erc20Balance
            }
    }

    @Test
    fun `GIVEN asset not included, WHEN getBalanceFor is called, THEN Erc20Balance-zero should be returned`() {
        val asset = CryptoCurrency.BTC

        erc20StoreService.getBalanceFor(asset = asset)
            .test()
            .await()
            .assertValue {
                it == Erc20Balance.zero(asset)
            }
    }

    @Test
    fun `WHEN getActiveAssets is called, THEN data-keys should be returned`() = runTest {
        val result = erc20StoreService.getActiveAssets().last()
        assertEquals(data.keys, result)
    }
}
