@file:Suppress("UnnecessaryVariable")

package com.blockchain.coincore.eth

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FeeSelection
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.testutil.CoincoreTestBase
import com.blockchain.core.chains.ethereum.EthDataManager
import com.blockchain.core.fees.FeeDataManager
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.testutils.ether
import com.blockchain.testutils.gwei
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import info.blockchain.wallet.api.data.FeeLimits
import info.blockchain.wallet.api.data.FeeOptions
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlin.test.assertEquals
import org.junit.Before
import org.junit.Test

class EthOnChainTxEngineTest : CoincoreTestBase() {

    private val ethDataManager: EthDataManager = mock()
    private val ethFeeOptions: FeeOptions = mock()

    private val feeManager: FeeDataManager = mock {
        on { ethFeeOptions }.thenReturn(Observable.just(ethFeeOptions))
    }
    private val walletPreferences: WalletStatusPrefs = mock {
        on { getFeeTypeForAsset(ASSET) }.thenReturn(FeeLevel.Regular.ordinal)
    }

    private val subject = EthOnChainTxEngine(
        ethDataManager = ethDataManager,
        feeManager = feeManager,
        requireSecondPassword = false,
        walletPreferences = walletPreferences,
        resolvedAddress = Single.just("")
    )

    @Before
    fun setup() {
        initMocks()
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
        verify(txTarget, atLeastOnce()).asset
        verify(sourceAccount, atLeastOnce()).currency

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test(expected = IllegalStateException::class)
    fun `inputs fail validation when source Asset incorrect`() {
        val sourceAccount = mock<EthCryptoWalletAccount> {
            on { currency }.thenReturn(WRONG_ASSET)
        }

        val txTarget = mockNonContractTarget()

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
        val sourceAccount = mockSourceAccount()
        val txTarget = mockNonContractTarget()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
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
                    it.limits == null &&
                    it.validationState == ValidationState.UNINITIALISED &&
                    it.engineState.isEmpty()
            }
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Regular) }
            .assertNoErrors()
            .assertComplete()

        verify(sourceAccount, atLeastOnce()).currency
        verify(walletPreferences).getFeeTypeForAsset(ASSET)
        verify(currencyPrefs).selectedFiatCurrency

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `update amount modifies the pendingTx correctly for regular fees`() {
        // Arrange
        val totalBalance = 21.ether()
        val actionableBalance = 20.ether()
        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)
        val txTarget = mockNonContractTarget()

        withDefaultFeeOptions()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = CryptoValue.zero(ASSET),
            totalBalance = CryptoValue.zero(ASSET),
            availableBalance = CryptoValue.zero(ASSET),
            feeForFullAvailable = CryptoValue.zero(ASSET),
            feeAmount = CryptoValue.zero(ASSET),
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = FeeLevel.Regular,
                availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS,
                asset = CryptoCurrency.ETHER
            )
        )

        val inputAmount = 2.ether()
        val expectedFee = (GAS_LIMIT * FEE_REGULAR).gwei()
        val expectedAvailable = actionableBalance - expectedFee

        // Act
        subject.doUpdateAmount(
            inputAmount,
            pendingTx
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.amount == inputAmount &&
                    it.totalBalance == totalBalance &&
                    it.availableBalance == expectedAvailable &&
                    it.feeAmount == expectedFee
            }
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Regular) }

        verify(sourceAccount, atLeastOnce()).currency
        verify(sourceAccount).balanceRx()
        verify(txTarget).isContract
        verify(feeManager).ethFeeOptions
        verify(ethFeeOptions).gasLimit
        verify(ethFeeOptions).regularFee
        verify(ethFeeOptions, times(2)).priorityFee

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `contract target, update amount modifies the pendingTx correctly for regular fees`() {
        // Arrange
        val totalBalance = 21.ether()
        val actionableBalance = 20.ether()
        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)
        val txTarget = mockContractTarget()

        withDefaultFeeOptions()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = CryptoValue.zero(ASSET),
            totalBalance = CryptoValue.zero(ASSET),
            availableBalance = CryptoValue.zero(ASSET),
            feeForFullAvailable = CryptoValue.zero(ASSET),
            feeAmount = CryptoValue.zero(ASSET),
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = FeeLevel.Regular,
                availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS,
                asset = CryptoCurrency.ETHER
            )
        )

        val inputAmount = 2.ether()
        val expectedFee = (GAS_LIMIT_CONTRACT * FEE_REGULAR).gwei()
        val expectedAvailable = actionableBalance - expectedFee

        // Act
        subject.doUpdateAmount(
            inputAmount,
            pendingTx
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.amount == inputAmount &&
                    it.totalBalance == totalBalance &&
                    it.availableBalance == expectedAvailable &&
                    it.feeAmount == expectedFee
            }
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Regular) }

        verify(sourceAccount, atLeastOnce()).currency
        verify(sourceAccount).balanceRx()
        verify(txTarget).isContract
        verify(feeManager).ethFeeOptions
        verify(ethFeeOptions).gasLimitContract
        verify(ethFeeOptions).regularFee
        verify(ethFeeOptions, times(2)).priorityFee

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `update amount modifies the pendingTx correctly for priority fees`() {
        // Arrange
        val totalBalance = 21.ether()
        val actionableBalance = 20.ether()
        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)
        val txTarget = mockNonContractTarget()

        withDefaultFeeOptions()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = CryptoValue.zero(CryptoCurrency.ETHER),
            totalBalance = CryptoValue.zero(CryptoCurrency.ETHER),
            availableBalance = CryptoValue.zero(CryptoCurrency.ETHER),
            feeForFullAvailable = CryptoValue.zero(CryptoCurrency.ETHER),
            feeAmount = CryptoValue.zero(CryptoCurrency.ETHER),
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = FeeLevel.Priority,
                availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS,
                asset = CryptoCurrency.ETHER
            )
        )

        val inputAmount = 2.ether()
        val expectedFee = (GAS_LIMIT * FEE_PRIORITY).gwei()
        val expectedAvailable = actionableBalance - expectedFee
        val expectedFullFee = expectedFee

        // Act
        subject.doUpdateAmount(
            inputAmount,
            pendingTx
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.amount == inputAmount &&
                    it.totalBalance == totalBalance &&
                    it.availableBalance == expectedAvailable &&
                    it.feeForFullAvailable == expectedFullFee &&
                    it.feeAmount == expectedFee
            }
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Priority) }

        verify(sourceAccount, atLeastOnce()).currency
        verify(sourceAccount).balanceRx()
        verify(txTarget).isContract
        verify(feeManager).ethFeeOptions
        verify(ethFeeOptions).gasLimit
        verify(ethFeeOptions).regularFee
        verify(ethFeeOptions, times(2)).priorityFee

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `update fee level from REGULAR to PRIORITY updates the pendingTx correctly`() {
        // Arrange
        val totalBalance = 21.ether()
        val actionableBalance = 20.ether()
        val inputAmount = 2.ether()
        val regularFee = (GAS_LIMIT * FEE_REGULAR).gwei()
        val regularAvailable = actionableBalance - regularFee
        val fullFee = regularFee

        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)
        val txTarget = mockNonContractTarget()

        withDefaultFeeOptions()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val feeSelection = FeeSelection(
            selectedLevel = FeeLevel.Regular,
            availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS,
            asset = CryptoCurrency.ETHER
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = regularAvailable,
            feeForFullAvailable = fullFee,
            feeAmount = regularFee,
            selectedFiat = TEST_USER_FIAT,
            feeSelection = feeSelection
        )

        val expectedFee = (GAS_LIMIT * FEE_PRIORITY).gwei()
        val expectedAvailable = actionableBalance - expectedFee
        val expectedFullFee = expectedFee
        val expectedFeeSelection = feeSelection.copy(
            selectedLevel = FeeLevel.Priority,
            feesForLevels = mapOf(
                FeeLevel.None to CryptoValue.zero(CryptoCurrency.ETHER),
                FeeLevel.Regular to regularFee,
                FeeLevel.Priority to expectedFee,
                FeeLevel.Custom to expectedFee
            )
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.Priority,
            -1
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.amount == inputAmount &&
                    it.totalBalance == totalBalance &&
                    it.availableBalance == expectedAvailable &&
                    it.feeForFullAvailable == expectedFullFee &&
                    it.feeAmount == expectedFee &&
                    it.feeSelection == expectedFeeSelection
            }
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Priority) }

        verify(sourceAccount, atLeastOnce()).currency
        verify(sourceAccount).balanceRx()
        verify(txTarget).isContract
        verify(feeManager).ethFeeOptions
        verify(ethFeeOptions).gasLimit
        verify(ethFeeOptions).regularFee
        verify(ethFeeOptions, times(2)).priorityFee
        verify(walletPreferences).setFeeTypeForAsset(ASSET, FeeLevel.Priority.ordinal)

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `contract target, update fee level from REGULAR to PRIORITY updates the pendingTx correctly`() {
        // Arrange
        val totalBalance = 21.ether()
        val actionableBalance = 20.ether()
        val inputAmount = 2.ether()
        val regularFee = (GAS_LIMIT_CONTRACT * FEE_REGULAR).gwei()
        val regularAvailable = actionableBalance - regularFee
        val fullFee = regularFee

        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)
        val txTarget = mockContractTarget()

        withDefaultFeeOptions()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val feeSelection = FeeSelection(
            selectedLevel = FeeLevel.Regular,
            availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS,
            asset = CryptoCurrency.ETHER
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = regularAvailable,
            feeForFullAvailable = fullFee,
            feeAmount = regularFee,
            selectedFiat = TEST_USER_FIAT,
            feeSelection = feeSelection
        )

        val expectedFee = (GAS_LIMIT_CONTRACT * FEE_PRIORITY).gwei()
        val expectedAvailable = actionableBalance - expectedFee
        val expectedFullFee = expectedFee
        val expectedFeeSelection = feeSelection.copy(
            selectedLevel = FeeLevel.Priority,
            feesForLevels = mapOf(
                FeeLevel.None to CryptoValue.zero(CryptoCurrency.ETHER),
                FeeLevel.Regular to regularFee,
                FeeLevel.Priority to expectedFee,
                FeeLevel.Custom to expectedFee
            )
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.Priority,
            -1
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.amount == inputAmount &&
                    it.totalBalance == totalBalance &&
                    it.availableBalance == expectedAvailable &&
                    it.feeForFullAvailable == expectedFullFee &&
                    it.feeAmount == expectedFee &&
                    it.feeSelection == expectedFeeSelection
            }
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Priority) }

        verify(sourceAccount, atLeastOnce()).currency
        verify(sourceAccount).balanceRx()
        verify(txTarget).isContract
        verify(feeManager).ethFeeOptions
        verify(ethFeeOptions).gasLimitContract
        verify(ethFeeOptions).regularFee
        verify(ethFeeOptions, times(2)).priorityFee
        verify(walletPreferences).setFeeTypeForAsset(ASSET, FeeLevel.Priority.ordinal)

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `update fee level from REGULAR to NONE is rejected`() {
        // Arrange
        val totalBalance = 21.ether()
        val actionableBalance = 20.ether()
        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)
        val txTarget = mockNonContractTarget()

        withDefaultFeeOptions()

        val inputAmount = 2.ether()
        val priorityFee = (GAS_LIMIT * FEE_PRIORITY).gwei()
        val available = actionableBalance - priorityFee
        val fullFee = priorityFee

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = available,
            feeForFullAvailable = fullFee,
            feeAmount = priorityFee,
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = FeeLevel.Regular,
                availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS
            )
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.None,
            -1
        ).test()

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `update fee level from REGULAR to CUSTOM is rejected`() {
        // Arrange
        val totalBalance = 21.ether()
        val actionableBalance = 20.ether()
        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)
        val txTarget = mockNonContractTarget()

        withDefaultFeeOptions()

        val inputAmount = 2.ether()
        val priorityFee = (GAS_LIMIT * FEE_PRIORITY).gwei()
        val expectedAvailable = totalBalance - priorityFee
        val fullFee = priorityFee

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = expectedAvailable,
            feeForFullAvailable = fullFee,
            feeAmount = priorityFee,
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = FeeLevel.Regular,
                availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS
            )
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.Custom,
            100
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.amount == inputAmount &&
                    it.totalBalance == totalBalance &&
                    it.availableBalance == expectedAvailable &&
                    it.feeAmount == priorityFee
            }.assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Regular) }

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `update fee level from REGULAR to REGULAR has no effect`() {
        // Arrange
        val totalBalance = 21.ether()
        val actionableBalance = 20.ether()
        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)
        val txTarget = mockNonContractTarget()

        withDefaultFeeOptions()

        val inputAmount = 2.ether()
        val priorityFee = (GAS_LIMIT * FEE_PRIORITY).gwei()
        val available = totalBalance - priorityFee
        val fullFee = priorityFee

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = available,
            feeForFullAvailable = fullFee,
            feeAmount = priorityFee,
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = FeeLevel.Regular,
                availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS
            )
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.Regular,
            -1
        ).test()

        noMoreInteractions(sourceAccount, txTarget)
    }

    private fun mockSourceAccount(
        totalBalance: Money = CryptoValue.zero(ASSET),
        availableBalance: Money = CryptoValue.zero(ASSET)
    ) = mock<EthCryptoWalletAccount> {
        on { currency }.thenReturn(ASSET)
        on { balanceRx() }.thenReturn(
            Observable.just(
                AccountBalance(
                    total = totalBalance,
                    withdrawable = availableBalance,
                    pending = CryptoValue.zero(CryptoCurrency.ETHER),
                    exchangeRate = ExchangeRate.identityExchangeRate(CryptoCurrency.ETHER)
                )
            )
        )
    }

    private fun mockNonContractTarget() = mock<EthAddress> {
        on { asset }.thenReturn(ASSET)
        on { isContract }.thenReturn(false)
    }

    private fun mockContractTarget() = mock<EthAddress> {
        on { asset }.thenReturn(ASSET)
        on { isContract }.thenReturn(true)
    }

    private fun withDefaultFeeOptions() {
        whenever(ethFeeOptions.gasLimit).thenReturn(GAS_LIMIT)
        whenever(ethFeeOptions.priorityFee).thenReturn(FEE_PRIORITY)
        whenever(ethFeeOptions.regularFee).thenReturn(FEE_REGULAR)
        whenever(ethFeeOptions.gasLimitContract).thenReturn(GAS_LIMIT_CONTRACT)
        whenever(ethFeeOptions.limits).thenReturn(FeeLimits(FEE_REGULAR, FEE_PRIORITY))
    }

    private fun verifyFeeLevels(feeSelection: FeeSelection, expectedLevel: FeeLevel) =
        feeSelection.selectedLevel == expectedLevel &&
            feeSelection.availableLevels == EXPECTED_AVAILABLE_FEE_LEVELS &&
            feeSelection.availableLevels.contains(feeSelection.selectedLevel) &&
            feeSelection.asset == ASSET &&
            feeSelection.customAmount == -1L

    private fun noMoreInteractions(sourceAccount: BlockchainAccount, txTarget: TransactionTarget) {
        verifyNoMoreInteractions(txTarget)
        verifyNoMoreInteractions(ethDataManager)
        verifyNoMoreInteractions(feeManager)
        verifyNoMoreInteractions(ethFeeOptions)
        verifyNoMoreInteractions(walletPreferences)
        verifyNoMoreInteractions(sourceAccount)
        verifyNoMoreInteractions(exchangeRates)
        verifyNoMoreInteractions(currencyPrefs)
    }

    companion object {
        private val ASSET = CryptoCurrency.ETHER
        private val WRONG_ASSET = CryptoCurrency.BTC
        private const val GAS_LIMIT = 3000L
        private const val GAS_LIMIT_CONTRACT = 5000L
        private const val FEE_PRIORITY = 5L
        private const val FEE_REGULAR = 2L

        private val EXPECTED_AVAILABLE_FEE_LEVELS = setOf(FeeLevel.Regular, FeeLevel.Priority)
    }
}
