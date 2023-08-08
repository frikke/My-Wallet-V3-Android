package com.blockchain.coincore.impl.txEngine

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FeeSelection
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.fiat.LinkedBankAccount
import com.blockchain.coincore.impl.txEngine.fiat.FiatWithdrawalTxEngine
import com.blockchain.coincore.testutil.CoincoreTestBase
import com.blockchain.core.limits.CUSTODIAL_LIMITS_ACCOUNT
import com.blockchain.core.limits.LimitsDataManager
import com.blockchain.core.limits.NON_CUSTODIAL_LIMITS_ACCOUNT
import com.blockchain.core.limits.TxLimit
import com.blockchain.core.limits.TxLimits
import com.blockchain.domain.paymentmethods.model.FiatWithdrawalFeeAndLimit
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test

class FiatWithdrawalTxEngineTest : CoincoreTestBase() {

    private val walletManager: CustodialWalletManager = mock()
    private val userIdentity: UserIdentity = mock()
    private val limitsDataManager: LimitsDataManager = mock {
        on { getLimits(any(), any(), any(), any(), any(), any()) }.thenReturn(
            Single.just(
                TxLimits(
                    min = TxLimit.Limited(FiatValue.fromMinor(TEST_API_FIAT, 100L.toBigInteger())),
                    max = TxLimit.Unlimited,
                    periodicLimits = emptyList(),
                    suggestedUpgrade = null
                )
            )
        )
    }

    private lateinit var subject: FiatWithdrawalTxEngine

    @Before
    fun setup() {
        initMocks()
        subject = FiatWithdrawalTxEngine(walletManager, limitsDataManager, userIdentity)
    }

    @Test
    fun `inputs validate when correct`() {
        val sourceAccount: FiatAccount = mock()
        val txTarget: LinkedBankAccount = mock()

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()

        verifyNoMoreInteractions(txTarget)
        verifyNoMoreInteractions(sourceAccount)
    }

    @Test(expected = IllegalStateException::class)
    fun `inputs fail validation when source Account incorrect`() {
        val sourceAccount: CryptoAccount = mock()
        val txTarget: LinkedBankAccount = mock()

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()
    }

    @Test(expected = IllegalStateException::class)
    fun `inputs fail validation when target account incorrect`() {
        val sourceAccount: FiatAccount = mock()
        val txTarget: CryptoAccount = mock()

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()
    }

    @Test
    fun `PendingTx is correctly initialised`() {
        val zeroFiat = FiatValue.zero(TEST_API_FIAT)
        val expectedBalance = FiatValue.fromMinor(TEST_API_FIAT, 10000L.toBigInteger())
        val expectedAccountBalance = FiatValue.fromMinor(TEST_API_FIAT, 100000L.toBigInteger())
        val balance: AccountBalance = mock {
            on { total }.thenReturn(expectedAccountBalance)
            on { withdrawable }.thenReturn(expectedBalance)
        }
        val sourceAccount = mock<FiatAccount>()
        whenever(sourceAccount.balanceRx(any())).thenReturn(Observable.just(balance))
        whenever(sourceAccount.currency).thenReturn(TEST_API_FIAT)

        val expectedMinAmountAndFee = FiatWithdrawalFeeAndLimit(
            minLimit = FiatValue.fromMinor(TEST_API_FIAT, 100L.toBigInteger()),
            fee = FiatValue.fromMinor(TEST_API_FIAT, 1000L.toBigInteger())
        )

        val txTarget: LinkedBankAccount = mock {
            on { currency }.thenReturn(TEST_API_FIAT)
            on { getWithdrawalFeeAndMinLimit() }.thenReturn(Single.just(expectedMinAmountAndFee))
        }

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.doInitialiseTx()
            .test()
            .assertValue {
                it.amount == zeroFiat &&
                    it.totalBalance == balance.total &&
                    it.availableBalance == balance.withdrawable - expectedMinAmountAndFee.fee &&
                    it.feeForFullAvailable == zeroFiat &&
                    it.feeAmount == expectedMinAmountAndFee.fee &&
                    it.selectedFiat == TEST_USER_FIAT &&
                    it.txConfirmations.isEmpty() &&
                    it.limits == TxLimits.withMinAndUnlimitedMax(min = expectedMinAmountAndFee.minLimit) &&
                    it.validationState == ValidationState.UNINITIALISED &&
                    it.engineState.isEmpty()
            }
            .assertValue { verifyFeeLevels(it.feeSelection) }
            .assertNoErrors()
            .assertComplete()

        verify(txTarget).getWithdrawalFeeAndMinLimit()
        verify(limitsDataManager).getLimits(
            outputCurrency = eq(TEST_API_FIAT),
            sourceCurrency = eq(TEST_API_FIAT),
            targetCurrency = eq(TEST_API_FIAT),
            sourceAccountType = eq(CUSTODIAL_LIMITS_ACCOUNT),
            targetAccountType = eq(NON_CUSTODIAL_LIMITS_ACCOUNT),
            legacyLimits = any()
        )
    }

