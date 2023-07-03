package piuk.blockchain.android.simplebuy

import com.blockchain.android.testutils.rxInit
import com.blockchain.core.announcements.DismissRecorder
import com.blockchain.core.buy.domain.SimpleBuyService
import com.blockchain.core.custodial.BrokerageDataManager
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.limits.LimitsDataManager
import com.blockchain.core.limits.TxLimit
import com.blockchain.core.limits.TxLimits
import com.blockchain.core.payments.PaymentsRepository
import com.blockchain.core.recurringbuy.domain.RecurringBuyService
import com.blockchain.coreandroid.remoteconfig.RemoteConfigRepository
import com.blockchain.domain.eligibility.EligibilityService
import com.blockchain.domain.eligibility.model.Region
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.CardService
import com.blockchain.domain.paymentmethods.PaymentMethodService
import com.blockchain.domain.paymentmethods.model.BankPartnerCallbackProvider
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.trade.TradeDataService
import com.blockchain.domain.trade.model.QuickFillRoundingData
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.datamanagers.BuySellOrder
import com.blockchain.nabu.datamanagers.CardAttributes
import com.blockchain.nabu.datamanagers.CardPaymentState
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.OrderState
import com.blockchain.nabu.datamanagers.PaymentAttributes
import com.blockchain.nabu.datamanagers.repositories.WithdrawLocksRepository
import com.blockchain.outcome.Outcome
import com.blockchain.payments.core.CardAcquirer
import com.blockchain.payments.core.CardProcessor
import com.blockchain.preferences.BankLinkingPrefs
import com.blockchain.preferences.OnboardingPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.reactivex.rxjava3.core.Single
import java.math.BigDecimal
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.domain.usecases.CancelOrderUseCase
import piuk.blockchain.android.domain.usecases.GetAvailablePaymentMethodsTypesUseCase

class SimpleBuyInteractorTest {

