package piuk.blockchain.android.ui.dashboard.coinview.recurringbuy

import com.blockchain.api.NabuApiExceptionFactory
import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.RemoteLogger
import com.blockchain.core.recurringbuy.RecurringBuy
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import retrofit2.HttpException

data class RecurringBuyModelState(
    val recurringBuy: RecurringBuy? = null,
    val error: RecurringBuyError = RecurringBuyError.None,
    val viewState: RecurringBuyViewState = RecurringBuyViewState.Loading
) : MviState

sealed class RecurringBuyViewState {
    object Loading : RecurringBuyViewState()
    object ShowRecurringBuy : RecurringBuyViewState()
}

sealed class RecurringBuyError {
    object None : RecurringBuyError()
    object RecurringBuyDelete : RecurringBuyError()
    object LoadFailed : RecurringBuyError()
    data class HttpError(val errorMessage: String) : RecurringBuyError()
    object UnknownError : RecurringBuyError()
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
                                process(
                                    RecurringBuyIntent.UpdateRecurringBuyError(RecurringBuyError.RecurringBuyDelete)
                                )
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
                            },
                            onError = { error ->
                                if (error is HttpException) {
                                    val exception = NabuApiExceptionFactory.fromResponseBody(error)
                                    process(
                                        RecurringBuyIntent.UpdateRecurringBuyError(
                                            RecurringBuyError.HttpError(
                                                exception.getServerSideErrorInfo()?.description
                                                    ?: exception.getErrorDescription()
                                            )
                                        )
                                    )
                                } else {
                                    process(
                                        RecurringBuyIntent.UpdateRecurringBuyError(
                                            RecurringBuyError.UnknownError
                                        )
                                    )
                                }
                            }
                        )
                }
            }
            is RecurringBuyIntent.LoadRecurringBuy -> interactor.getRecurringBuyById(intent.rbId)
                .subscribeBy(
                    onSuccess = {
                        process(RecurringBuyIntent.UpdateRecurringBuy(it))
                    },
                    onError = {
                        process(RecurringBuyIntent.UpdateRecurringBuyError(RecurringBuyError.LoadFailed))
                    }
                )
            is RecurringBuyIntent.UpdatePaymentDetails,
            RecurringBuyIntent.UpdateRecurringBuyState,
            is RecurringBuyIntent.UpdateRecurringBuyError,
            is RecurringBuyIntent.UpdateRecurringBuy -> null
        }
    }
}
