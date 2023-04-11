package com.blockchain.transactions.swap.enteramount

import com.blockchain.commonarch.presentation.mvi_v2.ViewState

data class EnterAmountViewState(
    val fromAsset: String,
    val toAsset: String
) : ViewState
