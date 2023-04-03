package com.blockchain.home.presentation.recurringbuy

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.data.DataResource

data class RecurringBuysViewState(
    val recurringBuys: DataResource<List<RecurringBuyViewState>>,
) : ViewState

data class RecurringBuyViewState(
    val id: String,
    val iconUrl: String,
    val description: TextValue,
    val status: TextValue
)
