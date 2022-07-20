package com.blockchain.core.store

import com.blockchain.api.services.InterestBalanceDetails
import com.blockchain.core.interest.data.InterestRepository
import com.blockchain.core.interest.data.InterestStore
import com.blockchain.core.interest.domain.InterestService
import com.blockchain.core.interest.domain.model.InterestAccountBalance
import com.blockchain.store.StoreRequest
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
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Test

class InterestServiceTest {
    private val assetCatalogue = mockk<AssetCatalogue>()
    private val interestStore = mockk<InterestStore>()

    private val interestService: InterestService = InterestRepository(
        assetCatalogue = assetCatalogue,
        interestStore = interestStore
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

    private val interestBalanceDetails = InterestBalanceDetails(
        assetTicker = "CRYPTO1",
        totalBalance = 1.toBigInteger(),
        pendingInterest = 2.toBigInteger(),
        pendingDeposit = 3.toBigInteger(),
        totalInterest = 4.toBigInteger(),
        pendingWithdrawal = 5.toBigInteger(),
        lockedBalance = 6.toBigInteger()
    )

    private val interestAccountBalance = InterestAccountBalance(
        totalBalance = CryptoValue.fromMinor(cryptoCurrency, 1.toBigInteger()),
        pendingInterest = CryptoValue.fromMinor(cryptoCurrency, 2.toBigInteger()),
        pendingDeposit = CryptoValue.fromMinor(cryptoCurrency, 3.toBigInteger()),
        totalInterest = CryptoValue.fromMinor(cryptoCurrency, 4.toBigInteger()),
        lockedBalance = CryptoValue.fromMinor(cryptoCurrency, 6.toBigInteger()),
        hasTransactions = true
    )

    private val data = mapOf(cryptoCurrency to interestAccountBalance)

    @Before
    fun setUp() {
        every { interestStore.stream(any()) } returns flowOf(StoreResponse.Data(listOf(interestBalanceDetails)))
        every { interestStore.invalidate() } just Runs

        every { assetCatalogue.fromNetworkTicker("CRYPTO1") } returns cryptoCurrency
    }

    @Test
    fun testGetBalances() {
        interestService.getBalances()
            .test()
            .await()
            .assertValue {
                it == data
            }
        verify(exactly = 1) { interestStore.stream(StoreRequest.Cached(true)) }
        verify(exactly = 1) { assetCatalogue.fromNetworkTicker("CRYPTO1") }
    }

    @Test
    fun testGetBalanceFor() {
        interestService.getBalanceFor(cryptoCurrency)
            .test()
            .await()
            .assertValue {
                it == interestAccountBalance
            }
        verify(exactly = 1) { interestStore.stream(StoreRequest.Cached(true)) }
        verify(exactly = 1) { assetCatalogue.fromNetworkTicker("CRYPTO1") }
    }

    @Test
    fun testGetActiveAssets() {
        interestService.getActiveAssets()
            .test()
            .await()
            .assertValue {
                it == setOf(cryptoCurrency)
            }
        verify(exactly = 1) { interestStore.stream(StoreRequest.Cached(false)) }
        verify(exactly = 1) { assetCatalogue.fromNetworkTicker("CRYPTO1") }
    }
}
