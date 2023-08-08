@file:Suppress("UnnecessaryVariable")

package com.blockchain.coincore.btc

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FeeSelection
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.testutil.CoincoreTestBase
import com.blockchain.core.chains.bitcoin.SendDataManager
import com.blockchain.core.fees.FeeDataManager
import com.blockchain.core.limits.TxLimits
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.satoshi
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.atMost
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import info.blockchain.wallet.api.data.FeeOptions
import info.blockchain.wallet.payload.data.XPub
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payload.model.Utxo
import info.blockchain.wallet.payment.OutputType
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.math.BigInteger
import kotlin.test.assertEquals
import org.bitcoinj.core.Coin
import org.bitcoinj.core.NetworkParameters
import org.junit.Before
import org.junit.Test

class BtcOnChainTxEngineTest : CoincoreTestBase() {

    private val btcDataManager: PayloadDataManager = mock {
        on { getAddressOutputType(TARGET_ADDRESS) }.thenReturn(OutputType.P2PKH)
        on { getXpubFormatOutputType(XPub.Format.LEGACY) }.thenReturn(OutputType.P2PKH)
        on { getXpubFormatOutputType(XPub.Format.SEGWIT) }.thenReturn(OutputType.P2WPKH)
    }
    private val sendDataManager: SendDataManager = mock()
    private val btcNetworkParams: NetworkParameters = mock()

    private val btcFeeOptions: FeeOptions = mock {
        on { regularFee }.thenReturn(FEE_REGULAR)
        on { priorityFee }.thenReturn(FEE_PRIORITY)
    }
    private val feeManager: FeeDataManager = mock {
        on { btcFeeOptions }.thenReturn(Observable.just(btcFeeOptions))
    }

    private val walletPreferences: WalletStatusPrefs = mock {
        on { getFeeTypeForAsset(ASSET) }.thenReturn(FeeLevel.Regular.ordinal)
    }

