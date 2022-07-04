package piuk.blockchain.android.ui.start

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
                exchangeRatesDataManager = get()
            )
        }

        factory {
            ManualPairingPresenter(
                appUtil = get(),
                authDataManager = get(),
                payloadDataManager = get(),
                authPrefs = get(),
                analytics = get(),
                remoteLogger = get()
            )
        }

        factory {
            PasswordRequiredPresenter(
                appUtil = get(),
                authPrefs = get(),
                authDataManager = get(),
                payloadDataManager = get(),
                remoteLogger = get()
            )
        }
    }
}
