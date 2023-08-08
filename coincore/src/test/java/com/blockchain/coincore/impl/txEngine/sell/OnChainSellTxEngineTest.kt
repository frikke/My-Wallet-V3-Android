package com.blockchain.coincore.impl.txEngine.sell

import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuErrorCodes
import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FeeSelection
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.btc.BtcAddress
import com.blockchain.coincore.btc.BtcCryptoWalletAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.coincore.impl.txEngine.OnChainTxEngineBase
import com.blockchain.coincore.impl.txEngine.TransferQuotesEngine
import com.blockchain.coincore.testutil.CoincoreTestBase
import com.blockchain.core.custodial.data.store.TradingStore
import com.blockchain.core.kyc.domain.model.KycTiers
import com.blockchain.core.limits.CUSTODIAL_LIMITS_ACCOUNT
import com.blockchain.core.limits.LimitsDataManager
import com.blockchain.core.limits.NON_CUSTODIAL_LIMITS_ACCOUNT
import com.blockchain.core.limits.TxLimit
import com.blockchain.core.limits.TxLimits
import com.blockchain.domain.trade.model.QuotePrice
import com.blockchain.domain.transactions.TransferDirection
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.datamanagers.TransferLimits
import com.blockchain.testutils.bitcoin
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.amshove.kluent.shouldEqual
import org.junit.Before
import org.junit.Test

class OnChainSellTxEngineTest : CoincoreTestBase() {

    private val walletManager: CustodialWalletManager = mock()
    private val quotesEngine: TransferQuotesEngine = mock()
    private val userIdentity: UserIdentity = mock()
    private val tradingStore: TradingStore = mock()

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

    private val onChainEngine: OnChainTxEngineBase = mock {
        on { sourceAsset }.thenReturn(SRC_ASSET)
    }

    private val subject = OnChainSellTxEngine(
        tradingStore = tradingStore,
        engine = onChainEngine,
        walletManager = walletManager,
        quotesEngine = quotesEngine,
        userIdentity = userIdentity,
        limitsDataManager = limitsDataManager
    )

    @Before
    fun setup() {
        initMocks()

        whenever(exchangeRates.getLastCryptoToUserFiatRate(SRC_ASSET))
            .thenReturn(
                ExchangeRate(
                    from = SRC_ASSET,
                    to = TEST_USER_FIAT,
                    rate = ASSET_TO_USER_FIAT_RATE
                )
            )

        whenever(exchangeRates.getLastCryptoToFiatRate(SRC_ASSET, TEST_API_FIAT))
            .thenReturn(
                ExchangeRate(
                    from = SRC_ASSET,
                    to = TEST_API_FIAT,
                    rate = ASSET_TO_API_FIAT_RATE
                )
            )
    }

