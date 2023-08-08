package com.blockchain.coincore.impl.txEngine

import com.blockchain.api.fees.AmountResponse
import com.blockchain.api.fees.ExchangedAmountResponse
import com.blockchain.api.fees.WithdrawFeesAndMinLimitResponse
import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FeeSelection
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.erc20.Erc20NonCustodialAccount
import com.blockchain.coincore.impl.CryptoAccountCompoundGroupTest.Companion.testValue
import com.blockchain.coincore.testutil.CoincoreTestBase
import com.blockchain.core.custodial.data.store.TradingStore
import com.blockchain.core.limits.CUSTODIAL_LIMITS_ACCOUNT
import com.blockchain.core.limits.LimitsDataManager
import com.blockchain.core.limits.NON_CUSTODIAL_LIMITS_ACCOUNT
import com.blockchain.core.limits.TxLimit
import com.blockchain.core.limits.TxLimits
import com.blockchain.data.DataResource
import com.blockchain.domain.paymentmethods.model.CryptoWithdrawalFeeAndLimit
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.testutils.lumens
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.math.BigInteger
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Test

class TradingToOnChainTxEngineTest : CoincoreTestBase() {

    private val isNoteSupported = false
    private val userIdentity: UserIdentity = mock()
    private val walletManager: CustodialWalletManager = mock()
    private val feesAndLimits = CryptoWithdrawalFeeAndLimit(minLimit = 5000.toBigInteger(), fee = BigInteger.ONE)
    private val limitsDataManager: LimitsDataManager = mock {
        on { getLimits(any(), any(), any(), any(), any(), any()) }.thenReturn(
            Single.just(
                TxLimits(
                    min = TxLimit.Limited(CryptoValue.fromMinor(ASSET, feesAndLimits.minLimit)),
                    max = TxLimit.Unlimited,
                    periodicLimits = emptyList(),
                    suggestedUpgrade = null
                )
            )
        )
    }
    private val tradingStore: TradingStore = mock()

    private val subject = TradingToOnChainTxEngine(
        tradingStore = tradingStore,
        walletManager = walletManager,
        isNoteSupported = isNoteSupported,
        limitsDataManager = limitsDataManager,
        userIdentity = userIdentity
    )

