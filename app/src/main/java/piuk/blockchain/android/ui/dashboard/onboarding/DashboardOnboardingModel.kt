package piuk.blockchain.android.ui.dashboard.onboarding

import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.domain.fiatcurrencies.FiatCurrenciesService
import com.blockchain.domain.onboarding.CompletableDashboardOnboardingStep
import com.blockchain.domain.onboarding.DashboardOnboardingStep
import com.blockchain.domain.paymentmethods.model.PaymentMethod
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.RemoteLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.domain.usecases.AvailablePaymentMethodType
import piuk.blockchain.android.domain.usecases.LinkAccess

class DashboardOnboardingModel(
    initialSteps: List<CompletableDashboardOnboardingStep>,
    private val interactor: DashboardOnboardingInteractor,
    private val fiatCurrenciesService: FiatCurrenciesService,
    uiScheduler: Scheduler,
    environmentConfig: EnvironmentConfig,
    remoteLogger: RemoteLogger
) : MviModel<DashboardOnboardingState, DashboardOnboardingIntent>(
    initialState = DashboardOnboardingState(steps = initialSteps),
    uiScheduler = uiScheduler,
    environmentConfig = environmentConfig,
    remoteLogger = remoteLogger,
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
                return fetchEligiblePaymentMethodsAndNavigateToAddPaymentMethod()
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

    private fun fetchEligiblePaymentMethodsAndNavigateToAddPaymentMethod(): Single<DashboardOnboardingIntent> =
        interactor.getAvailablePaymentMethodTypes(fiatCurrenciesService.selectedTradingCurrency)
            .map { available ->
                val paymentMethods = available
                    .filter { method -> method.linkAccess == LinkAccess.GRANTED }
                    .mapNotNull { method -> method.toPaymentMethod() }
                if (paymentMethods.isNotEmpty()) {
                    DashboardOnboardingIntent.NavigateTo(
                        DashboardOnboardingNavigationAction.AddPaymentMethod(paymentMethods)
                    )
                } else {
                    DashboardOnboardingIntent.FetchFailed(IllegalStateException())
                }
            }.onErrorReturn {
                DashboardOnboardingIntent.FetchFailed(it)
            }

    private fun handlePaymentMethodClicked(type: PaymentMethodType): Single<DashboardOnboardingIntent> =
        when (type) {
            PaymentMethodType.PAYMENT_CARD ->
                Single.just(DashboardOnboardingIntent.NavigateTo(DashboardOnboardingNavigationAction.AddCard))
            PaymentMethodType.FUNDS -> Single.just(
                DashboardOnboardingIntent.NavigateTo(
                    DashboardOnboardingNavigationAction.WireTransferAccountDetails(
                        fiatCurrenciesService.selectedTradingCurrency
                    )
                )
            )
            PaymentMethodType.BANK_TRANSFER ->
                interactor.linkBank(fiatCurrenciesService.selectedTradingCurrency)
                    .map {
                        DashboardOnboardingIntent.NavigateTo(
                            DashboardOnboardingNavigationAction.LinkBank(it)
                        ) as DashboardOnboardingIntent
                    }
                    .onErrorReturn { DashboardOnboardingIntent.FetchFailed(it) }
            else -> throw IllegalStateException()
        }

    private fun AvailablePaymentMethodType.toPaymentMethod(): PaymentMethod? =
        when (type) {
            PaymentMethodType.PAYMENT_CARD ->
                PaymentMethod.UndefinedCard(
                    limits,
                    canBeUsedForPayment,
                    PaymentMethod.UndefinedCard.mapCardFundSources(cardFundSources)
                )
            PaymentMethodType.GOOGLE_PAY -> PaymentMethod.GooglePay(limits, canBeUsedForPayment)
            PaymentMethodType.BANK_TRANSFER ->
                PaymentMethod.UndefinedBankTransfer(limits, canBeUsedForPayment)
            PaymentMethodType.BANK_ACCOUNT ->
                if (canBeUsedForPayment) {
                    PaymentMethod.UndefinedBankAccount(
                        currency,
                        limits,
                        canBeUsedForPayment
                    )
                } else null
            PaymentMethodType.FUNDS,
            PaymentMethodType.UNKNOWN -> null
        }
}
