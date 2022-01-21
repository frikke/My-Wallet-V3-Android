package piuk.blockchain.android.ui.settings.v2.profile

import com.blockchain.commonarch.presentation.base.ActivityIndicator
import com.blockchain.commonarch.presentation.base.trackProgress
import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.CrashLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber

class ProfileModel(
    initialState: ProfileState,
    mainScheduler: Scheduler,
    private val interactor: ProfileInteractor,
    private val _activityIndicator: Lazy<ActivityIndicator?>,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger
) : MviModel<ProfileState, ProfileIntent>(
    initialState,
    mainScheduler,
    environmentConfig,
    crashLogger
) {

    private val activityIndicator: ActivityIndicator? by unsafeLazy {
        _activityIndicator.value
    }

    override fun performAction(
        previousState: ProfileState,
        intent: ProfileIntent
    ): Disposable? =
        when (intent) {
            is ProfileIntent.SaveEmail -> {
                interactor.saveEmail(
                    email = intent.email
                ).trackProgress(activityIndicator)
                    .subscribeBy(
                        onSuccess = {
                            process(ProfileIntent.SaveEmailSucceeded(it))
                            process(ProfileIntent.ResendEmail)
                        },
                        onError = {
                            Timber.e("SaveEmail failure " + it)
                            process(ProfileIntent.SaveEmailFailed)
                        }
                    )
            }
            is ProfileIntent.SavePhoneNumber -> {
                interactor.savePhoneNumber(
                    mobileWithPrefix = intent.phoneNumber
                ).trackProgress(activityIndicator)
                    .subscribeBy(
                        onSuccess = { userInfoSettings ->
                            process(ProfileIntent.SavePhoneNumberSucceeded(userInfoSettings))
                        },
                        onError = {
                            Timber.e("SavePhoneNumber failure " + it)
                            process(ProfileIntent.SavePhoneNumberFailed)
                        }
                    )
            }
            is ProfileIntent.LoadProfile -> {
                interactor.fetchProfileSettings()
                    .trackProgress(activityIndicator)
                    .subscribeBy(
                        onSuccess = { userInfo ->
                            process(
                                ProfileIntent.LoadProfileSucceeded(
                                    userInfoSettings = userInfo
                                )
                            )
                        }, onError = {
                        process(ProfileIntent.LoadProfileFailed)
                        Timber.e("LoadProfile failure " + it)
                    }
                    )
            }
            is ProfileIntent.ResendEmail -> {
                interactor.resendEmail(
                    email = previousState.userInfoSettings?.email.orEmpty()
                ).trackProgress(activityIndicator)
                    .subscribeBy(
                        onSuccess = {
                            process(ProfileIntent.ResendEmailSucceeded(it))
                        },
                        onError = {
                            Timber.e("ResendEmail failure " + it)
                            process(ProfileIntent.ResendEmailFailed)
                        }
                    )
            }
            is ProfileIntent.ResendCodeSMS -> {
                interactor.resendCodeSMS(
                    mobileWithPrefix = previousState.userInfoSettings?.mobileWithPrefix.orEmpty()
                ).trackProgress(activityIndicator)
                    .subscribeBy(
                        onSuccess = {
                            process(ProfileIntent.ResendCodeSMSSucceeded(it))
                        },
                        onError = {
                            Timber.e("ResendCodeSMS failure " + it)
                            process(ProfileIntent.ResendCodeSMSFailed)
                        }
                    )
            }
            is ProfileIntent.SaveEmailFailed,
            is ProfileIntent.SaveEmailSucceeded,
            is ProfileIntent.SavePhoneNumberSucceeded,
            is ProfileIntent.SavePhoneNumberFailed,
            is ProfileIntent.ResetEmailSentVerification,
            is ProfileIntent.ResetCodeSentVerification,
            is ProfileIntent.ResendEmailSucceeded,
            is ProfileIntent.ResendEmailFailed,
            is ProfileIntent.ResendCodeSMSSucceeded,
            is ProfileIntent.ResendCodeSMSFailed,
            is ProfileIntent.LoadProfileSucceeded,
            is ProfileIntent.LoadProfileFailed,
            is ProfileIntent.ClearErrors -> null
        }
}
