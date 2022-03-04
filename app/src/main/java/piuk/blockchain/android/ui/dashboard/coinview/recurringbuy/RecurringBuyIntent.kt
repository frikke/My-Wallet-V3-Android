package piuk.blockchain.android.ui.dashboard.coinview.recurringbuy

import com.blockchain.commonarch.presentation.mvi.MviIntent
import com.blockchain.nabu.models.data.RecurringBuy
import com.blockchain.nabu.models.data.RecurringBuyPaymentDetails
import com.blockchain.nabu.models.data.RecurringBuyState

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

    object UpdateRecurringBuyError : RecurringBuyIntent() {
        override fun reduce(oldState: RecurringBuyModelState): RecurringBuyModelState =
            oldState.copy(
                error = RecurringBuyError.RecurringBuyDelete
            )
    }

    class UpdateRecurringBuy(private val rb: RecurringBuy) : RecurringBuyIntent() {
        override fun reduce(oldState: RecurringBuyModelState): RecurringBuyModelState =
            oldState.copy(recurringBuy = rb)
    }
}
