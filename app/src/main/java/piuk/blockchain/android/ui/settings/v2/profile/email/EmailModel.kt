package piuk.blockchain.android.ui.settings.v2.profile.email

import com.blockchain.api.services.WalletSettingsService
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

class EmailModel(
    initialState: EmailState,
    mainScheduler: Scheduler,
    private val interactor: EmailInteractor,
    private val _activityIndicator: Lazy<ActivityIndicator?>,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger
) : MviModel<EmailState, EmailIntent>(
    initialState,
    mainScheduler,
    environmentConfig,
    crashLogger
) {

    private val activityIndicator: ActivityIndicator? by unsafeLazy {
        _activityIndicator.value
    }

    override fun performAction(
        previousState: EmailState,
        intent: EmailIntent
    ): Disposable? =
        when (intent) {
            is EmailIntent.SaveEmail -> {
                interactor.saveEmail(
                    email = intent.email
                ).trackProgress(activityIndicator)
                    .subscribeBy(
                        onSuccess = {
                            process(EmailIntent.SaveEmailSucceeded(it))
                        },
                        onError = {
                            Timber.e("SaveEmail failure " + it)
                            process(EmailIntent.SaveEmailFailed)
                        }
                    )
            }
            is EmailIntent.InvalidateCache -> {
                interactor.invalidateCache
                null
            }
            is EmailIntent.LoadProfile -> {
                interactor.cachedSettings
                    .trackProgress(activityIndicator)
                    .subscribeBy(
                        onSuccess = { userInfo ->
                            process(
                                EmailIntent.LoadProfileSucceeded(
                                    userInfoSettings = WalletSettingsService.UserInfoSettings(
                                        userInfo.email, emailVerified = userInfo.isEmailVerified,
                                        mobileWithPrefix = userInfo.smsNumber, mobileVerified = userInfo.isSmsVerified,
                                        authType = userInfo.authType, smsDialCode = userInfo.smsDialCode
                                    )
                                )
                            )
                        }, onError = {
                        process(EmailIntent.FetchProfile)
                        Timber.e("EmailIntent.LoadProfile failure " + it)
                    }
                    )
            }
            is EmailIntent.FetchProfile -> {
                interactor.fetchProfileSettings()
                    .trackProgress(activityIndicator)
                    .subscribeBy(
                        onSuccess = { userInfo ->
                            process(
                                EmailIntent.LoadProfileSucceeded(
                                    userInfoSettings = userInfo
                                )
                            )
                        }, onError = {
                        process(EmailIntent.LoadProfileFailed)
                        Timber.e("EmailIntent.FetchProfile failure " + it)
                    }
                    )
            }
            is EmailIntent.ResendEmail -> {
                interactor.resendEmail(
                    email = previousState.userInfoSettings?.email.orEmpty()
                ).trackProgress(activityIndicator)
                    .subscribeBy(
                        onSuccess = {
                            process(EmailIntent.ResendEmailSucceeded(it))
                        },
                        onError = {
                            Timber.e("ResendEmail failure " + it)
                            process(EmailIntent.ResendEmailFailed)
                        }
                    )
            }
            is EmailIntent.SaveEmailFailed,
            is EmailIntent.SaveEmailSucceeded,
            is EmailIntent.ResetEmailSentVerification,
            is EmailIntent.ResendEmailSucceeded,
            is EmailIntent.ResendEmailFailed,
            is EmailIntent.LoadProfileSucceeded,
            is EmailIntent.LoadProfileFailed,
            is EmailIntent.ClearErrors -> null
        }
}
