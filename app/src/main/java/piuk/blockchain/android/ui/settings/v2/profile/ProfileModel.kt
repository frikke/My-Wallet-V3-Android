package piuk.blockchain.android.ui.settings.v2.profile

import com.blockchain.logging.CrashLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import timber.log.Timber

class ProfileModel(
    initialState: ProfileState,
    mainScheduler: Scheduler,
    private val interactor: ProfileInteractor,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger
) : MviModel<ProfileState, ProfileIntent>(
    initialState,
    mainScheduler,
    environmentConfig,
    crashLogger
) {
    override fun performAction(
        previousState: ProfileState,
        intent: ProfileIntent
    ): Disposable? =
        when (intent) {
            is ProfileIntent.SaveProfile -> {
                interactor.saveProfile(
                    email = intent.userInfoSettings.email.orEmpty(),
                    mobileWithPrefix = intent.userInfoSettings.mobileWithPrefix.orEmpty()
                ).subscribeBy(
                    onSuccess = { (email, settings) ->
                        process(ProfileIntent.SaveProfileSucceeded(email, settings))
                    },
                    onError = {
                        Timber.e("SaveProfile failure " + it)
                        process(ProfileIntent.SaveProfileFailed)
                    }
                )
            }
            is ProfileIntent.LoadProfile -> {
                interactor.fetchProfileSettings()
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
            is ProfileIntent.SaveAndSendEmail -> {
                interactor.saveAndSendEmail(
                    email = intent.email
                ).subscribeBy(
                    onSuccess = {
                        process(ProfileIntent.SaveAndSendEmailSucceeded)
                    },
                    onError = {
                        Timber.e("SaveAndSendEmail failure " + it)
                        process(ProfileIntent.SaveAndSendEmailFailed)
                    }
                )
            }
            is ProfileIntent.SaveAndSendSMS -> {
                interactor.saveAndSendSMS(
                    mobileWithPrefix = intent.mobileWithPrefix
                ).subscribeBy(
                    onSuccess = {
                        process(ProfileIntent.SaveAndSendSMSSucceeded)
                    },
                    onError = {
                        Timber.e("SaveAndSendSMS failure " + it)
                        process(ProfileIntent.SaveAndSendSMSFailed)
                    }
                )
            }
            is ProfileIntent.VerifyPhoneNumber -> {
                interactor.verifyPhoneNumber(
                    code = intent.code
                ).subscribeBy(
                    onComplete = {
                        process(ProfileIntent.LoadProfile)
                    },
                    onError = {
                        Timber.e("VerifyPhoneNumber failure " + it)
                        process(ProfileIntent.VerifyPhoneNumberFailed)
                    }
                )
            }
            is ProfileIntent.ResetEmailSentVerification,
            is ProfileIntent.ResetCodeSentVerification,
            is ProfileIntent.SaveAndSendEmailSucceeded,
            is ProfileIntent.SaveAndSendEmailFailed,
            is ProfileIntent.SaveAndSendSMSSucceeded,
            is ProfileIntent.SaveAndSendSMSFailed,
            is ProfileIntent.VerifyPhoneNumberFailed,
            is ProfileIntent.LoadProfileSucceeded,
            is ProfileIntent.LoadProfileFailed,
            is ProfileIntent.SaveProfileFailed,
            is ProfileIntent.SaveProfileSucceeded,
            is ProfileIntent.UpdateProfileView -> null
        }
}
