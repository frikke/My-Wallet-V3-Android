package com.blockchain.coincore.impl

import com.blockchain.banking.BankPartnerCallbackProvider
import com.blockchain.bitpay.BitPayDataManager
import com.blockchain.bitpay.BitPayInvoiceTarget
import com.blockchain.bitpay.BitpayTxEngine
import com.blockchain.core.interest.InterestBalanceDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.preferences.WalletStatus
import com.nhaarman.mockitokotlin2.mock
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.TransferError
import com.blockchain.coincore.btc.BtcOnChainTxEngine
import com.blockchain.coincore.fiat.LinkedBankAccount
import com.blockchain.coincore.impl.txEngine.FiatDepositTxEngine
import com.blockchain.coincore.impl.txEngine.FiatWithdrawalTxEngine
import com.blockchain.coincore.impl.txEngine.TradingToOnChainTxEngine
import com.blockchain.coincore.impl.txEngine.TransferQuotesEngine
import com.blockchain.coincore.impl.txEngine.interest.InterestDepositOnChainTxEngine
import com.blockchain.coincore.impl.txEngine.interest.InterestDepositTradingEngine
import com.blockchain.coincore.impl.txEngine.interest.InterestWithdrawOnChainTxEngine
import com.blockchain.coincore.impl.txEngine.interest.InterestWithdrawTradingTxEngine
import com.blockchain.coincore.impl.txEngine.sell.OnChainSellTxEngine
import com.blockchain.coincore.impl.txEngine.sell.TradingSellTxEngine
import com.blockchain.coincore.impl.txEngine.swap.OnChainSwapTxEngine
import com.blockchain.coincore.impl.txEngine.swap.TradingToTradingSwapTxEngine
import com.blockchain.coincore.testutil.CoincoreTestBase.Companion.SECONDARY_TEST_ASSET
import com.blockchain.coincore.testutil.CoincoreTestBase.Companion.TEST_ASSET
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.repositories.WithdrawLocksRepository
import java.lang.IllegalStateException

class TxProcessorFactoryTest {

    private val bitPayManager: BitPayDataManager = mock()
    private val exchangeRates: ExchangeRatesDataManager = mock()
    private val walletManager: CustodialWalletManager = mock()
    private val interestBalances: InterestBalanceDataManager = mock()
    private val walletPrefs: WalletStatus = mock()
    private val bankPartnerCallbackProvider: BankPartnerCallbackProvider = mock()
    private val quotesEngine: TransferQuotesEngine = mock()
    private val analytics: Analytics = mock()
    private val userIdentity: UserIdentity = mock()
    private val withdrawalLocksRepository: WithdrawLocksRepository = mock()

    private lateinit var subject: TxProcessorFactory

    @Before
    fun setup() {
        subject = TxProcessorFactory(
            bitPayManager = bitPayManager,
            exchangeRates = exchangeRates,
            walletManager = walletManager,
            interestBalances = interestBalances,
            walletPrefs = walletPrefs,
            bankPartnerCallbackProvider = bankPartnerCallbackProvider,
            quotesEngine = quotesEngine,
            analytics = analytics,
            userIdentity = userIdentity,
            withdrawLocksRepository = withdrawalLocksRepository
        )
    }

