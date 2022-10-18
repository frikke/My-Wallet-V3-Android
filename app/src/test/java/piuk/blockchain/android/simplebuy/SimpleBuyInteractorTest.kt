package piuk.blockchain.android.simplebuy

import com.blockchain.analytics.Analytics
import com.blockchain.banking.BankPartnerCallbackProvider
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.Coincore
import com.blockchain.core.custodial.BrokerageDataManager
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.limits.LimitsDataManager
import com.blockchain.core.limits.TxLimit
import com.blockchain.core.limits.TxLimits
import com.blockchain.core.payments.PaymentsRepository
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.coreandroid.remoteconfig.RemoteConfigRepository
import com.blockchain.domain.eligibility.EligibilityService
import com.blockchain.domain.eligibility.model.Region
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.CardService
import com.blockchain.domain.paymentmethods.PaymentMethodService
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.BuySellOrder
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.PaymentAttributes
import com.blockchain.nabu.datamanagers.SimpleBuyEligibilityProvider
import com.blockchain.nabu.datamanagers.repositories.WithdrawLocksRepository
import com.blockchain.outcome.Outcome
import com.blockchain.payments.core.CardAcquirer
import com.blockchain.payments.core.CardProcessor
import com.blockchain.preferences.BankLinkingPrefs
import com.blockchain.preferences.OnboardingPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Single
import java.math.BigDecimal
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.domain.repositories.TradeDataService
import piuk.blockchain.android.domain.usecases.CancelOrderUseCase
import piuk.blockchain.android.domain.usecases.GetAvailablePaymentMethodsTypesUseCase
import piuk.blockchain.android.ui.transactionflow.engine.domain.QuickFillRoundingService
import piuk.blockchain.android.ui.transactionflow.engine.domain.model.QuickFillRoundingData

class SimpleBuyInteractorTest {

    private lateinit var subject: SimpleBuyInteractor
    private val kycService: KycService = mock()
    private val custodialWalletManager: CustodialWalletManager = mock()
    private val limitsDataManager: LimitsDataManager = mock()
    private val withdrawLocksRepository: WithdrawLocksRepository = mock()
    private val analytics: Analytics = mock()
    private val bankPartnerCallbackProvider: BankPartnerCallbackProvider = mock()
    private val eligibilityProvider: SimpleBuyEligibilityProvider = mock()
    private val exchangeRatesDataManager: ExchangeRatesDataManager = mock()
    private val coincore: Coincore = mock()
    private val userIdentity: UserIdentity = mock()
    private val bankLinkingPrefs: BankLinkingPrefs = mock()
    private val cardProcessors: Map<CardAcquirer, CardProcessor> = mock()
    private val cancelOrderUseCase: CancelOrderUseCase = mock()
    private val getAvailablePaymentMethodsTypesUseCase: GetAvailablePaymentMethodsTypesUseCase = mock()
    private val bankService: BankService = mock()
    private val cardService: CardService = mock()
    private val paymentMethodService: PaymentMethodService = mock()
    private val paymentsRepository: PaymentsRepository = mock()
    private val cardRejectionCheckFeatureFlag: FeatureFlag = mock()
    private val rbFrequencySuggestion: FeatureFlag = mock()
    private val rbExperimentFF: FeatureFlag = mock()
    private val remoteConfigRepository: RemoteConfigRepository = mock()
    private val tradeDataService: TradeDataService = mock()
    private val buyQuoteRefreshFF: FeatureFlag = mock()
    private val plaidFF: FeatureFlag = mock()
    private val cardPaymentAsyncFF: FeatureFlag = mock()
    private val feynmanEnterAmountScreenFF: FeatureFlag = mock()
    private val feynmanCheckoutScreenFF: FeatureFlag = mock()
    private val brokerageDataManager: BrokerageDataManager = mock()
    private val simpleBuyPrefs: SimpleBuyPrefs = mock()
    private val onboardingPrefs: OnboardingPrefs = mock()
    private val eligibilityService: EligibilityService = mock {
        onBlocking { getStatesList(any(), any()) }.thenReturn(
            Outcome.Success(
                listOf(
                    Region.State("US", "Florida", false, "US-FL"),
                    Region.State("US", "Georgia", false, "US-GA"),
                    Region.State("US", "North Carolina", false, "US-NC")
                )
            )
        )
    }
    private val quickFillRoundingService: QuickFillRoundingService = mock()

