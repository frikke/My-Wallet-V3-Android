package piuk.blockchain.android.ui.launcher.loader

import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.koin.termsAndConditionsFeatureFlag
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
                crashLogger = get(),
                interactor = get(),
                prefs = get(),
                payloadDataManager = get(),
                prerequisites = get(),
                appUtil = get(),
                authPrefs = get()
            )
        }

        factory {
            LoaderInteractor(
                walletPrefs = get(),
                securityPrefs = get(),
                payloadDataManager = get(),
                prefs = get(),
                analytics = get(),
                currencyPrefs = get(),
                nabuUserDataManager = get(),
                assetCatalogue = get(),
                notificationTokenManager = get(),
                settingsDataManager = get(),
                prerequisites = get(),
                ioScheduler = Schedulers.io(),
                deepLinkPersistence = get(),
                termsAndConditionsFeatureFlag = get(termsAndConditionsFeatureFlag)
            )
        }
    }
}
