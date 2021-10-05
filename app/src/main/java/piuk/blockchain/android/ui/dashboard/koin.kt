package piuk.blockchain.android.ui.dashboard

import com.blockchain.koin.interestAccountFeatureFlag
import com.blockchain.koin.payloadScopeQualifier
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsInteractor
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsModel
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsState
import piuk.blockchain.android.ui.dashboard.model.DashboardActionAdapter
import piuk.blockchain.android.ui.dashboard.model.DashboardModel
import piuk.blockchain.android.ui.dashboard.model.DashboardState
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
                custodialWalletManager = get(),
                simpleBuyPrefs = get(),
                userIdentity = get(),
                analytics = get(),
                crashLogger = get(),
                linkedBanksFactory = get(),
                gatedFeatures = get()
            )
        }

        scoped {
            AssetDetailsModel(
                initialState = AssetDetailsState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                environmentConfig = get(),
                crashLogger = get()
            )
        }

        factory {
            AssetDetailsInteractor(
                interestFeatureFlag = get(interestAccountFeatureFlag),
                dashboardPrefs = get(),
                coincore = get(),
                custodialWalletManager = get()
            )
        }

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
    }
}
