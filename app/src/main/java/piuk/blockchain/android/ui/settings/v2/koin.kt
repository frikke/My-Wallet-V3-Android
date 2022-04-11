package piuk.blockchain.android.ui.settings.v2

import com.blockchain.koin.intercomChatFeatureFlag
import com.blockchain.koin.payloadScopeQualifier
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.koin.dsl.module
import piuk.blockchain.android.support.SupportInteractor
import piuk.blockchain.android.support.SupportModel
import piuk.blockchain.android.support.SupportState
import piuk.blockchain.android.ui.settings.v2.account.AccountInteractor
import piuk.blockchain.android.ui.settings.v2.account.AccountModel
import piuk.blockchain.android.ui.settings.v2.account.AccountState
import piuk.blockchain.android.ui.settings.v2.notifications.NotificationsInteractor
import piuk.blockchain.android.ui.settings.v2.notifications.NotificationsModel
import piuk.blockchain.android.ui.settings.v2.notifications.NotificationsState
import piuk.blockchain.android.ui.settings.v2.profile.ProfileInteractor
import piuk.blockchain.android.ui.settings.v2.profile.ProfileModel
import piuk.blockchain.android.ui.settings.v2.profile.ProfileState
import piuk.blockchain.android.ui.settings.v2.profile.email.EmailInteractor
import piuk.blockchain.android.ui.settings.v2.profile.email.EmailModel
import piuk.blockchain.android.ui.settings.v2.profile.email.EmailState
import piuk.blockchain.android.ui.settings.v2.profile.phone.PhoneInteractor
import piuk.blockchain.android.ui.settings.v2.profile.phone.PhoneModel
import piuk.blockchain.android.ui.settings.v2.profile.phone.PhoneState
import piuk.blockchain.android.ui.settings.v2.security.SecurityInteractor
import piuk.blockchain.android.ui.settings.v2.security.SecurityModel
import piuk.blockchain.android.ui.settings.v2.security.SecurityState
import piuk.blockchain.android.ui.settings.v2.security.password.PasswordChangeInteractor
import piuk.blockchain.android.ui.settings.v2.security.password.PasswordChangeModel
import piuk.blockchain.android.ui.settings.v2.security.password.PasswordChangeState
import piuk.blockchain.android.ui.settings.v2.security.pin.PinInteractor
import piuk.blockchain.android.ui.settings.v2.security.pin.PinModel
import piuk.blockchain.android.ui.settings.v2.security.pin.PinState
import piuk.blockchain.android.ui.settings.v2.sheets.sms.SMSVerificationInteractor
import piuk.blockchain.android.ui.settings.v2.sheets.sms.SMSVerificationModel
import piuk.blockchain.android.ui.settings.v2.sheets.sms.SMSVerificationState
import piuk.blockchain.android.util.AppUtil

val redesignSettingsModule = module {

    scope(payloadScopeQualifier) {
        factory {
            SettingsModel(
                initialState = SettingsState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                environmentConfig = get(),
                remoteLogger = get()
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
                remoteLogger = get()
            )
        }

        factory {
            ProfileModel(
                initialState = ProfileState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                _activityIndicator = lazy { get<AppUtil>().activityIndicator },
                environmentConfig = get(),
                remoteLogger = get()
            )
        }

        factory {
            ProfileInteractor(
                settingsDataManager = get(),
                authPrefs = get(),
            )
        }

        factory {
            PhoneModel(
                initialState = PhoneState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                _activityIndicator = lazy { get<AppUtil>().activityIndicator },
                environmentConfig = get(),
                remoteLogger = get()
            )
        }

        factory {
            PhoneInteractor(
                settingsDataManager = get(),
                authPrefs = get(),
                nabuUserSync = get()
            )
        }

        factory {
            EmailModel(
                initialState = EmailState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                _activityIndicator = lazy { get<AppUtil>().activityIndicator },
                environmentConfig = get(),
                remoteLogger = get()
            )
        }

        factory {
            EmailInteractor(
                emailUpdater = get(),
                settingsDataManager = get(),
                authPrefs = get()
            )
        }

        factory {
            NotificationsModel(
                initialState = NotificationsState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                environmentConfig = get(),
                remoteLogger = get()
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
            AccountModel(
                initialState = AccountState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                environmentConfig = get(),
                remoteLogger = get()
            )
        }

        factory {
            AccountInteractor(
                settingsDataManager = get(),
                exchangeRates = get(),
                blockchainCardRepository = get(),
                currencyPrefs = get(),
                exchangeLinkingState = get()
            )
        }

        factory {
            SecurityModel(
                initialState = SecurityState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                environmentConfig = get(),
                remoteLogger = get()
            )
        }

        factory {
            SecurityInteractor(
                settingsDataManager = get(),
                biometricsController = get(),
                pinRepository = get(),
                payloadManager = get(),
                backupPrefs = get(),
                authPrefs = get(),
                securityPrefs = get()
            )
        }

        factory {
            PasswordChangeModel(
                initialState = PasswordChangeState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                environmentConfig = get(),
                remoteLogger = get()
            )
        }

        factory {
            PasswordChangeInteractor(
                payloadManager = get(),
                authDataManager = get(),
                pinRepository = get()
            )
        }

        factory {
            PinModel(
                initialState = PinState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                environmentConfig = get(),
                remoteLogger = get(),
                specificAnalytics = get(),
                analytics = get()
            )
        }

        factory {
            PinInteractor(
                apiStatus = get(),
                authDataManager = get(),
                payloadManager = get(),
                pinRepository = get(),
                biometricsController = get(),
                mobileNoticeRemoteConfig = get(),
                persistentPrefs = get(),
                walletStatus = get(),
                authPrefs = get(),
                credentialsWiper = get(),
                walletOptionsDataManager = get(),
                defaultLabels = get()
            )
        }

        factory {
            SupportModel(
                initialState = SupportState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                environmentConfig = get(),
                remoteLogger = get()
            )
        }

        factory {
            SupportInteractor(
                userIdentity = get(),
                isIntercomEnabledFlag = get(intercomChatFeatureFlag)
            )
        }
    }
}