    private lateinit var subject: SimpleBuyInteractor
    private val kycService: KycService = mock()
    private val custodialWalletManager: CustodialWalletManager = mock()
    private val limitsDataManager: LimitsDataManager = mock()
    private val withdrawLocksRepository: WithdrawLocksRepository = mock()
    private val bankPartnerCallbackProvider: BankPartnerCallbackProvider = mock()
    private val simpleBuyService: SimpleBuyService = mock()
    private val bankLinkingPrefs: BankLinkingPrefs = mock()
    private val cardProcessors: Map<CardAcquirer, CardProcessor> = mock()
    private val cancelOrderUseCase: CancelOrderUseCase = mock()
    private val getAvailablePaymentMethodsTypesUseCase: GetAvailablePaymentMethodsTypesUseCase = mock()
    private val bankService: BankService = mock()
    private val cardService: CardService = mock()
    private val paymentMethodService: PaymentMethodService = mock()
    private val paymentsRepository: PaymentsRepository = mock()
    private val rbExperimentFF: FeatureFlag = mock()
    private val remoteConfigRepository: RemoteConfigRepository = mock()
    private val tradeDataService: TradeDataService = mockk()
    private val buyQuoteRefreshFF: FeatureFlag = mock()
    private val plaidFF: FeatureFlag = mock()
    private val cardPaymentAsyncFF: FeatureFlag = mock()
    private val feynmanEnterAmountScreenFF: FeatureFlag = mock()
    private val feynmanCheckoutScreenFF: FeatureFlag = mock()
    private val improvedPaymentUxFF: FeatureFlag = mock()
    private val brokerageDataManager: BrokerageDataManager = mock()
    private val simpleBuyPrefs: SimpleBuyPrefs = mock()
    private val onboardingPrefs: OnboardingPrefs = mock()
    private val recurringBuyService: RecurringBuyService = mock()
    private val dismissRecorder: DismissRecorder = mock()
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

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setup() {
        subject = SimpleBuyInteractor(
            withdrawLocksRepository = withdrawLocksRepository,
            kycService = kycService,
            custodialWalletManager = custodialWalletManager,
            limitsDataManager = limitsDataManager,
            simpleBuyService = simpleBuyService,
            bankLinkingPrefs = bankLinkingPrefs,
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
            rbExperimentFF = rbExperimentFF,
            remoteConfigRepository = remoteConfigRepository,
            feynmanEnterAmountFF = feynmanEnterAmountScreenFF,
            feynmanCheckoutFF = feynmanCheckoutScreenFF,
            tradeDataService = tradeDataService,
            brokerageDataManager = brokerageDataManager,
            improvedPaymentUxFF = improvedPaymentUxFF,
            recurringBuyService = recurringBuyService,
            dismissRecorder = dismissRecorder
        )

        coEvery { tradeDataService.getQuickFillRoundingForBuy() } returns Outcome.Success(
            listOf(
                QuickFillRoundingData.BuyRoundingData(2, 10),
                QuickFillRoundingData.BuyRoundingData(2, 50),
                QuickFillRoundingData.BuyRoundingData(2, 100)
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

        val test = subject.getPrefillAndQuickFillAmounts(
            limits,
            assetCode,
            fiatCurrency,
            false,
            FiatValue.zero(FiatCurrency.Dollars)
        ).test()
        test.assertValue {
            it.first == defaultAmount &&
                it.second?.maxAmount == maxAmount &&
                it.second!!.quickFillButtons[0].amount == FiatValue.fromMajor(fiatCurrency, BigDecimal(100)) &&
                it.second!!.quickFillButtons[1].amount == FiatValue.fromMajor(fiatCurrency, BigDecimal(200)) &&
                it.second!!.quickFillButtons[2].amount == FiatValue.fromMajor(fiatCurrency, BigDecimal(400))
        }

        coVerify(exactly = 1) { tradeDataService.getQuickFillRoundingForBuy() }
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

        val test = subject.getPrefillAndQuickFillAmounts(
            limits,
            assetCode,
            fiatCurrency,
            false,
            FiatValue.zero(FiatCurrency.Dollars)
        ).test()
        test.assertValue {
            it.first == prefilledAmount &&
                it.second?.maxAmount == maxAmount &&
                it.second!!.quickFillButtons[0].amount == FiatValue.fromMajor(fiatCurrency, BigDecimal(200)) &&
                it.second!!.quickFillButtons[1].amount == FiatValue.fromMajor(fiatCurrency, BigDecimal(400))
        }

        coVerify(exactly = 1) { tradeDataService.getQuickFillRoundingForBuy() }
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

        val test = subject.getPrefillAndQuickFillAmounts(
            limits,
            assetCode,
            fiatCurrency,
            false,
            FiatValue.zero(FiatCurrency.Dollars)
        ).test()
        test.assertValue {
            it.first == prefilledAmount &&
                it.second?.maxAmount == maxAmount
        }

        coVerify(exactly = 1) { tradeDataService.getQuickFillRoundingForBuy() }
    }

    @Test
    fun `when previous buy amount available for pair then it is returned and quick fill buttons are correct`() {
        whenever(simpleBuyPrefs.getLastAmount("BTC-USD")).thenReturn("100")
        val fiatCurrency = FiatCurrency.Dollars
        val assetCode = "BTC"
        val maxAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(1000))
        val minAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(80))
        val limits = TxLimits(min = TxLimit.Limited(minAmount), max = TxLimit.Limited(maxAmount))

        val test = subject.getPrefillAndQuickFillAmounts(
            limits,
            assetCode,
            fiatCurrency,
            false,
            FiatValue.zero(FiatCurrency.Dollars)
        ).test()
        test.assertValue {
            it.first == FiatValue.fromMajor(fiatCurrency, BigDecimal(100)) &&
                it.second?.maxAmount == limits.maxAmount &&
                it.second!!.quickFillButtons[0].amount == FiatValue.fromMajor(fiatCurrency, BigDecimal(200)) &&
                it.second!!.quickFillButtons[1].amount == FiatValue.fromMajor(fiatCurrency, BigDecimal(400)) &&
                it.second!!.quickFillButtons[2].amount == FiatValue.fromMajor(fiatCurrency, BigDecimal(800))
        }

        coVerify(exactly = 1) { tradeDataService.getQuickFillRoundingForBuy() }
    }

