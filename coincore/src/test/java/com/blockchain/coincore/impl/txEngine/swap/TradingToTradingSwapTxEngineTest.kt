package com.blockchain.coincore.impl.txEngine.swap

import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorCodes
import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FeeSelection
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.btc.BtcCryptoWalletAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.coincore.impl.txEngine.TransferQuotesEngine
import com.blockchain.coincore.testutil.CoincoreTestBase
import com.blockchain.coincore.xlm.XlmCryptoWalletAccount
import com.blockchain.core.custodial.data.store.TradingStore
import com.blockchain.core.limits.CUSTODIAL_LIMITS_ACCOUNT
import com.blockchain.core.limits.LimitsDataManager
import com.blockchain.core.limits.TxLimit
import com.blockchain.core.limits.TxLimits
import com.blockchain.domain.trade.model.QuotePrice
import com.blockchain.domain.transactions.TransferDirection
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.datamanagers.TransferLimits
import com.blockchain.nabu.datamanagers.repositories.swap.SwapTransactionsStore
import com.blockchain.testutils.bitcoin
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlin.test.assertEquals
import org.junit.Before
import org.junit.Test

class TradingToTradingSwapTxEngineTest : CoincoreTestBase() {

    private val walletManager: CustodialWalletManager = mock()
    private val quotesEngine: TransferQuotesEngine = mock()
    private val userIdentity: UserIdentity = mock()
    private val swapTransactionsStore: SwapTransactionsStore = mock()
    private val limitsDataManager: LimitsDataManager = mock {
        on { getLimits(any(), any(), any(), any(), any(), any()) }.thenReturn(
            Single.just(
                TxLimits(
                    min = TxLimit.Limited(MIN_GOLD_LIMIT_ASSET),
                    max = TxLimit.Limited(MAX_GOLD_LIMIT_ASSET),
                    periodicLimits = emptyList(),
                    suggestedUpgrade = null
                )
            )
        )
    }
    private val tradingStore: TradingStore = mock()

    private val environmentConfig: EnvironmentConfig = mock()

    private val subject = TradingToTradingSwapTxEngine(
        tradingStore = tradingStore,
        walletManager = walletManager,
        quotesEngine = quotesEngine,
        limitsDataManager = limitsDataManager,
        userIdentity = userIdentity,
        swapTransactionsStore = swapTransactionsStore
    )

    @Before
    fun setup() {
        initMocks()

        whenever(exchangeRates.getLastCryptoToUserFiatRate(SRC_ASSET))
            .thenReturn(
                ExchangeRate(
                    from = SRC_ASSET,
                    to = TEST_USER_FIAT,
                    rate = EXCHANGE_RATE
                )
            )
    }

