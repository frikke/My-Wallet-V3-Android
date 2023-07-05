package com.blockchain.home.presentation.failedbalances

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource

data class FailedBalancesViewState(
    val failedNetworkNames: DataResource<List<String>>?
) : ViewState
