package piuk.blockchain.android.ui.dashboard.onboarding

import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.datamanagers.UndefinedPaymentMethod
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.preferences.CurrencyPrefs
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.domain.usecases.CompletableDashboardOnboardingStep
import piuk.blockchain.android.domain.usecases.DashboardOnboardingStep
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

class DashboardOnboardingModel(
    initialSteps: List<CompletableDashboardOnboardingStep>,
    private val interactor: DashboardOnboardingInteractor,
    private val currencyPrefs: CurrencyPrefs,
    uiScheduler: Scheduler,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger
) : MviModel<DashboardOnboardingState, DashboardOnboardingIntent>(
    initialState = DashboardOnboardingState(steps = initialSteps),
    uiScheduler = uiScheduler,
    environmentConfig = environmentConfig,
    crashLogger = crashLogger,
) {

    override fun performAction(
        previousState: DashboardOnboardingState,
        intent: DashboardOnboardingIntent
    ): Disposable? = when (intent) {
        DashboardOnboardingIntent.FetchSteps -> interactor.getSteps().subscribeBy(
            onSuccess = { steps ->
                process(DashboardOnboardingIntent.FetchStepsSuccess(steps))
            },
            onError = {
                process(DashboardOnboardingIntent.FetchFailed(it))
            }
        )
        is DashboardOnboardingIntent.StepClicked -> handleStepClicked(previousState, intent.clickedStep)
        DashboardOnboardingIntent.TradingCurrencyChanged ->
            handlePaymentMethodStepClicked()
                .subscribeBy(
                    onSuccess = {
                        process(it)
                    }
                )
        is DashboardOnboardingIntent.PaymentMethodClicked ->
            handlePaymentMethodClicked(intent.type)
                .subscribeBy(
                    onSuccess = {
                        process(it)
                    }
                )
        is DashboardOnboardingIntent.NavigateTo,
        is DashboardOnboardingIntent.FetchFailed,
        is DashboardOnboardingIntent.FetchStepsSuccess,
        DashboardOnboardingIntent.ClearNavigation,
        DashboardOnboardingIntent.ClearError -> null
    }

    private fun handlePaymentMethodStepClicked(): Single<DashboardOnboardingIntent> =
        interactor.getSupportedCurrencies()
            .flatMap { supportedCurrencies ->
                val tradingCurrency = currencyPrefs.tradingCurrency
                if (!supportedCurrencies.contains(tradingCurrency)) {
                    Single.just(
                        DashboardOnboardingIntent.NavigateTo(
                            DashboardOnboardingNavigationAction.SelectTradingCurrency(
                                supportedCurrencies, tradingCurrency
                            )
                        )
                    )
                } else {
                    fetchEligiblePaymentMethodsAndNavigateToAddPaymentMethod(tradingCurrency)
                }
            }
            .onErrorReturn { DashboardOnboardingIntent.FetchFailed(it) }

    private fun fetchEligiblePaymentMethodsAndNavigateToAddPaymentMethod(
        tradingCurrency: String
    ): Single<DashboardOnboardingIntent> = interactor.getEligiblePaymentMethods(tradingCurrency)
        .map {
            val paymentMethods = it.filter { it is UndefinedPaymentMethod }
            if (paymentMethods.isNotEmpty()) {
                DashboardOnboardingIntent.NavigateTo(
                    DashboardOnboardingNavigationAction.AddPaymentMethod(paymentMethods)
                )
            } else {
                DashboardOnboardingIntent.FetchFailed(IllegalStateException())
            } as DashboardOnboardingIntent
        }.onErrorReturn {
            DashboardOnboardingIntent.FetchFailed(it)
        }

    private fun handleStepClicked(previousState: DashboardOnboardingState, step: DashboardOnboardingStep): Disposable? {
        val hasUpgradedToGold =
            previousState.steps.find { it.step == DashboardOnboardingStep.UPGRADE_TO_GOLD }?.isCompleted ?: false

        if (!hasUpgradedToGold) {
            process(DashboardOnboardingIntent.NavigateTo(DashboardOnboardingNavigationAction.StartKyc))
            return null
        }

        val navigation = when (step) {
            DashboardOnboardingStep.UPGRADE_TO_GOLD -> DashboardOnboardingNavigationAction.StartKyc
            DashboardOnboardingStep.LINK_PAYMENT_METHOD -> {
                return handlePaymentMethodStepClicked()
                    .subscribeBy(
                        onSuccess = {
                            process(it)
                        }
                    )
            }
            DashboardOnboardingStep.BUY -> DashboardOnboardingNavigationAction.OpenBuy
        }

        process(DashboardOnboardingIntent.NavigateTo(navigation))
        return null
    }

    private fun handlePaymentMethodClicked(type: PaymentMethodType): Single<DashboardOnboardingIntent> =
        when (type) {
            PaymentMethodType.PAYMENT_CARD ->
                Single.just(DashboardOnboardingIntent.NavigateTo(DashboardOnboardingNavigationAction.AddCard))
            PaymentMethodType.FUNDS -> Single.just(
                DashboardOnboardingIntent.NavigateTo(
                    DashboardOnboardingNavigationAction.WireTransferAccountDetails(currencyPrefs.tradingCurrency)
                )
            )
            PaymentMethodType.BANK_TRANSFER ->
                interactor.linkBank(currencyPrefs.tradingCurrency)
                    .map {
                        DashboardOnboardingIntent.NavigateTo(
                            DashboardOnboardingNavigationAction.LinkBank(it)
                        ) as DashboardOnboardingIntent
                    }
                    .onErrorReturn { DashboardOnboardingIntent.FetchFailed(it) }
            else -> throw IllegalStateException()
        }
}
