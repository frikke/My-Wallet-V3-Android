package piuk.blockchain.android.ui.dashboard.onboarding

import com.blockchain.core.payments.PaymentsDataManager
import com.blockchain.core.payments.model.LinkBankTransfer
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.domain.usecases.AvailablePaymentMethodType
import piuk.blockchain.android.domain.usecases.CompletableDashboardOnboardingStep
import piuk.blockchain.android.domain.usecases.GetAvailablePaymentMethodsTypesUseCase
import piuk.blockchain.android.domain.usecases.GetDashboardOnboardingStepsUseCase

class DashboardOnboardingInteractor(
    private val getDashboardOnboardingUseCase: GetDashboardOnboardingStepsUseCase,
    private val custodialWalletManager: CustodialWalletManager,
    private val paymentsDataManager: PaymentsDataManager,
    private val getAvailablePaymentMethodsTypesUseCase: GetAvailablePaymentMethodsTypesUseCase
) {

    fun getSteps(): Single<List<CompletableDashboardOnboardingStep>> = getDashboardOnboardingUseCase(Unit)

    fun getSupportedCurrencies(): Single<List<FiatCurrency>> = custodialWalletManager.getSupportedFiatCurrencies()

    fun getAvailablePaymentMethodTypes(currency: FiatCurrency): Single<List<AvailablePaymentMethodType>> =
        getAvailablePaymentMethodsTypesUseCase.invoke(
            GetAvailablePaymentMethodsTypesUseCase.Request(
                currency = currency,
                fetchSddLimits = false,
                onlyEligible = true
            )
        )

    fun linkBank(fiatCurrency: FiatCurrency): Single<LinkBankTransfer> =
        paymentsDataManager.linkBank(fiatCurrency)
}
