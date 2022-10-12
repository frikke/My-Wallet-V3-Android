package com.blockchain.coincore.impl.txEngine

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.FeeInfo
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FeeSelection
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.eth.EthCryptoWalletAccount
import com.blockchain.coincore.eth.EthereumSendTransactionTarget
import com.blockchain.coincore.eth.EthereumSignMessageTarget
import com.blockchain.coincore.impl.txEngine.walletconnect.WalletConnectTransactionEngine
import com.blockchain.coincore.testutil.CoincoreTestBase
import com.blockchain.coincore.testutil.USD
import com.blockchain.core.chains.ethereum.EthDataManager
import com.blockchain.core.fees.FeeDataManager
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test

class WalletConnectTransactionEngineTest : CoincoreTestBase() {

    private lateinit var subject: WalletConnectTransactionEngine
    private val feesDataManager: FeeDataManager = mock()
    private val ethDataManager: EthDataManager = mock()

    @Before
    fun setup() {
        initMocks()
        subject = WalletConnectTransactionEngine(
            feeManager = feesDataManager,
            ethDataManager = ethDataManager
        )
    }

    @Test
    fun `inputs validate when correct`() {
        val sourceAccount: EthCryptoWalletAccount = mock {
            on { currency }.thenReturn(CryptoCurrency.ETHER)
        }
        val txTarget: EthereumSendTransactionTarget = mock()

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()

        // Assert
        verify(sourceAccount).currency
    }

    @Test(expected = IllegalStateException::class)
    fun `inputs should fail validation when target account type is incorrect`() {
        val sourceAccount: EthCryptoWalletAccount = mock {
            on { currency }.thenReturn(CryptoCurrency.ETHER)
        }

        val txTarget: EthereumSignMessageTarget = mock()
        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()
    }

    @Test
    fun `pending Tx is initialised correctly`() {
        // Arrange
        val sourceAccount = mock<EthCryptoWalletAccount> {
            on { currency }.thenReturn(CryptoCurrency.ETHER)
        }
        val testAmount = Money.fromMinor(CryptoCurrency.ETHER, 500.toBigInteger())

        val txTarget: EthereumSendTransactionTarget = mock {
            on { amount }.thenReturn(testAmount)
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
                it.amount == testAmount &&
                    it.feeAmount.isZero &&
                    it.totalBalance.isZero &&
                    it.feeSelection == FeeSelection(
                    selectedLevel = FeeLevel.Regular,
                    availableLevels = setOf(FeeLevel.Regular),
                    asset = CryptoCurrency.ETHER
                )
            }
            .assertNoErrors()
            .assertComplete()
    }

