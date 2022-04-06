package piuk.blockchain.android.ui.dashboard.coinview.recurringbuy

import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.RemoteLogger
import com.blockchain.nabu.models.data.RecurringBuy
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy

data class RecurringBuyModelState(
    val recurringBuy: RecurringBuy? = null,
    val error: RecurringBuyError = RecurringBuyError.None
) : MviState

enum class RecurringBuyError {
    None,
    RecurringBuyDelete
}

class RecurringBuyModel(
    initialState: RecurringBuyModelState,
    mainScheduler: Scheduler,
    private val interactor: RecurringBuyInteractor,
    environmentConfig: EnvironmentConfig,
    remoteLogger: RemoteLogger
) : MviModel<RecurringBuyModelState, RecurringBuyIntent>(
    initialState, mainScheduler, environmentConfig, remoteLogger
) {
    override fun performAction(
        previousState: RecurringBuyModelState,
        intent: RecurringBuyIntent
    ): Disposable? {
        return when (intent) {
            is RecurringBuyIntent.DeleteRecurringBuy -> {
                previousState.recurringBuy?.let {
                    interactor.deleteRecurringBuy(it.id)
                        .subscribeBy(
                            onComplete = {
                                process(RecurringBuyIntent.UpdateRecurringBuyState)
                            },
                            onError = {
                                process(RecurringBuyIntent.UpdateRecurringBuyError)
                            }
                        )
                }
            }
            is RecurringBuyIntent.GetPaymentDetails -> {
                previousState.recurringBuy?.let {
                    interactor.loadPaymentDetails(
                        it.paymentMethodType, it.paymentMethodId.orEmpty(), it.amount.currencyCode
                    )
                        .subscribeBy(
                            onSuccess = { details ->
                                process(RecurringBuyIntent.UpdatePaymentDetails(details))
                            }
                        )
                }
            }
            is RecurringBuyIntent.UpdatePaymentDetails,
            RecurringBuyIntent.UpdateRecurringBuyState,
            RecurringBuyIntent.UpdateRecurringBuyError,
            is RecurringBuyIntent.UpdateRecurringBuy -> null
        }
    }
}
