package piuk.blockchain.android.ui.dashboard

import com.blockchain.domain.onboarding.CompletableDashboardOnboardingStep
import com.blockchain.koin.assetOrderingFeatureFlag
import com.blockchain.koin.cowboysPromoFeatureFlag
import com.blockchain.koin.defaultOrder
import com.blockchain.koin.exchangeWAPromptFeatureFlag
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.paymentUxAssetDisplayBalanceFeatureFlag
import com.blockchain.koin.paymentUxTotalDisplayBalanceFeatureFlag
import com.blockchain.koin.sellOrder
import com.blockchain.koin.swapSourceOrder
import com.blockchain.koin.swapTargetOrder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.domain.usecases.ShouldShowExchangeCampaignUseCase
import piuk.blockchain.android.ui.cowboys.CowboysPromoDataProvider
import piuk.blockchain.android.ui.dashboard.assetdetails.StateAwareActionsComparator
import piuk.blockchain.android.ui.dashboard.coinview.recurringbuy.RecurringBuyInteractor
import piuk.blockchain.android.ui.dashboard.coinview.recurringbuy.RecurringBuyModel
import piuk.blockchain.android.ui.dashboard.coinview.recurringbuy.RecurringBuyModelState
import piuk.blockchain.android.ui.dashboard.model.DashboardActionInteractor
import piuk.blockchain.android.ui.dashboard.model.DashboardModel
import piuk.blockchain.android.ui.dashboard.model.DashboardState
import piuk.blockchain.android.ui.dashboard.model.ShouldAssetShowUseCase
import piuk.blockchain.android.ui.dashboard.onboarding.DashboardOnboardingInteractor
import piuk.blockchain.android.ui.dashboard.onboarding.DashboardOnboardingModel
import piuk.blockchain.android.ui.transfer.AccountsSorting
import piuk.blockchain.android.ui.transfer.DefaultAccountsSorting
import piuk.blockchain.android.ui.transfer.SellAccountsSorting
import piuk.blockchain.android.ui.transfer.SwapSourceAccountsSorting
import piuk.blockchain.android.ui.transfer.SwapTargetAccountsSorting

val dashboardModule = module {

    scope(payloadScopeQualifier) {

        factory {
            DashboardModel(
                initialState = DashboardState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                balancesCache = get(),
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
                productsEligibilityStore = get(),
                totalDisplayBalanceFF = get(paymentUxTotalDisplayBalanceFeatureFlag),
                assetDisplayBalanceFF = get(paymentUxAssetDisplayBalanceFeatureFlag),
                shouldAssetShowUseCase = get()
            )
        }

        factory {
            ShouldAssetShowUseCase(
                assetDisplayBalanceFF = get(paymentUxAssetDisplayBalanceFeatureFlag),
                localSettingsPrefs = get(),
                watchlistService = get()
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

        factory(defaultOrder) {
            DefaultAccountsSorting(currencyPrefs = get())
        }.bind(AccountsSorting::class)

        factory(swapSourceOrder) {
            SwapSourceAccountsSorting(
                assetListOrderingFF = get(assetOrderingFeatureFlag),
                dashboardAccountsSorter = get(defaultOrder),
                sellAccountsSorting = get(sellOrder),
                momentLogger = get()
            )
        }.bind(AccountsSorting::class)

        factory(swapTargetOrder) {
            SwapTargetAccountsSorting(
                currencyPrefs = get(),
                exchangeRatesDataManager = get(),
                watchlistDataManager = get(),
            )
        }.bind(AccountsSorting::class)

        factory(sellOrder) {
            SellAccountsSorting(
                coincore = get(),
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

        factory {
            ShouldShowExchangeCampaignUseCase(
                exchangeWAPromptFF = get(exchangeWAPromptFeatureFlag),
                exchangeCampaignPrefs = get(),
                mercuryExperimentsService = get()
            )
        }
    }
}
