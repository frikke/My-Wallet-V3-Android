package com.blockchain.home.presentation.recurringbuy.list

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.data.DataResource

data class RecurringBuysViewState(
    val recurringBuys: DataResource<RecurringBuyEligibleState>,
) : ViewState

sealed interface RecurringBuyEligibleState {
    data class Eligible(
        val recurringBuys: List<RecurringBuyViewState>
    ) : RecurringBuyEligibleState

    object NotEligible : RecurringBuyEligibleState
}

data class RecurringBuyViewState(
    val id: String,
    val iconUrl: String,
    val description: TextValue,
    val status: TextValue
)
