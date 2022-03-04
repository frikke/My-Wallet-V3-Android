package piuk.blockchain.android.ui.start

import com.blockchain.koin.landingCtaFeatureFlag
import com.blockchain.koin.payloadScopeQualifier
import org.koin.dsl.module

val startupUiModule = module {

    scope(payloadScopeQualifier) {
        factory {
            LandingPresenter(
                environmentSettings = get(),
                prefs = get(),
                onboardingPrefs = get(),
                rootUtil = get(),
                apiStatus = get(),
                assetCatalogue = get(),
                exchangeRatesDataManager = get(),
                landingCtaFF = get(landingCtaFeatureFlag)
            )
        }

        factory {
            ManualPairingPresenter(
                appUtil = get(),
                authDataManager = get(),
                payloadDataManager = get(),
                prefs = get(),
                analytics = get(),
                crashLogger = get()
            )
        }

        factory {
            PasswordRequiredPresenter(
                appUtil = get(),
                prefs = get(),
                authDataManager = get(),
                payloadDataManager = get(),
                crashLogger = get()
            )
        }
    }
}
