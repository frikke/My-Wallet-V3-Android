package com.blockchain.coincore.impl.txEngine

import com.blockchain.banking.BankPartnerCallbackProvider
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FeeSelection
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TxResult
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.fiat.LinkedBankAccount
import com.blockchain.coincore.testutil.CoincoreTestBase
import com.blockchain.core.limits.LimitsDataManager
import com.blockchain.core.limits.TxLimit
import com.blockchain.core.limits.TxLimits
import com.blockchain.nabu.Feature
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.PaymentLimits
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.datamanagers.repositories.WithdrawLocksRepository
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Single
import java.math.BigInteger
import org.junit.Before
import org.junit.Test

class FiatDepositTxEngineTest : CoincoreTestBase() {

    private lateinit var subject: FiatDepositTxEngine
    private val walletManager: CustodialWalletManager = mock()
    private val withdrawalLocksRepository: WithdrawLocksRepository = mock()
    private val bankPartnerCallbackProvider: BankPartnerCallbackProvider = mock()
    private val limits = PaymentLimits(
        min = FiatValue.fromMinor(TEST_USER_FIAT, 100L),
        max = FiatValue.fromMinor(TEST_USER_FIAT, 1000L)
    )

    private val limitsDataManager: LimitsDataManager = mock {
        on { getLimits(any(), any(), any(), any(), any(), any()) }.thenReturn(
            Single.just(
                TxLimits(
                    min = TxLimit.Limited(limits.min),
                    max = TxLimit.Limited(limits.max),
                    periodicLimits = emptyList(),
                    suggestedUpgrade = null
                )
            )
        )
    }

    private val userIdentity: UserIdentity = mock {
        on { isVerifiedFor(Feature.TierLevel(Tier.GOLD)) }.thenReturn(Single.just(true))
    }

    @Before
    fun setup() {
        initMocks()
        subject = FiatDepositTxEngine(
            walletManager = walletManager,
            bankPartnerCallbackProvider = bankPartnerCallbackProvider,
            userIdentity = userIdentity,
            withdrawLocksRepository = withdrawalLocksRepository,
            limitsDataManager = limitsDataManager
        )
    }

    @Test
    fun `inputs validate when correct`() {
        val sourceAccount: LinkedBankAccount = mock()
        val txTarget: FiatAccount = mock()

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
        val sourceAccount: FiatAccount = mock()
        val txTarget: FiatAccount = mock {
            on { fiatCurrency }.thenReturn(TGT_ASSET)
        }

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
        val sourceAccount: LinkedBankAccount = mock()
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

        whenever(walletManager.getBankTransferLimits(TEST_USER_FIAT, true))
            .thenReturn(Single.just(limits))

        whenever(
            withdrawalLocksRepository.getWithdrawLockTypeForPaymentMethod(
                paymentMethodType = PaymentMethodType.BANK_TRANSFER,
                fiatCurrency = TEST_USER_FIAT
            )
        ).thenReturn(Single.just(BigInteger.TEN))

        val sourceAccount: LinkedBankAccount = mock {
            on { fiatCurrency }.thenReturn(TEST_USER_FIAT)
        }
        val txTarget: FiatAccount = mock {
            on { fiatCurrency }.thenReturn(TGT_ASSET)
        }

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val zeroFiat = FiatValue.zero(TEST_USER_FIAT)

        subject.doInitialiseTx()
            .test()
            .assertValue {
                it.amount == zeroFiat &&
                    it.totalBalance == zeroFiat &&
                    it.availableBalance == zeroFiat &&
                    it.feeForFullAvailable == zeroFiat &&
                    it.feeAmount == zeroFiat &&
                    it.selectedFiat == TEST_USER_FIAT &&
                    it.confirmations.isEmpty() &&
                    it.limits == TxLimits.fromAmounts(min = limits.min, max = limits.max) &&
                    it.validationState == ValidationState.UNINITIALISED &&
                    it.engineState.containsKey(WITHDRAW_LOCKS)
            }
            .assertValue { verifyFeeLevels(it.feeSelection) }
            .assertNoErrors()
            .assertComplete()
    }

