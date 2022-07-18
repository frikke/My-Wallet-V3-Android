package com.blockchain.coincore.impl

import com.blockchain.analytics.Analytics
import com.blockchain.banking.BankPartnerCallbackProvider
import com.blockchain.bitpay.BitPayDataManager
import com.blockchain.bitpay.BitPayInvoiceTarget
import com.blockchain.bitpay.BitpayTxEngine
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
import com.blockchain.coincore.testutil.EUR
import com.blockchain.core.custodial.data.store.TradingDataSource
import com.blockchain.core.interest.data.store.InterestDataSource
import com.blockchain.core.limits.LimitsDataManager
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.repositories.WithdrawLocksRepository
import com.blockchain.preferences.WalletStatusPrefs
import com.nhaarman.mockitokotlin2.mock
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test

class TxProcessorFactoryTest {

    private val bitPayManager: BitPayDataManager = mock()
    private val exchangeRates: ExchangeRatesDataManager = mock()
    private val walletManager: CustodialWalletManager = mock()
    private val interestDataSource: InterestDataSource = mock()
    private val tradingDataSource: TradingDataSource = mock()
    private val walletPrefs: WalletStatusPrefs = mock()
    private val bankPartnerCallbackProvider: BankPartnerCallbackProvider = mock()
    private val quotesEngine: TransferQuotesEngine = mock()
    private val analytics: Analytics = mock()
    private val limitsDataManager: LimitsDataManager = mock()
    private val userIdentity: UserIdentity = mock()
    private val withdrawalLocksRepository: WithdrawLocksRepository = mock()
    private val bankService: BankService = mock()
    private val plaidFeatureFlag: FeatureFlag = mock()

    private lateinit var subject: TxProcessorFactory

    @Before
    fun setup() {
        subject = TxProcessorFactory(
            bitPayManager = bitPayManager,
            exchangeRates = exchangeRates,
            walletManager = walletManager,
            interestDataSource = interestDataSource,
            tradingDataSource = tradingDataSource,
            walletPrefs = walletPrefs,
            limitsDataManager = limitsDataManager,
            bankPartnerCallbackProvider = bankPartnerCallbackProvider,
            quotesEngine = quotesEngine,
            ethMessageSigner = mock(),
            analytics = analytics,
            userIdentity = userIdentity,
            withdrawLocksRepository = withdrawalLocksRepository,
            bankService = bankService,
            ethDataManager = mock(),
            fees = mock(),
            swapTransactionsCache = mock(),
            plaidFeatureFlag = plaidFeatureFlag
        )
    }

