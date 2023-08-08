package com.blockchain.home.presentation.recurringbuy.list

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.blockchain.home.presentation.SectionSize

sealed interface RecurringBuysIntent : Intent<RecurringBuysModelState> {
    data class LoadRecurringBuys(
        val sectionSize: SectionSize,
        val includeInactive: Boolean = false
    ) : RecurringBuysIntent
}
