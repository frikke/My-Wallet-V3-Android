package com.blockchain.coincore.impl

import com.blockchain.coincore.testutil.CoincoreTestBase
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.data.DataResource
import com.blockchain.earn.domain.models.interest.InterestAccountBalance
import com.blockchain.earn.domain.service.InterestService
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.testutils.testValue
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.ExchangeRate
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.TestScheduler
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx3.asFlow
import org.junit.Before
import org.junit.Test

class CryptoInterestAccountBalanceTest : CoincoreTestBase() {

    private val custodialManager: CustodialWalletManager = mock()
    private val interestService: InterestService = mock()
    private val identity: UserIdentity = mock()
    private val kycService: KycService = mock()

    private val subject = CustodialInterestAccount(
        currency = TEST_ASSET,
        label = "Test Account",
        exchangeRates = exchangeRates,
        custodialWalletManager = custodialManager,
        interestService = interestService,
        identity = identity,
        internalAccountLabel = "Trading Account",
        kycService = kycService
    )

    @Before
    fun setup() {
        initMocks()
    }

    @Test
    fun `Balance is fetched correctly and is non-zero`() {
        whenever(exchangeRates.exchangeRateToUserFiatFlow(TEST_ASSET))
            .thenReturn(flowOf(DataResource.Data(TEST_TO_USER_RATE_1)))

        val balance = InterestAccountBalance(
            totalBalance = 100.testValue(TEST_ASSET),
            pendingInterest = 90.testValue(TEST_ASSET),
            pendingDeposit = 80.testValue(TEST_ASSET),
            totalInterest = 70.testValue(TEST_ASSET),
            lockedBalance = 60.testValue(TEST_ASSET)
        )

        whenever(interestService.getBalanceFor(eq(TEST_ASSET), any()))
            .thenReturn(Observable.just(balance))

        subject.balanceRx()
            .test()
            .assertComplete()
            .assertValue {
                it.total == balance.totalBalance &&
                    it.withdrawable == 40.testValue(TEST_ASSET) &&
                    it.pending == balance.pendingDeposit &&
                    it.exchangeRate == TEST_TO_USER_RATE_1
            }
    }

    @Test
    fun `Balance is fetched correctly and is zero`() {

        whenever(exchangeRates.exchangeRateToUserFiatFlow(TEST_ASSET))
            .thenReturn(flowOf(DataResource.Data(TEST_TO_USER_RATE_1)))

        val balance = InterestAccountBalance(
            totalBalance = 0.testValue(TEST_ASSET),
            pendingInterest = 0.testValue(TEST_ASSET),
            pendingDeposit = 80.testValue(TEST_ASSET),
            totalInterest = 0.testValue(TEST_ASSET),
            lockedBalance = 0.testValue(TEST_ASSET)
        )

        whenever(interestService.getBalanceFor(eq(TEST_ASSET), any()))
            .thenReturn(Observable.just(balance))

        subject.balanceRx()
            .test()
            .assertComplete()
            .assertValue {
                it.total == balance.totalBalance &&
                    it.exchangeRate == TEST_TO_USER_RATE_1
            }
    }

    @Test
    fun `rate changes are propagated correctly`() {
        val scheduler = TestScheduler()

        val rates = listOf(TEST_TO_USER_RATE_1, TEST_TO_USER_RATE_2)
        val rateSource = Observable.zip(
            Observable.interval(1, TimeUnit.SECONDS, scheduler),
            Observable.fromIterable(rates)
        ) { /* tick */ _, rate -> rate as ExchangeRate }.asFlow()

        whenever(exchangeRates.exchangeRateToUserFiatFlow(TEST_ASSET))
            .thenReturn(
                rateSource.map { DataResource.Data(it) }
            )

        val balance = InterestAccountBalance(
            totalBalance = 100.testValue(TEST_ASSET),
            pendingInterest = 90.testValue(TEST_ASSET),
            pendingDeposit = 80.testValue(TEST_ASSET),
            totalInterest = 70.testValue(TEST_ASSET),
            lockedBalance = 60.testValue(TEST_ASSET)
        )

        whenever(interestService.getBalanceFor(eq(TEST_ASSET), any()))
            .thenReturn(Observable.just(balance))

        val testSubscriber = subject.balanceRx()
            .subscribeOn(scheduler)
            .test()
            .assertNoValues()
            .assertNotComplete()

        scheduler.advanceTimeBy(3, TimeUnit.SECONDS)

        testSubscriber.apply {
            assertValueCount(2)
            assertValueAt(0) { it.total == balance.totalBalance && it.exchangeRate == TEST_TO_USER_RATE_1 }
            assertValueAt(1) { it.total == balance.totalBalance && it.exchangeRate == TEST_TO_USER_RATE_2 }
            assertComplete()
        }
    }

    companion object {

        private val TEST_ASSET = object : CryptoCurrency(
            displayTicker = "NOPE",
            networkTicker = "NOPE",
            name = "Not a real thing",
            categories = setOf(AssetCategory.TRADING),
            precisionDp = 8,
            requiredConfirmations = 3,
            colour = "000000"
        ) {}

        private val RATE_1 = 1.2.toBigDecimal()
        private val RATE_2 = 1.4.toBigDecimal()

        private val TEST_TO_USER_RATE_1 = ExchangeRate(
            from = TEST_ASSET,
            to = TEST_USER_FIAT,
            rate = RATE_1
        )

        private val TEST_TO_USER_RATE_2 = ExchangeRate(
            from = TEST_ASSET,
            to = TEST_USER_FIAT,
            rate = RATE_2
        )
    }
}
