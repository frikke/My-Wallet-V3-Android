package com.blockchain.transactions.swap.selecttarget

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.tablerow.BalanceChange
import com.blockchain.data.DataResource

data class SelectTargetViewState(
    val prices: DataResource<List<BalanceChange>>
) : ViewState