    @Test
    fun `inputs validate when correct`() {
        val sourceAccount = mockSourceAccount()
        val txTarget = mockTransactionTarget()

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

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test(expected = IllegalStateException::class)
    fun `inputs fail validation when source Account incorrect`() {
        val sourceAccount: CustodialTradingAccount = mock {
            on { currency }.thenReturn(WRONG_ASSET)
        }

        val txTarget = mockTransactionTarget()

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
        val sourceAccount = mockSourceAccount()
        val txTarget: CryptoAccount = mock {
            on { currency }.thenReturn(WRONG_ASSET)
        }

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()
    }

    /* todo fix test once assertInputsValid is called for decoration engine and making engine.start returns completable
     @Test(expected = IllegalStateException::class)
     fun `inputs fail validation when on chain engine validation fails`() {
         val sourceAccount = mockSourceAccount()
         val txTarget = mockTransactionTarget()

         val txQuote: TransferQuote = mock {
             on { sampleDepositAddress }.thenReturn(SAMPLE_DEPOSIT_ADDRESS
         }
         val pricedQuote: PricedQuote = mock {
             on { transferQuote }.thenReturn(txQuote
         }
         whenever(quotesEngine.pricedQuote).thenReturn(Observable.just(pricedQuote))
         whenever(onChainEngine.assertInputsValid()).thenThrow(IllegalStateException())

         // Act
         subject.start(
             sourceAccount,
             txTarget,
             exchangeRates
         )
         subject.assertInputsValid()
     }*/

    @Test
    fun `asset is returned correctly`() {
        // Arrange
        val sourceAccount = mockSourceAccount()
        val txTarget = mockTransactionTarget()

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val asset = subject.sourceAsset

        // Assert
        asset shouldEqual SRC_ASSET

        verify(sourceAccount, atLeastOnce()).currency
        verify(txTarget, atLeastOnce()).currency
        verifyQuotesEngineStarted()

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `PendingTx is correctly initialised`() {
        // Arrange
        val totalBalance: Money = 21.bitcoin()
        val availableBalance: Money = 20.bitcoin()

        whenUserIsGold()
        whenOnChainEngineInitOK(totalBalance, availableBalance)

        val sourceAccount = mockSourceAccount(totalBalance, availableBalance)
        val txTarget = mockTransactionTarget()

        val txPrice = mock<QuotePrice>()

        whenever(quotesEngine.getPriceQuote()).thenReturn(Observable.just(txPrice))
        whenever(quotesEngine.getSampleDepositAddress()).thenReturn(Single.just(SAMPLE_DEPOSIT_ADDRESS))

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
                    it.availableBalance == availableBalance &&
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
        verify(txTarget, atLeastOnce()).currency
        verifyQuotesEngineStarted()
        verifyOnChainEngineStarted(sourceAccount)
        verifyLimitsFetched()
        verify(quotesEngine).getPriceQuote()
        verify(quotesEngine).getSampleDepositAddress()
        verify(onChainEngine).doInitialiseTx()
        // todo fix once start engine returns completable
        // verify(onChainEngine).assertInputsValid()

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `PendingTx is correctly initialised with flat regular fees`() {
        // Arrange
        val totalBalance: Money = 21.bitcoin()
        val availableBalance: Money = 20.bitcoin()

        whenUserIsGold()
        whenOnChainEngineInitOK(totalBalance, availableBalance)

        val sourceAccount = mockSourceAccount(totalBalance, availableBalance)

        val txTarget = mockTransactionTarget()

        val txPrice = mock<QuotePrice>()

        whenever(quotesEngine.getPriceQuote()).thenReturn(Observable.just(txPrice))
        whenever(quotesEngine.getSampleDepositAddress()).thenReturn(Single.just(SAMPLE_DEPOSIT_ADDRESS))

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
                    it.availableBalance == availableBalance &&
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
        verify(txTarget, atLeastOnce()).currency
        verifyQuotesEngineStarted()
        verifyOnChainEngineStarted(sourceAccount)
        verifyLimitsFetched()
        verify(quotesEngine).getPriceQuote()
        verify(quotesEngine).getSampleDepositAddress()
        verify(onChainEngine).doInitialiseTx()
        // todo fix once start engine returns completable
        // verify(onChainEngine).assertInputsValid()

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `PendingTx initialisation when limit reached`() {
        // Arrange
        val totalBalance: Money = 21.bitcoin()
        val availableBalance: Money = 20.bitcoin()

        val sourceAccount = mockSourceAccount(totalBalance, availableBalance)
        val txTarget = mockTransactionTarget()

        val error: NabuApiException = mock {
            on { getErrorCode() }.thenReturn(NabuErrorCodes.PendingOrdersLimitReached)
        }

        whenever(quotesEngine.getPriceQuote()).thenReturn(Observable.error(error))
        whenever(quotesEngine.getSampleDepositAddress()).thenReturn(Single.just(SAMPLE_DEPOSIT_ADDRESS))

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
                    it.feeAmount == CryptoValue.zero(SRC_ASSET) &&
                    it.selectedFiat == TEST_API_FIAT &&
                    it.txConfirmations.isEmpty() &&
                    it.limits == null &&
                    it.validationState == ValidationState.PENDING_ORDERS_LIMIT_REACHED &&
                    it.engineState.isEmpty()
            }
            .assertValue {
                // Special case - when init fails because limits, we expect an empty fee selection:
                it.feeSelection.selectedLevel == FeeLevel.None &&
                    it.feeSelection.availableLevels.size == 1 &&
                    it.feeSelection.availableLevels.contains(FeeLevel.None)
            }
            .assertNoErrors()
            .assertComplete()

        verify(sourceAccount, atLeastOnce()).currency
        verify(txTarget, atLeastOnce()).currency
        verifyQuotesEngineStarted()
        verify(quotesEngine).getPriceQuote()
        verify(quotesEngine).getSampleDepositAddress()

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `update amount modifies the pendingTx correctly`() {
        // Arrange
        val totalBalance: Money = 21.bitcoin()
        val availableBalance: Money = 20.bitcoin()

        val sourceAccount = mockSourceAccount(totalBalance, availableBalance)
        val txTarget = mockTransactionTarget()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val inputAmount = 2.bitcoin()
        val expectedFee = 0.bitcoin()

        val expectedFeeLevel = FeeLevel.Priority
        val expectedAvailableFeeLevels = setOf(FeeLevel.Priority)

        val pendingTx = PendingTx(
            amount = CryptoValue.zero(SRC_ASSET),
            totalBalance = CryptoValue.zero(SRC_ASSET),
            availableBalance = CryptoValue.zero(SRC_ASSET),
            feeForFullAvailable = CryptoValue.zero(SRC_ASSET),
            feeAmount = CryptoValue.zero(SRC_ASSET),
            selectedFiat = TEST_API_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = expectedFeeLevel,
                availableLevels = expectedAvailableFeeLevels,
                asset = FEE_ASSET
            )
        )

        whenever(onChainEngine.doUpdateAmount(inputAmount, pendingTx))
            .thenReturn(
                Single.just(
                    pendingTx.copy(
                        amount = inputAmount,
                        totalBalance = totalBalance,
                        availableBalance = availableBalance,
                        feeAmount = expectedFee
                    )
                )
            )

        // Act
        subject.doUpdateAmount(
            inputAmount,
            pendingTx
        ).test()
            .assertValue {
                it.amount == inputAmount &&
                    it.totalBalance == totalBalance &&
                    it.availableBalance == availableBalance &&
                    it.feeAmount == expectedFee
            }
            .assertValue { verifyFeeLevels(it.feeSelection) }
            .assertComplete()
            .assertNoErrors()

        verify(sourceAccount, atLeastOnce()).currency
        verify(txTarget, atLeastOnce()).currency
        verifyQuotesEngineStarted()
        verify(quotesEngine).updateAmount(inputAmount)
        verify(onChainEngine).doUpdateAmount(inputAmount, pendingTx)

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `doUpdateFeeLevel has no effect`() {
        // Arrange
        val totalBalance: Money = 21.bitcoin()
        val availableBalance: Money = 20.bitcoin()

        val sourceAccount = mockSourceAccount(totalBalance, availableBalance)
        val txTarget = mockTransactionTarget()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val initialFeeLevel = FeeLevel.Priority
        val expectedAvailableFeeLevels = setOf(FeeLevel.Priority)

        val pendingTx = PendingTx(
            amount = CryptoValue.zero(SRC_ASSET),
            totalBalance = CryptoValue.zero(SRC_ASSET),
            availableBalance = CryptoValue.zero(SRC_ASSET),
            feeForFullAvailable = CryptoValue.zero(SRC_ASSET),
            feeAmount = CryptoValue.zero(SRC_ASSET),
            selectedFiat = TEST_API_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = initialFeeLevel,
                availableLevels = expectedAvailableFeeLevels,
                asset = FEE_ASSET
            )
        )

        whenever(
            onChainEngine.doUpdateFeeLevel(
                pendingTx,
                FeeLevel.Regular,
                -1
            )
        ).thenReturn(
            Single.just(
                pendingTx.copy(
                    feeSelection = pendingTx.feeSelection.copy(selectedLevel = FeeLevel.Regular)
                )
            )
        )

        // Act
        subject.doUpdateFeeLevel(
            pendingTx,
            FeeLevel.Priority,
            -1
        ).test()
            .assertValue {
                verifyFeeLevels(it.feeSelection)
            }
            .assertComplete()
            .assertNoErrors()

        verify(sourceAccount, atLeastOnce()).currency
        verify(txTarget, atLeastOnce()).currency
        verifyQuotesEngineStarted()

        noMoreInteractions(sourceAccount, txTarget)
    }

    private fun mockSourceAccount(
        totalBalance: Money = CryptoValue.zero(SRC_ASSET),
        availableBalance: Money = CryptoValue.zero(SRC_ASSET)
    ) = mock<BtcCryptoWalletAccount> {
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

    private fun mockTransactionTarget() = mock<FiatAccount> {
        on { currency }.thenReturn(TEST_API_FIAT)
    }

    private fun whenOnChainEngineInitOK(
        totalBalance: Money,
        availableBalance: Money
    ) {
        val initialisedPendingTx = PendingTx(
            amount = CryptoValue.zero(SRC_ASSET),
            totalBalance = totalBalance,
            availableBalance = availableBalance,
            feeForFullAvailable = CryptoValue.zero(SRC_ASSET),
            feeAmount = CryptoValue.zero(SRC_ASSET),
            selectedFiat = TEST_USER_FIAT,
            feeSelection = FeeSelection(
                selectedLevel = FeeLevel.Priority,
                availableLevels = setOf(FeeLevel.Priority),
                asset = FEE_ASSET
            )
        )
        whenever(onChainEngine.doInitialiseTx()).thenReturn(Single.just(initialisedPendingTx))
    }

    private fun whenUserIsGold() {
        val kycTiers: KycTiers = mock()

        whenever(
            walletManager.getProductTransferLimits(
                currency = TEST_API_FIAT,
                product = Product.SELL,
                orderDirection = TransferDirection.FROM_USERKEY
            )
        ).thenReturn(
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
        verify(walletManager).getProductTransferLimits(
            currency = TEST_API_FIAT,
            product = Product.SELL,
            orderDirection = TransferDirection.FROM_USERKEY
        )
        verify(limitsDataManager).getLimits(
            outputCurrency = eq(SRC_ASSET),
            sourceCurrency = eq(SRC_ASSET),
            targetCurrency = eq(TEST_API_FIAT),
            sourceAccountType = eq(NON_CUSTODIAL_LIMITS_ACCOUNT),
            targetAccountType = eq(CUSTODIAL_LIMITS_ACCOUNT),
            legacyLimits = any()
        )
    }

    private fun verifyOnChainEngineStarted(srcAccount: CryptoAccount) {
        verify(onChainEngine).start(
            sourceAccount = eq(srcAccount),
            txTarget = argThat { this is BtcAddress && address == SAMPLE_DEPOSIT_ADDRESS },
            exchangeRates = eq(exchangeRates),
            refreshTrigger = any()
        )
    }

    private fun verifyQuotesEngineStarted() {
        verify(quotesEngine).start(
            Product.SELL,
            TransferDirection.FROM_USERKEY,
            CurrencyPair(SRC_ASSET, TEST_API_FIAT)
        )
    }

    private fun verifyFeeLevels(
        feeSelection: FeeSelection,
        feeAsset: AssetInfo? = FEE_ASSET
    ) = feeSelection.selectedLevel == EXPECTED_FEE_LEVEL &&
        feeSelection.availableLevels == EXPECTED_FEE_OPTIONS &&
        feeSelection.availableLevels.contains(feeSelection.selectedLevel) &&
        feeSelection.asset == feeAsset &&
        feeSelection.customAmount == -1L

    private fun noMoreInteractions(sourceAccount: BlockchainAccount, txTarget: TransactionTarget) {
        verifyNoMoreInteractions(txTarget)
        verifyNoMoreInteractions(walletManager)
        verifyNoMoreInteractions(currencyPrefs)
        verifyNoMoreInteractions(exchangeRates)
        verifyNoMoreInteractions(quotesEngine)
        verifyNoMoreInteractions(onChainEngine)
        verifyNoMoreInteractions(sourceAccount)
    }

    companion object {
        private val SRC_ASSET = CryptoCurrency.BTC

        private val WRONG_ASSET = CryptoCurrency.BTC
        private val FEE_ASSET = CryptoCurrency.BTC
        private val EXPECTED_FEE_LEVEL = FeeLevel.Priority
        private val EXPECTED_FEE_OPTIONS = setOf(FeeLevel.Priority)

        private const val SAMPLE_DEPOSIT_ADDRESS = "initial quote deposit address"

        private val MIN_GOLD_LIMIT = FiatValue.fromMajor(TEST_API_FIAT, 100.toBigDecimal())
        private val MAX_GOLD_ORDER = FiatValue.fromMajor(TEST_API_FIAT, 500.toBigDecimal())
        private val MAX_GOLD_LIMIT = FiatValue.fromMajor(TEST_API_FIAT, 2000.toBigDecimal())

        private val ASSET_TO_API_FIAT_RATE = 2.toBigDecimal()
        private val ASSET_TO_USER_FIAT_RATE = 5.toBigDecimal()

        private val MIN_GOLD_LIMIT_ASSET = CryptoValue.fromMajor(SRC_ASSET, 50.toBigDecimal())
        private val MAX_GOLD_ORDER_ASSET = CryptoValue.fromMajor(SRC_ASSET, 250.toBigDecimal())
        private val MAX_GOLD_LIMIT_ASSET = CryptoValue.fromMajor(SRC_ASSET, 1000.toBigDecimal())
    }
}
