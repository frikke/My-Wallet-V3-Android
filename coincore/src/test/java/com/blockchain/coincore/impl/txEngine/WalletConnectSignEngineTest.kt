package com.blockchain.coincore.impl.txEngine

import com.blockchain.coincore.FeeSelection
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.eth.EthCryptoWalletAccount
import com.blockchain.coincore.eth.EthOnChainTxEngine
import com.blockchain.coincore.eth.EthSignMessage
import com.blockchain.coincore.eth.EthereumSignMessageTarget
import com.blockchain.coincore.eth.WalletConnectTarget
import com.blockchain.coincore.impl.txEngine.walletconnect.WalletConnectSignEngine
import com.blockchain.coincore.testutil.CoincoreTestBase
import com.blockchain.coincore.testutil.USD
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.androidcore.data.ethereum.EthMessageSigner

class WalletConnectSignEngineTest : CoincoreTestBase() {

    private lateinit var subject: WalletConnectSignEngine
    private val ethSigner: EthMessageSigner = mock()
    private val onChainEngine: EthOnChainTxEngine = mock {
        on { sourceAsset }.thenReturn(CryptoCurrency.ETHER)
    }

    @Before
    fun setup() {
        initMocks()
        subject = WalletConnectSignEngine(
            assetEngine = onChainEngine,
            ethMessageSigner = ethSigner
        )
    }

