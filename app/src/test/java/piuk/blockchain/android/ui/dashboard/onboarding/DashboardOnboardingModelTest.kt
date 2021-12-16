package piuk.blockchain.android.ui.dashboard.onboarding

import com.blockchain.android.testutils.rxInit
import com.blockchain.nabu.datamanagers.PaymentLimits
import com.blockchain.nabu.datamanagers.PaymentMethod
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.data.BankPartner
import com.blockchain.nabu.models.data.LinkBankAttributes
import com.blockchain.nabu.models.data.LinkBankTransfer
import com.blockchain.preferences.CurrencyPrefs
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.domain.usecases.CompletableDashboardOnboardingStep
import piuk.blockchain.android.domain.usecases.DashboardOnboardingStep
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

class DashboardOnboardingModelTest {

    private val interactor: DashboardOnboardingInteractor = mock {
        on { getSteps() }.thenReturn(Single.just(STEPS_INITIAL))
    }

    private lateinit var model: DashboardOnboardingModel

    private val currencyPrefs: CurrencyPrefs = mock()
    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() }.thenReturn(false)
    }

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        model = DashboardOnboardingModel(
            initialSteps = STEPS_INITIAL,
            interactor = interactor,
            currencyPrefs = currencyPrefs,
            uiScheduler = Schedulers.io(),
            environmentConfig = environmentConfig,
            crashLogger = mock()
        )
    }

    @Test
    fun `fetching steps success should show steps`() {
        val steps = listOf(
            CompletableDashboardOnboardingStep(DashboardOnboardingStep.UPGRADE_TO_GOLD, true),
            CompletableDashboardOnboardingStep(DashboardOnboardingStep.LINK_PAYMENT_METHOD, false),
            CompletableDashboardOnboardingStep(DashboardOnboardingStep.BUY, false),
        )
        whenever(interactor.getSteps()).thenReturn(Single.just(steps))

        val state = model.state.test()
        model.process(DashboardOnboardingIntent.FetchSteps)

        state.assertValueAt(0) {
            it.steps == STEPS_INITIAL
        }
        state.assertValueAt(1) {
            it.steps == steps
        }

        verify(interactor).getSteps()
    }

    @Test
    fun `fetching steps failure should show error`() {
        val error = IllegalStateException("error")
        whenever(interactor.getSteps()).thenReturn(Single.error(error))

        val state = model.state.test()
        model.process(DashboardOnboardingIntent.FetchSteps)

        state.assertValueAt(1) {
            it.errorState == DashboardOnboardingError.Error(error)
        }
    }

    @Test
    fun `clicking upgrade to gold step should navigate to kyc`() {
        val state = model.state.test()
        model.process(DashboardOnboardingIntent.StepClicked(DashboardOnboardingStep.UPGRADE_TO_GOLD))

        state.assertValueAt(1) {
            it.navigationAction == DashboardOnboardingNavigationAction.StartKyc
        }
    }

    @Test
    fun `given upgrade to gold step not complete, clicking link payment method step should navigate to kyc`() {
        whenever(interactor.getSteps()).thenReturn(Single.just(STEPS_INITIAL))

        val state = model.state.test()
        model.process(DashboardOnboardingIntent.StepClicked(DashboardOnboardingStep.LINK_PAYMENT_METHOD))

        state.assertValueAt(1) {
            it.navigationAction == DashboardOnboardingNavigationAction.StartKyc
        }
    }

    @Test
    fun `given upgrade to gold step complete, clicking link payment method step should check if user has a supported trading currency`() {
        whenever(interactor.getSteps()).thenReturn(Single.just(STEPS_UPGRADE_TO_GOLD_COMPLETE))

        val state = model.state.test()
        model.process(DashboardOnboardingIntent.FetchSteps)
        model.process(DashboardOnboardingIntent.StepClicked(DashboardOnboardingStep.LINK_PAYMENT_METHOD))

        verify(interactor).getSupportedCurrencies()
    }

    @Test
    fun `given user clicked link payment method step, fetching supported currencies success, if user currency is not supported should navigate to select trading currency`() {
        val userCurrency = "EUR"
        val supportedCurrencies = listOf("USD", "GBP")
        whenever(interactor.getSteps()).thenReturn(Single.just(STEPS_UPGRADE_TO_GOLD_COMPLETE))
        whenever(currencyPrefs.tradingCurrency).thenReturn(userCurrency)
        whenever(interactor.getSupportedCurrencies()).thenReturn(Single.just(supportedCurrencies))

        val state = model.state.test()
        model.process(DashboardOnboardingIntent.FetchSteps)
        model.process(DashboardOnboardingIntent.StepClicked(DashboardOnboardingStep.LINK_PAYMENT_METHOD))

        state.assertValueAt(2) {
            it.navigationAction == DashboardOnboardingNavigationAction.SelectTradingCurrency(
                supportedCurrencies, userCurrency
            )
        }
    }

    @Test
    fun `given user clicked link payment method step, fetching supported currencies success, if user currency is supported should fetch eligible payment methods`() {
        val currency = "EUR"
        whenever(interactor.getSteps()).thenReturn(Single.just(STEPS_UPGRADE_TO_GOLD_COMPLETE))
        whenever(currencyPrefs.tradingCurrency).thenReturn(currency)
        whenever(interactor.getSupportedCurrencies()).thenReturn(Single.just(listOf(currency)))

        val state = model.state.test()
        model.process(DashboardOnboardingIntent.FetchSteps)
        model.process(DashboardOnboardingIntent.StepClicked(DashboardOnboardingStep.LINK_PAYMENT_METHOD))

        verify(interactor).getEligiblePaymentMethods(currency)
    }

    @Test
    fun `given user clicked link payment method step, fetching supported currencies failure should show error`() {
        val currency = "EUR"
        val error = IllegalStateException("error")
        whenever(interactor.getSteps()).thenReturn(Single.just(STEPS_UPGRADE_TO_GOLD_COMPLETE))
        whenever(currencyPrefs.tradingCurrency).thenReturn(currency)
        whenever(interactor.getSupportedCurrencies()).thenReturn(Single.error(error))

        val state = model.state.test()
        model.process(DashboardOnboardingIntent.FetchSteps)
        model.process(DashboardOnboardingIntent.StepClicked(DashboardOnboardingStep.LINK_PAYMENT_METHOD))

        state.assertValueAt(2) {
            it.errorState == DashboardOnboardingError.Error(error)
        }
    }

    @Test
    fun `given user clicked link payment method step, on trading currency changed should check trading currency again fetch eligible payment methods`() {
        val userCurrency = "EUR"
        val supportedCurrencies = listOf("USD", "GBP")
        whenever(interactor.getSteps()).thenReturn(Single.just(STEPS_UPGRADE_TO_GOLD_COMPLETE))
        whenever(currencyPrefs.tradingCurrency).thenReturn(userCurrency)
        whenever(interactor.getSupportedCurrencies()).thenReturn(Single.just(supportedCurrencies))

        val state = model.state.test()
        model.process(DashboardOnboardingIntent.FetchSteps)
        model.process(DashboardOnboardingIntent.StepClicked(DashboardOnboardingStep.LINK_PAYMENT_METHOD))

        val newUserCurrency = "USD"
        whenever(currencyPrefs.tradingCurrency).thenReturn(newUserCurrency)

        model.process(DashboardOnboardingIntent.TradingCurrencyChanged)

        verify(interactor).getEligiblePaymentMethods(newUserCurrency)
    }

    @Test
    fun `given user clicked link payment method step, fetching eligible payment methods success should navigate to add payment method`() {
        val currency = "EUR"
        val paymentMethods = listOf(
            PaymentMethod.UndefinedCard(PaymentLimits(0, 0, currency), true),
            PaymentMethod.UndefinedBankAccount(currency, PaymentLimits(0, 0, currency), true),
            PaymentMethod.UndefinedBankTransfer(PaymentLimits(0, 0, currency), true)
        )
        whenever(interactor.getSteps()).thenReturn(Single.just(STEPS_UPGRADE_TO_GOLD_COMPLETE))
        whenever(currencyPrefs.tradingCurrency).thenReturn(currency)
        whenever(interactor.getSupportedCurrencies()).thenReturn(Single.just(listOf(currency)))
        whenever(interactor.getEligiblePaymentMethods(currency)).thenReturn(Single.just(paymentMethods))

        val state = model.state.test()
        model.process(DashboardOnboardingIntent.FetchSteps)
        model.process(DashboardOnboardingIntent.StepClicked(DashboardOnboardingStep.LINK_PAYMENT_METHOD))

        state.assertValueAt(2) {
            it.navigationAction == DashboardOnboardingNavigationAction.AddPaymentMethod(paymentMethods)
        }
    }

    @Test
    fun `given user clicked link payment method step, fetching eligible payment methods failure should show error`() {
        val currency = "EUR"
        val error = IllegalStateException("error")
        whenever(interactor.getSteps()).thenReturn(Single.just(STEPS_UPGRADE_TO_GOLD_COMPLETE))
        whenever(currencyPrefs.tradingCurrency).thenReturn(currency)
        whenever(interactor.getSupportedCurrencies()).thenReturn(Single.just(listOf(currency)))
        whenever(interactor.getEligiblePaymentMethods(currency)).thenReturn(Single.error(error))

        val state = model.state.test()
        model.process(DashboardOnboardingIntent.FetchSteps)
        model.process(DashboardOnboardingIntent.StepClicked(DashboardOnboardingStep.LINK_PAYMENT_METHOD))

        state.assertValueAt(2) {
            it.errorState == DashboardOnboardingError.Error(error)
        }
    }

    @Test
    fun `clicking add card payment method should navigate to Add card`() {
        val state = model.state.test()
        model.process(DashboardOnboardingIntent.PaymentMethodClicked(PaymentMethodType.PAYMENT_CARD))

        state.assertValueAt(1) {
            it.navigationAction == DashboardOnboardingNavigationAction.AddCard
        }
    }

    @Test
    fun `clicking wire transfer payment method should navigate to wire transfer account details`() {
        val currency = "EUR"
        whenever(currencyPrefs.tradingCurrency).thenReturn(currency)

        val state = model.state.test()
        model.process(DashboardOnboardingIntent.PaymentMethodClicked(PaymentMethodType.FUNDS))

        state.assertValueAt(1) {
            it.navigationAction == DashboardOnboardingNavigationAction.WireTransferAccountDetails(currency)
        }
    }

    @Test
    fun `clicking link bank payment method should fetch link bank transfer`() {
        val currency = "EUR"
        whenever(currencyPrefs.tradingCurrency).thenReturn(currency)

        val state = model.state.test()
        model.process(DashboardOnboardingIntent.PaymentMethodClicked(PaymentMethodType.BANK_TRANSFER))

        verify(interactor).linkBank(currency)
    }

    @Test
    fun `fetching link bank transfer success should navigate to link bank`() {
        val currency = "EUR"
        val linkBankTransfer = LinkBankTransfer(
            "id",
            BankPartner.YAPILY,
            object : LinkBankAttributes {}
        )
        whenever(currencyPrefs.tradingCurrency).thenReturn(currency)
        whenever(interactor.linkBank(currency)).thenReturn(Single.just(linkBankTransfer))

        val state = model.state.test()
        model.process(DashboardOnboardingIntent.PaymentMethodClicked(PaymentMethodType.BANK_TRANSFER))

        state.assertValueAt(1) {
            it.navigationAction == DashboardOnboardingNavigationAction.LinkBank(linkBankTransfer)
        }
    }

    @Test
    fun `fetching link bank transfer failure should show error`() {
        val currency = "EUR"
        val error = IllegalStateException("error")
        whenever(currencyPrefs.tradingCurrency).thenReturn(currency)
        whenever(interactor.linkBank(currency)).thenReturn(Single.error(error))

        val state = model.state.test()
        model.process(DashboardOnboardingIntent.PaymentMethodClicked(PaymentMethodType.BANK_TRANSFER))

        state.assertValueAt(1) {
            it.errorState == DashboardOnboardingError.Error(error)
        }
    }

    @Test
    fun `given gold user clicking buy step should navigate to buy`() {
        whenever(interactor.getSteps()).thenReturn(Single.just(STEPS_UPGRADE_TO_GOLD_COMPLETE))

        val state = model.state.test()
        model.process(DashboardOnboardingIntent.FetchSteps)
        model.process(DashboardOnboardingIntent.StepClicked(DashboardOnboardingStep.BUY))

        state.assertValueAt(2) {
            it.navigationAction == DashboardOnboardingNavigationAction.OpenBuy
        }
    }

    companion object {
        private val STEPS_INITIAL = DashboardOnboardingStep.values().map {
            CompletableDashboardOnboardingStep(it, false)
        }

        private val STEPS_UPGRADE_TO_GOLD_COMPLETE = DashboardOnboardingStep.values().map {
            val isComplete = when (it) {
                DashboardOnboardingStep.UPGRADE_TO_GOLD -> true
                DashboardOnboardingStep.LINK_PAYMENT_METHOD -> false
                DashboardOnboardingStep.BUY -> false
            }
            CompletableDashboardOnboardingStep(it, isComplete)
        }
    }
}
