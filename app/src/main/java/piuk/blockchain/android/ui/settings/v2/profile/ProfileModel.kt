package piuk.blockchain.android.ui.settings.v2.profile

import com.blockchain.extensions.exhaustive
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
            is ProfileIntent.UpdateProfile -> {
                interactor.saveProfile(
                    email = intent.userInfoSettings.email.orEmpty(),
                    phone = intent.userInfoSettings.mobile.orEmpty()
                ).subscribeBy(
                    onSuccess = {
                        process(ProfileIntent.ProfileUpdatedSuccessfully)
                    }, onError = {
                    Timber.e("save profile failure")
                    process(ProfileIntent.ProfileUpdateFailed)
                }
                )
            }
            is ProfileIntent.LoadProfile -> {
                interactor.fetchProfileSettings()
                    .subscribeBy(
                        onSuccess = { userInfo ->
                            process(
                                ProfileIntent.ProfileInfoLoaded(
                                    userInfoSettings = userInfo
                                )
                            )
                        }, onError = {
                        Timber.e("LoadProfile failure")
                        process(ProfileIntent.ProfileInfoFailed)
                    }
                    )
            }
            is ProfileIntent.ProfileInfoLoaded,
            is ProfileIntent.ProfileInfoFailed,
            is ProfileIntent.ProfileUpdateFailed,
            is ProfileIntent.ProfileUpdatedSuccessfully,
            is ProfileIntent.UpdateProfileView -> null
        }.exhaustive
}
