package piuk.blockchain.android.ui.settings.profile.phone

import com.blockchain.api.services.WalletSettingsService
import com.blockchain.commonarch.presentation.base.ActivityIndicator
import com.blockchain.commonarch.presentation.base.trackProgress
import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.core.settings.InvalidPhoneNumber
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.RemoteLogger
import com.blockchain.utils.unsafeLazy
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import timber.log.Timber

class PhoneModel(
    initialState: PhoneState,
    mainScheduler: Scheduler,
    private val interactor: PhoneInteractor,
    private val _activityIndicator: Lazy<ActivityIndicator?>,
    environmentConfig: EnvironmentConfig,
    remoteLogger: RemoteLogger
) : MviModel<PhoneState, PhoneIntent>(
    initialState,
    mainScheduler,
    environmentConfig,
    remoteLogger
) {

    private val activityIndicator: ActivityIndicator? by unsafeLazy {
        _activityIndicator.value
    }

    override fun performAction(
        previousState: PhoneState,
        intent: PhoneIntent
    ): Disposable? =
        when (intent) {
            is PhoneIntent.SavePhoneNumber -> {
                interactor.savePhoneNumber(
                    mobileWithPrefix = intent.phoneNumber
                ).trackProgress(activityIndicator)
                    .subscribeBy(
                        onSuccess = { settings ->
                            process(PhoneIntent.SavePhoneNumberSucceeded(settings))
                        },
                        onError = {
                            Timber.e("SavePhoneNumber failure " + it)
                            if (it is InvalidPhoneNumber) {
                                process(PhoneIntent.PhoneNumberNotValid)
                            } else {
                                process(PhoneIntent.SavePhoneNumberFailed)
                            }
                        }
                    )
            }
            is PhoneIntent.LoadProfile -> {
                interactor.cachedSettings
                    .trackProgress(activityIndicator)
                    .subscribeBy(
                        onSuccess = { userInfo ->
                            process(
                                PhoneIntent.LoadProfileSucceeded(
                                    userInfoSettings = WalletSettingsService.UserInfoSettings(
                                        userInfo.email, emailVerified = userInfo.isEmailVerified,
                                        mobileWithPrefix = userInfo.smsNumber, mobileVerified = userInfo.isSmsVerified,
                                        authType = userInfo.authType, smsDialCode = userInfo.smsDialCode
                                    )
                                )
                            )
                        }, onError = {
                        process(PhoneIntent.FetchProfile)
                        Timber.e("PhoneIntent.LoadProfile failure " + it)
                    }
                    )
            }
            is PhoneIntent.FetchProfile -> {
                interactor.fetchProfileSettings()
                    .trackProgress(activityIndicator)
                    .subscribeBy(
                        onSuccess = { userInfo ->
                            process(
                                PhoneIntent.LoadProfileSucceeded(
                                    userInfoSettings = userInfo
                                )
                            )
                        }, onError = {
                        process(PhoneIntent.LoadProfileFailed)
                        Timber.e("PhoneIntent.FetchProfile failure " + it)
                    }
                    )
            }
            is PhoneIntent.ResendCodeSMS -> {
                interactor.resendCodeSMS(
                    mobileWithPrefix = previousState.userInfoSettings?.mobileWithPrefix.orEmpty()
                ).trackProgress(activityIndicator)
                    .subscribeBy(
                        onSuccess = {
                            process(PhoneIntent.ResendCodeSMSSucceeded(it))
                        },
                        onError = {
                            Timber.e("ResendCodeSMS failure " + it)
                            process(PhoneIntent.ResendCodeSMSFailed)
                        }
                    )
            }
            is PhoneIntent.SavePhoneNumberSucceeded,
            is PhoneIntent.SavePhoneNumberFailed,
            is PhoneIntent.ResetCodeSentVerification,
            is PhoneIntent.ResendCodeSMSSucceeded,
            is PhoneIntent.ResendCodeSMSFailed,
            is PhoneIntent.PhoneNumberNotValid,
            is PhoneIntent.LoadProfileSucceeded,
            is PhoneIntent.LoadProfileFailed,
            is PhoneIntent.ClearErrors -> null
        }
}
