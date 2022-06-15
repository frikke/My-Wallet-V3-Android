package piuk.blockchain.android.ui.home.models

import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.RemoteLogger
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import timber.log.Timber

class ActionsSheetModel(
    initialState: ActionsSheetState,
    mainScheduler: Scheduler,
    private val interactor: ActionsSheetInteractor,
    environmentConfig: EnvironmentConfig,
    remoteLogger: RemoteLogger
) : MviModel<ActionsSheetState, ActionsSheetIntent>(
    initialState,
    mainScheduler,
    environmentConfig,
    remoteLogger
) {

    override fun performAction(previousState: ActionsSheetState, intent: ActionsSheetIntent): Disposable? =
        when (intent) {
            is ActionsSheetIntent.CheckForPendingBuys -> interactor.checkForPendingBuys()
                .subscribeBy(
                    onSuccess = { actionIntent ->
                        process(actionIntent)
                    },
                    onError = {
                        // if this errors, proceed to Buy screen
                        Timber.e("Error getting user access to SimpleBuy ${it.message}")
                        process(ActionsSheetIntent.UpdateFlowToLaunch(FlowToLaunch.BuyFlow))
                    }
                )
            is ActionsSheetIntent.CheckCtaOrdering -> interactor.getFabCtaOrdering()
                .subscribeBy(
                    onSuccess = {
                        process(ActionsSheetIntent.UpdateCtaOrdering(it))
                    },
                    onError = {
                        // if this errors, update to cater for most widespread users >90%
                        Timber.e("Error getting CTA ordering ${it.message}")
                        process(ActionsSheetIntent.UpdateCtaOrdering(SplitButtonCtaOrdering.BUY_END))
                    }
                )
            else -> null
        }
}
