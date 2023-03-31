package com.blockchain.home.presentation.recurringbuy.detail

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.home.presentation.SectionSize
import com.blockchain.home.presentation.recurringbuy.list.RecurringBuysIntent
import com.blockchain.home.presentation.recurringbuy.list.RecurringBuysModelState

sealed interface RecurringBuysDetailIntent : Intent<RecurringBuysDetailModelState> {
    data class LoadRecurringBuy(
        val includeInactive: Boolean = false
    ) : RecurringBuysDetailIntent
}
