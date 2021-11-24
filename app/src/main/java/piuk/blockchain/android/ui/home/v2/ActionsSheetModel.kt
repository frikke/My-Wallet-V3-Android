package piuk.blockchain.android.ui.home.v2

import com.blockchain.logging.CrashLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import timber.log.Timber

class ActionsSheetModel(
    initialState: ActionsSheetState,
    mainScheduler: Scheduler,
    private val interactor: ActionsSheetInteractor,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger
) : MviModel<ActionsSheetState, ActionsSheetIntent>(
    initialState,
    mainScheduler,
    environmentConfig,
    crashLogger
) {

    override fun performAction(previousState: ActionsSheetState, intent: ActionsSheetIntent): Disposable? =
        when (intent) {
            is ActionsSheetIntent.CheckForPendingBuys -> interactor.getUserAccessToSimpleBuy().subscribeBy(
                onSuccess = { actionIntent ->
                    process(actionIntent)
                },
                onError = {
                    // if this errors, proceed to Buy screen
                    Timber.e("Error getting user access to SimpleBuy ${it.message}")
                    process(ActionsSheetIntent.UpdateFlowToLaunch(FlowToLaunch.BuyFlow))
                }
            )
            else -> null
        }
}