    @Before
    fun setup() {
        subject = SimpleBuyInteractor(
            withdrawLocksRepository = withdrawLocksRepository,
            kycService = kycService,
            custodialWalletManager = custodialWalletManager,
            limitsDataManager = limitsDataManager,
            coincore = coincore,
            userIdentity = userIdentity,
            eligibilityProvider = eligibilityProvider,
            bankLinkingPrefs = bankLinkingPrefs,
            analytics = analytics,
            exchangeRatesDataManager = exchangeRatesDataManager,
            bankPartnerCallbackProvider = bankPartnerCallbackProvider,
            cardProcessors = cardProcessors,
            cancelOrderUseCase = cancelOrderUseCase,
            getAvailablePaymentMethodsTypesUseCase = getAvailablePaymentMethodsTypesUseCase,
            bankService = bankService,
            cardService = cardService,
            paymentMethodService = paymentMethodService,
            paymentsRepository = paymentsRepository,
            simpleBuyPrefs = simpleBuyPrefs,
            onboardingPrefs = onboardingPrefs,
            eligibilityService = eligibilityService,
            cardPaymentAsyncFF = cardPaymentAsyncFF,
            buyQuoteRefreshFF = buyQuoteRefreshFF,
            plaidFF = plaidFF,
            rbFrequencySuggestionFF = rbFrequencySuggestion,
            cardRejectionFF = cardRejectionCheckFeatureFlag,
            rbExperimentFF = rbExperimentFF,
            remoteConfigRepository = remoteConfigRepository,
            tradeDataService = tradeDataService,
            feynmanEnterAmountFF = feynmanEnterAmountScreenFF,
            feynmanCheckoutFF = feynmanCheckoutScreenFF,
            quickFillRoundingService = quickFillRoundingService,
            brokerageDataManager = brokerageDataManager
        )

        whenever(quickFillRoundingService.getQuickFillRoundingForAction(AssetAction.Buy)).thenReturn(
            Single.just(
                listOf(
                    QuickFillRoundingData.BuyRoundingData(2, 10),
                    QuickFillRoundingData.BuyRoundingData(2, 50),
                    QuickFillRoundingData.BuyRoundingData(2, 100)
                )
            )
        )
    }

