package com.blockchain.coincore.impl.txEngine.interest

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.btc.BtcCryptoWalletAccount
import com.blockchain.coincore.impl.CustodialInterestAccount
import com.blockchain.coincore.testutil.CoincoreTestBase
import com.blockchain.core.custodial.data.store.TradingStore
import com.blockchain.core.limits.TxLimits
import com.blockchain.earn.domain.models.interest.InterestLimits
import com.blockchain.earn.domain.service.InterestService
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.storedatasource.FlushableDataSource
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.amshove.kluent.shouldEqual
import org.junit.Before
import org.junit.Test

class InterestDepositTradingEngineTest : CoincoreTestBase() {

    private fun mockTransactionTarget() = mock<CustodialInterestAccount> {
        on { currency }.thenReturn(ASSET)
    }

    private val custodialWalletManager: CustodialWalletManager = mock()
    private val interestBalanceStore: FlushableDataSource = mock()
    private val interestService: InterestService = mock()
    private val tradingStore: TradingStore = mock()

    private lateinit var subject: InterestDepositTradingEngine

    @Before
    fun setUp() {
        initMocks()

        whenever(exchangeRates.getLastCryptoToUserFiatRate(ASSET))
            .thenReturn(
                ExchangeRate(
                    from = ASSET,
                    to = TEST_USER_FIAT,
                    rate = ASSET_TO_USER_FIAT_RATE
                )
            )

        whenever(exchangeRates.getLastCryptoToFiatRate(ASSET, TEST_API_FIAT))
            .thenReturn(
                ExchangeRate(
                    from = ASSET,
                    to = TEST_API_FIAT,
                    rate = ASSET_TO_API_FIAT_RATE
                )
            )

        subject = InterestDepositTradingEngine(
            interestBalanceStore = interestBalanceStore,
            interestService = interestService,
            tradingStore = tradingStore,
            walletManager = custodialWalletManager
        )
    }

    @Test(expected = IllegalStateException::class)
    fun `inputs fail validation when assets mismatched`() {
        val sourceAccount = mockSourceAccount()
        val txTarget: CustodialInterestAccount = mock {
            on { currency }.thenReturn(WRONG_ASSET)
        }

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()
    }

    @Test
    fun `asset is returned correctly`() {
        // Arrange
        val sourceAccount = mockSourceAccount()
        val txTarget = mockTransactionTarget()

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val asset = subject.sourceAsset

        // Assert
        asset shouldEqual ASSET

        verify(sourceAccount, atLeastOnce()).currency

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `PendingTx is correctly initialised`() {
        // Arrange
        val sourceAccount = mockSourceAccount()
        val txTarget = mockTransactionTarget()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val limits = mock<InterestLimits> {
            on { minDepositFiatValue }.thenReturn(MIN_DEPOSIT_AMOUNT_FIAT)
        }

        whenever(interestService.getLimitsForAsset(ASSET)).thenReturn(Single.just(limits))

        // Act
        subject.doInitialiseTx()
            .test()
            .assertValue {
                it.amount == CryptoValue.zero(ASSET) &&
                    it.totalBalance == CryptoValue.zero(ASSET) &&
                    it.availableBalance == CryptoValue.zero(ASSET) &&
                    it.feeAmount == CryptoValue.zero(ASSET) &&
                    it.selectedFiat == TEST_USER_FIAT &&
                    it.txConfirmations.isEmpty() &&
                    it.limits == TxLimits.withMinAndUnlimitedMax(MIN_DEPOSIT_AMOUNT_CRYPTO) &&
                    it.validationState == ValidationState.UNINITIALISED &&
                    it.engineState.isEmpty()
            }
            .assertNoErrors()
            .assertComplete()

        verify(sourceAccount, atLeastOnce()).currency

        verify(interestService).getLimitsForAsset(ASSET)
        verify(currencyPrefs).selectedFiatCurrency
        verify(sourceAccount).balanceRx()
        verify(exchangeRates).getLastCryptoToFiatRate(ASSET, TEST_API_FIAT)

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `when initialising, if getInterestLimits() returns error, then initialisation fails`() {
        // Arrange
        val sourceAccount = mockSourceAccount()
        val txTarget = mockTransactionTarget()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        whenever(interestService.getLimitsForAsset(ASSET))
            .thenReturn(Single.error(NoSuchElementException()))

        // Act
        subject.doInitialiseTx()
            .test()
            .assertError(NoSuchElementException::class.java)

        verify(sourceAccount, atLeastOnce()).currency

        verify(interestService).getLimitsForAsset(ASSET)
        verify(sourceAccount).balanceRx()

        noMoreInteractions(sourceAccount, txTarget)

        noMoreInteractions(sourceAccount, txTarget)
    }

    private fun noMoreInteractions(sourceAccount: BlockchainAccount, txTarget: TransactionTarget) {
        verifyNoMoreInteractions(txTarget)
        verifyNoMoreInteractions(custodialWalletManager)
        verifyNoMoreInteractions(currencyPrefs)
        verifyNoMoreInteractions(exchangeRates)
        verifyNoMoreInteractions(sourceAccount)
    }

    private fun mockSourceAccount(
        totalBalance: Money = CryptoValue.zero(ASSET),
        availableBalance: Money = CryptoValue.zero(ASSET)
    ) = mock<BtcCryptoWalletAccount> {
        on { currency }.thenReturn(ASSET)
        on { balanceRx() }.thenReturn(
            Observable.just(
                AccountBalance(
                    total = totalBalance,
                    withdrawable = availableBalance,
                    pending = Money.zero(totalBalance.currency),
                    exchangeRate = ExchangeRate.identityExchangeRate(totalBalance.currency)
                )
            )
        )
    }

    companion object {
        private val ASSET = CryptoCurrency.BTC
        private val WRONG_ASSET = CryptoCurrency.XLM

        private val ASSET_TO_API_FIAT_RATE = 10.toBigDecimal()
        private val ASSET_TO_USER_FIAT_RATE = 5.toBigDecimal()
        private val MIN_DEPOSIT_AMOUNT_FIAT = FiatValue.fromMajor(TEST_API_FIAT, 10.toBigDecimal())
        private val MIN_DEPOSIT_AMOUNT_CRYPTO = CryptoValue.fromMajor(ASSET, 1.toBigDecimal())
    }
}
