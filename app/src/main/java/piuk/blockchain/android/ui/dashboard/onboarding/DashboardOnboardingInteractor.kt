package piuk.blockchain.android.ui.dashboard.onboarding

import com.blockchain.domain.onboarding.CompletableDashboardOnboardingStep
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.domain.usecases.AvailablePaymentMethodType
import piuk.blockchain.android.domain.usecases.GetAvailablePaymentMethodsTypesUseCase
import piuk.blockchain.android.domain.usecases.GetDashboardOnboardingStepsUseCase

class DashboardOnboardingInteractor(
    private val getDashboardOnboardingUseCase: GetDashboardOnboardingStepsUseCase,
    private val bankService: BankService,
    private val getAvailablePaymentMethodsTypesUseCase: GetAvailablePaymentMethodsTypesUseCase
) {

    fun getSteps(): Single<List<CompletableDashboardOnboardingStep>> = getDashboardOnboardingUseCase(Unit)

    fun getAvailablePaymentMethodTypes(currency: FiatCurrency): Single<List<AvailablePaymentMethodType>> =
        getAvailablePaymentMethodsTypesUseCase.invoke(
            GetAvailablePaymentMethodsTypesUseCase.Request(
                currency = currency,
                fetchSddLimits = false,
                onlyEligible = true
            )
        )

    fun linkBank(fiatCurrency: FiatCurrency): Single<LinkBankTransfer> =
        bankService.linkBank(fiatCurrency)
}
