package piuk.blockchain.android.ui.launcher.loader

import com.blockchain.koin.payloadScopeQualifier
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import org.koin.dsl.module

val loaderModule = module {

    scope(payloadScopeQualifier) {
        factory {
            LoaderModel(
                initialState = LoaderState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                environmentConfig = get(),
                remoteLogger = get(),
                interactor = get(),
                authPrefs = get(),
                payloadDataManager = get(),
                prerequisites = get(),
                appUtil = get()
            )
        }

        factory {
            LoaderInteractor(
                walletPrefs = get(),
                payloadDataManager = get(),
                analytics = get(),
                currencyPrefs = get(),
                nabuUserDataManager = get(),
                assetCatalogue = get(),
                notificationTokenManager = get(),
                settingsDataManager = get(),
                prerequisites = get(),
                ioScheduler = Schedulers.io(),
                deepLinkPersistence = get(),
                referralService = get(),
                fiatCurrenciesService = get(),
                cowboysPrefs = get(),
                userIdentity = get(),
                kycService = get(),
                coincore = get(),
                walletModeServices = getAll(),
                experimentsStore = get(),
                fraudService = get()
            )
        }
    }
}