    @Test
    fun `when no previous buy amount for pair available then default value is returned and quick fill buttons are correct`() {
        whenever(simpleBuyPrefs.getLastAmount(any())).thenReturn("")

        val fiatCurrency = FiatCurrency.Dollars
        val assetCode = "BTC"
        val maxAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(1000))
        val minAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(10))
        val limits = TxLimits(min = TxLimit.Limited(minAmount), max = TxLimit.Limited(maxAmount))
        val defaultAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(50))

        val test = subject.getPrefillAndQuickFillAmounts(limits, assetCode, fiatCurrency).test()
        test.assertValue {
            it.first == defaultAmount &&
                it.second?.maxAmount == maxAmount &&
                it.second!!.quickFillButtons[0].amount == FiatValue.fromMajor(fiatCurrency, BigDecimal(100)) &&
                it.second!!.quickFillButtons[1].amount == FiatValue.fromMajor(fiatCurrency, BigDecimal(200)) &&
                it.second!!.quickFillButtons[2].amount == FiatValue.fromMajor(fiatCurrency, BigDecimal(400))
        }

        verify(quickFillRoundingService).getQuickFillRoundingForAction(AssetAction.Buy)
        verifyNoMoreInteractions(quickFillRoundingService)
    }

    @Test
    fun `when no previous buy amount for pair available and default value is lower than min returned and quick fill buttons are correct`() {
        whenever(simpleBuyPrefs.getLastAmount(any())).thenReturn("")
        val fiatCurrency = FiatCurrency.Dollars
        val assetCode = "BTC"
        val maxAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(1000))
        val minAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(100))
        val limits = TxLimits(min = TxLimit.Limited(minAmount), max = TxLimit.Limited(maxAmount))
        val defaultAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(50))

        val prefilledAmount = if (defaultAmount < minAmount) minAmount else defaultAmount

        val test = subject.getPrefillAndQuickFillAmounts(limits, assetCode, fiatCurrency).test()
        test.assertValue {
            it.first == prefilledAmount &&
                it.second?.maxAmount == maxAmount &&
                it.second!!.quickFillButtons[0].amount == FiatValue.fromMajor(fiatCurrency, BigDecimal(200)) &&
                it.second!!.quickFillButtons[1].amount == FiatValue.fromMajor(fiatCurrency, BigDecimal(400))
        }

        verify(quickFillRoundingService).getQuickFillRoundingForAction(AssetAction.Buy)
        verifyNoMoreInteractions(quickFillRoundingService)
    }

    @Test
    fun `when no previous buy amount for pair available and default value is higher than max returned and quick fill buttons are correct`() {
        whenever(simpleBuyPrefs.getLastAmount(any())).thenReturn("")
        val fiatCurrency = FiatCurrency.Dollars
        val assetCode = "BTC"
        val maxAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(40))
        val minAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(10))
        val limits = TxLimits(min = TxLimit.Limited(minAmount), max = TxLimit.Limited(maxAmount))
        val defaultAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(50))

        val prefilledAmount = if (defaultAmount > maxAmount) maxAmount else defaultAmount

        val test = subject.getPrefillAndQuickFillAmounts(limits, assetCode, fiatCurrency).test()
        test.assertValue {
            it.first == prefilledAmount &&
                it.second?.maxAmount == maxAmount
        }

        verify(quickFillRoundingService).getQuickFillRoundingForAction(AssetAction.Buy)
        verifyNoMoreInteractions(quickFillRoundingService)
    }

    @Test
    fun `when previous buy amount available for pair then it is returned and quick fill buttons are correct`() {
        whenever(simpleBuyPrefs.getLastAmount("BTC-USD")).thenReturn("100")
        val fiatCurrency = FiatCurrency.Dollars
        val assetCode = "BTC"
        val maxAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(1000))
        val minAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(80))
        val limits = TxLimits(min = TxLimit.Limited(minAmount), max = TxLimit.Limited(maxAmount))

        val test = subject.getPrefillAndQuickFillAmounts(limits, assetCode, fiatCurrency).test()
        test.assertValue {
            it.first == FiatValue.fromMajor(fiatCurrency, BigDecimal(100)) &&
                it.second?.maxAmount == limits.maxAmount &&
                it.second!!.quickFillButtons[0].amount == FiatValue.fromMajor(fiatCurrency, BigDecimal(200)) &&
                it.second!!.quickFillButtons[1].amount == FiatValue.fromMajor(fiatCurrency, BigDecimal(400)) &&
                it.second!!.quickFillButtons[2].amount == FiatValue.fromMajor(fiatCurrency, BigDecimal(800))
        }

        verify(quickFillRoundingService).getQuickFillRoundingForAction(AssetAction.Buy)
        verifyNoMoreInteractions(quickFillRoundingService)
    }

    @Test
    fun `when first quick fill button over payment limit then none are returned`() {
        whenever(simpleBuyPrefs.getLastAmount("BTC-USD")).thenReturn("100")
        val fiatCurrency = FiatCurrency.Dollars
        val assetCode = "BTC"
        val maxAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(80))
        val minAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(10))
        val limits = TxLimits(min = TxLimit.Limited(minAmount), max = TxLimit.Limited(maxAmount))

        val test = subject.getPrefillAndQuickFillAmounts(limits, assetCode, fiatCurrency).test()
        test.assertValue {
            it.first == maxAmount &&
                it.second?.maxAmount == maxAmount &&
                it.second!!.quickFillButtons.isEmpty()
        }

        verify(quickFillRoundingService).getQuickFillRoundingForAction(AssetAction.Buy)
        verifyNoMoreInteractions(quickFillRoundingService)
    }

    @Test
    fun `when second quick fill button over payment limit then last two are not returned`() {
        whenever(simpleBuyPrefs.getLastAmount("BTC-USD")).thenReturn("50")
        val fiatCurrency = FiatCurrency.Dollars
        val assetCode = "BTC"
        val maxAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(300))
        val minAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(100))
        val limits = TxLimits(min = TxLimit.Limited(minAmount), max = TxLimit.Limited(maxAmount))

        val test = subject.getPrefillAndQuickFillAmounts(limits, assetCode, fiatCurrency).test()
        test.assertValue {
            it.first == minAmount &&
                it.second?.maxAmount == maxAmount &&
                it.second!!.quickFillButtons.size == 1 &&
                it.second!!.quickFillButtons[0].amount == FiatValue.fromMajor(fiatCurrency, BigDecimal(200))
        }

        verify(quickFillRoundingService).getQuickFillRoundingForAction(AssetAction.Buy)
        verifyNoMoreInteractions(quickFillRoundingService)
    }

    @Test
    fun `when third quick fill button over payment limit then last is not returned`() {
        whenever(simpleBuyPrefs.getLastAmount("BTC-USD")).thenReturn("50")
        val fiatCurrency = FiatCurrency.Dollars
        val assetCode = "BTC"
        val maxAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(500))
        val minAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(100))
        val limits = TxLimits(min = TxLimit.Limited(minAmount), max = TxLimit.Limited(maxAmount))

        val test = subject.getPrefillAndQuickFillAmounts(limits, assetCode, fiatCurrency).test()
        test.assertValue {
            it.first == limits.minAmount &&
                it.second?.maxAmount == limits.maxAmount &&
                it.second!!.quickFillButtons.size == 2 &&
                it.second!!.quickFillButtons[0].amount == FiatValue.fromMajor(fiatCurrency, BigDecimal(200)) &&
                it.second!!.quickFillButtons[1].amount == FiatValue.fromMajor(fiatCurrency, BigDecimal(400))
        }

        verify(quickFillRoundingService).getQuickFillRoundingForAction(AssetAction.Buy)
        verifyNoMoreInteractions(quickFillRoundingService)
    }

    @Test
    fun `when a list of States is requested, check if list is valid`() {
        val test = subject.getListOfStates("US").test()
        test.await().assertValue {
            it.isNotEmpty() &&
                it.size == 3 &&
                it.contains(Region.State("US", "Georgia", false, "US-GA"))
        }
    }

    @Test
    fun `pollForOrderStatus should finish when OrderState is FINISHED`() {
        val order: BuySellOrder = mock()
        whenever(order.state).thenReturn(OrderState.FINISHED)
        whenever(cardPaymentAsyncFF.enabled).thenReturn(Single.just(false))
        whenever(custodialWalletManager.getBuyOrder(ORDER_ID)).thenReturn(Single.just(order))

        subject.pollForOrderStatus(ORDER_ID).test()
            .assertComplete()
            .assertValue { it.value == order }
    }

    @Test
    fun `pollForOrderStatus should finish when OrderState is FAILED`() {
        val order: BuySellOrder = mock()
        whenever(order.state).thenReturn(OrderState.FAILED)
        whenever(cardPaymentAsyncFF.enabled).thenReturn(Single.just(false))
        whenever(custodialWalletManager.getBuyOrder(ORDER_ID)).thenReturn(Single.just(order))

        subject.pollForOrderStatus(ORDER_ID).test()
            .assertComplete()
            .assertValue { it.value == order }
    }

    @Test
    fun `pollForOrderStatus should finish when OrderState is CANCELED`() {
        val order: BuySellOrder = mock()
        whenever(order.state).thenReturn(OrderState.CANCELED)
        whenever(cardPaymentAsyncFF.enabled).thenReturn(Single.just(false))
        whenever(custodialWalletManager.getBuyOrder(ORDER_ID)).thenReturn(Single.just(order))

        subject.pollForOrderStatus(ORDER_ID).test()
            .assertComplete()
            .assertValue { it.value == order }
    }

    @Test
    fun `pollForOrderStatus for PAYMENT_CARD should finish when attributes are returned`() {
        val order: BuySellOrder = mock()
        val attributes: PaymentAttributes = mock()
        whenever(order.attributes).thenReturn(attributes)
        whenever(order.paymentMethodType).thenReturn(PaymentMethodType.PAYMENT_CARD)
        whenever(cardPaymentAsyncFF.enabled).thenReturn(Single.just(true))
        whenever(custodialWalletManager.getBuyOrder(ORDER_ID)).thenReturn(Single.just(order))

        subject.pollForOrderStatus(ORDER_ID).test()
            .assertComplete()
            .assertValue { it.value == order }
    }

    @Test
    fun `pollForOrderStatus for GOOGLE_PAY should finish when attributes are returned`() {
        val order: BuySellOrder = mock()
        val attributes: PaymentAttributes = mock()
        whenever(order.attributes).thenReturn(attributes)
        whenever(order.paymentMethodType).thenReturn(PaymentMethodType.GOOGLE_PAY)
        whenever(cardPaymentAsyncFF.enabled).thenReturn(Single.just(true))
        whenever(custodialWalletManager.getBuyOrder(ORDER_ID)).thenReturn(Single.just(order))

        subject.pollForOrderStatus(ORDER_ID).test()
            .assertComplete()
            .assertValue { it.value == order }
    }

    private companion object {
        private const val ORDER_ID = "orderId"
    }
}