    @Test
    fun `build confirmations should initialise the correct values`() {
        // Arrange
        val sourceAccount = mock<EthCryptoWalletAccount> {
            on { currency }.thenReturn(CryptoCurrency.ETHER)
            on { balanceRx }.thenReturn(
                Observable.just(
                    AccountBalance(
                        total = Money.fromMinor(CryptoCurrency.ETHER, 9654784874001545.toBigInteger()),
                        withdrawable = Money.fromMinor(CryptoCurrency.ETHER, 9654784874001545.toBigInteger()),
                        pending = Money.zero(CryptoCurrency.ETHER),
                        exchangeRate = ExchangeRate.identityExchangeRate(CryptoCurrency.ETHER),
                    )
                )
            )
        }

        whenever(exchangeRates.getLastCryptoToUserFiatRate(CryptoCurrency.ETHER)).thenReturn(
            ExchangeRate(
                rate = 0.01.toBigDecimal(),
                from = CryptoCurrency.ETHER,
                to = TEST_USER_FIAT
            )
        )

        val target: EthereumSendTransactionTarget = mock {
            on { dAppAddress }.thenReturn("address")
            on { dAppName }.thenReturn("dAppName")
            on { label }.thenReturn("Dapp!")
            on { address }.thenReturn("0x61b0a")
            on { dAppLogoURL }.thenReturn("dAppLogoUrl")
            on { amount }.thenReturn(Money.fromMinor(CryptoCurrency.ETHER, 123500.toBigInteger()))
            on { transactionSource }.thenReturn("TransactionSource")
            on { gasPrice }.thenReturn(22345.toBigInteger())
            on { gasLimit }.thenReturn(12228.toBigInteger())
        }

        subject.start(
            sourceAccount,
            target,
            exchangeRates
        )
        // Act
        subject.doBuildConfirmations(
            PendingTx(
                amount = Money.fromMinor(CryptoCurrency.ETHER, 873957385.toBigInteger()),
                availableBalance = Money.zero(CryptoCurrency.ETHER),
                totalBalance = Money.zero(CryptoCurrency.ETHER),
                feeForFullAvailable = Money.zero(CryptoCurrency.ETHER),
                feeAmount = Money.zero(CryptoCurrency.ETHER),
                feeSelection = FeeSelection(selectedLevel = FeeLevel.Regular),
                selectedFiat = TEST_USER_FIAT
            )
        ).test()
            .assertValue {
                it.availableBalance == Money.fromMinor(CryptoCurrency.ETHER, 9654784600766885.toBigInteger()) &&
                    it.feeForFullAvailable == Money.fromMinor(CryptoCurrency.ETHER, 273234660.toBigInteger()) &&
                    it.feeAmount == Money.fromMinor(CryptoCurrency.ETHER, 273234660.toBigInteger()) &&
                    it.txConfirmations.size == 6 &&
                    it.txConfirmations[0] == TxConfirmationValue.WalletConnectHeader(
                    dAppLogo = "dAppLogoUrl",
                    dAppUrl = "address",
                    dAppName = "Dapp!"
                ) && it.txConfirmations[1] == TxConfirmationValue.From(
                    sourceAccount = sourceAccount,
                    sourceAsset = CryptoCurrency.ETHER
                ) && it.txConfirmations[2] == TxConfirmationValue.ToWithNameAndAddress(
                    label = "Dapp!",
                    address = "0x61b0a"
                ) && it.txConfirmations[3] == TxConfirmationValue.Amount(
                    amount = Money.fromMinor(CryptoCurrency.ETHER, 873957385.toBigInteger()),
                    isImportant = false
                ) && it.txConfirmations[4] == TxConfirmationValue.CompoundNetworkFee(
                    sendingFeeInfo = FeeInfo(
                        asset = CryptoCurrency.ETHER,
                        fiatAmount = Money.zero(TEST_USER_FIAT),
                        feeAmount = Money.fromMinor(CryptoCurrency.ETHER, 273234660.toBigInteger())
                    ),
                    feeLevel = FeeLevel.Regular
                ) && it.txConfirmations[5] == TxConfirmationValue.Total(
                    totalWithFee = Money.fromMinor(CryptoCurrency.ETHER, 1147192045.toBigInteger()),
                    exchange = Money.zero(TEST_USER_FIAT)
                )
            }
            .assertNoErrors()
            .assertComplete()
    }

    @Test
    fun `PendingTx should be in state INVALID ADDR when dapp source address is not same as sourceAccount address`() {
        // Arrange
        val mockReceiveAddress: ReceiveAddress = mock {
            on { address }.thenReturn("123")
        }
        val sourceAccount = mock<EthCryptoWalletAccount> {
            on { currency }.thenReturn(CryptoCurrency.ETHER)
            on { receiveAddress }.thenReturn(Single.just(mockReceiveAddress))
        }

        val target: EthereumSendTransactionTarget = mock {
            on { transactionSource }.thenReturn("0x61b0a")
        }

        subject.start(
            sourceAccount,
            target,
            exchangeRates
        )

        // Act
        subject.doValidateAll(
            PendingTx(
                amount = Money.fromMinor(CryptoCurrency.ETHER, 52485.toBigInteger()),
                availableBalance = Money.zero(CryptoCurrency.ETHER),
                totalBalance = Money.zero(CryptoCurrency.ETHER),
                feeForFullAvailable = Money.zero(CryptoCurrency.ETHER),
                feeAmount = Money.zero(CryptoCurrency.ETHER),
                feeSelection = FeeSelection(),
                selectedFiat = USD
            )
        )
            .test()
            .assertValue {
                it.validationState == ValidationState.INVALID_ADDRESS
            }
            .assertNoErrors()
            .assertComplete()
    }

