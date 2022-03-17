package com.blockchain.coincore.xlm

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.testutil.CoincoreTestBase
import com.blockchain.core.price.ExchangeRate
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.WalletStatus
import com.blockchain.sunriver.BalanceAndMin
import com.blockchain.sunriver.XlmAccountReference
import com.blockchain.sunriver.XlmDataManager
import com.blockchain.sunriver.XlmFeesFetcher
import com.blockchain.testutils.lumens
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import junit.framework.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager

class XlmAccountBalanceTest : CoincoreTestBase() {

    private val payloadManager: PayloadDataManager = mock()

    private val xlmDataManager: XlmDataManager = mock()
    private val xlmFeesFetcher: XlmFeesFetcher = mock()
    private val walletOptionsDataManager: WalletOptionsDataManager = mock()
    private val walletPreferences: WalletStatus = mock()
    private val custodialWalletManager: CustodialWalletManager = mock()
    private val xlmAccountReference = XlmAccountReference(
        label = "Test Xlm Account",
        accountId = "Test XLM Address"
    )

    private val subject =
        XlmCryptoWalletAccount(
            payloadManager = payloadManager,
            xlmAccountReference = xlmAccountReference,
            xlmManager = xlmDataManager,
            exchangeRates = exchangeRates,
            xlmFeesFetcher = xlmFeesFetcher,
            walletOptionsDataManager = walletOptionsDataManager,
            walletPreferences = walletPreferences,
            custodialWalletManager = custodialWalletManager,
            entitySwitchSilverEligibilityFeatureFlag = mock(),
            identity = mock(),
            addressResolver = mock()
        )

    @Before
    fun setup() {
        initMocks()
        whenever(exchangeRates.exchangeRateToUserFiat(CryptoCurrency.XLM))
            .thenReturn(Observable.just(XLM_TO_USER_RATE))
    }

    @Test
    fun `balances calculated correctly`() {

        val xlmBalance = 100.lumens()
        val xlmMinimum = 10.lumens()
        val xlmBalanceAndMin = BalanceAndMin(
            xlmBalance,
            xlmMinimum
        )

        whenever(xlmDataManager.getBalanceAndMin()).thenReturn(Single.just(xlmBalanceAndMin))

        subject.balance
            .test()
            .assertValue {
                it.total == xlmBalance &&
                    it.withdrawable == xlmBalance - xlmMinimum &&
                    it.pending.isZero &&
                    it.exchangeRate == XLM_TO_USER_RATE
            }
    }

    @Test
    fun `account balance is correct`() {

        val xlmMinimum = 10.lumens()
        val xlmBalanceAndMin = BalanceAndMin(
            100.lumens(),
            xlmMinimum
        )

        whenever(xlmDataManager.getBalanceAndMin()).thenReturn(Single.just(xlmBalanceAndMin))

        subject.balance
            .test()
            .assertValue {
                it == AccountBalance(
                    total = 100.lumens(),
                    withdrawable = 100.lumens() - xlmMinimum,
                    pending = 0.lumens(),
                    exchangeRate = XLM_TO_USER_RATE
                )
            }

        assert(subject.isFunded)
    }

    @Test
    fun `pending balance calculated correctly`() {
        // Arrange
        val xlmBalance = 100.lumens()
        val xlmMinimum = 10.lumens()
        val xlmBalanceAndMin = BalanceAndMin(
            xlmBalance,
            xlmMinimum
        )

        whenever(xlmDataManager.getBalanceAndMin()).thenReturn(Single.just(xlmBalanceAndMin))

        subject.balance.map { it.pending }
            .test()
            .assertValue { it.isZero }

        assert(subject.isFunded)
    }

    @Test
    fun `actionable balance calculated correctly for non-zero balance`() {
        // Arrange
        val xlmBalance = 100.lumens()
        val xlmMinimum = 10.lumens()
        val xlmBalanceAndMin = BalanceAndMin(
            xlmBalance,
            xlmMinimum
        )

        whenever(xlmDataManager.getBalanceAndMin()).thenReturn(Single.just(xlmBalanceAndMin))

        subject.balance
            .test()
            .assertValue {
                it.total == xlmBalance &&
                    it.withdrawable == xlmBalance - xlmMinimum &&
                    it.pending.isZero &&
                    it.exchangeRate == XLM_TO_USER_RATE
            }

        assert(subject.isFunded)
    }

    @Test
    fun `actionable balance calculated correctly for low balance`() {

        val xlmBalance = 5.lumens() // This shouldn't happen, but we should still handle it correctly
        val xlmMinimum = 10.lumens()
        val xlmBalanceAndMin = BalanceAndMin(
            xlmBalance,
            xlmMinimum
        )

        whenever(xlmDataManager.getBalanceAndMin()).thenReturn(Single.just(xlmBalanceAndMin))

        subject.balance
            .test()
            .assertValue {
                it.total == 5.lumens() &&
                    it.withdrawable.isZero &&
                    it.pending.isZero &&
                    it.exchangeRate == XLM_TO_USER_RATE
            }

        assert(subject.isFunded)
    }

    @Test
    fun `actionable balance calculated correctly for zero balance`() {

        val xlmBalance = 0.lumens()
        val xlmMinimum = 10.lumens()
        val xlmBalanceAndMin = BalanceAndMin(
            xlmBalance,
            xlmMinimum
        )

        whenever(xlmDataManager.getBalanceAndMin())
            .thenReturn(Single.just(xlmBalanceAndMin))

        subject.balance
            .test()
            .assertValue {
                it.total.isZero &&
                    it.withdrawable.isZero &&
                    it.pending.isZero &&
                    it.exchangeRate == XLM_TO_USER_RATE
            }

        assertFalse(subject.isFunded)
    }

    companion object {
        private val XLM_TO_USER_RATE = ExchangeRate(
            from = CryptoCurrency.XLM,
            to = TEST_USER_FIAT,
            rate = 1.2.toBigDecimal()
        )
    }
}
