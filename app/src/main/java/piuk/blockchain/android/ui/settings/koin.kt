package piuk.blockchain.android.ui.settings

import com.blockchain.koin.blockchainCardFeatureFlag
import com.blockchain.koin.cardRejectionCheckFeatureFlag
import com.blockchain.koin.hideDustFeatureFlag
import com.blockchain.koin.intercomChatFeatureFlag
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.presentation.BackupPhrasePinService
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import piuk.blockchain.android.support.SupportInteractor
import piuk.blockchain.android.support.SupportModel
import piuk.blockchain.android.support.SupportState
import piuk.blockchain.android.ui.settings.account.AccountInteractor
import piuk.blockchain.android.ui.settings.account.AccountModel
import piuk.blockchain.android.ui.settings.account.AccountState
import piuk.blockchain.android.ui.settings.appprefs.LocalSettingsViewModel
import piuk.blockchain.android.ui.settings.notificationpreferences.NotificationPreferencesInteractor
import piuk.blockchain.android.ui.settings.notificationpreferences.NotificationPreferencesViewModel
import piuk.blockchain.android.ui.settings.notificationpreferences.details.NotificationPreferencesDetailsInteractor
import piuk.blockchain.android.ui.settings.notificationpreferences.details.NotificationPreferencesDetailsViewModel
import piuk.blockchain.android.ui.settings.profile.ProfileInteractor
import piuk.blockchain.android.ui.settings.profile.ProfileModel
import piuk.blockchain.android.ui.settings.profile.ProfileState
import piuk.blockchain.android.ui.settings.profile.email.EmailInteractor
import piuk.blockchain.android.ui.settings.profile.email.EmailModel
import piuk.blockchain.android.ui.settings.profile.email.EmailState
import piuk.blockchain.android.ui.settings.profile.phone.PhoneInteractor
import piuk.blockchain.android.ui.settings.profile.phone.PhoneModel
import piuk.blockchain.android.ui.settings.profile.phone.PhoneState
import piuk.blockchain.android.ui.settings.security.SecurityInteractor
import piuk.blockchain.android.ui.settings.security.SecurityModel
import piuk.blockchain.android.ui.settings.security.SecurityState
import piuk.blockchain.android.ui.settings.security.password.PasswordChangeInteractor
import piuk.blockchain.android.ui.settings.security.password.PasswordChangeModel
import piuk.blockchain.android.ui.settings.security.password.PasswordChangeState
import piuk.blockchain.android.ui.settings.security.pin.PinInteractor
import piuk.blockchain.android.ui.settings.security.pin.PinModel
import piuk.blockchain.android.ui.settings.security.pin.PinState
import piuk.blockchain.android.ui.settings.security.pin.requests.BackupPhrasePinRequest
import piuk.blockchain.android.ui.settings.sheets.sms.SMSVerificationInteractor
import piuk.blockchain.android.ui.settings.sheets.sms.SMSVerificationModel
import piuk.blockchain.android.ui.settings.sheets.sms.SMSVerificationState
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
                kycService = get(),
                credentialsWiper = get(),
                bankService = get(),
                cardService = get(),
                getAvailablePaymentMethodsTypesUseCase = get(),
                currencyPrefs = get(),
                referralService = get(),
                nabuUserIdentity = get(),
                cardRejectionFF = get(cardRejectionCheckFeatureFlag),
                dustBalancesFF = get(hideDustFeatureFlag)
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
                nabuUserSync = get(),
                getUserStore = get()
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
                authPrefs = get(),
                getUserStore = get()
            )
        }

        factory {
            NotificationPreferencesInteractor(get())
        }

        viewModel {
            NotificationPreferencesViewModel(
                interactor = get(),
                ioDispatcher = Dispatchers.IO,
                analytics = get()
            )
        }

        viewModel {
            NotificationPreferencesDetailsViewModel(
                interactor = get(),
                analytics = get()
            )
        }

        factory {
            NotificationPreferencesDetailsInteractor(userDataManager = get())
        }

        viewModel {
            LocalSettingsViewModel(
                localSettingsPrefs = get()
            )
        }

        factory {
            AccountModel(
                initialState = AccountState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                environmentConfig = get(),
                walletModeCache = get(),
                remoteLogger = get()
            )
        }

        factory {
            AccountInteractor(
                settingsDataManager = get(),
                exchangeRates = get(),
                blockchainCardRepository = get(),
                currencyPrefs = get(),
                exchangeLinkingState = get(),
                localSettingsPrefs = get(),
                fiatCurrenciesService = get(),
                blockchainCardFF = get(blockchainCardFeatureFlag),
                dustBalancesFF = get(hideDustFeatureFlag)
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
                analytics = get(),
                momentLogger = get()
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
                sessionPrefs = get(),
                remoteLogger = get(),
                authPrefs = get(),
                credentialsWiper = get(),
                walletOptionsDataManager = get(),
                defaultLabels = get(),
                isIntercomEnabledFlag = get(intercomChatFeatureFlag)
            )
        }

        scoped<BackupPhrasePinService> {
            BackupPhrasePinRequest(
                secondPasswordDialog = get()
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
                kycService = get(),
                isIntercomEnabledFlag = get(intercomChatFeatureFlag)
            )
        }
    }
}
