package com.blockchain.walletconnect.ui.networks

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed class SelectNetworkIntents : Intent<SelectNetworkModelState> {
    data class LoadSupportedNetworks(val preSelectedChainId: Int) : SelectNetworkIntents()
    data class LoadIconForNetworks(
        val networks: List<NetworkInfo>,
        val selectedNetwork: NetworkInfo?
    ) : SelectNetworkIntents()
    data class SelectNetwork(val chainId: Int) : SelectNetworkIntents()
}
