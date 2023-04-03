package com.blockchain.home.presentation.recurringbuy.detail

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.core.recurringbuy.domain.model.RecurringBuy
import com.blockchain.data.DataResource

data class RecurringBuysDetailModelState(
    val recurringBuy: DataResource<RecurringBuy> = DataResource.Loading,
    val cancelationInProgress: Boolean = false
) : ModelState