    @Test
    fun `update amount modifies the pendingTx correctly`() {
        val sourceAccount: FiatAccount = mock {
            on { currency }.thenReturn(TEST_API_FIAT)
        }
        val txTarget: LinkedBankAccount = mock()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val zeroFiat = FiatValue.zero(TEST_API_FIAT)
        val pendingTx = PendingTx(
            amount = zeroFiat,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TEST_API_FIAT,
            feeSelection = FeeSelection()
        )

        val inputAmount = FiatValue.fromMinor(TEST_API_FIAT, 1000L.toBigInteger())

        subject.doUpdateAmount(
            inputAmount,
            pendingTx
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.amount == inputAmount &&
                    it.totalBalance == zeroFiat &&
                    it.availableBalance == zeroFiat &&
                    it.feeAmount == zeroFiat
            }
            .assertValue { verifyFeeLevels(it.feeSelection) }
    }

    @Test
    fun `validate amount when pendingTx uninitialised`() {
        val sourceAccount: FiatAccount = mock {
            on { currency }.thenReturn(TEST_API_FIAT)
        }
        val txTarget: LinkedBankAccount = mock()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val zeroFiat = FiatValue.zero(TEST_API_FIAT)
        val pendingTx = PendingTx(
            amount = zeroFiat,
            validationState = ValidationState.UNINITIALISED,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TEST_API_FIAT,
            feeSelection = FeeSelection()
        )

        subject.doValidateAmount(
            pendingTx
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.amount == zeroFiat &&
                    it.totalBalance == zeroFiat &&
                    it.availableBalance == zeroFiat &&
                    it.feeAmount == zeroFiat
            }
            .assertValue { verifyFeeLevels(it.feeSelection) }
    }

    @Test
    fun `validate amount when limits not set`() {
        val sourceAccount: FiatAccount = mock {
            on { currency }.thenReturn(TEST_API_FIAT)
        }
        val txTarget: LinkedBankAccount = mock()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(TEST_API_FIAT, 1000L.toBigInteger())
        val zeroFiat = FiatValue.zero(TEST_API_FIAT)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TEST_API_FIAT,
            feeSelection = FeeSelection(),
            limits = null
        )

