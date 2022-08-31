package piuk.blockchain.android.ui.dashboard

import com.blockchain.koin.cowboysPromoFeatureFlag
import com.blockchain.koin.payloadScopeQualifier
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.domain.usecases.CompletableDashboardOnboardingStep
import piuk.blockchain.android.ui.cowboys.CowboysPromoDataProvider
import piuk.blockchain.android.ui.dashboard.assetdetails.StateAwareActionsComparator
import piuk.blockchain.android.ui.dashboard.coinview.CoinViewInteractor
import piuk.blockchain.android.ui.dashboard.coinview.CoinViewModel
import piuk.blockchain.android.ui.dashboard.coinview.CoinViewState
import piuk.blockchain.android.ui.dashboard.coinview.recurringbuy.RecurringBuyInteractor
import piuk.blockchain.android.ui.dashboard.coinview.recurringbuy.RecurringBuyModel
import piuk.blockchain.android.ui.dashboard.coinview.recurringbuy.RecurringBuyModelState
import piuk.blockchain.android.ui.dashboard.model.DashboardActionInteractor
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
                remoteLogger = get(),
                appRatingService = get()
            )
        }

        factory {
            DashboardActionInteractor(
                coincore = get(),
                exchangeRates = get(),
                payloadManager = get(),
                currencyPrefs = get(),
                onboardingPrefs = get(),
                custodialWalletManager = get(),
                bankService = get(),
                simpleBuyPrefs = get(),
                userIdentity = get(),
                kycService = get(),
                dataRemediationService = get(),
                walletModeService = get(),
                analytics = get(),
                walletModeBalanceCache = get(),
                remoteLogger = get(),
                linkedBanksFactory = get(),
                getDashboardOnboardingStepsUseCase = get(),
                nftWaitlistService = get(),
                nftAnnouncementPrefs = get(),
                referralPrefs = get(),
                cowboysFeatureFlag = get(cowboysPromoFeatureFlag),
                settingsDataManager = get(),
                cowboysDataProvider = get(),
                referralService = get(),
                cowboysPrefs = get(),
                productsEligibilityStore = get()
            )
        }

        factory {
            StateAwareActionsComparator()
        }.bind(Comparator::class)

        factory {
            BalanceAnalyticsReporter(
                analytics = get()
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
                fiatCurrenciesService = get(),
                uiScheduler = AndroidSchedulers.mainThread(),
                environmentConfig = get(),
                remoteLogger = get()
            )
        }

        factory {
            DashboardOnboardingInteractor(
                getDashboardOnboardingUseCase = get(),
                bankService = get(),
                getAvailablePaymentMethodsTypesUseCase = get()
            )
        }

        factory {
            CoinViewModel(
                initialState = CoinViewState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                environmentConfig = get(),
                remoteLogger = get(),
                walletModeService = get()
            )
        }
        factory {
            CoinViewInteractor(
                coincore = get(),
                tradeDataService = get(),
                currencyPrefs = get(),
                dashboardPrefs = get(),
                identity = get(),
                kycService = get(),
                custodialWalletManager = get(),
                assetActionsComparator = get(),
                assetsManager = get(),
                walletModeService = get(),
                watchlistDataManager = get(),
            )
        }

        factory {
            RecurringBuyModel(
                initialState = RecurringBuyModelState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                environmentConfig = get(),
                remoteLogger = get()
            )
        }

        factory {
            RecurringBuyInteractor(
                tradeDataService = get(),
                bankService = get(),
                cardService = get()
            )
        }

        scoped { WalletModeBalanceCache(coincore = get()) }

        scoped {
            CowboysPromoDataProvider(
                config = get(),
                json = get()
            )
        }
    }
}
