package piuk.blockchain.android.ui.dashboard

import com.blockchain.koin.entitySwitchSilverEligibilityFeatureFlag
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.redesignPart2CoinViewFeatureFlag
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.domain.usecases.CompletableDashboardOnboardingStep
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsInteractor
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsModel
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsState
import piuk.blockchain.android.ui.dashboard.assetdetails.StateAwareActionsComparator
import piuk.blockchain.android.ui.dashboard.coinview.CoinViewInteractor
import piuk.blockchain.android.ui.dashboard.coinview.CoinViewModel
import piuk.blockchain.android.ui.dashboard.coinview.CoinViewState
import piuk.blockchain.android.ui.dashboard.coinview.recurringbuy.RecurringBuyInteractor
import piuk.blockchain.android.ui.dashboard.coinview.recurringbuy.RecurringBuyModel
import piuk.blockchain.android.ui.dashboard.coinview.recurringbuy.RecurringBuyModelState
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
                redesignCoinViewFlag = get(redesignPart2CoinViewFeatureFlag)
            )
        }

        scoped {
            AssetDetailsModel(
                initialState = AssetDetailsState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                assetActionsComparator = get(),
                entitySwitchSilverEligibilityFeatureFlag = get(entitySwitchSilverEligibilityFeatureFlag),
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
            StateAwareActionsComparator()
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

        factory {
            CoinViewModel(
                initialState = CoinViewState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                environmentConfig = get(),
                crashLogger = get()
            )
        }
        factory {
            CoinViewInteractor(
                coincore = get(),
                tradeDataManager = get(),
                currencyPrefs = get(),
                dashboardPrefs = get(),
                identity = get(),
                custodialWalletManager = get(),
                watchlistDataManager = get(),
                assetActionsComparator = get()
            )
        }

        factory {
            RecurringBuyModel(
                initialState = RecurringBuyModelState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                environmentConfig = get(),
                crashLogger = get()
            )
        }

        factory {
            RecurringBuyInteractor(
                tradeDataManager = get(),
                paymentsDataManager = get()
            )
        }
    }
}
