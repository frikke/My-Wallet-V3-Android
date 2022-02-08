package piuk.blockchain.android.ui.dashboard

import com.blockchain.koin.dashboardOnboardingFeatureFlag
import com.blockchain.koin.payloadScopeQualifier
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.domain.usecases.CompletableDashboardOnboardingStep
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetActionsComparator
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsInteractor
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsModel
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsState
import piuk.blockchain.android.ui.dashboard.model.DashboardActionAdapter
import piuk.blockchain.android.ui.dashboard.model.DashboardModel
import piuk.blockchain.android.ui.dashboard.model.DashboardState
import piuk.blockchain.android.ui.dashboard.onboarding.DashboardOnboardingInteractor
import piuk.blockchain.android.ui.dashboard.onboarding.DashboardOnboardingModel
import piuk.blockchain.android.ui.transfer.AccountsSorting
import piuk.blockchain.android.ui.transfer.DashboardAccountsSorting

val dashboardModule = module {

    scope(payloadScopeQualifier) {

        factory {
            DashboardModel(
                initialState = DashboardState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                environmentConfig = get(),
                crashLogger = get()
            )
        }

        factory {
            DashboardActionAdapter(
                coincore = get(),
                exchangeRates = get(),
                payloadManager = get(),
                currencyPrefs = get(),
                onboardingPrefs = get(),
                custodialWalletManager = get(),
                paymentsDataManager = get(),
                simpleBuyPrefs = get(),
                userIdentity = get(),
                analytics = get(),
                crashLogger = get(),
                linkedBanksFactory = get(),
                getDashboardOnboardingStepsUseCase = get(),
                dashboardOnboardingFlag = get(dashboardOnboardingFeatureFlag)
            )
        }

        scoped {
            AssetDetailsModel(
                initialState = AssetDetailsState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                assetActionsComparator = get(),
                environmentConfig = get(),
                crashLogger = get()
            )
        }

        factory {
            AssetDetailsInteractor(
                dashboardPrefs = get(),
                coincore = get(),
                userIdentity = get(),
                custodialWalletManager = get(),
                paymentsDataManager = get()
            )
        }

        factory {
            AssetActionsComparator()
        }.bind(Comparator::class)

        factory {
            BalanceAnalyticsReporter(
                analytics = get(),
                coincore = get()
            )
        }

        factory {
            DashboardAccountsSorting(
                dashboardPrefs = get(),
                assetCatalogue = get()
            )
        }.bind(AccountsSorting::class)

        factory { params ->
            DashboardOnboardingModel(
                initialSteps = params.getOrNull<List<CompletableDashboardOnboardingStep>>() ?: emptyList(),
                interactor = get(),
                currencyPrefs = get(),
                uiScheduler = AndroidSchedulers.mainThread(),
                environmentConfig = get(),
                crashLogger = get()
            )
        }

        factory {
            DashboardOnboardingInteractor(
                getDashboardOnboardingUseCase = get(),
                custodialWalletManager = get(),
                paymentsDataManager = get(),
                getAvailablePaymentMethodsTypesUseCase = get()
            )
        }
    }
}
