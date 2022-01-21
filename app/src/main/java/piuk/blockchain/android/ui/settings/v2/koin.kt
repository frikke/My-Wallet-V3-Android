package piuk.blockchain.android.ui.settings.v2

import com.blockchain.koin.payloadScope
import com.blockchain.koin.payloadScopeQualifier
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.koin.core.qualifier.named
import org.koin.dsl.module
import piuk.blockchain.android.ui.settings.v2.account.AccountInteractor
import piuk.blockchain.android.ui.settings.v2.account.AccountModel
import piuk.blockchain.android.ui.settings.v2.account.AccountState
import piuk.blockchain.android.ui.settings.v2.notifications.NotificationsInteractor
import piuk.blockchain.android.ui.settings.v2.notifications.NotificationsModel
import piuk.blockchain.android.ui.settings.v2.notifications.NotificationsState
import piuk.blockchain.android.ui.settings.v2.profile.ProfileInteractor
import piuk.blockchain.android.ui.settings.v2.profile.ProfileModel
import piuk.blockchain.android.ui.settings.v2.profile.ProfileState
import piuk.blockchain.android.ui.settings.v2.profile.SMSVerificationInteractor
import piuk.blockchain.android.ui.settings.v2.profile.SMSVerificationModel
import piuk.blockchain.android.ui.settings.v2.profile.SMSVerificationState
import piuk.blockchain.android.util.AppUtil

val profileScope = named("ProfileScope")

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
                paymentsDataManager = get(),
                getAvailablePaymentMethodsTypesUseCase = get(),
                currencyPrefs = get()
            )
        }

        factory {
            SMSVerificationInteractor(
                settingsDataManager = get(),
                nabuUserSync = get(),
                payloadDataManager = get()
            )
        }

        factory {
            SMSVerificationModel(
                initialState = SMSVerificationState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                _activityIndicator = lazy { get<AppUtil>().activityIndicator },
                environmentConfig = get(),
                crashLogger = get()
            )
        }

        scope(profileScope) {
            scoped {
                ProfileModel(
                    initialState = ProfileState(),
                    mainScheduler = AndroidSchedulers.mainThread(),
                    interactor = get(),
                    _activityIndicator = lazy { get<AppUtil>().activityIndicator },
                    environmentConfig = get(),
                    crashLogger = get()
                )
            }

            scoped {
                ProfileInteractor(
                    emailUpdater = payloadScope.get(),
                    settingsDataManager = payloadScope.get(),
                    authPrefs = payloadScope.get(),
                    nabuUserSync = payloadScope.get(),
                    payloadDataManager = payloadScope.get()
                )
            }
        }

        factory {
            NotificationsModel(
                initialState = NotificationsState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                environmentConfig = get(),
                crashLogger = get()
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
            NotificationsInteractor(
                notificationPrefs = get(),
                notificationTokenManager = get(),
                settingsDataManager = get(),
                payloadDataManager = get()
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
