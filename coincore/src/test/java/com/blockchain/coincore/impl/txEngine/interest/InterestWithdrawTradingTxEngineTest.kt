package com.blockchain.coincore.impl.txEngine.interest

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.FeeSelection
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.impl.CustodialInterestAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.coincore.testutil.CoincoreTestBase
import com.blockchain.core.custodial.data.store.TradingStore
import com.blockchain.core.limits.TxLimits
import com.blockchain.domain.paymentmethods.model.CryptoWithdrawalFeeAndLimit
import com.blockchain.earn.domain.models.interest.InterestLimits
import com.blockchain.earn.domain.service.InterestService
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
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
import java.math.BigInteger
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class InterestWithdrawTradingTxEngineTest : CoincoreTestBase() {

    private fun mockTransactionTarget() = mock<CustodialTradingAccount> {
        on { currency }.thenReturn(ASSET)
    }

    private val custodialWalletManager: CustodialWalletManager = mock()
    private val interestBalanceStore: FlushableDataSource = mock()
    private val interestService: InterestService = mock()
    private val tradingStore: TradingStore = mock()

    private lateinit var subject: InterestWithdrawTradingTxEngine

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
        subject = InterestWithdrawTradingTxEngine(
            interestBalanceStore = interestBalanceStore,
            interestService = interestService,
            tradingStore = tradingStore,
            walletManager = custodialWalletManager
        )
    }

    @Test(expected = IllegalStateException::class)
    fun `inputs fail validation when assets mismatched`() {
        val sourceAccount = mockSourceAccount()
        val txTarget: CustodialTradingAccount = mock {
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
        asset shouldBeEqualTo ASSET

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
            on { maxWithdrawalFiatValue }.thenReturn(MAX_WITHDRAW_AMOUNT_FIAT)
        }

        val fees = mock<CryptoWithdrawalFeeAndLimit> {
            on { minLimit }.thenReturn(MIN_WITHDRAW_AMOUNT)
            on { fee }.thenReturn(BigInteger.ZERO)
        }

        whenever(interestService.getLimitsForAsset(ASSET)).thenReturn(Single.just(limits))
        whenever(custodialWalletManager.fetchCryptoWithdrawFeeAndMinLimit(ASSET, Product.SAVINGS)).thenReturn(
            Single.just(fees)
        )

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
                    it.limits == TxLimits.fromAmounts(
                    CryptoValue.fromMinor(ASSET, fees.minLimit), MAX_WITHDRAW_AMOUNT_CRYPTO
                ) &&
                    it.validationState == ValidationState.UNINITIALISED &&
                    it.engineState.isEmpty()
            }
            .assertNoErrors()
            .assertComplete()

        verify(sourceAccount, atLeastOnce()).currency

        verify(interestService).getLimitsForAsset(ASSET)
        verify(custodialWalletManager).fetchCryptoWithdrawFeeAndMinLimit(ASSET, Product.SAVINGS)
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

        whenever(custodialWalletManager.fetchCryptoWithdrawFeeAndMinLimit(ASSET, Product.SAVINGS)).thenReturn(
            Single.just(mock())
        )

        // Act
        subject.doInitialiseTx()
            .test()
            .assertError(NoSuchElementException::class.java)

        verify(sourceAccount, atLeastOnce()).currency

        verify(interestService).getLimitsForAsset(ASSET)
        verify(custodialWalletManager).fetchCryptoWithdrawFeeAndMinLimit(ASSET, Product.SAVINGS)
        verify(sourceAccount).balanceRx()

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `when initialising, if fetchCryptoWithdrawFeeAndMinLimit() returns error, then initialisation fails`() {
        // Arrange
        val sourceAccount = mockSourceAccount()
        val txTarget = mockTransactionTarget()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        whenever(interestService.getLimitsForAsset(ASSET)).thenReturn(
            Single.just(mock())
        )
        whenever(
            custodialWalletManager.fetchCryptoWithdrawFeeAndMinLimit(
                ASSET, Product.SAVINGS
            )
        ).thenReturn(
            Single.error(Exception())
        )

        // Act
        subject.doInitialiseTx()
            .test()
            .assertError(Exception::class.java)

        verify(sourceAccount, atLeastOnce()).currency

        verify(interestService).getLimitsForAsset(ASSET)
        verify(custodialWalletManager).fetchCryptoWithdrawFeeAndMinLimit(
            ASSET, Product.SAVINGS
        )
        verify(sourceAccount).balanceRx()

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `when building confirmations, it add the right ones`() {
        val sourceAccount = mockSourceAccount()
        val txTarget = mockTransactionTarget()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val money = CryptoValue.fromMajor(ASSET, 10.toBigDecimal())
        val mockPendingTx =
            PendingTx(
                amount = money,
                totalBalance = money,
                availableBalance = money,
                feeForFullAvailable = money,
                feeAmount = money,
                feeSelection = FeeSelection(),
                selectedFiat = TEST_USER_FIAT,
                txConfirmations = listOf(),
                limits = TxLimits.fromAmounts(min = money, max = money)
            )

        // Act
        subject.doBuildConfirmations(mockPendingTx)
            .test()
            .assertValue { pTx ->
                pTx.txConfirmations.find { it is TxConfirmationValue.From } != null &&
                    pTx.txConfirmations.find { it is TxConfirmationValue.To } != null &&
                    pTx.txConfirmations.find { it is TxConfirmationValue.Total } != null
            }
            .assertNoErrors()
            .assertComplete()
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
        availableBalance: Money = CryptoValue.zero(ASSET),
    ) = mock<CustodialInterestAccount> {
        on { currency }.thenReturn(ASSET)
        on { balanceRx() }.thenReturn(
            Observable.just(
                AccountBalance(
                    total = totalBalance,
                    dashboardDisplay = totalBalance,
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
        private val MIN_WITHDRAW_AMOUNT = 1.toBigInteger()
        private val MAX_WITHDRAW_AMOUNT_FIAT = FiatValue.fromMajor(TEST_API_FIAT, 10.toBigDecimal())
        private val MAX_WITHDRAW_AMOUNT_CRYPTO = CryptoValue.fromMajor(ASSET, 1.toBigDecimal())
    }
}
