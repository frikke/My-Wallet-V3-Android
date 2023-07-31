package com.dex.presentation.network

import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.data.DataResource

data class SelectNetworkViewState(
    val networks: DataResource<List<DexNetworkViewState>>,
) : ViewState

data class DexNetworkViewState(
    val chainId: Int,
    val logo: String,
    val name: String,
    val selected: Boolean
)
