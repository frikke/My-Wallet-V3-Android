package com.blockchain.home.presentation.recurringbuy.detail

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.recurringbuy.list.RecurringBuyEligibleState
import com.blockchain.home.presentation.recurringbuy.list.RecurringBuyViewState

data class RecurringBuyDetailViewState(
    val detail: DataResource<RecurringBuyDetail>,
) : ViewState

data class RecurringBuyDetail(
    val iconUrl: String,
    val amount: String,
    val assetName: String,
    val paymentMethod: String,
    val frequency: TextValue,
    val nextBuy: String,
)
