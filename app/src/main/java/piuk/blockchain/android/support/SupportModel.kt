package piuk.blockchain.android.support

import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.RemoteLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy

class SupportModel(
    initialState: SupportState,
    mainScheduler: Scheduler,
    private val interactor: SupportInteractor,
    environmentConfig: EnvironmentConfig,
    remoteLogger: RemoteLogger
) : MviModel<SupportState, SupportIntent>(
    initialState,
    mainScheduler,
    environmentConfig,
    remoteLogger
) {
    override fun performAction(
        previousState: SupportState,
        intent: SupportIntent
    ): Disposable? =
        when (intent) {
            SupportIntent.LoadUserInfo ->
                interactor.loadUserInformation()
                    .subscribeBy(
                        onSuccess = {
                            process(SupportIntent.UpdateViewState(SupportViewState.ShowInfo(it)))
                        },
                        onError = {
                            process(SupportIntent.UpdateErrorState(SupportError.ErrorLoadingProfileInfo))
                        }
                    )
            SupportIntent.ResetErrorState,
            SupportIntent.ResetViewState,
            is SupportIntent.UpdateViewState,
            is SupportIntent.UpdateErrorState,
            is SupportIntent.OnTopicSelected -> null
        }
}
