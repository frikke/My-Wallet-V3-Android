package piuk.blockchain.android.ui.home.v2

import com.blockchain.logging.CrashLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

class RedesignModel(
    initialState: RedesignState,
    mainScheduler: Scheduler,
    private val interactor: RedesignInteractor,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger
) : MviModel<RedesignState, RedesignIntent>(
    initialState,
    mainScheduler,
    environmentConfig,
    crashLogger
) {
    override fun performAction(previousState: RedesignState, intent: RedesignIntent): Disposable? {
        // TODO not yet implemented
        return null
    }
}