    @Test
    fun `update amount modifies the pendingTx correctly`() {
        val sourceAccount: LinkedBankAccount = mock()
        val txTarget: FiatAccount = mock {
            on { fiatCurrency }.thenReturn(TGT_ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val zeroFiat = FiatValue.zero(TGT_ASSET)
        val pendingTx = PendingTx(
            amount = zeroFiat,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TGT_ASSET,
            feeSelection = FeeSelection()
        )

        val inputAmount = FiatValue.fromMinor(TGT_ASSET, 1000L)

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
        val sourceAccount: LinkedBankAccount = mock()
        val txTarget: FiatAccount = mock {
            on { fiatCurrency }.thenReturn(TGT_ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val zeroFiat = FiatValue.zero(TGT_ASSET)
        val pendingTx = PendingTx(
            amount = zeroFiat,
            validationState = ValidationState.UNINITIALISED,
            totalBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            availableBalance = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TGT_ASSET,
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
        val sourceAccount: LinkedBankAccount = mock()
        val txTarget: FiatAccount = mock {
            on { fiatCurrency }.thenReturn(TGT_ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(TGT_ASSET, 1000L)
        val zeroFiat = FiatValue.zero(TGT_ASSET)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TGT_ASSET,
            feeSelection = FeeSelection(),
            limits = null
        )

        subject.doValidateAmount(
            pendingTx
        ).test()
            .assertNoErrors()
            .assertComplete()
            .assertValue {
                it.validationState == ValidationState.UNKNOWN_ERROR
            }
    }

    @Test
    fun `validate amount when under min limit`() {
        val sourceAccount: LinkedBankAccount = mock()
        val txTarget: FiatAccount = mock {
            on { fiatCurrency }.thenReturn(TGT_ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(TGT_ASSET, 1000L)
        val minLimit = FiatValue.fromMinor(TGT_ASSET, 2000L)
        val maxLimit = FiatValue.fromMinor(TGT_ASSET, 10000L)

        val zeroFiat = FiatValue.zero(TGT_ASSET)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TGT_ASSET,
            feeSelection = FeeSelection(),
            limits = TxLimits.fromAmounts(min = minLimit, max = maxLimit)
        )

        subject.doValidateAmount(
            pendingTx
        ).test()
            .assertNoErrors()
            .assertComplete()
            .assertValue {
                it.validationState == ValidationState.UNDER_MIN_LIMIT
            }
    }

    @Test
    fun `validate amount when over max limit`() {
        val sourceAccount: LinkedBankAccount = mock()
        val txTarget: FiatAccount = mock {
            on { fiatCurrency }.thenReturn(TGT_ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(TGT_ASSET, 1000000L)
        val minLimit = FiatValue.fromMinor(TGT_ASSET, 2000L)
        val maxLimit = FiatValue.fromMinor(TGT_ASSET, 10000L)

        val zeroFiat = FiatValue.zero(TGT_ASSET)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TGT_ASSET,
            feeSelection = FeeSelection(),
            limits = TxLimits.fromAmounts(min = minLimit, max = maxLimit)
        )

        subject.doValidateAmount(
            pendingTx
        ).test()
            .assertNoErrors()
            .assertComplete()
            .assertValue {
                it.validationState == ValidationState.OVER_GOLD_TIER_LIMIT
            }
    }

    @Test
    fun `validate amount when correct`() {
        val sourceAccount: LinkedBankAccount = mock()
        val txTarget: FiatAccount = mock {
            on { fiatCurrency }.thenReturn(TGT_ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(TGT_ASSET, 3000L)
        val minLimit = FiatValue.fromMinor(TGT_ASSET, 2000L)
        val maxLimit = FiatValue.fromMinor(TGT_ASSET, 10000L)

        val zeroFiat = FiatValue.zero(TGT_ASSET)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TGT_ASSET,
            feeSelection = FeeSelection(),
            limits = null
        )

        subject.doValidateAmount(
            pendingTx
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.amount == pendingTx.amount &&
                    it.limits == pendingTx.limits
            }
    }

    @Test
    fun `executing tx works`() {
        val bankAccountAddress = LinkedBankAccount.BankAccountAddress("address", "label")
        val sourceAccount: LinkedBankAccount = mock {
            on { receiveAddress }.thenReturn(Single.just(bankAccountAddress))
        }
        val txTarget: FiatAccount = mock {
            on { fiatCurrency }.thenReturn(TGT_ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(TGT_ASSET, 3000L)
        val minLimit = FiatValue.fromMinor(TGT_ASSET, 2000L)
        val maxLimit = FiatValue.fromMinor(TGT_ASSET, 10000L)

        val zeroFiat = FiatValue.zero(TGT_ASSET)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TGT_ASSET,
            feeSelection = FeeSelection(),
            limits = TxLimits.fromAmounts(min = minLimit, max = maxLimit)
        )

        val txId = "12234"
        whenever(walletManager.startBankTransfer(bankAccountAddress.address, amount, TGT_ASSET)).thenReturn(
            Single.just(txId)
        )
        subject.doExecute(
            pendingTx, ""
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it is TxResult.HashedTxResult &&
                    it.txId == txId
            }

        verify(walletManager).startBankTransfer(bankAccountAddress.address, amount, TGT_ASSET)
    }

    @Test
    fun `executing tx throws exception`() {
        val bankAccountAddress = LinkedBankAccount.BankAccountAddress("address", "label")
        val sourceAccount: LinkedBankAccount = mock {
            on { receiveAddress }.thenReturn(Single.just(bankAccountAddress))
        }
        val txTarget: FiatAccount = mock {
            on { fiatCurrency }.thenReturn(TGT_ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(TGT_ASSET, 3000L)
        val minLimit = FiatValue.fromMinor(TGT_ASSET, 2000L)
        val maxLimit = FiatValue.fromMinor(TGT_ASSET, 10000L)

        val zeroFiat = FiatValue.zero(TGT_ASSET)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = TGT_ASSET,
            feeSelection = FeeSelection(),
            limits = TxLimits.fromAmounts(min = minLimit, max = maxLimit)
        )

        val exception = IllegalStateException("")
        whenever(walletManager.startBankTransfer(bankAccountAddress.address, amount, TGT_ASSET)).thenReturn(
            Single.error(exception)
        )

        subject.doExecute(
            pendingTx, ""
        ).test()
            .assertError {
                it == exception
            }

        verify(walletManager).startBankTransfer(bankAccountAddress.address, amount, TGT_ASSET)
    }

    private fun verifyFeeLevels(feeSelection: FeeSelection) =
        feeSelection.selectedLevel == FeeLevel.None &&
            feeSelection.availableLevels == setOf(FeeLevel.None) &&
            feeSelection.availableLevels.contains(feeSelection.selectedLevel) &&
            feeSelection.customAmount == -1L &&
            feeSelection.asset == null

    companion object {
        private const val TGT_ASSET = "USD"
    }
}
