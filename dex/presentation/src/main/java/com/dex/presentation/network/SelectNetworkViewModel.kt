package com.dex.presentation.network

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.EmptyNavEvent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.mapList
import com.blockchain.data.updateDataWith
import com.blockchain.store.filterListData
import com.dex.domain.DexChainService
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.CoinNetwork
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SelectNetworkViewModel(
    private val dexChainService: DexChainService,
    private val assetCatalogue: AssetCatalogue
) : MviViewModel<
    SelectNetworkIntent,
    SelectNetworkViewState,
    SelectNetworkModelState,
    EmptyNavEvent,
    ModelConfigArgs.NoArgs
    >(
    initialState = SelectNetworkModelState(
        selectedChainId = dexChainService.selectedChainId()
    )
) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    init {
        viewModelScope.launch {
            dexChainService.supportedNetworks()
                .filterListData {
                    it.chainId != null &&
                        assetCatalogue.assetInfoFromNetworkTicker(it.networkTicker) != null
                }
                .collectLatest { networksDataResource ->
                    updateState {
                        it.copy(
                            networks = it.networks.updateDataWith(networksDataResource)
                        )
                    }
                }
        }
    }

    override fun reduce(state: SelectNetworkModelState) = state.run {
        SelectNetworkViewState(
            networks = networks.mapList {
                it.toDexNetwork(selectedChainId)
            }
        )
    }

    override suspend fun handleIntent(
        modelState: SelectNetworkModelState,
        intent: SelectNetworkIntent
    ) {
        when (intent) {
            is SelectNetworkIntent.UpdateNetwork -> {
                dexChainService.updateSelectedNetwork(intent.chainId)
            }
        }
    }

    private fun CoinNetwork.toDexNetwork(selectedChainId: Int): DexNetwork {
        val chainId = chainId
        val assetInfo = assetCatalogue.assetInfoFromNetworkTicker(networkTicker)
        check(chainId != null)
        check(assetInfo != null)
        return DexNetwork(
            chainId = chainId,
            icon = assetInfo.logo,
            name = name,
            selected = chainId == selectedChainId
        )
    }
}