    @Test
    fun `when first quick fill button over payment limit then none are returned`() {
        whenever(simpleBuyPrefs.getLastAmount("BTC-USD")).thenReturn("100")
        val fiatCurrency = FiatCurrency.Dollars
        val assetCode = "BTC"
        val maxAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(80))
        val minAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(10))
        val limits = TxLimits(min = TxLimit.Limited(minAmount), max = TxLimit.Limited(maxAmount))

        val test = subject.getPrefillAndQuickFillAmounts(
            limits,
            assetCode,
            fiatCurrency,
            false,
            FiatValue.zero(FiatCurrency.Dollars)
        ).test()
        test.assertValue {
            it.first == maxAmount &&
                it.second?.maxAmount == maxAmount &&
                it.second!!.quickFillButtons.isEmpty()
        }

        coVerify(exactly = 1) { tradeDataService.getQuickFillRoundingForBuy() }
    }

    @Test
    fun `when second quick fill button over payment limit then last two are not returned`() {
        whenever(simpleBuyPrefs.getLastAmount("BTC-USD")).thenReturn("50")
        val fiatCurrency = FiatCurrency.Dollars
        val assetCode = "BTC"
        val maxAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(300))
        val minAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(100))
        val limits = TxLimits(min = TxLimit.Limited(minAmount), max = TxLimit.Limited(maxAmount))

        val test = subject.getPrefillAndQuickFillAmounts(
            limits,
            assetCode,
            fiatCurrency,
            false,
            FiatValue.zero(FiatCurrency.Dollars)
        ).test()
        test.assertValue {
            it.first == minAmount &&
                it.second?.maxAmount == maxAmount &&
                it.second!!.quickFillButtons.size == 1 &&
                it.second!!.quickFillButtons[0].amount == FiatValue.fromMajor(fiatCurrency, BigDecimal(200))
        }

        coVerify(exactly = 1) { tradeDataService.getQuickFillRoundingForBuy() }
    }

    @Test
    fun `when third quick fill button over payment limit then last is not returned`() = runTest {
        whenever(simpleBuyPrefs.getLastAmount("BTC-USD")).thenReturn("50")
        val fiatCurrency = FiatCurrency.Dollars
        val assetCode = "BTC"
        val maxAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(500))
        val minAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(100))
        val limits = TxLimits(min = TxLimit.Limited(minAmount), max = TxLimit.Limited(maxAmount))

        val test = subject.getPrefillAndQuickFillAmounts(
            limits,
            assetCode,
            fiatCurrency,
            false,
            FiatValue.zero(FiatCurrency.Dollars)
        ).test()
        test.assertValue {
            it.first == limits.minAmount &&
                it.second?.maxAmount == limits.maxAmount &&
                it.second!!.quickFillButtons.size == 2 &&
                it.second!!.quickFillButtons[0].amount == FiatValue.fromMajor(fiatCurrency, BigDecimal(200)) &&
                it.second!!.quickFillButtons[1].amount == FiatValue.fromMajor(fiatCurrency, BigDecimal(400))
        }

        coVerify(exactly = 1) { tradeDataService.getQuickFillRoundingForBuy() }
    }

    @Test
    fun `when amount comes from deeplink given previous amount then it is respected`() {
        whenever(simpleBuyPrefs.getLastAmount("BTC-USD")).thenReturn("50")
        val fiatCurrency = FiatCurrency.Dollars
        val assetCode = "BTC"
        val maxAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(2000))
        val minAmount = FiatValue.fromMajor(fiatCurrency, BigDecimal(20))
        val limits = TxLimits(min = TxLimit.Limited(minAmount), max = TxLimit.Limited(maxAmount))

        val prepopulatedAmount = Money.fromMajor(FiatCurrency.Dollars, BigDecimal(50))
        val test = subject.getPrefillAndQuickFillAmounts(
            limits,
            assetCode,
            fiatCurrency,
            true,
            prepopulatedAmount
        ).test()

        test.assertValue {
            it.first == prepopulatedAmount &&
                it.second?.maxAmount == limits.maxAmount &&
                it.second!!.quickFillButtons.size == 3 &&
                it.second!!.quickFillButtons[0].amount == FiatValue.fromMajor(fiatCurrency, BigDecimal(100)) &&
                it.second!!.quickFillButtons[1].amount == FiatValue.fromMajor(fiatCurrency, BigDecimal(200)) &&
                it.second!!.quickFillButtons[2].amount == FiatValue.fromMajor(fiatCurrency, BigDecimal(400))
        }

        coVerify(exactly = 1) { tradeDataService.getQuickFillRoundingForBuy() }
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

        subject.pollForOrderStatus(ORDER_ID, false, false).test()
            .assertComplete()
            .assertValue { it.value == order }
    }

    @Test
    fun `pollForOrderStatus should finish when OrderState is FAILED`() {
        val order: BuySellOrder = mock()
        whenever(order.state).thenReturn(OrderState.FAILED)
        whenever(cardPaymentAsyncFF.enabled).thenReturn(Single.just(false))
        whenever(custodialWalletManager.getBuyOrder(ORDER_ID)).thenReturn(Single.just(order))

        subject.pollForOrderStatus(ORDER_ID, false, false).test()
            .assertComplete()
            .assertValue { it.value == order }
    }

    @Test
    fun `pollForOrderStatus should finish when OrderState is CANCELED`() {
        val order: BuySellOrder = mock()
        whenever(order.state).thenReturn(OrderState.CANCELED)
        whenever(cardPaymentAsyncFF.enabled).thenReturn(Single.just(false))
        whenever(custodialWalletManager.getBuyOrder(ORDER_ID)).thenReturn(Single.just(order))

        subject.pollForOrderStatus(ORDER_ID, false, false).test()
            .assertComplete()
            .assertValue { it.value == order }
    }

    @Test
    fun `pollForOrderStatus should finish when needCvv is TRUE`() {
        val order: BuySellOrder = mock()
        val attributes = PaymentAttributes(
            paymentId = null,
            authorisationUrl = null,
            cardAttributes = CardAttributes.Empty,
            needCvv = true
        )
        whenever(order.attributes).thenReturn(attributes)
        whenever(order.paymentMethodType).thenReturn(PaymentMethodType.PAYMENT_CARD)
        whenever(cardPaymentAsyncFF.enabled).thenReturn(Single.just(true))
        whenever(custodialWalletManager.getBuyOrder(ORDER_ID)).thenReturn(Single.just(order))

        subject.pollForOrderStatus(ORDER_ID, false, false).test()
            .assertComplete()
            .assertValue { it.value == order }
    }

    @Test
    fun `pollForOrderStatus should finish when cardPaymentState is WAITING_FOR_3DS`() {
        val order: BuySellOrder = mock()
        val attributes = PaymentAttributes(
            paymentId = null,
            authorisationUrl = null,
            cardAttributes = CardAttributes.EveryPay("", CardPaymentState.WAITING_FOR_3DS)
        )
        whenever(order.attributes).thenReturn(attributes)
        whenever(order.paymentMethodType).thenReturn(PaymentMethodType.PAYMENT_CARD)
        whenever(cardPaymentAsyncFF.enabled).thenReturn(Single.just(true))
        whenever(custodialWalletManager.getBuyOrder(ORDER_ID)).thenReturn(Single.just(order))

        subject.pollForOrderStatus(ORDER_ID, false, false).test()
            .assertComplete()
            .assertValue { it.value == order }
    }

    private companion object {
        private const val ORDER_ID = "orderId"
    }
}