    private val subject = BtcOnChainTxEngine(
        btcDataManager = btcDataManager,
        sendDataManager = sendDataManager,
        feeManager = feeManager,
        requireSecondPassword = false,
        walletPreferences = walletPreferences,
        resolvedAddress = mock()
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
        verify(sourceAccount, atLeastOnce()).currency
        verify(txTarget).asset

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test(expected = IllegalStateException::class)
    fun `inputs fail validation when source Asset incorrect`() {
        val sourceAccount: BtcCryptoWalletAccount = mock {
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
        verify(sourceAccount, atLeastOnce()).currency
        verify(txTarget).asset

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
        verify(sourceAccount, atLeastOnce()).currency

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
                    it.feeAmount == CryptoValue.zero(ASSET) &&
                    it.selectedFiat == TEST_USER_FIAT &&
                    it.txConfirmations.isEmpty() &&
                    it.limits == TxLimits.fromAmounts(
                    Money.fromMinor(
                        ASSET,
                        BigInteger.valueOf(Coin.parseCoin("0.000005460").longValue())
                    ),
                    Money.fromMinor(
                        ASSET,
                        2_100_000_000_000_000L.toBigInteger()
                    )
                ) &&
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
        val inputAmount = 2.bitcoin()
        val feePerKb = (FEE_REGULAR * 1000).satoshi()
        val feePerKbPriority = (FEE_PRIORITY * 1000).satoshi()
        val totalFee = (FEE_REGULAR * 1000 * 3).satoshi()

        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
            on { address }.thenReturn(TARGET_ADDRESS)
        }

        val totalBalance = 21.bitcoin()
        val actionableBalance = 19.bitcoin()
        val totalSweepable = totalBalance - totalFee

        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)

        val unspentOutputs = listOf<Utxo>(mock(), mock())
        whenever(sendDataManager.getUnspentBtcOutputs(SOURCE_XPUBS))
            .thenReturn(Single.just(unspentOutputs))

        whenever(
            sendDataManager.getMaximumAvailable(
                ASSET,
                unspentOutputs,
                TARGET_OUTPUT_TYPE,
                feePerKb
            )
        ).thenReturn(
            SendDataManager.MaxAvailable(
                totalSweepable as CryptoValue,
                (totalBalance - totalFee) as CryptoValue
            )
        )

        val utxoBundle: SpendableUnspentOutputs = mock {
            on { absoluteFee }.thenReturn(totalFee.toBigInteger())
        }
        val utxoBundlePriority: SpendableUnspentOutputs = mock {
            on { absoluteFee }.thenReturn(feePerKbPriority.toBigInteger())
        }

        whenever(
            sendDataManager.getSpendableCoins(
                unspentOutputs,
                TARGET_OUTPUT_TYPE,
                CHANGE_OUTPUT_TYPE,
                inputAmount,
                feePerKb
            )
        ).thenReturn(utxoBundle)

        whenever(
            sendDataManager.getSpendableCoins(
                unspentOutputs,
                TARGET_OUTPUT_TYPE,
                CHANGE_OUTPUT_TYPE,
                inputAmount,
                feePerKbPriority
            )
        ).thenReturn(utxoBundlePriority)

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
                asset = CryptoCurrency.BTC
            )
        )

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
                    it.availableBalance == totalSweepable &&
                    it.feeAmount == totalFee
            }
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Regular) }

        verify(txTarget, atMost(2)).address
        verify(sourceAccount, atLeastOnce()).currency
        verify(sourceAccount, atMost(2)).xpubs
        verify(sourceAccount, atLeastOnce()).balanceRx()
        verify(btcDataManager, atMost(2)).getAddressOutputType(TARGET_ADDRESS)
        verify(btcDataManager, atLeastOnce()).getXpubFormatOutputType(XPub.Format.LEGACY)
        verify(feeManager).btcFeeOptions
        verify(btcFeeOptions, atLeastOnce()).priorityFee
        verify(btcFeeOptions, atLeastOnce()).regularFee
        verify(sendDataManager).getUnspentBtcOutputs(SOURCE_XPUBS)
        verify(sendDataManager).getMaximumAvailable(ASSET, unspentOutputs, TARGET_OUTPUT_TYPE, feePerKb)
        verify(utxoBundlePriority).absoluteFee
        verify(utxoBundle, times(2)).absoluteFee
        verify(sendDataManager)
            .getSpendableCoins(unspentOutputs, TARGET_OUTPUT_TYPE, CHANGE_OUTPUT_TYPE, inputAmount, feePerKb)
        verify(sendDataManager)
            .getSpendableCoins(unspentOutputs, TARGET_OUTPUT_TYPE, CHANGE_OUTPUT_TYPE, inputAmount, feePerKbPriority)

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `update amount modifies the pendingTx correctly for priority fees`() {
        // Arrange
        val inputAmount = 2.bitcoin()
        val feePerKbRegular = (FEE_REGULAR * 1000).satoshi()
        val feePerKb = (FEE_PRIORITY * 1000).satoshi()
        val totalFee = (FEE_REGULAR * 1000 * 3).satoshi()

        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
            on { address }.thenReturn(TARGET_ADDRESS)
        }

        val totalBalance = 21.bitcoin()
        val actionableBalance = 19.bitcoin()
        val totalSweepable = totalBalance - totalFee
        val fullFee = totalBalance - actionableBalance

        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)

        val unspentOutputs = listOf<Utxo>(mock(), mock())
        whenever(sendDataManager.getUnspentBtcOutputs(SOURCE_XPUBS))
            .thenReturn(Single.just(unspentOutputs))

        whenever(
            sendDataManager.getMaximumAvailable(
                ASSET,
                unspentOutputs,
                TARGET_OUTPUT_TYPE,
                feePerKb
            )
        ).thenReturn(
            SendDataManager.MaxAvailable(
                totalSweepable as CryptoValue,
                fullFee as CryptoValue
            )
        )

        val utxoBundleRegular: SpendableUnspentOutputs = mock {
            on { absoluteFee }.thenReturn(feePerKbRegular.toBigInteger())
        }

        val utxoBundle: SpendableUnspentOutputs = mock {
            on { absoluteFee }.thenReturn(totalFee.toBigInteger())
        }

        whenever(
            sendDataManager.getSpendableCoins(
                unspentOutputs,
                TARGET_OUTPUT_TYPE,
                CHANGE_OUTPUT_TYPE,
                inputAmount,
                feePerKbRegular
            )
        ).thenReturn(utxoBundleRegular)

        whenever(
            sendDataManager.getSpendableCoins(
                unspentOutputs,
                TARGET_OUTPUT_TYPE,
                CHANGE_OUTPUT_TYPE,
                inputAmount,
                feePerKb
            )
        ).thenReturn(utxoBundle)

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
                selectedLevel = FeeLevel.Priority,
                availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS,
                asset = CryptoCurrency.BTC
            )
        )

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
                    it.availableBalance == totalSweepable &&
                    it.feeAmount == totalFee
            }
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Priority) }

        verify(txTarget, atMost(2)).address
        verify(sourceAccount, atLeastOnce()).currency
        verify(sourceAccount, atMost(2)).xpubs
        verify(sourceAccount, atLeastOnce()).balanceRx()
        verify(btcDataManager, atMost(2)).getAddressOutputType(TARGET_ADDRESS)
        verify(btcDataManager, atLeastOnce()).getXpubFormatOutputType(XPub.Format.LEGACY)
        verify(feeManager).btcFeeOptions
        verify(btcFeeOptions, atLeastOnce()).regularFee
        verify(btcFeeOptions, atLeastOnce()).priorityFee
        verify(sendDataManager).getUnspentBtcOutputs(SOURCE_XPUBS)
        verify(sendDataManager).getMaximumAvailable(ASSET, unspentOutputs, TARGET_OUTPUT_TYPE, feePerKb)
        verify(utxoBundleRegular).absoluteFee
        verify(utxoBundle, times(2)).absoluteFee
        verify(sendDataManager)
            .getSpendableCoins(unspentOutputs, TARGET_OUTPUT_TYPE, CHANGE_OUTPUT_TYPE, inputAmount, feePerKbRegular)
        verify(sendDataManager)
            .getSpendableCoins(unspentOutputs, TARGET_OUTPUT_TYPE, CHANGE_OUTPUT_TYPE, inputAmount, feePerKb)

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `update fee level from REGULAR to PRIORITY updates the pendingTx correctly`() {
        // Arrange
        val inputAmount = 2.bitcoin()
        val regularFee = (FEE_REGULAR * 1000 * 3).satoshi()
        val totalBalance = 21.bitcoin()
        val actionableBalance = 19.bitcoin()
        val regularSweepable = totalBalance - regularFee
        val fullFee = totalBalance - actionableBalance

        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)

        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
            on { address }.thenReturn(TARGET_ADDRESS)
        }

        val unspentOutputs = listOf<Utxo>(mock(), mock())
        whenever(sendDataManager.getUnspentBtcOutputs(SOURCE_XPUBS))
            .thenReturn(Single.just(unspentOutputs))

        val feePerKbRegular = (FEE_REGULAR * 1000).satoshi()
        val feePerKb = (FEE_PRIORITY * 1000).satoshi()
        val priorityFee = (FEE_PRIORITY * 1000 * 3).satoshi()
        val prioritySweepable = totalBalance - priorityFee

        whenever(
            sendDataManager.getMaximumAvailable(
                ASSET,
                unspentOutputs,
                TARGET_OUTPUT_TYPE,
                feePerKb
            )
        ).thenReturn(
            SendDataManager.MaxAvailable(
                prioritySweepable as CryptoValue,
                priorityFee
            )
        )

        val utxoBundleRegular: SpendableUnspentOutputs = mock {
            on { absoluteFee }.thenReturn(regularFee.toBigInteger())
        }
        val utxoBundle: SpendableUnspentOutputs = mock {
            on { absoluteFee }.thenReturn(priorityFee.toBigInteger())
        }

        whenever(
            sendDataManager.getSpendableCoins(
                unspentOutputs,
                TARGET_OUTPUT_TYPE,
                CHANGE_OUTPUT_TYPE,
                inputAmount,
                feePerKbRegular
            )
        ).thenReturn(utxoBundleRegular)

        whenever(
            sendDataManager.getSpendableCoins(
                unspentOutputs,
                TARGET_OUTPUT_TYPE,
                CHANGE_OUTPUT_TYPE,
                inputAmount,
                feePerKb
            )
        ).thenReturn(utxoBundle)

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = regularSweepable,
            feeForFullAvailable = fullFee,
            feeAmount = regularFee,
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = FeeLevel.Regular,
                availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS,
                asset = CryptoCurrency.BTC
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
                    it.availableBalance == prioritySweepable &&
                    it.feeForFullAvailable == priorityFee &&
                    it.feeAmount == priorityFee
            }
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Priority) }

        verify(txTarget, atMost(2)).address
        verify(sourceAccount, atLeastOnce()).currency
        verify(sourceAccount, atMost(2)).xpubs
        verify(sourceAccount, atLeastOnce()).balanceRx()
        verify(btcDataManager, atMost(2)).getAddressOutputType(TARGET_ADDRESS)
        verify(btcDataManager, atLeastOnce()).getXpubFormatOutputType(XPub.Format.LEGACY)
        verify(feeManager).btcFeeOptions
        verify(btcFeeOptions, atLeastOnce()).regularFee
        verify(btcFeeOptions, atLeastOnce()).priorityFee
        verify(sendDataManager).getUnspentBtcOutputs(SOURCE_XPUBS)
        verify(sendDataManager).getMaximumAvailable(ASSET, unspentOutputs, TARGET_OUTPUT_TYPE, feePerKb)
        verify(utxoBundleRegular).absoluteFee
        verify(utxoBundle, times(2)).absoluteFee
        verify(sendDataManager)
            .getSpendableCoins(unspentOutputs, TARGET_OUTPUT_TYPE, CHANGE_OUTPUT_TYPE, inputAmount, feePerKbRegular)
        verify(sendDataManager)
            .getSpendableCoins(unspentOutputs, TARGET_OUTPUT_TYPE, CHANGE_OUTPUT_TYPE, inputAmount, feePerKb)
        verify(walletPreferences).setFeeTypeForAsset(ASSET, FeeLevel.Priority.ordinal)

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `update fee level from REGULAR to NONE is rejected`() {
        // Arrange
        val inputAmount = 2.bitcoin()
        val regularFee = (FEE_REGULAR * 1000 * 3).satoshi()
        val totalBalance = 21.bitcoin()
        val actionableBalance = 19.bitcoin()
        val regularSweepable = totalBalance - regularFee

        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)

        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
            on { address }.thenReturn(TARGET_ADDRESS)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = regularSweepable,
            feeForFullAvailable = regularFee,
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

    @Test
    fun `update fee level from REGULAR to REGULAR has no effect`() {
        // Arrange
        val inputAmount = 2.bitcoin()
        val regularFee = (FEE_REGULAR * 1000 * 3).satoshi()
        val totalBalance = 21.bitcoin()
        val actionableBalance = 19.bitcoin()
        val regularSweepable = totalBalance - regularFee

        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)

        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
            on { address }.thenReturn(TARGET_ADDRESS)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = regularSweepable,
            feeForFullAvailable = regularFee,
            feeAmount = regularFee,
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = FeeLevel.Regular,
                availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS,
                asset = CryptoCurrency.BTC
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
                    it.availableBalance == regularSweepable &&
                    it.feeAmount == regularFee
            }
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Regular) }

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `update fee level from REGULAR to CUSTOM updates the pendingTx correctly`() {
        // Arrange
        val inputAmount = 2.bitcoin()
        val regularFee = (FEE_REGULAR * 1000).satoshi()
        val priorityFee = (FEE_PRIORITY * 1000).satoshi()
        val totalBalance = 21.bitcoin()
        val actionableBalance = 19.bitcoin()
        val regularSweepable = totalBalance - regularFee
        val fullFee = regularFee

        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)

        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
            on { address }.thenReturn(TARGET_ADDRESS)
        }

        val unspentOutputs = listOf<Utxo>(mock(), mock())
        whenever(sendDataManager.getUnspentBtcOutputs(SOURCE_XPUBS))
            .thenReturn(Single.just(unspentOutputs))

        val feeCustom = 7L
        val feePerKb = (feeCustom * 1000).satoshi()
        val expectedFee = (feeCustom * 1000 * 3).satoshi()
        val expectedSweepable = totalBalance - expectedFee

        whenever(
            sendDataManager.getMaximumAvailable(
                ASSET,
                unspentOutputs,
                TARGET_OUTPUT_TYPE,
                feePerKb
            )
        ).thenReturn(
            SendDataManager.MaxAvailable(
                expectedSweepable as CryptoValue,
                expectedFee
            )
        )

        val utxoBundleRegular: SpendableUnspentOutputs = mock {
            on { absoluteFee }.thenReturn(regularFee.toBigInteger())
        }
        val utxoBundlePriority: SpendableUnspentOutputs = mock {
            on { absoluteFee }.thenReturn(priorityFee.toBigInteger())
        }
        val utxoBundle: SpendableUnspentOutputs = mock {
            on { absoluteFee }.thenReturn(expectedFee.toBigInteger())
        }

        whenever(
            sendDataManager.getSpendableCoins(
                unspentOutputs,
                TARGET_OUTPUT_TYPE,
                CHANGE_OUTPUT_TYPE,
                inputAmount,
                regularFee
            )
        ).thenReturn(utxoBundleRegular)

        whenever(
            sendDataManager.getSpendableCoins(
                unspentOutputs,
                TARGET_OUTPUT_TYPE,
                CHANGE_OUTPUT_TYPE,
                inputAmount,
                priorityFee
            )
        ).thenReturn(utxoBundlePriority)

        whenever(
            sendDataManager.getSpendableCoins(
                unspentOutputs,
                TARGET_OUTPUT_TYPE,
                CHANGE_OUTPUT_TYPE,
                inputAmount,
                feePerKb
            )
        ).thenReturn(utxoBundle)

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = regularSweepable,
            feeForFullAvailable = fullFee,
            feeAmount = regularFee,
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = FeeLevel.Regular,
                availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS,
                asset = CryptoCurrency.BTC
            )
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.Custom,
            feeCustom
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.amount == inputAmount &&
                    it.totalBalance == totalBalance &&
                    it.availableBalance == expectedSweepable &&
                    it.feeForFullAvailable == expectedFee &&
                    it.feeAmount == expectedFee
            }
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Custom, feeCustom) }

        verify(txTarget, atMost(2)).address
        verify(sourceAccount, atLeastOnce()).currency
        verify(sourceAccount, atLeastOnce()).balanceRx()
        verify(sourceAccount, atMost(2)).xpubs
        verify(btcDataManager, atMost(2)).getAddressOutputType(TARGET_ADDRESS)
        verify(btcDataManager, atLeastOnce()).getXpubFormatOutputType(XPub.Format.LEGACY)
        verify(feeManager).btcFeeOptions
        verify(btcFeeOptions, atLeastOnce()).regularFee
        verify(btcFeeOptions, atLeastOnce()).priorityFee
        verify(btcFeeOptions, atLeastOnce()).limits
        verify(sendDataManager).getUnspentBtcOutputs(SOURCE_XPUBS)
        verify(sendDataManager).getMaximumAvailable(ASSET, unspentOutputs, TARGET_OUTPUT_TYPE, feePerKb)
        verify(utxoBundleRegular).absoluteFee
        verify(utxoBundlePriority).absoluteFee
        verify(utxoBundle).absoluteFee
        verify(sendDataManager)
            .getSpendableCoins(unspentOutputs, TARGET_OUTPUT_TYPE, CHANGE_OUTPUT_TYPE, inputAmount, regularFee)
        verify(sendDataManager)
            .getSpendableCoins(unspentOutputs, TARGET_OUTPUT_TYPE, CHANGE_OUTPUT_TYPE, inputAmount, priorityFee)
        verify(sendDataManager)
            .getSpendableCoins(unspentOutputs, TARGET_OUTPUT_TYPE, CHANGE_OUTPUT_TYPE, inputAmount, feePerKb)
        verify(walletPreferences).setFeeTypeForAsset(ASSET, FeeLevel.Custom.ordinal)

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `changing the custom fee level updates the pendingTx correctly`() {
        // Arrange
        val inputAmount = 2.bitcoin()
        val totalBalance = 21.bitcoin()
        val actionableBalance = 19.bitcoin()

        val initialCustomFee = 7L
        val initialFee = (initialCustomFee * 1000 * 3).satoshi()
        val customSweepable = totalBalance - initialFee
        val fullFee = initialFee

        val sourceAccount = mockSourceAccount(totalBalance, actionableBalance)

        val txTarget: CryptoAddress = mock {
            on { asset }.thenReturn(ASSET)
            on { address }.thenReturn(TARGET_ADDRESS)
        }

        val unspentOutputs = listOf<Utxo>(mock(), mock())
        whenever(sendDataManager.getUnspentBtcOutputs(SOURCE_XPUBS))
            .thenReturn(Single.just(unspentOutputs))

        val feeCustom = 15L
        val feePerKb = (feeCustom * 1000).satoshi()
        val feePerKbRegular = (FEE_REGULAR * 1000).satoshi()
        val feePerKbPriority = (FEE_PRIORITY * 1000).satoshi()
        val expectedFee = (feeCustom * 1000 * 3).satoshi()
        val expectedSweepable = totalBalance - expectedFee

        whenever(
            sendDataManager.getMaximumAvailable(
                ASSET,
                unspentOutputs,
                TARGET_OUTPUT_TYPE,
                feePerKb
            )
        ).thenReturn(
            SendDataManager.MaxAvailable(
                expectedSweepable as CryptoValue,
                expectedFee
            )
        )

        val utxoBundle: SpendableUnspentOutputs = mock {
            on { absoluteFee }.thenReturn(expectedFee.toBigInteger())
        }

        val utxoBundleRegular: SpendableUnspentOutputs = mock {
            on { absoluteFee }.thenReturn(feePerKbRegular.toBigInteger())
        }

        val utxoBundlePriority: SpendableUnspentOutputs = mock {
            on { absoluteFee }.thenReturn(feePerKbPriority.toBigInteger())
        }

        whenever(
            sendDataManager.getSpendableCoins(
                unspentOutputs,
                TARGET_OUTPUT_TYPE,
                CHANGE_OUTPUT_TYPE,
                inputAmount,
                feePerKbRegular
            )
        ).thenReturn(utxoBundleRegular)

        whenever(
            sendDataManager.getSpendableCoins(
                unspentOutputs,
                TARGET_OUTPUT_TYPE,
                CHANGE_OUTPUT_TYPE,
                inputAmount,
                feePerKbPriority
            )
        ).thenReturn(utxoBundlePriority)

        whenever(
            sendDataManager.getSpendableCoins(
                unspentOutputs,
                TARGET_OUTPUT_TYPE,
                CHANGE_OUTPUT_TYPE,
                inputAmount,
                feePerKb
            )
        ).thenReturn(utxoBundle)

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = customSweepable,
            feeForFullAvailable = fullFee,
            feeAmount = initialFee,
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = FeeLevel.Custom,
                customAmount = initialCustomFee,
                availableLevels = EXPECTED_AVAILABLE_FEE_LEVELS,
                asset = CryptoCurrency.BTC
            )
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.Custom,
            feeCustom
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.amount == inputAmount &&
                    it.totalBalance == totalBalance &&
                    it.availableBalance == expectedSweepable &&
                    it.feeForFullAvailable == expectedFee &&
                    it.feeAmount == expectedFee
            }
            .assertValue { verifyFeeLevels(it.feeSelection, FeeLevel.Custom, feeCustom) }

        verify(txTarget, atMost(2)).address
        verify(sourceAccount, atLeastOnce()).currency
        verify(sourceAccount, atLeastOnce()).balanceRx()
        verify(sourceAccount, atMost(2)).xpubs
        verify(btcDataManager, atMost(2)).getAddressOutputType(TARGET_ADDRESS)
        verify(btcDataManager, atLeastOnce()).getXpubFormatOutputType(XPub.Format.LEGACY)
        verify(feeManager).btcFeeOptions
        verify(btcFeeOptions, atLeastOnce()).regularFee
        verify(btcFeeOptions, atLeastOnce()).priorityFee
        verify(btcFeeOptions, atLeastOnce()).limits
        verify(sendDataManager).getUnspentBtcOutputs(SOURCE_XPUBS)
        verify(sendDataManager).getMaximumAvailable(ASSET, unspentOutputs, TARGET_OUTPUT_TYPE, feePerKb)
        verify(utxoBundle).absoluteFee
        verify(utxoBundleRegular).absoluteFee
        verify(utxoBundlePriority).absoluteFee
        verify(sendDataManager)
            .getSpendableCoins(unspentOutputs, TARGET_OUTPUT_TYPE, CHANGE_OUTPUT_TYPE, inputAmount, feePerKbRegular)
        verify(sendDataManager)
            .getSpendableCoins(unspentOutputs, TARGET_OUTPUT_TYPE, CHANGE_OUTPUT_TYPE, inputAmount, feePerKbPriority)
        verify(sendDataManager)
            .getSpendableCoins(unspentOutputs, TARGET_OUTPUT_TYPE, CHANGE_OUTPUT_TYPE, inputAmount, feePerKb)
        verify(walletPreferences).setFeeTypeForAsset(ASSET, FeeLevel.Custom.ordinal)

        noMoreInteractions(sourceAccount, txTarget)
    }

    private fun verifyFeeLevels(feeSelection: FeeSelection, expectedLevel: FeeLevel, customFee: Long = -1) =
        feeSelection.selectedLevel == expectedLevel &&
            feeSelection.availableLevels == EXPECTED_AVAILABLE_FEE_LEVELS &&
            feeSelection.availableLevels.contains(feeSelection.selectedLevel) &&
            feeSelection.customAmount == customFee &&
            feeSelection.asset == CryptoCurrency.BTC

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
        on { xpubs }.thenReturn(SOURCE_XPUBS)
    }

    private fun noMoreInteractions(sourceAccount: BlockchainAccount, txTarget: TransactionTarget) {
        verifyNoMoreInteractions(txTarget)
        verifyNoMoreInteractions(btcDataManager)
        verifyNoMoreInteractions(sendDataManager)
        verifyNoMoreInteractions(btcNetworkParams)
        verifyNoMoreInteractions(feeManager)
        verifyNoMoreInteractions(btcFeeOptions)
        verifyNoMoreInteractions(walletPreferences)
        verifyNoMoreInteractions(sourceAccount)
        verifyNoMoreInteractions(exchangeRates)
        verifyNoMoreInteractions(currencyPrefs)
    }

    companion object {
        private val ASSET = CryptoCurrency.BTC
        private val WRONG_ASSET = CryptoCurrency.ETHER
        private const val SOURCE_XPUB = "VALID_BTC_XPUB"
        private val SOURCE_XPUBS = XPubs(
            XPub(address = "VALID_BTC_XPUB", derivation = XPub.Format.LEGACY)
        )
        private const val TARGET_ADDRESS = "VALID_BTC_ADDRESS"
        private const val FEE_REGULAR = 5L
        private const val FEE_PRIORITY = 11L

        private val TARGET_OUTPUT_TYPE = OutputType.P2PKH
        private val CHANGE_OUTPUT_TYPE = OutputType.P2PKH

        private val EXPECTED_AVAILABLE_FEE_LEVELS = setOf(FeeLevel.Regular, FeeLevel.Priority, FeeLevel.Custom)
    }
}