    @Test
    fun `inputs validate when correct`() {
        val sourceAccount: CustodialTradingAccount = mock {
            on { currency }.thenReturn(SRC_ASSET)
        }

        val txTarget: CustodialTradingAccount = mock {
            on { currency }.thenReturn(TGT_ASSET)
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
        verify(txTarget, atLeastOnce()).currency
        verifyQuotesEngineStarted()

        noMoreInteractions(txTarget)
    }

    @Test(expected = IllegalStateException::class)
    fun `inputs fail validation when source Account incorrect`() {
        val sourceAccount: BtcCryptoWalletAccount = mock {
            on { currency }.thenReturn(SRC_ASSET)
        }

        val txTarget: CustodialTradingAccount = mock {
            on { currency }.thenReturn(TGT_ASSET)
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
    fun `inputs fail validation when assets match`() {
        val sourceAccount: CustodialTradingAccount = mock {
            on { currency }.thenReturn(SRC_ASSET)
        }

        val txTarget: CustodialTradingAccount = mock {
            on { currency }.thenReturn(SRC_ASSET)
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
    fun `inputs fail validation when target incorrect`() {
        val sourceAccount: CustodialTradingAccount = mock {
            on { currency }.thenReturn(SRC_ASSET)
        }

        val txTarget: XlmCryptoWalletAccount = mock {
            on { currency }.thenReturn(TGT_ASSET)
        }

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()
    }

    @Test
    fun `asset is returned correctly`() {
        // Arrange
        val sourceAccount: CustodialTradingAccount = mock {
            on { currency }.thenReturn(SRC_ASSET)
        }

        val txTarget: CustodialTradingAccount = mock {
            on { currency }.thenReturn(TGT_ASSET)
        }

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val asset = subject.sourceAsset

        // Assert
        assertEquals(asset, SRC_ASSET)

        verify(sourceAccount, atLeastOnce()).currency
        verify(txTarget, atLeastOnce()).currency
        verifyQuotesEngineStarted()

        noMoreInteractions(txTarget)
    }

    @Test
    fun `PendingTx is correctly initialised`() {
        // Arrange
        val totalBalance: Money = 21.bitcoin()
        val availableBalance: Money = 20.bitcoin()

        mockLimits()

        val sourceAccount = fundedSourceAccount(totalBalance, availableBalance)

        val txTarget: CustodialTradingAccount = mock {
            on { currency }.thenReturn(TGT_ASSET)
        }

        val txPrice = mock<QuotePrice> {
            on { rawPrice }.thenReturn(INITIAL_QUOTE_PRICE)
            on { networkFee }.thenReturn(NETWORK_FEE)
        }

        whenever(quotesEngine.getPriceQuote()).thenReturn(Observable.just(txPrice))
        whenever(quotesEngine.getSampleDepositAddress()).thenReturn(Single.just(SAMPLE_DEPOSIT_ADDRESS))

        whenever(quotesEngine.getLatestPrice()).thenReturn(txPrice)

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val minForFee = NETWORK_FEE.toBigDecimal().divide(INITIAL_QUOTE_PRICE.toBigDecimal())
        val expectedMinLimit = MIN_GOLD_LIMIT_ASSET + CryptoValue.fromMajor(SRC_ASSET, minForFee)

        // Act
        subject.doInitialiseTx()
            .test()
            .assertValue {
                it.amount == CryptoValue.zero(SRC_ASSET) &&
                    it.totalBalance == totalBalance &&
                    it.availableBalance == totalBalance &&
                    it.feeAmount == CryptoValue.zero(SRC_ASSET) &&
                    it.selectedFiat == TEST_USER_FIAT &&
                    it.txConfirmations.isEmpty() &&
                    it.limits == TxLimits.fromAmounts(min = expectedMinLimit, max = MAX_GOLD_LIMIT_ASSET) &&
                    it.validationState == ValidationState.UNINITIALISED
            }
            .assertValue { verifyFeeLevels(it.feeSelection) }
            .assertNoErrors()
            .assertComplete()

        verify(sourceAccount, atLeastOnce()).currency
        verify(txTarget, atLeastOnce()).currency
        verify(currencyPrefs).selectedFiatCurrency
        verifyQuotesEngineStarted()
        verifyLimitsFetched()
        verify(quotesEngine).getPriceQuote()
        verify(quotesEngine, atLeastOnce()).getLatestPrice()

        noMoreInteractions(txTarget)
    }

    @Test
    fun `PendingTx initialisation when limit reached`() {
        // Arrange
        val totalBalance: Money = 21.bitcoin()
        val availableBalance: Money = 20.bitcoin()

        val sourceAccount = fundedSourceAccount(totalBalance, availableBalance)

        val txTarget: CustodialTradingAccount = mock {
            on { currency }.thenReturn(TGT_ASSET)
        }

        val error: NabuApiException = mock {
            on { getErrorCode() }.thenReturn(NabuErrorCodes.PendingOrdersLimitReached)
        }

        whenever(quotesEngine.getPriceQuote()).thenReturn(Observable.error(error))

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        // Act
        subject.doInitialiseTx()
            .test()
            .assertValue {
                it.amount == CryptoValue.zero(SRC_ASSET) &&
                    it.totalBalance == CryptoValue.zero(SRC_ASSET) &&
                    it.availableBalance == CryptoValue.zero(SRC_ASSET) &&
                    it.feeForFullAvailable == CryptoValue.zero(SRC_ASSET) &&
                    it.feeAmount == CryptoValue.zero(SRC_ASSET) &&
                    it.selectedFiat == TEST_USER_FIAT &&
                    it.txConfirmations.isEmpty() &&
                    it.limits == null &&
                    it.validationState == ValidationState.PENDING_ORDERS_LIMIT_REACHED &&
                    it.engineState.isEmpty()
            }
            .assertValue { verifyFeeLevels(it.feeSelection) }
            .assertNoErrors()
            .assertComplete()

        verify(sourceAccount, atLeastOnce()).currency
        verify(txTarget, atLeastOnce()).currency
        verify(currencyPrefs).selectedFiatCurrency
        verifyQuotesEngineStarted()
        verify(quotesEngine).getPriceQuote()

        noMoreInteractions(txTarget)
    }

    @Test
    fun `update amount modifies the pendingTx correctly`() {
        // Arrange
        val totalBalance: Money = 21.bitcoin()
        val availableBalance: Money = 20.bitcoin()

        val sourceAccount = fundedSourceAccount(totalBalance, availableBalance)

        val txTarget: CustodialTradingAccount = mock {
            on { currency }.thenReturn(TGT_ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = CryptoValue.zero(SRC_ASSET),
            totalBalance = CryptoValue.zero(SRC_ASSET),
            availableBalance = CryptoValue.zero(SRC_ASSET),
            feeForFullAvailable = CryptoValue.zero(SRC_ASSET),
            feeAmount = CryptoValue.zero(SRC_ASSET),
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection()
        )

        val inputAmount = 2.bitcoin()
        val expectedFee = 0.bitcoin()

        // Act
        subject.doUpdateAmount(
            inputAmount,
            pendingTx
        ).test()
            .assertValue {
                it.amount == inputAmount &&
                    it.totalBalance == totalBalance &&
                    it.availableBalance == totalBalance &&
                    it.feeAmount == expectedFee
            }
            .assertValue { verifyFeeLevels(it.feeSelection) }
            .assertComplete()
            .assertNoErrors()

        verify(sourceAccount, atLeastOnce()).currency
        verify(txTarget, atLeastOnce()).currency
        verifyQuotesEngineStarted()

        verify(quotesEngine).updateAmount(inputAmount)

        noMoreInteractions(txTarget)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `update fee level from NONE to PRIORITY is rejected`() {
        // Arrange
        val totalBalance: Money = 21.bitcoin()
        val availableBalance: Money = 20.bitcoin()
        val inputAmount = 2.bitcoin()
        val zeroBtc = 0.bitcoin()

        val sourceAccount = fundedSourceAccount(totalBalance, availableBalance)

        val txTarget: CustodialTradingAccount = mock {
            on { currency }.thenReturn(TGT_ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = totalBalance,
            feeForFullAvailable = zeroBtc,
            feeAmount = zeroBtc,
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
        // Arrange
        val totalBalance: Money = 21.bitcoin()
        val availableBalance: Money = 20.bitcoin()
        val inputAmount = 2.bitcoin()
        val zeroBtc = 0.bitcoin()

        val sourceAccount = fundedSourceAccount(totalBalance, availableBalance)

        val txTarget: CustodialTradingAccount = mock {
            on { currency }.thenReturn(TGT_ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = totalBalance,
            feeForFullAvailable = zeroBtc,
            feeAmount = zeroBtc,
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
        // Arrange
        val totalBalance: Money = 21.bitcoin()
        val availableBalance: Money = 20.bitcoin()
        val inputAmount = 2.bitcoin()
        val zeroBtc = 0.bitcoin()

        val sourceAccount = fundedSourceAccount(totalBalance, availableBalance)

        val txTarget: CustodialTradingAccount = mock {
            on { currency }.thenReturn(TGT_ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = totalBalance,
            feeForFullAvailable = zeroBtc,
            feeAmount = zeroBtc,
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
        // Arrange
        val totalBalance: Money = 21.bitcoin()
        val availableBalance: Money = 20.bitcoin()
        val inputAmount = 2.bitcoin()
        val zeroBtc = 0.bitcoin()

        val sourceAccount = fundedSourceAccount(totalBalance, availableBalance)

        val txTarget: CustodialTradingAccount = mock {
            on { currency }.thenReturn(TGT_ASSET)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = inputAmount,
            totalBalance = totalBalance,
            availableBalance = totalBalance,
            feeForFullAvailable = zeroBtc,
            feeAmount = zeroBtc,
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
                    it.availableBalance == totalBalance &&
                    it.feeAmount == zeroBtc
            }
            .assertValue { verifyFeeLevels(it.feeSelection) }
            .assertComplete()
            .assertNoErrors()

        verify(sourceAccount, atLeastOnce()).currency
        verify(txTarget, atLeastOnce()).currency
        verifyQuotesEngineStarted()

        noMoreInteractions(txTarget)
    }

    private fun fundedSourceAccount(totalBalance: Money, availableBalance: Money) =
        mock<CustodialTradingAccount> {
            on { currency }.thenReturn(SRC_ASSET)
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

    private fun mockLimits() {
        whenever(walletManager.getProductTransferLimits(TEST_USER_FIAT, Product.TRADE, TransferDirection.INTERNAL))
            .thenReturn(
                Single.just(
                    TransferLimits(
                        minLimit = MIN_GOLD_LIMIT,
                        maxOrder = MAX_GOLD_ORDER,
                        maxLimit = MAX_GOLD_LIMIT
                    )
                )
            )
    }

    private fun verifyLimitsFetched() {
        verify(walletManager).getProductTransferLimits(TEST_USER_FIAT, Product.TRADE, TransferDirection.INTERNAL)
        verify(limitsDataManager).getLimits(
            outputCurrency = eq(SRC_ASSET),
            sourceCurrency = eq(SRC_ASSET),
            targetCurrency = eq(TGT_ASSET),
            sourceAccountType = eq(CUSTODIAL_LIMITS_ACCOUNT),
            targetAccountType = eq(CUSTODIAL_LIMITS_ACCOUNT),
            legacyLimits = any()
        )
    }

    private fun verifyQuotesEngineStarted() {
        verify(quotesEngine).start(
            Product.TRADE,
            TransferDirection.INTERNAL,
            CurrencyPair(SRC_ASSET, TGT_ASSET)
        )
    }

    private fun verifyFeeLevels(
        feeSelection: FeeSelection
    ) = feeSelection.selectedLevel == FeeLevel.None &&
        feeSelection.availableLevels == setOf(FeeLevel.None) &&
        feeSelection.availableLevels.contains(feeSelection.selectedLevel) &&
        feeSelection.asset == null &&
        feeSelection.customAmount == -1L

    private fun noMoreInteractions(txTarget: TransactionTarget) {
        verifyNoMoreInteractions(txTarget)
        verifyNoMoreInteractions(walletManager)
        verifyNoMoreInteractions(currencyPrefs)
        verifyNoMoreInteractions(exchangeRates)
        verifyNoMoreInteractions(quotesEngine)
        verifyNoMoreInteractions(environmentConfig)
    }

    companion object {
        private val SRC_ASSET = CryptoCurrency.BTC
        private val TGT_ASSET = CryptoCurrency.XLM
        private val EXCHANGE_RATE = 2.toBigDecimal() // 1 btc == 2 INR

        private const val SAMPLE_DEPOSIT_ADDRESS = "initial quote deposit address"

        private val NETWORK_FEE = CryptoValue.fromMajor(CryptoCurrency.BTC, 0.1.toBigDecimal())

        private val INITIAL_QUOTE_PRICE = CryptoValue.fromMajor(CryptoCurrency.BTC, 10.toBigDecimal())

        private val MIN_GOLD_LIMIT = FiatValue.fromMajor(TEST_USER_FIAT, 100.toBigDecimal())
        private val MAX_GOLD_ORDER = FiatValue.fromMajor(TEST_USER_FIAT, 500.toBigDecimal())
        private val MAX_GOLD_LIMIT = FiatValue.fromMajor(TEST_USER_FIAT, 2000.toBigDecimal())

        private val MIN_GOLD_LIMIT_ASSET = CryptoValue.fromMajor(SRC_ASSET, 50.toBigDecimal())
        private val MAX_GOLD_LIMIT_ASSET = CryptoValue.fromMajor(SRC_ASSET, 1000.toBigDecimal())
    }
}