    @Before
    fun setup() {
        initMocks()
        whenever(withdrawFeesStore.stream(any())).thenReturn(
            flowOf(
                DataResource.Data(
                    WithdrawFeesAndMinLimitResponse(
                        minAmount = ExchangedAmountResponse(
                            AmountResponse(
                                "XLM",
                                "10"
                            )
                        ),
                        totalFees = ExchangedAmountResponse(
                            AmountResponse(
                                "XLM",
                                "1"
                            )
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `inputs validate when correct`() {
        val sourceAccount = mockSourceAccount()
        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()

        // Assert
        verify(txTarget).asset
        verify(sourceAccount).currency

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test(expected = IllegalStateException::class)
    fun `inputs fail validation when source Asset incorrect`() {
        val sourceAccount = mock<Erc20NonCustodialAccount> {
            on { currency }.thenReturn(WRONG_ASSET)
        }

        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()

        // Assert
        verify(txTarget).asset
        verify(sourceAccount).currency

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `asset is returned correctly`() {
        // Arrange
        val sourceAccount = mockSourceAccount()
        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val asset = subject.sourceAsset

        // Assert
        assertEquals(asset, ASSET)
        verify(sourceAccount).currency

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `PendingTx is correctly initialised`() {
        // Arrange
        val totalBalance = 21.lumens()
        val actionableBalance = 20.lumens()
        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)
        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

        whenever(walletManager.fetchCryptoWithdrawFeeAndMinLimit(ASSET, Product.BUY))
            .thenReturn(Single.just(feesAndLimits))

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        // Act
        subject.doInitialiseTx()
            .test()
            .assertValue {
                it.amount == CryptoValue.zero(txTarget.asset) &&
                    it.totalBalance == totalBalance &&
                    it.availableBalance ==
                    actionableBalance.minus(CryptoValue.fromMinor(txTarget.asset, 1.toBigInteger())) &&
                    it.feeForFullAvailable == CryptoValue.fromMinor(txTarget.asset, 1.toBigInteger()) &&
                    it.selectedFiat == TEST_USER_FIAT &&
                    it.txConfirmations.isEmpty() &&
                    it.limits == TxLimits.withMinAndUnlimitedMax(
                    CryptoValue.fromMinor(ASSET, feesAndLimits.minLimit)
                ) &&
                    it.validationState == ValidationState.UNINITIALISED &&
                    it.engineState.isEmpty()
            }
            .assertValue { verifyFeeLevels(it.feeSelection) }
            .assertNoErrors()
            .assertComplete()

        verify(sourceAccount, atLeastOnce()).currency
        verify(withdrawFeesStore).stream(any())
        verify(limitsDataManager).getLimits(
            outputCurrency = eq(ASSET),
            sourceCurrency = eq(ASSET),
            targetCurrency = eq(ASSET),
            sourceAccountType = eq(CUSTODIAL_LIMITS_ACCOUNT),
            targetAccountType = eq(NON_CUSTODIAL_LIMITS_ACCOUNT),
            legacyLimits = any()
        )
    }

    @Test
    fun `update amount modifies the pendingTx correctly`() {
        // Arrange
        val totalBalance = 21.lumens()
        val actionableBalance = 20.lumens()
        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)
        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = CryptoValue.zero(txTarget.asset),
            totalBalance = totalBalance,
            availableBalance = actionableBalance.minus(
                CryptoValue.fromMinor(txTarget.asset, feesAndLimits.fee)
            ),
            feeForFullAvailable = CryptoValue.fromMinor(txTarget.asset, feesAndLimits.fee),
            feeAmount = CryptoValue.zero(txTarget.asset),
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection()
        )

        val inputAmount = 2.lumens()

        // Act
        subject.doUpdateAmount(
            inputAmount,
            pendingTx
        ).test()
            .assertValue {
                it.amount == inputAmount
            }
            .assertComplete()
            .assertNoErrors()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `update fee level from NONE to PRIORITY is rejected`() {
        val totalBalance = 21.lumens()
        val actionableBalance = 20.lumens()
        val inputAmount = 2.lumens()
        val zeroPax = 0.lumens()

        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)
        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = actionableBalance,
            feeAmount = zeroPax,
            feeForFullAvailable = zeroPax,
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection()
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.Priority,
            -1
        ).test()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `update fee level from NONE to REGULAR is rejected`() {
        val totalBalance = 21.lumens()
        val actionableBalance = 20.lumens()
        val inputAmount = 2.lumens()
        val zeroPax = 0.lumens()

        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)
        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = actionableBalance,
            feeForFullAvailable = zeroPax,
            feeAmount = zeroPax,
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection()
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.Regular,
            -1
        ).test()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `update fee level from NONE to CUSTOM is rejected`() {
        val totalBalance = 21.lumens()
        val actionableBalance = 20.lumens()
        val inputAmount = 2.lumens()
        val zeroPax = 0.lumens()

        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)
        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = actionableBalance,
            feeForFullAvailable = zeroPax,
            feeAmount = zeroPax,
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection()
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.Custom,
            100
        ).test()
    }

    @Test
    fun `update fee level from NONE to NONE has no effect`() {
        val totalBalance = 21.lumens()
        val actionableBalance = 20.lumens()
        val inputAmount = 2.lumens()
        val zeroPax = 0.lumens()

        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)
        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = actionableBalance,
            feeForFullAvailable = zeroPax,
            feeAmount = zeroPax,
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection()
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.None,
            -1
        ).test()
            .assertValue {
                it.amount == inputAmount &&
                    it.totalBalance == totalBalance &&
                    it.availableBalance == actionableBalance &&
                    it.feeAmount == zeroPax
            }
            .assertValue { verifyFeeLevels(it.feeSelection) }
            .assertComplete()
            .assertNoErrors()

        noMoreInteractions(sourceAccount, txTarget)
    }

    private fun mockSourceAccount(
        totalBalance: Money = CryptoValue.zero(ASSET),
        actionable: Money = CryptoValue.zero(ASSET)
    ): Erc20NonCustodialAccount {
        val accountBalance = AccountBalance(
            total = totalBalance,
            pending = 0.testValue(),
            withdrawable = actionable,
            exchangeRate = ExchangeRate(
                from = TEST_ASSET,
                to = TEST_USER_FIAT,
                rate = 1.2.toBigDecimal()
            )
        )
        return mock {
            on { currency }.thenReturn(ASSET)
            on { balanceRx() }.thenReturn(
                Observable.just(
                    accountBalance
                )
            )
        }
    }

    private fun verifyFeeLevels(feeSelection: FeeSelection) =
        feeSelection.selectedLevel == FeeLevel.None &&
            feeSelection.availableLevels == setOf(FeeLevel.None) &&
            feeSelection.availableLevels.contains(feeSelection.selectedLevel) &&
            feeSelection.asset == null &&
            feeSelection.customAmount == -1L

    private fun noMoreInteractions(sourceAccount: BlockchainAccount, txTarget: TransactionTarget) {
        verifyNoMoreInteractions(txTarget)
        verifyNoMoreInteractions(sourceAccount)
        verifyNoMoreInteractions(walletManager)
        verifyNoMoreInteractions(currencyPrefs)
        verifyNoMoreInteractions(exchangeRates)
    }

    companion object {
        private val ASSET = CryptoCurrency.XLM
        private val WRONG_ASSET = CryptoCurrency.BTC
    }
}
