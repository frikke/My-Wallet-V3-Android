package piuk.blockchain.android.ui.reset

import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.RemoteLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable

class ResetAccountModel(
    initialState: ResetAccountState,
    mainScheduler: Scheduler,
    environmentConfig: EnvironmentConfig,
    remoteLogger: RemoteLogger
) : MviModel<ResetAccountState, ResetAccountIntents>(
    initialState,
    mainScheduler,
    environmentConfig,
    remoteLogger
) {
    override fun performAction(
        previousState: ResetAccountState,
        intent: ResetAccountIntents
    ): Disposable? = null
}
