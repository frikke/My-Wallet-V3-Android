package com.dex.presentation.network

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource

data class SelectNetworkViewState(
    val networks: DataResource<List<DexNetwork>>,
) : ViewState

data class DexNetwork(
    val chainId: Int,
    val icon: String,
    val name: String,
    val selected: Boolean
)