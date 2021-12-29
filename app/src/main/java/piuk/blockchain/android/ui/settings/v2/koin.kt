package piuk.blockchain.android.ui.settings.v2

import com.blockchain.koin.payloadScopeQualifier
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.koin.dsl.module
import piuk.blockchain.android.ui.settings.v2.account.AccountInteractor
import piuk.blockchain.android.ui.settings.v2.account.AccountModel
import piuk.blockchain.android.ui.settings.v2.account.AccountState
import piuk.blockchain.android.ui.settings.v2.profile.ProfileInteractor
import piuk.blockchain.android.ui.settings.v2.profile.ProfileModel
import piuk.blockchain.android.ui.settings.v2.profile.ProfileState

val redesignSettingsModule = module {

    scope(payloadScopeQualifier) {
        factory {
            SettingsModel(
                initialState = SettingsState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                environmentConfig = get(),
                crashLogger = get()
            )
        }

        factory {
            SettingsInteractor(
                userIdentity = get(),
                database = get(),
                credentialsWiper = get(),
                custodialWalletManager = get(),
                currencyPrefs = get()
            )
        }

        factory {
            ProfileModel(
                initialState = ProfileState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                environmentConfig = get(),
                crashLogger = get()
            )
        }

        factory {
            ProfileInteractor(
                emailUpdater = get(),
                settingsDataManager = get(),
                prefs = get()
            )
        }

        factory {
            AccountModel(
                initialState = AccountState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                environmentConfig = get(),
                crashLogger = get()
            )
        }

        factory {
            AccountInteractor(
                settingsDataManager = get(),
                exchangeRates = get(),
                currencyPrefs = get(),
                exchangeLinkingState = get()
            )
        }
    }
}
