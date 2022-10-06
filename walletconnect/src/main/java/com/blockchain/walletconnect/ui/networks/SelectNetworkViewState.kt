package com.blockchain.walletconnect.ui.networks

import com.blockchain.commonarch.presentation.mvi_v2.ViewState

data class SelectNetworkViewState(
    val networks: List<NetworkInfo>,
    val selectedNetwork: NetworkInfo?
) : ViewState
