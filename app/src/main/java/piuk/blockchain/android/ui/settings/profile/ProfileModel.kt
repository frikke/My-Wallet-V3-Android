package piuk.blockchain.android.ui.settings.profile

import com.blockchain.api.services.WalletSettingsService
import com.blockchain.commonarch.presentation.base.ActivityIndicator
import com.blockchain.commonarch.presentation.base.trackProgress
import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.RemoteLogger
import com.blockchain.utils.unsafeLazy
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import timber.log.Timber

class ProfileModel(
    initialState: ProfileState,
    mainScheduler: Scheduler,
    private val interactor: ProfileInteractor,
    private val _activityIndicator: Lazy<ActivityIndicator?>,
    environmentConfig: EnvironmentConfig,
    remoteLogger: RemoteLogger
) : MviModel<ProfileState, ProfileIntent>(
    initialState,
    mainScheduler,
    environmentConfig,
    remoteLogger
) {

    private val activityIndicator: ActivityIndicator? by unsafeLazy {
        _activityIndicator.value
    }

    override fun performAction(
        previousState: ProfileState,
        intent: ProfileIntent
    ): Disposable? =
        when (intent) {
            is ProfileIntent.LoadProfile -> {
                interactor.cachedSettings
                    .trackProgress(activityIndicator)
                    .subscribeBy(
                        onSuccess = { userInfo ->
                            process(
                                ProfileIntent.LoadProfileSucceeded(
                                    userInfoSettings = WalletSettingsService.UserInfoSettings(
                                        userInfo.email, emailVerified = userInfo.isEmailVerified,
                                        mobileWithPrefix = userInfo.smsNumber, mobileVerified = userInfo.isSmsVerified,
                                        authType = userInfo.authType, smsDialCode = userInfo.smsDialCode
                                    )
                                )
                            )
                        }, onError = {
                        process(ProfileIntent.FetchProfile)
                        Timber.e("ProfileIntent.LoadProfile failure " + it)
                    }
                    )
            }
            is ProfileIntent.FetchProfile -> {
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
                        Timber.e("ProfileIntent.FetchProfile failure " + it)
                    }
                    )
            }
            is ProfileIntent.LoadProfileSucceeded,
            is ProfileIntent.LoadProfileFailed -> null
        }
}
