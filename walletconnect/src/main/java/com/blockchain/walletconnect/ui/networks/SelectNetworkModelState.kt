package com.blockchain.walletconnect.ui.networks

import com.blockchain.commonarch.presentation.mvi_v2.ModelState

data class SelectNetworkModelState(
    val networks: List<NetworkInfo> = emptyList(),
    val selectedNetwork: NetworkInfo? = null
) : ModelState
