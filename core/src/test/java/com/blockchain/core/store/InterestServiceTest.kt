package com.blockchain.core.store

import com.blockchain.api.services.InterestBalanceDetails
import com.blockchain.core.TransactionsCache
import com.blockchain.core.interest.data.InterestRepository
import com.blockchain.core.interest.data.datasources.InterestAvailableAssetsTimedCache
import com.blockchain.core.interest.data.datasources.InterestBalancesStore
import com.blockchain.core.interest.data.datasources.InterestEligibilityTimedCache
import com.blockchain.core.interest.data.datasources.InterestLimitsTimedCache
import com.blockchain.core.interest.domain.InterestService
import com.blockchain.core.interest.domain.model.InterestAccountBalance
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.service.NabuService
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

class InterestServiceTest {
    private val assetCatalogue = mockk<AssetCatalogue>()
    private val interestBalancesStore = mockk<InterestBalancesStore>()
    private val interestEligibilityTimedCache = mockk<InterestEligibilityTimedCache>()
    private val interestAvailableAssetsTimedCache = mockk<InterestAvailableAssetsTimedCache>()
    private val interestLimitsTimedCache = mockk<InterestLimitsTimedCache>()
    private val authenticator = mockk<Authenticator>()
    private val nabuService = mockk<NabuService>()
    private val transactionsCache = mockk<TransactionsCache>()

    private val interestService: InterestService = InterestRepository(
        assetCatalogue = assetCatalogue,
        interestBalancesStore = interestBalancesStore,
        interestEligibilityTimedCache = interestEligibilityTimedCache,
        interestAvailableAssetsTimedCache = interestAvailableAssetsTimedCache,
        interestLimitsTimedCache = interestLimitsTimedCache,
        authenticator = authenticator,
        nabuService = nabuService,
        transactionsCache = transactionsCache
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

        every { interestBalancesStore.stream(any()) } returns flowOf(DataResource.Data(listOf(interestBalanceDetails)))
        every { interestBalancesStore.invalidate() } just Runs

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
        verify(exactly = 1) { interestBalancesStore.stream(FreshnessStrategy.Cached(true)) }
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
        verify(exactly = 1) { interestBalancesStore.stream(FreshnessStrategy.Cached(true)) }
        verify(exactly = 1) { assetCatalogue.fromNetworkTicker("CRYPTO1") }
    }

    @Test
    fun testGetActiveAssets() = runTest {
        val result = interestService.getActiveAssets().last()

        assertEquals(setOf(cryptoCurrency), result)

        verify(exactly = 1) { interestBalancesStore.stream(FreshnessStrategy.Cached(true)) }
        verify(exactly = 1) { assetCatalogue.fromNetworkTicker("CRYPTO1") }
    }

    // todo (othman) more unit tests
}
