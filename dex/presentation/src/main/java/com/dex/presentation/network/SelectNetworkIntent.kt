package com.dex.presentation.network

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface SelectNetworkIntent : Intent<SelectNetworkModelState> {
    data class UpdateSelectedNetwork(val chainId: Int) : SelectNetworkIntent
}