    @Test
    fun onChainBitpayProcessor() {
        val mockBaseEngine: BtcOnChainTxEngine = mock()

        val target: BitPayInvoiceTarget = mock()
        val action = AssetAction.Send
        val source: CryptoNonCustodialAccount = mock {
            on { createTxEngine(target, action) }.thenReturn(mockBaseEngine)
            // this Crypto instance needs to live here as Bitpay only accepts BTC and BCH
            on { currency }.thenReturn(CryptoCurrency.BTC)
        }

        subject.createProcessor(source, target, action)
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

        val mockReceiveAddress: ReceiveAddress = mock()
        val target: CryptoInterestAccount = mock {
            on { receiveAddress }.thenReturn(Single.just(mockReceiveAddress))
        }
        val action = AssetAction.Send
        val source: CryptoNonCustodialAccount = mock {
            on { createTxEngine(target, action) }.thenReturn(mockBaseEngine)
            on { currency }.thenReturn(TEST_ASSET)
        }

        subject.createProcessor(source, target, action)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == mockReceiveAddress &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is InterestDepositOnChainTxEngine &&
                    (it.engine as InterestDepositOnChainTxEngine).run {
                        this.walletManager == walletManager &&
                            this.onChainEngine == mockBaseEngine
                    }
            }
    }

    @Test
    fun onChainAddressProcessor() {
        val mockBaseEngine: BtcOnChainTxEngine = mock()

        val target: CryptoAddress = mock()
        val action = AssetAction.Send
        val source: CryptoNonCustodialAccount = mock {
            on { createTxEngine(target, action) }.thenReturn(mockBaseEngine)
            on { currency }.thenReturn(TEST_ASSET)
        }

        subject.createProcessor(source, target, action)
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

        val target: CryptoNonCustodialAccount = mock {
            on { currency }.thenReturn(SECONDARY_TEST_ASSET)
        }
        val action = AssetAction.Swap
        val source: CryptoNonCustodialAccount = mock {
            on { createTxEngine(target, action) }.thenReturn(mockBaseEngine)
            on { currency }.thenReturn(TEST_ASSET)
        }

        subject.createProcessor(source, target, action)
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

        val mockReceiveAddress: ReceiveAddress = mock()
        val target: CryptoAccount = mock {
            on { receiveAddress }.thenReturn(Single.just(mockReceiveAddress))
        }
        val action = AssetAction.Send
        val source: CryptoNonCustodialAccount = mock {
            on { createTxEngine(target, action) }.thenReturn(mockBaseEngine)
            on { currency }.thenReturn(TEST_ASSET)
        }

        subject.createProcessor(source, target, action)
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

        val target: FiatAccount = mock {
            on { currency }.thenReturn(EUR)
        }
        val action = AssetAction.Send
        val source: CryptoNonCustodialAccount = mock {
            on { createTxEngine(target, action) }.thenReturn(mockBaseEngine)
            on { currency }.thenReturn(TEST_ASSET)
        }

        subject.createProcessor(source, target, action)
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
            on { currency }.thenReturn(TEST_ASSET)
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
                        this.walletManager == walletManager
                    }
            }
    }

    @Test
    fun tradingToOnChainNoteNotSupportedSendProcessor() {
        val source: CustodialTradingAccount = mock {
            on { currency }.thenReturn(TEST_ASSET)
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
                        this.walletManager == walletManager
                    }
            }
    }

    @Test
    fun tradingToInterestProcessor() {
        val source: CustodialTradingAccount = mock {
            on { currency }.thenReturn(TEST_ASSET)
        }

        val target: CryptoInterestAccount = mock {
            on { currency }.thenReturn(TEST_ASSET)
        }

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == target &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is InterestDepositTradingEngine &&
                    (it.engine as InterestDepositTradingEngine).run {
                        this.walletManager == walletManager
                    }
            }
    }

    @Test
    fun tradingToFiatProcessor() {
        val source: CustodialTradingAccount = mock {
            on { currency }.thenReturn(TEST_ASSET)
        }

        val target: FiatAccount = mock {
            on { currency }.thenReturn(EUR)
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
            on { currency }.thenReturn(TEST_ASSET)
        }

        val target: CustodialTradingAccount = mock {
            on { currency }.thenReturn(SECONDARY_TEST_ASSET)
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
            on { currency }.thenReturn(TEST_ASSET)
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
                        this.walletManager == walletManager
                    }
            }
    }

    @Test
    fun tradingToOnChainNoteNotSupportedProcessor() {
        val source: CustodialTradingAccount = mock {
            on { currency }.thenReturn(TEST_ASSET)
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
            on { currency }.thenReturn(TEST_ASSET)
        }
        val target: CustodialTradingAccount = mock {
            on { currency }.thenReturn(TEST_ASSET)
        }

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == target &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is InterestWithdrawTradingTxEngine &&
                    (it.engine as InterestWithdrawTradingTxEngine).run {
                        this.walletManager == walletManager
                    }
            }
    }

    @Test
    fun interestWithdrawalToOnChainProcessor() {
        val source: CryptoInterestAccount = mock {
            on { currency }.thenReturn(TEST_ASSET)
        }
        val target: CryptoNonCustodialAccount = mock {
            on { currency }.thenReturn(TEST_ASSET)
        }

        subject.createProcessor(source, target, AssetAction.Send)
            .test()
            .assertValue {
                it.sourceAccount == source &&
                    it.txTarget == target &&
                    it.exchangeRates == exchangeRates &&
                    it.engine is InterestWithdrawOnChainTxEngine &&
                    (it.engine as InterestWithdrawOnChainTxEngine).run {
                        this.walletManager == walletManager
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
