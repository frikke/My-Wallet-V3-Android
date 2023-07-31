package com.blockchain.home.presentation.recurringbuy.detail

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.data.DataResource

sealed interface RecurringBuysDetailIntent : Intent<RecurringBuysDetailModelState> {
    data class LoadRecurringBuy(
        val includeInactive: Boolean = false
    ) : RecurringBuysDetailIntent

    object CancelRecurringBuy : RecurringBuysDetailIntent {
        override fun isValidFor(modelState: RecurringBuysDetailModelState): Boolean {
            return modelState.recurringBuy is DataResource.Data && !modelState.cancelationInProgress
        }
    }
}
