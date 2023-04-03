package com.blockchain.home.presentation.recurringbuy.list

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.core.recurringbuy.domain.model.RecurringBuy
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.SectionSize

data class RecurringBuysModelState(
    val recurringBuys: DataResource<List<RecurringBuy>?> = DataResource.Loading,
    val sectionSize: SectionSize = SectionSize.All,
    val lastFreshDataTime: Long = 0
) : ModelState
