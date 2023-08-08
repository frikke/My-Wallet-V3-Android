package com.blockchain.coincore.erc20

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FeeSelection
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.testutil.CoincoreTestBase
import com.blockchain.core.chains.erc20.Erc20DataManager
import com.blockchain.core.fees.FeeDataManager
import com.blockchain.data.DataResource
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.testutils.gwei
import com.blockchain.testutils.numberToBigDecimal
import com.blockchain.unifiedcryptowallet.domain.balances.NetworkBalance
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CoinNetwork
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import info.blockchain.wallet.api.data.FeeLimits
import info.blockchain.wallet.api.data.FeeOptions
import io.mockk.coEvery
import io.reactivex.rxjava3.core.Observable
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Test

@Suppress("UnnecessaryVariable")
class Erc20OnChainTxEngineTest : CoincoreTestBase() {

    private val erc20DataManager: Erc20DataManager = mock()
    private val ethFeeOptions: FeeOptions = mock()

    private val feeManager: FeeDataManager = mock {
        on { getErc20FeeOptions("ETH", CONTRACT_ADDRESS) }.thenReturn(Observable.just(ethFeeOptions))
    }
    private val walletPreferences: WalletStatusPrefs = mock {
        on { getFeeTypeForAsset(ASSET) }.thenReturn(FeeLevel.Regular.ordinal)
    }

    private lateinit var subject: Erc20OnChainTxEngine

    @Before
    fun setup() {
        initMocks()
        subject = Erc20OnChainTxEngine(
            erc20DataManager = erc20DataManager,
            feeManager = feeManager,
            requireSecondPassword = false,
            walletPreferences = walletPreferences,
            resolvedAddress = mock()
        )
        coEvery {
            (unifiedBalancesService.balances(anyOrNull(), any()))
        }.returns(
            flowOf(
                DataResource.Data(
                    listOf(
                        NetworkBalance(
                            currency = CryptoCurrency.ETHER,
                            balance = Money.fromMinor(CryptoCurrency.ETHER, 11111.toBigInteger()),
                            unconfirmedBalance = Money.fromMinor(CryptoCurrency.ETHER, 11111.toBigInteger()),
                            exchangeRate = ExchangeRate.identityExchangeRate(CryptoCurrency.ETHER)
                        )
                    )
                )
            )
        )
        whenever(assetCatalogue.assetInfoFromNetworkTicker("ETH")).thenReturn(
            CryptoCurrency.ETHER
        )
    }

