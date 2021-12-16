package piuk.blockchain.android.ui.dashboard.onboarding

import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.PaymentMethod
import com.blockchain.nabu.models.data.LinkBankTransfer
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.domain.usecases.CompletableDashboardOnboardingStep
import piuk.blockchain.android.domain.usecases.GetDashboardOnboardingStepsUseCase

class DashboardOnboardingInteractor(
    private val getDashboardOnboardingUseCase: GetDashboardOnboardingStepsUseCase,
    private val custodialWalletManager: CustodialWalletManager
) {

    fun getSteps(): Single<List<CompletableDashboardOnboardingStep>> = getDashboardOnboardingUseCase(Unit)

    fun getSupportedCurrencies(): Single<List<String>> = custodialWalletManager.getSupportedFiatCurrencies()

    fun getEligiblePaymentMethods(currency: String): Single<List<PaymentMethod>> =
        custodialWalletManager.fetchSuggestedPaymentMethod(
            fiatCurrency = currency,
            fetchSddLimits = false,
            onlyEligible = true
        )

    fun linkBank(fiatCurrency: String): Single<LinkBankTransfer> =
        custodialWalletManager.linkToABank(fiatCurrency)
}
