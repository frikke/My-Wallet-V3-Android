package piuk.blockchain.android.ui.dashboard.coinview.recurringbuy

import com.blockchain.commonarch.presentation.mvi.MviIntent
import com.blockchain.core.recurringbuy.domain.model.RecurringBuy
import com.blockchain.core.recurringbuy.domain.model.RecurringBuyState
import com.blockchain.domain.paymentmethods.model.RecurringBuyPaymentDetails

sealed class RecurringBuyIntent : MviIntent<RecurringBuyModelState> {

    object DeleteRecurringBuy : RecurringBuyIntent() {
        override fun reduce(oldState: RecurringBuyModelState): RecurringBuyModelState = oldState
    }

    object GetPaymentDetails : RecurringBuyIntent() {
        override fun reduce(oldState: RecurringBuyModelState): RecurringBuyModelState = oldState
    }

    class UpdatePaymentDetails(
        private val recurringBuyPaymentDetails: RecurringBuyPaymentDetails
    ) : RecurringBuyIntent() {
        override fun reduce(oldState: RecurringBuyModelState): RecurringBuyModelState =
            oldState.copy(
                recurringBuy = oldState.recurringBuy?.copy(paymentDetails = recurringBuyPaymentDetails)
            )
    }

    object UpdateRecurringBuyState : RecurringBuyIntent() {
        override fun reduce(oldState: RecurringBuyModelState): RecurringBuyModelState =
            oldState.copy(
                recurringBuy = oldState.recurringBuy?.copy(state = RecurringBuyState.INACTIVE)
            )
    }

    class UpdateRecurringBuyError(val recurringBuyError: RecurringBuyError) : RecurringBuyIntent() {
        override fun reduce(oldState: RecurringBuyModelState): RecurringBuyModelState =
            oldState.copy(
                error = recurringBuyError
            )
    }

    class LoadRecurringBuy(val rbId: String) : RecurringBuyIntent() {
        override fun reduce(oldState: RecurringBuyModelState): RecurringBuyModelState = oldState
    }

    class UpdateRecurringBuy(private val rb: RecurringBuy) : RecurringBuyIntent() {
        override fun reduce(oldState: RecurringBuyModelState): RecurringBuyModelState =
            oldState.copy(recurringBuy = rb, viewState = RecurringBuyViewState.ShowRecurringBuy)
    }
}
