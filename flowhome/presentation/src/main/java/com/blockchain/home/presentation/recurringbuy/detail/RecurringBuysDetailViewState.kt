package com.blockchain.home.presentation.recurringbuy.detail

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.data.DataResource

data class RecurringBuyDetailViewState(
    val detail: DataResource<RecurringBuyDetail>,
    val cancelationInProgress: Boolean
) : ViewState

data class RecurringBuyDetail(
    val iconUrl: String,
    val amount: String,
    val assetName: String,
    val assetTicker: String,
    val paymentMethod: String,
    val frequency: TextValue,
    val nextBuy: String
)