        subject.doValidateAmount(
            pendingTx
        ).test()
            .assertNotComplete()
            .assertError {
                it is MissingLimitsException
            }
    }

    @Test
    fun `validate amount when under min limit`() {
        val sourceAccount: FiatAccount = mock {
            on { currency }.thenReturn(TEST_API_FIAT)
        }
        val txTarget: LinkedBankAccount = mock()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(TEST_API_FIAT, 1000L.toBigInteger())
        val minLimit = FiatValue.fromMinor(TEST_API_FIAT, 2000L.toBigInteger())
        val maxLimit = FiatValue.fromMinor(TEST_API_FIAT, 10000L.toBigInteger())

        val zeroFiat = FiatValue.zero(TEST_API_FIAT)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TEST_API_FIAT,
            feeSelection = FeeSelection(),
            limits = TxLimits.fromAmounts(min = minLimit, max = maxLimit)
        )

        subject.doValidateAmount(
            pendingTx
        ).test()
            .assertComplete()
            .assertValue {
                it.validationState == ValidationState.UNDER_MIN_LIMIT
            }
    }

    @Test
    fun `validate amount when over max limit`() {
        val sourceAccount: FiatAccount = mock {
            on { currency }.thenReturn(TEST_API_FIAT)
        }

        val txTarget: LinkedBankAccount = mock()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(TEST_API_FIAT, 1000000L.toBigInteger())
        val minLimit = FiatValue.fromMinor(TEST_API_FIAT, 2000L.toBigInteger())
        val availableToWithdraw = FiatValue.fromMinor(TEST_API_FIAT, 10000L.toBigInteger())

        val zeroFiat = FiatValue.zero(TEST_API_FIAT)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = availableToWithdraw,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TEST_API_FIAT,
            feeSelection = FeeSelection(),
            limits = TxLimits.withMinAndUnlimitedMax(min = minLimit)
        )

        subject.doValidateAmount(
            pendingTx
        ).test()
            .assertComplete()
            .assertValue {
                it.validationState == ValidationState.INSUFFICIENT_FUNDS
            }
    }

    @Test
    fun `validate amount when over available balance`() {
        val sourceAccount: FiatAccount = mock {
            on { currency }.thenReturn(TEST_API_FIAT)
        }
        val txTarget: LinkedBankAccount = mock()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(TEST_API_FIAT, 3000L.toBigInteger())
        val minLimit = FiatValue.fromMinor(TEST_API_FIAT, 2000L.toBigInteger())
        val maxLimit = FiatValue.fromMinor(TEST_API_FIAT, 10000L.toBigInteger())

        val zeroFiat = FiatValue.zero(TEST_API_FIAT)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TEST_API_FIAT,
            feeSelection = FeeSelection(),
            limits = TxLimits.fromAmounts(min = minLimit, max = maxLimit)
        )

        subject.doValidateAmount(
            pendingTx
        ).test()
            .assertComplete()
            .assertValue {
                it.validationState == ValidationState.INSUFFICIENT_FUNDS
            }
    }

    @Test
    fun `validate amount when correct`() {
        val sourceAccount: FiatAccount = mock {
            on { currency }.thenReturn(TEST_API_FIAT)
        }
        val txTarget: LinkedBankAccount = mock()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val balance = FiatValue.fromMinor(TEST_API_FIAT, 4000L.toBigInteger())
        val amount = FiatValue.fromMinor(TEST_API_FIAT, 3000L.toBigInteger())
        val minLimit = FiatValue.fromMinor(TEST_API_FIAT, 2000L.toBigInteger())
        val maxLimit = FiatValue.fromMinor(TEST_API_FIAT, 10000L.toBigInteger())

        val zeroFiat = FiatValue.zero(TEST_API_FIAT)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = balance,
            availableBalance = balance,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TEST_API_FIAT,
            feeSelection = FeeSelection(),
            limits = TxLimits.fromAmounts(min = minLimit, max = maxLimit)
        )

        subject.doValidateAmount(
            pendingTx
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.amount == pendingTx.amount &&
                    it.limits == pendingTx.limits
                it.validationState == ValidationState.CAN_EXECUTE
            }
    }

    @Test
    fun `executing tx works`() {
        val bankAccountAddress = LinkedBankAccount.BankAccountAddress("address", "label")
        val sourceAccount: FiatAccount = mock {
            on { currency }.thenReturn(TEST_API_FIAT)
        }
        val txTarget: LinkedBankAccount = mock {
            on { receiveAddress }.thenReturn(Single.just(bankAccountAddress))
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(TEST_API_FIAT, 3000L.toBigInteger())
        val minLimit = FiatValue.fromMinor(TEST_API_FIAT, 2000L.toBigInteger())
        val maxLimit = FiatValue.fromMinor(TEST_API_FIAT, 10000L.toBigInteger())

        val zeroFiat = FiatValue.zero(TEST_API_FIAT)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TEST_API_FIAT,
            feeSelection = FeeSelection(),
            limits = TxLimits.fromAmounts(min = minLimit, max = maxLimit)
        )

        whenever(walletManager.createWithdrawOrder(amount, bankAccountAddress.address)).thenReturn(
            Completable.complete()
        )

        subject.doExecute(
            pendingTx,
            ""
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it is TxResult.UnHashedTxResult &&
                    it.amount == pendingTx.amount
            }

        verify(walletManager).createWithdrawOrder(amount, bankAccountAddress.address)
    }

    @Test
    fun `executing tx throws exception`() {
        val bankAccountAddress = LinkedBankAccount.BankAccountAddress("address", "label")
        val sourceAccount: FiatAccount = mock {
            on { currency }.thenReturn(TEST_API_FIAT)
        }
        val txTarget: LinkedBankAccount = mock {
            on { receiveAddress }.thenReturn(Single.just(bankAccountAddress))
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(TEST_API_FIAT, 3000L.toBigInteger())
        val minLimit = FiatValue.fromMinor(TEST_API_FIAT, 2000L.toBigInteger())
        val maxLimit = FiatValue.fromMinor(TEST_API_FIAT, 10000L.toBigInteger())

        val zeroFiat = FiatValue.zero(TEST_API_FIAT)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TEST_API_FIAT,
            feeSelection = FeeSelection(),
            limits = TxLimits.fromAmounts(min = minLimit, max = maxLimit)
        )

        val exception = IllegalStateException("")
        whenever(walletManager.createWithdrawOrder(amount, bankAccountAddress.address)).thenReturn(
            Completable.error(exception)
        )

        subject.doExecute(
            pendingTx,
            ""
        ).test()
            .assertError {
                it == exception
            }

        verify(walletManager).createWithdrawOrder(amount, bankAccountAddress.address)
    }

    private fun verifyFeeLevels(feeSelection: FeeSelection) =
        feeSelection.selectedLevel == FeeLevel.None &&
            feeSelection.availableLevels == setOf(FeeLevel.None) &&
            feeSelection.availableLevels.contains(feeSelection.selectedLevel) &&
            feeSelection.customAmount == -1L &&
            feeSelection.asset == null
}