    @Test
    fun `inputs validate when correct`() {
        val sourceAccount = mockSourceAccount()
        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
            on { address }.thenReturn(TARGET_ADDRESS)
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
        verify(txTarget).address

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test(expected = IllegalStateException::class)
    fun `inputs fail validation when source Asset incorrect`() {
        val sourceAccount = mock<Erc20NonCustodialAccount>() {
            on { currency }.thenReturn(WRONG_ASSET)
        }
        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
            on { address }.thenReturn(TARGET_ADDRESS)
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
        val sourceAccount = mockSourceAccount()
        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

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
                    it.feeAmount == CryptoValue.zero(CryptoCurrency.ETHER) &&
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
        val totalBalance = 21.dummies()
        val actionableBalance = 20.dummies()
        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)

        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

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

        val inputAmount = 2.dummies()
        val expectedFee = (GAS_LIMIT_CONTRACT * FEE_REGULAR).gwei()
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
                    it.availableBalance == actionableBalance &&
                    it.feeForFullAvailable == expectedFullFee &&
                    it.feeAmount == expectedFee
            }
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Regular) }

        verify(sourceAccount, atLeastOnce()).currency
        verify(sourceAccount).balanceRx()
        verify(feeManager).getErc20FeeOptions("ETH", CONTRACT_ADDRESS)
        verify(ethFeeOptions).gasLimitContract
        verify(ethFeeOptions).regularFee
        verify(ethFeeOptions, times(2)).priorityFee

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `update amount modifies the pendingTx correctly for priority fees`() {
        // Arrange
        val totalBalance = 21.dummies()
        val actionableBalance = 20.dummies()
        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)

        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

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

        val inputAmount = 2.dummies()
        val expectedFee = (GAS_LIMIT_CONTRACT * FEE_PRIORITY).gwei()

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
                    it.availableBalance == actionableBalance &&
                    it.feeAmount == expectedFee
            }
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Priority) }

        verify(sourceAccount, atLeastOnce()).currency
        verify(sourceAccount).balanceRx()
        verify(feeManager).getErc20FeeOptions("ETH", CONTRACT_ADDRESS)
        verify(ethFeeOptions).gasLimitContract
        verify(ethFeeOptions).regularFee
        verify(ethFeeOptions, times(2)).priorityFee

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `update fee level from REGULAR to PRIORITY updates the pendingTx correctly`() {
        // Arrange
        val totalBalance = 21.dummies()
        val availableBalance = 20.dummies()

        val inputAmount = 2.dummies()
        val regularFee = (GAS_LIMIT_CONTRACT * FEE_REGULAR).gwei()

        val sourceAccount = mockSourceAccount(totalBalance, availableBalance)

        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

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
            availableBalance = availableBalance,
            feeForFullAvailable = regularFee,
            feeAmount = regularFee,
            selectedFiat = TEST_USER_FIAT,
            feeSelection = feeSelection
        )

        val expectedFee = (GAS_LIMIT_CONTRACT * FEE_PRIORITY).gwei()
        val fullFee = expectedFee

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
                    it.availableBalance == availableBalance &&
                    it.feeForFullAvailable == fullFee &&
                    it.feeAmount == expectedFee &&
                    it.feeSelection == expectedFeeSelection
            }
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Priority) }

        verify(sourceAccount, atLeastOnce()).currency
        verify(sourceAccount).balanceRx()
        verify(feeManager).getErc20FeeOptions("ETH", CONTRACT_ADDRESS)
        verify(ethFeeOptions).gasLimitContract
        verify(ethFeeOptions, times(2)).priorityFee
        verify(ethFeeOptions).regularFee
        verify(walletPreferences).setFeeTypeForAsset(ASSET, FeeLevel.Priority.ordinal)

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `update fee level from REGULAR to NONE is rejected`() {
        // Arrange
        val totalBalance = 21.dummies()
        val availableBalance = 20.dummies()
        val inputAmount = 2.dummies()
        val regularFee = (GAS_LIMIT_CONTRACT * FEE_REGULAR).gwei()
        val fullFee = regularFee

        val sourceAccount = mockSourceAccount(totalBalance, availableBalance)

        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

        withDefaultFeeOptions()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = availableBalance,
            feeForFullAvailable = fullFee,
            feeAmount = regularFee,
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
        val totalBalance = 21.dummies()
        val availableBalance = 20.dummies()
        val inputAmount = 2.dummies()
        val regularFee = (GAS_LIMIT_CONTRACT * FEE_REGULAR).gwei()
        val fullFee = regularFee

        val sourceAccount = mockSourceAccount(totalBalance, availableBalance)

        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

        withDefaultFeeOptions()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = availableBalance,
            feeForFullAvailable = fullFee,
            feeAmount = regularFee,
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

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `update fee level from REGULAR to REGULAR has no effect`() {
        // Arrange
        val totalBalance = 21.dummies()
        val availableBalance = 20.dummies()
        val inputAmount = 2.dummies()
        val regularFee = (GAS_LIMIT_CONTRACT * FEE_REGULAR).gwei()
        val fullFee = regularFee

        val sourceAccount = mockSourceAccount(totalBalance, availableBalance)

        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
        }

        withDefaultFeeOptions()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = availableBalance,
            feeForFullAvailable = fullFee,
            feeAmount = regularFee,
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = FeeLevel.Regular,
                availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS,
                asset = CryptoCurrency.ETHER
            )
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.Regular,
            -1
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.amount == inputAmount &&
                    it.totalBalance == totalBalance &&
                    it.availableBalance == availableBalance &&
                    it.feeAmount == regularFee
            }
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Regular) }

        noMoreInteractions(sourceAccount, txTarget)
    }

    private fun mockSourceAccount(
        totalBalance: Money = CryptoValue.zero(ASSET),
        availableBalance: Money = CryptoValue.zero(ASSET)
    ) = mock<Erc20NonCustodialAccount> {
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
            feeSelection.asset == CryptoCurrency.ETHER &&
            feeSelection.customAmount == -1L

    private fun noMoreInteractions(sourceAccount: BlockchainAccount, txTarget: TransactionTarget) {
        verifyNoMoreInteractions(txTarget)
        verifyNoMoreInteractions(erc20DataManager)
        verifyNoMoreInteractions(feeManager)
        verifyNoMoreInteractions(ethFeeOptions)
        verifyNoMoreInteractions(walletPreferences)
        verifyNoMoreInteractions(sourceAccount)
        verifyNoMoreInteractions(exchangeRates)
        verifyNoMoreInteractions(currencyPrefs)
    }

    companion object {
        private const val CONTRACT_ADDRESS = "0x123456545654656"
        val coinNetwork: CoinNetwork = mock {
            on { networkTicker }.thenReturn("ETH")
            on { nativeAssetTicker }.thenReturn("ETH")
        }

        @Suppress("ClassName")
        private object DUMMY_ERC20 : CryptoCurrency(
            networkTicker = "DUMMY",
            displayTicker = "DUMMY",
            name = "Dummies",
            categories = setOf(AssetCategory.TRADING, AssetCategory.NON_CUSTODIAL),
            precisionDp = 8,
            coinNetwork = coinNetwork,
            l2identifier = CONTRACT_ADDRESS,
            requiredConfirmations = 5,
            colour = "#123456"
        )

        fun Number.dummies() = CryptoValue.fromMajor(DUMMY_ERC20, numberToBigDecimal())

        private val ASSET = DUMMY_ERC20
        private val WRONG_ASSET = CryptoCurrency.BTC
        private const val TARGET_ADDRESS = "VALID_PAX_ADDRESS"
        private const val GAS_LIMIT = 3000L
        private const val GAS_LIMIT_CONTRACT = 5000L
        private const val FEE_PRIORITY = 5L
        private const val FEE_REGULAR = 2L

        private val EXPECTED_AVAILABLE_FEE_LEVELS = setOf(FeeLevel.Regular, FeeLevel.Priority)
    }
}