    @Test
    fun `inputs validate when correct`() {
        val sourceAccount: EthCryptoWalletAccount = mock {
            on { currency }.thenReturn(CryptoCurrency.ETHER)
        }
        val txTarget: WalletConnectTarget = mock()

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
    fun `inputs fail validation when source Asset incorrect`() {
        val sourceAccount = mock<EthCryptoWalletAccount> {
            on { currency }.thenReturn(CryptoCurrency.BTC)
        }

        val txTarget: WalletConnectTarget = mock()
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

    @Test
    fun `pending Tx is initialised correctly`() {
        // Arrange
        val sourceAccount = mock<EthCryptoWalletAccount> {
            on { currency }.thenReturn(CryptoCurrency.ETHER)
        }
        whenever(onChainEngine.doInitialiseTx()).thenReturn(
            Single.just(
                PendingTx(
                    amount = Money.zero(CryptoCurrency.ETHER),
                    availableBalance = Money.zero(CryptoCurrency.ETHER),
                    totalBalance = Money.zero(CryptoCurrency.ETHER),
                    feeForFullAvailable = Money.zero(CryptoCurrency.ETHER),
                    feeAmount = Money.zero(CryptoCurrency.ETHER),
                    feeSelection = FeeSelection(),
                    selectedFiat = USD
                )
            )
        )
        val txTarget: WalletConnectTarget = mock()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        // Act
        subject.doInitialiseTx()
            .test()
            .assertValue {
                it.amount == CryptoValue.zero(CryptoCurrency.ETHER) &&
                    it.validationState == ValidationState.UNINITIALISED &&
                    it.engineState.isEmpty()
            }
            .assertNoErrors()
            .assertComplete()
        verify(onChainEngine).doInitialiseTx()
    }

    @Test
    fun `build confirmations should initialise the correct values`() {
        // Arrange
        val sourceAccount = mock<EthCryptoWalletAccount> {
            on { currency }.thenReturn(CryptoCurrency.ETHER)
        }
        val target = EthereumSignMessageTarget(
            dAppAddress = "address",
            dAppName = "dAppName",
            dAppLogoUrl = "dAppLogoUrl",
            message = EthSignMessage(
                listOf(
                    "0xBb588Db4b713456e3Cc6f9Bf21Fd67650F317B3C",
                    "0x4d7920656d61696c206973206a6f686e40646f652e6" +
                        "36f6d202d204d6f6e2c203331204a616e20323032322031353a33333a353520474d54"
                ),
                EthSignMessage.SignType.MESSAGE
            ),
            onTxCompleted = { Completable.complete() },
        )

        subject.start(
            sourceAccount,
            target,
            exchangeRates
        )
        // Act
        subject.doBuildConfirmations(
            PendingTx(
                amount = Money.zero(CryptoCurrency.ETHER),
                availableBalance = Money.zero(CryptoCurrency.ETHER),
                totalBalance = Money.zero(CryptoCurrency.ETHER),
                feeForFullAvailable = Money.zero(CryptoCurrency.ETHER),
                feeAmount = Money.zero(CryptoCurrency.ETHER),
                feeSelection = FeeSelection(),
                selectedFiat = USD
            )
        ).test()
            .assertValue {
                it.confirmations == listOf(
                    TxConfirmationValue.WalletConnectHeader(
                        dAppName = "dAppName",
                        dAppLogo = "dAppLogoUrl",
                        dAppUrl = "address"
                    ),
                    TxConfirmationValue.DAppInfo(
                        name = "dAppName",
                        url = "address"
                    ),
                    TxConfirmationValue.Chain(
                        assetInfo = CryptoCurrency.ETHER
                    ),
                    TxConfirmationValue.SignEthMessage(
                        message = "My email is john@doe.com - Mon, 31 Jan 2022 15:33:55 GMT",
                        dAppName = "dAppName"
                    )

                )
            }
            .assertNoErrors()
            .assertComplete()
    }

    @Test
    fun `PendingTx should be in  state INVALID ADDR when dapp address is not same as sourceAccount address`() {
        // Arrange
        val mockReceiveAddress: ReceiveAddress = mock {
            on { address }.thenReturn("123")
        }
        val sourceAccount = mock<EthCryptoWalletAccount> {
            on { currency }.thenReturn(CryptoCurrency.ETHER)
            on { receiveAddress }.thenReturn(Single.just(mockReceiveAddress))
        }

        val target = EthereumSignMessageTarget(
            dAppAddress = "456",
            dAppName = "dAppName",
            dAppLogoUrl = "dAppLogoUrl",
            message = EthSignMessage(
                listOf(
                    "0xBb588Db4b713456e3Cc6f9Bf21Fd67650F317B3C",
                    "0x4d7920656d61696c206973206a6f686e40646f652e6" +
                        "36f6d202d204d6f6e2c203331204a616e20323032322031353a33333a353520474d54"
                ),
                EthSignMessage.SignType.MESSAGE
            ),
            onTxCompleted = { Completable.complete() },
        )

        subject.start(
            sourceAccount,
            target,
            exchangeRates
        )

        // Act
        subject.doValidateAll(
            PendingTx(
                amount = Money.zero(CryptoCurrency.ETHER),
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
    fun `PendingTx should be in  state CAN EXECUTE when dapp address is the same as sourceAccount address`() {
        // Arrange
        val mockReceiveAddress: ReceiveAddress = mock {
            on { address }.thenReturn("123")
        }
        val sourceAccount = mock<EthCryptoWalletAccount> {
            on { currency }.thenReturn(CryptoCurrency.ETHER)
            on { receiveAddress }.thenReturn(Single.just(mockReceiveAddress))
        }

        val target = EthereumSignMessageTarget(
            dAppAddress = "123",
            dAppName = "dAppName",
            dAppLogoUrl = "dAppLogoUrl",
            message = EthSignMessage(
                listOf(
                    "0xBb588Db4b713456e3Cc6f9Bf21Fd67650F317B3C",
                    "0x4d7920656d61696c206973206a6f686e40646f652e6" +
                        "36f6d202d204d6f6e2c203331204a616e20323032322031353a33333a353520474d54"
                ),
                EthSignMessage.SignType.MESSAGE
            ),
            onTxCompleted = { Completable.complete() },
        )

        subject.start(
            sourceAccount,
            target,
            exchangeRates
        )

        // Act
        subject.doValidateAll(
            PendingTx(
                amount = Money.zero(CryptoCurrency.ETHER),
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
}
