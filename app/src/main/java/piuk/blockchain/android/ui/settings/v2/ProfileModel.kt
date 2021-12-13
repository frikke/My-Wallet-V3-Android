package piuk.blockchain.android.ui.settings.v2

import com.blockchain.logging.CrashLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.CompositeDisposable
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

    private val compositeDisposable = CompositeDisposable()

    fun clearDisposables() = compositeDisposable.clear()

    override fun performAction(
        previousState: ProfileState,
        intent: ProfileIntent
    ): Disposable? =
        when (intent) {
            is ProfileIntent.UpdateProfile -> {
                interactor.saveProfile(intent.email, intent.phoneNumber)
                    .subscribeBy(
                        onSuccess = {
                            process(ProfileIntent.ProfileUpdatedSuccessfully)
                        }, onError = {
                        Timber.e("save profile failure")
                        process(ProfileIntent.ProfileUpdateFailed)
                    }
                    )
            }
            is ProfileIntent.ProfileUpdateFailed,
            is ProfileIntent.ProfileUpdatedSuccessfully,
            is ProfileIntent.UpdateProfileView -> null
        }
}