    @Test
    fun onChainBitpayProcessor() {
        val mockBaseEngine: BtcOnChainTxEngine = mock()
        val source: CryptoNonCustodialAccount = mock {
            on { createTxEngine() }.thenReturn(mockBaseEngine)
            // this Crypto instance needs to live here as Bitpay only accepts BTC and BCH
            on { asset }.thenReturn(CryptoCurrency.BTC)
        }

        val target: BitPayInvoiceTarget = mock()

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == target &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is BitpayTxEngine &&
                    (it.engine as BitpayTxEngine).run {
                        this.bitPayDataManager == bitPayManager &&
                            this.walletPrefs == walletPrefs &&
                            this.assetEngine == mockBaseEngine &&
                            this.analytics == analytics
                    }
            }
    }

    @Test
    fun onChainInterestProcessor() {
        val mockBaseEngine: BtcOnChainTxEngine = mock()
        val source: CryptoNonCustodialAccount = mock {
            on { createTxEngine() }.thenReturn(mockBaseEngine)
            on { asset }.thenReturn(TEST_ASSET)
        }

        val mockReceiveAddress: ReceiveAddress = mock()
        val target: CryptoInterestAccount = mock {
            on { receiveAddress }.thenReturn(Single.just(mockReceiveAddress))
        }

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == mockReceiveAddress &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is InterestDepositOnChainTxEngine &&
                    (it.engine as InterestDepositOnChainTxEngine).run {
                        this.walletManager == walletManager &&
                            this.interestBalances == interestBalances &&
                            this.onChainEngine == mockBaseEngine
                    }
            }
    }

    @Test
    fun onChainAddressProcessor() {
        val mockBaseEngine: BtcOnChainTxEngine = mock()
        val source: CryptoNonCustodialAccount = mock {
            on { createTxEngine() }.thenReturn(mockBaseEngine)
            on { asset }.thenReturn(TEST_ASSET)
        }

        val target: CryptoAddress = mock()

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == target &&
                    it.exchangeRates == exchangeRates &&
                    it.engine == mockBaseEngine
            }
    }

    @Test
    fun onChainCryptoAccountSwapActionProcessor() {
        val mockBaseEngine: BtcOnChainTxEngine = mock()
        val source: CryptoNonCustodialAccount = mock {
            on { createTxEngine() }.thenReturn(mockBaseEngine)
            on { asset }.thenReturn(TEST_ASSET)
        }

        val target: CryptoNonCustodialAccount = mock {
            on { asset }.thenReturn(SECONDARY_TEST_ASSET)
        }

        subject.createProcessor(source, target, AssetAction.Swap)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == target &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is OnChainSwapTxEngine &&
                    (it.engine as OnChainSwapTxEngine).run {
                        this.walletManager == walletManager &&
                            this.userIdentity == userIdentity &&
                            this.engine == mockBaseEngine
                    }
            }
    }

    @Test
    fun onChainCryptoAccountOtherActionProcessor() {
        val mockBaseEngine: BtcOnChainTxEngine = mock()
        val source: CryptoNonCustodialAccount = mock {
            on { createTxEngine() }.thenReturn(mockBaseEngine)
            on { asset }.thenReturn(TEST_ASSET)
        }

        val mockReceiveAddress: ReceiveAddress = mock()
        val target: CryptoAccount = mock {
            on { receiveAddress }.thenReturn(Single.just(mockReceiveAddress))
        }

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == mockReceiveAddress &&
                    it.exchangeRates == exchangeRates &&
                    it.engine == mockBaseEngine
            }
    }

    @Test
    fun onChainFiatAccountProcessor() {
        val mockBaseEngine: BtcOnChainTxEngine = mock()
        val source: CryptoNonCustodialAccount = mock {
            on { createTxEngine() }.thenReturn(mockBaseEngine)
            on { asset }.thenReturn(TEST_ASSET)
        }

        val target: FiatAccount = mock {
            on { fiatCurrency }.thenReturn("EUR")
        }

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == target &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is OnChainSellTxEngine &&
                    (it.engine as OnChainSellTxEngine).run {
                        this.engine == mockBaseEngine &&
                            this.walletManager == walletManager &&
                            this.userIdentity == userIdentity
                    }
            }
    }

    @Test
    fun onChainToUnknownProcessor() {
        val source: CryptoNonCustodialAccount = mock()
        val target: LinkedBankAccount.BankAccountAddress = mock()

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertError {
                it is TransferError
            }
    }

    @Test
    fun tradingToOnChainNoteSupportedSendProcessor() {
        val source: CustodialTradingAccount = mock {
            on { asset }.thenReturn(TEST_ASSET)
            on { isNoteSupported }.thenReturn(true)
        }

        val target: CryptoAddress = mock {
            on { asset }.thenReturn(TEST_ASSET)
        }

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == target &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is TradingToOnChainTxEngine &&
                    (it.engine as TradingToOnChainTxEngine).run {
                        this.isNoteSupported == source.isNoteSupported &&
                            this.walletManager == walletManager
                    }
            }
    }

    @Test
    fun tradingToOnChainNoteNotSupportedSendProcessor() {
        val source: CustodialTradingAccount = mock {
            on { asset }.thenReturn(TEST_ASSET)
            on { isNoteSupported }.thenReturn(false)
        }

        val target: CryptoAddress = mock {
            on { asset }.thenReturn(TEST_ASSET)
        }

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == target &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is TradingToOnChainTxEngine &&
                    (it.engine as TradingToOnChainTxEngine).run {
                        this.isNoteSupported == source.isNoteSupported &&
                            this.walletManager == walletManager
                    }
            }
    }

    @Test
    fun tradingToInterestProcessor() {
        val source: CustodialTradingAccount = mock {
            on { asset }.thenReturn(TEST_ASSET)
        }

        val target: CryptoInterestAccount = mock {
            on { asset }.thenReturn(TEST_ASSET)
        }

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == target &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is InterestDepositTradingEngine &&
                    (it.engine as InterestDepositTradingEngine).run {
                        this.interestBalances == interestBalances &&
                            this.walletManager == walletManager
                    }
            }
    }

    @Test
    fun tradingToFiatProcessor() {
        val source: CustodialTradingAccount = mock {
            on { asset }.thenReturn(TEST_ASSET)
        }

        val target: FiatAccount = mock {
            on { fiatCurrency }.thenReturn("EUR")
        }

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == target &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is TradingSellTxEngine &&
                    (it.engine as TradingSellTxEngine).run {
                        this.userIdentity == userIdentity &&
                            this.walletManager == walletManager
                    }
            }
    }

    @Test
    fun tradingToTradingProcessor() {
        val source: CustodialTradingAccount = mock {
            on { asset }.thenReturn(TEST_ASSET)
        }

        val target: CustodialTradingAccount = mock {
            on { asset }.thenReturn(SECONDARY_TEST_ASSET)
        }

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == target &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is TradingToTradingSwapTxEngine &&
                    (it.engine as TradingToTradingSwapTxEngine).run {
                        this.userIdentity == userIdentity &&
                            this.walletManager == walletManager
                    }
            }
    }

    @Test
    fun tradingToOnChainNoteSupportedProcessor() {
        val source: CustodialTradingAccount = mock {
            on { asset }.thenReturn(TEST_ASSET)
            on { isNoteSupported }.thenReturn(true)
        }

        val mockReceiveAddress: CryptoAddress = mock {
            on { asset }.thenReturn(TEST_ASSET)
        }
        val target: CryptoAccount = mock {
            on { receiveAddress }.thenReturn(Single.just(mockReceiveAddress))
        }

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == mockReceiveAddress &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is TradingToOnChainTxEngine &&
                    (it.engine as TradingToOnChainTxEngine).run {
                        this.isNoteSupported == source.isNoteSupported &&
                            this.walletManager == walletManager
                    }
            }
    }

    @Test
    fun tradingToOnChainNoteNotSupportedProcessor() {
        val source: CustodialTradingAccount = mock {
            on { asset }.thenReturn(TEST_ASSET)
            on { isNoteSupported }.thenReturn(false)
        }

        val mockReceiveAddress: CryptoAddress = mock {
            on { asset }.thenReturn(TEST_ASSET)
        }
        val target: CryptoAccount = mock {
            on { receiveAddress }.thenReturn(Single.just(mockReceiveAddress))
        }

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == mockReceiveAddress &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is TradingToOnChainTxEngine &&
                    (it.engine as TradingToOnChainTxEngine).run {
                        this.isNoteSupported == source.isNoteSupported &&
                            this.walletManager == walletManager
                    }
            }
    }

    @Test
    fun tradingToUnknownProcessor() {
        val source: CustodialTradingAccount = mock()
        val target: LinkedBankAccount.BankAccountAddress = mock()

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertError {
                it is TransferError
            }
    }

    @Test
    fun interestWithdrawalToTradingProcessor() {
        val source: CryptoInterestAccount = mock {
            on { asset }.thenReturn(TEST_ASSET)
        }
        val target: CustodialTradingAccount = mock {
            on { asset }.thenReturn(TEST_ASSET)
        }

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == target &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is InterestWithdrawTradingTxEngine &&
                    (it.engine as InterestWithdrawTradingTxEngine).run {
                        this.walletManager == walletManager &&
                            this.interestBalances == interestBalances
                    }
            }
    }

    @Test
    fun interestWithdrawalToOnChainProcessor() {
        val source: CryptoInterestAccount = mock {
            on { asset }.thenReturn(TEST_ASSET)
        }
        val target: CryptoNonCustodialAccount = mock {
            on { asset }.thenReturn(TEST_ASSET)
        }

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == target &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is InterestWithdrawOnChainTxEngine &&
                    (it.engine as InterestWithdrawOnChainTxEngine).run {
                        this.walletManager == walletManager &&
                            this.interestBalances == interestBalances
                    }
            }
    }

    @Test
    fun interestWithdrawalUnknownProcessor() {
        val source: CryptoInterestAccount = mock()
        val target: LinkedBankAccount.BankAccountAddress = mock()

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertError {
                it is IllegalStateException
            }
    }

    @Test
    fun fiatDepositProcessor() {
        val source: LinkedBankAccount = mock()
        val target: FiatAccount = mock()

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == target &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is FiatDepositTxEngine &&
                    (it.engine as FiatDepositTxEngine).run {
                        this.walletManager == walletManager &&
                            this.bankPartnerCallbackProvider == bankPartnerCallbackProvider
                    }
            }
    }

    @Test
    fun fiatDepositUnknownProcessor() {
        val source: LinkedBankAccount = mock()
        val target: LinkedBankAccount.BankAccountAddress = mock()

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertError {
                it is IllegalStateException
            }
    }

    @Test
    fun fiatWithdrawalProcessor() {
        val source: FiatAccount = mock()
        val target: LinkedBankAccount = mock()

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == target &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is FiatWithdrawalTxEngine &&
                    (it.engine as FiatWithdrawalTxEngine).run {
                        this.walletManager == walletManager
                    }
            }
    }

    @Test
    fun fiatWithdrawalUnknownProcessor() {
        val source: FiatAccount = mock()
        val target: LinkedBankAccount.BankAccountAddress = mock()

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertError {
                it is IllegalStateException
            }
    }
}