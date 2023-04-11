package com.blockchain.transactions.swap.enteramount

import com.blockchain.commonarch.presentation.mvi_v2.ModelState

data class EnterAmountModelState(
    val fromAsset: String = "",
    val toAsset: String = ""
) : ModelState