    @Test
    fun `PendingTx should be in state INSUFFICIENT FUNDS when source account doesnt have enough balance`() {
        // Arrange
        val mockReceiveAddress: ReceiveAddress = mock {
            on { address }.thenReturn("123A")
        }
        val sourceAccount = mock<EthCryptoWalletAccount> {
            on { currency }.thenReturn(CryptoCurrency.ETHER)
            on { balanceRx }.thenReturn(
                Observable.just(
                    AccountBalance(
                        withdrawable = Money.fromMinor(
                            CryptoCurrency.ETHER, 65.toBigInteger()
                        ),
                        total = Money.fromMinor(
                            CryptoCurrency.ETHER, 65.toBigInteger()
                        ),
                        pending = Money.fromMinor(
                            CryptoCurrency.ETHER, 65.toBigInteger()
                        ),
                        exchangeRate = ExchangeRate.identityExchangeRate(CryptoCurrency.ETHER)
                    )
                )
            )
            on { receiveAddress }.thenReturn(Single.just(mockReceiveAddress))
        }

        val target: EthereumSendTransactionTarget = mock {
            on { transactionSource }.thenReturn("123a")
            on { gasPrice }.thenReturn(22345.toBigInteger())
            on { gasLimit }.thenReturn(12228.toBigInteger())
        }

        subject.start(
            sourceAccount,
            target,
            exchangeRates
        )

        // Act
        subject.doValidateAll(
            PendingTx(
                amount = Money.fromMinor(CryptoCurrency.ETHER, 52485.toBigInteger()),
                availableBalance = Money.zero(CryptoCurrency.ETHER),
                totalBalance = Money.zero(CryptoCurrency.ETHER),
                feeForFullAvailable = Money.zero(CryptoCurrency.ETHER),
                feeAmount = Money.zero(CryptoCurrency.ETHER),
                feeSelection = FeeSelection(),
                selectedFiat = USD
            )
        )
            .test()
            .assertValue {
                it.validationState == ValidationState.INSUFFICIENT_FUNDS
            }
            .assertNoErrors()
            .assertComplete()
    }

    @Test
    fun `PendingTx should be in state HAS TX IN FLIGHT when source account has tx in flight`() {
        // Arrange
        val mockReceiveAddress: ReceiveAddress = mock {
            on { address }.thenReturn("123A")
        }
        val sourceAccount = mock<EthCryptoWalletAccount> {
            on { currency }.thenReturn(CryptoCurrency.ETHER)
            on { balanceRx }.thenReturn(
                Observable.just(
                    AccountBalance(
                        withdrawable = Money.fromMinor(
                            CryptoCurrency.ETHER, 68645465.toBigInteger()
                        ),
                        total = Money.fromMinor(
                            CryptoCurrency.ETHER, 68645465.toBigInteger()
                        ),
                        pending = Money.fromMinor(
                            CryptoCurrency.ETHER, 68645465.toBigInteger()
                        ),
                        exchangeRate = ExchangeRate.identityExchangeRate(CryptoCurrency.ETHER)
                    )
                )
            )
            on { receiveAddress }.thenReturn(Single.just(mockReceiveAddress))
        }

        whenever(ethDataManager.isLastTxPending()).thenReturn(Single.just(true))

        val target: EthereumSendTransactionTarget = mock {
            on { transactionSource }.thenReturn("123a")
            on { gasPrice }.thenReturn(45.toBigInteger())
            on { gasLimit }.thenReturn(28.toBigInteger())
        }

        subject.start(
            sourceAccount,
            target,
            exchangeRates
        )

        // Act
        subject.doValidateAll(
            PendingTx(
                amount = Money.fromMinor(CryptoCurrency.ETHER, 52485.toBigInteger()),
                availableBalance = Money.zero(CryptoCurrency.ETHER),
                totalBalance = Money.zero(CryptoCurrency.ETHER),
                feeForFullAvailable = Money.zero(CryptoCurrency.ETHER),
                feeAmount = Money.zero(CryptoCurrency.ETHER),
                feeSelection = FeeSelection(),
                selectedFiat = USD
            )
        )
            .test()
            .assertValue {
                it.validationState == ValidationState.HAS_TX_IN_FLIGHT
            }
            .assertNoErrors()
            .assertComplete()
    }
}
