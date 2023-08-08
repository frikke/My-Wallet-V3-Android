package com.blockchain.coincore.impl.txEngine.sell

import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorCodes
import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FeeSelection
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.btc.BtcCryptoWalletAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.coincore.impl.txEngine.TransferQuotesEngine
import com.blockchain.coincore.testutil.CoincoreTestBase
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
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class TradingSellTxEngineTest : CoincoreTestBase() {

    private val walletManager: CustodialWalletManager = mock()
    private val quotesEngine: TransferQuotesEngine = mock()
    private val environmentConfig: EnvironmentConfig = mock()
    private val userIdentity: UserIdentity = mock()
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

    private val subject = TradingSellTxEngine(
        tradingStore = tradingStore,
        walletManager = walletManager,
        quotesEngine = quotesEngine,
        userIdentity = userIdentity,
        limitsDataManager = limitsDataManager
    )

    @Before
    fun setup() {
        initMocks()

        whenever(exchangeRates.getLastCryptoToFiatRate(SRC_ASSET, TEST_API_FIAT))
            .thenReturn(
                ExchangeRate(
                    from = SRC_ASSET,
                    to = TEST_API_FIAT,
                    rate = EXCHANGE_RATE
                )
            )
    }

    @Test
    fun `inputs validate when correct`() {
        val sourceAccount: CustodialTradingAccount = mock {
            on { currency }.thenReturn(SRC_ASSET)
        }
        val txTarget: FiatAccount = mock {
            on { currency }.thenReturn(TEST_API_FIAT)
        }

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()

        // Assert
        verify(txTarget).currency
        verify(sourceAccount, atLeastOnce()).currency
        verify(quotesEngine).start(
            Product.SELL,
            TransferDirection.INTERNAL,
            CurrencyPair(SRC_ASSET, TEST_API_FIAT)
        )

        noMoreInteractions(txTarget)
    }

    @Test(expected = IllegalStateException::class)
    fun `inputs fail validation when source Account incorrect`() {
        val sourceAccount: BtcCryptoWalletAccount = mock {
            on { currency }.thenReturn(SRC_ASSET)
        }

        val txTarget: FiatAccount = mock {
            on { currency }.thenReturn(TEST_API_FIAT)
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
        val sourceAccount: CustodialTradingAccount = mock {
            on { currency }.thenReturn(SRC_ASSET)
        }

        val txTarget: CryptoAccount = mock {
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

    @Test
    fun `asset is returned correctly`() {
        // Arrange
        val sourceAccount: CustodialTradingAccount = mock {
            on { currency }.thenReturn(SRC_ASSET)
        }
        val txTarget: FiatAccount = mock {
            on { currency }.thenReturn(TEST_API_FIAT)
        }

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val asset = subject.sourceAsset

        // Assert
        asset shouldBeEqualTo SRC_ASSET

        verify(sourceAccount, atLeastOnce()).currency
        verify(txTarget).currency
        verifyQuotesEngineStarted()

        noMoreInteractions(txTarget)
    }

    @Test
    fun `PendingTx is correctly initialised`() {
        // Arrange
        val totalBalance: Money = 21.bitcoin()
        val actionableBalance: Money = 20.bitcoin()

        mockLimits()

        val sourceAccount = fundedSourceAccount(totalBalance, actionableBalance)
        val txTarget: FiatAccount = mock {
            on { currency }.thenReturn(TEST_API_FIAT)
        }

        val pricedQuote: QuotePrice = mock()
        whenever(quotesEngine.getPriceQuote()).thenReturn(Observable.just(pricedQuote))

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
                    it.totalBalance == totalBalance &&
                    it.availableBalance == totalBalance &&
                    it.feeAmount == CryptoValue.zero(SRC_ASSET) &&
                    it.selectedFiat == TEST_API_FIAT &&
                    it.txConfirmations.isEmpty() &&
                    it.limits == TxLimits.fromAmounts(min = MIN_GOLD_LIMIT_ASSET, max = MAX_GOLD_LIMIT_ASSET) &&
                    it.validationState == ValidationState.UNINITIALISED &&
                    it.engineState.isEmpty()
            }
            .assertValue { verifyFeeLevels(it.feeSelection) }
            .assertNoErrors()
            .assertComplete()

        verify(sourceAccount, atLeastOnce()).currency
        verify(sourceAccount, atLeastOnce()).currency
        verify(txTarget, atLeastOnce()).currency
        verifyQuotesEngineStarted()
        verify(quotesEngine).getPriceQuote()
        verifyLimitsFetched()

        noMoreInteractions(txTarget)
    }

    @Test
    fun `PendingTx initialisation when limit reached`() {
        // Arrange
        val totalBalance: Money = 21.bitcoin()
        val actionableBalance: Money = 20.bitcoin()

        val sourceAccount = fundedSourceAccount(totalBalance, actionableBalance)
        val txTarget: FiatAccount = mock {
            on { currency }.thenReturn(TEST_API_FIAT)
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
                    it.selectedFiat == TEST_API_FIAT &&
                    it.txConfirmations.isEmpty() &&
                    it.limits == null &&
                    it.validationState == ValidationState.PENDING_ORDERS_LIMIT_REACHED &&
                    it.engineState.isEmpty()
            }
            .assertValue { verifyFeeLevels(it.feeSelection) }
            .assertNoErrors()
            .assertComplete()

        verify(sourceAccount, atLeastOnce()).currency
        verify(sourceAccount).balanceRx()
        verify(txTarget, atLeastOnce()).currency
        verifyQuotesEngineStarted()
        verify(quotesEngine).getPriceQuote()

        noMoreInteractions(txTarget)
    }

    @Test
    fun `update amount modifies the pendingTx correctly`() {
        // Arrange
        val totalBalance: Money = 21.bitcoin()
        val actionableBalance: Money = 20.bitcoin()

        val sourceAccount = fundedSourceAccount(totalBalance, actionableBalance)
        val txTarget: FiatAccount = mock {
            on { currency }.thenReturn(TEST_API_FIAT)
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
            selectedFiat = TEST_API_FIAT,
            feeSelection = FeeSelection()
        )

        val inputAmount = 2.bitcoin()
        val expectedFee = 0.bitcoin()

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
                    it.availableBalance == totalBalance &&
                    it.feeAmount == expectedFee
            }
            .assertValue { verifyFeeLevels(it.feeSelection) }

        verify(sourceAccount, atLeastOnce()).currency
        verify(sourceAccount).balanceRx()
        verify(txTarget, atLeastOnce()).currency
        verifyQuotesEngineStarted()
        verify(quotesEngine).updateAmount(inputAmount)

        noMoreInteractions(txTarget)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `update fee level from NONE to PRIORITY is rejected`() {
        // Arrange
        val totalBalance: Money = 21.bitcoin()
        val actionableBalance: Money = 20.bitcoin()
        val inputAmount = 2.bitcoin()
        val zeroBtc = 0.bitcoin()

        val sourceAccount = fundedSourceAccount(totalBalance, actionableBalance)
        val txTarget: FiatAccount = mock {
            on { currency }.thenReturn(TEST_API_FIAT)
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
            selectedFiat = TEST_API_FIAT,
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
        val actionableBalance: Money = 20.bitcoin()
        val inputAmount = 2.bitcoin()
        val zeroBtc = 0.bitcoin()

        val sourceAccount = fundedSourceAccount(totalBalance, actionableBalance)
        val txTarget: FiatAccount = mock {
            on { currency }.thenReturn(TEST_API_FIAT)
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
            selectedFiat = TEST_API_FIAT,
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
        val actionableBalance: Money = 20.bitcoin()
        val inputAmount = 2.bitcoin()
        val zeroBtc = 0.bitcoin()

        val sourceAccount = fundedSourceAccount(totalBalance, actionableBalance)
        val txTarget: FiatAccount = mock {
            on { currency }.thenReturn(TEST_API_FIAT)
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
            selectedFiat = TEST_API_FIAT,
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
        val actionableBalance: Money = 20.bitcoin()
        val inputAmount = 2.bitcoin()
        val zeroBtc = 0.bitcoin()

        val sourceAccount = fundedSourceAccount(totalBalance, actionableBalance)
        val txTarget: FiatAccount = mock {
            on { currency }.thenReturn(TEST_API_FIAT)
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
            selectedFiat = TEST_API_FIAT,
            feeSelection = FeeSelection()
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.None,
            -1
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.amount == inputAmount &&
                    it.totalBalance == totalBalance &&
                    it.availableBalance == totalBalance &&
                    it.feeAmount == zeroBtc
            }
            .assertValue { verifyFeeLevels(it.feeSelection) }

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
        whenever(walletManager.getProductTransferLimits(TEST_API_FIAT, Product.SELL, TransferDirection.INTERNAL))
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
        verify(walletManager).getProductTransferLimits(TEST_API_FIAT, Product.SELL, TransferDirection.INTERNAL)
        verify(limitsDataManager).getLimits(
            outputCurrency = eq(SRC_ASSET),
            sourceCurrency = eq(SRC_ASSET),
            targetCurrency = eq(TEST_API_FIAT),
            sourceAccountType = eq(CUSTODIAL_LIMITS_ACCOUNT),
            targetAccountType = eq(CUSTODIAL_LIMITS_ACCOUNT),
            legacyLimits = any()
        )
    }

    private fun verifyQuotesEngineStarted() {
        verify(quotesEngine).start(
            Product.SELL,
            TransferDirection.INTERNAL,
            CurrencyPair(SRC_ASSET, TEST_API_FIAT)
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
        private val EXCHANGE_RATE = 2.toBigDecimal() // 1 btc == 2 EUR

        private val MIN_GOLD_LIMIT = FiatValue.fromMajor(TEST_API_FIAT, 100.toBigDecimal())
        private val MAX_GOLD_ORDER = FiatValue.fromMajor(TEST_API_FIAT, 500.toBigDecimal())
        private val MAX_GOLD_LIMIT = FiatValue.fromMajor(TEST_API_FIAT, 2000.toBigDecimal())

        private val MIN_GOLD_LIMIT_ASSET = CryptoValue.fromMajor(SRC_ASSET, 50.toBigDecimal())
        private val MAX_GOLD_LIMIT_ASSET = CryptoValue.fromMajor(SRC_ASSET, 1000.toBigDecimal())
    }
}
