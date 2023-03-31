package com.blockchain.home.presentation.recurringbuy

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.core.recurringbuy.domain.model.RecurringBuy
import com.blockchain.data.DataResource

data class RecurringBuysModelState(
    val recurringBuys: DataResource<List<RecurringBuy>> = DataResource.Loading,
    val lastFreshDataTime: Long = 0
) : ModelState
