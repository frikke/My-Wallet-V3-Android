package com.dex.presentation.network

import com.blockchain.commonarch.presentation.mvi_v2.Intent
import com.dex.presentation.SettingsModelState

sealed interface SelectNetworkIntent : Intent<SelectNetworkModelState> {
    data class UpdateNetwork(val chainId: Int): SelectNetworkIntent
}
