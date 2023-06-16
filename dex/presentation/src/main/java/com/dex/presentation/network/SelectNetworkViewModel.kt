package com.dex.presentation.network

import androidx.lifecycle.viewModelScope
import com.blockchain.commonarch.presentation.mvi_v2.EmptyNavEvent
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.data.DataResource
import com.blockchain.data.mapList
import com.dex.domain.DexNetworkService
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.CoinNetwork
import kotlinx.coroutines.launch

class SelectNetworkViewModel(
    private val dexNetworkService: DexNetworkService,
    private val assetCatalogue: AssetCatalogue
) : MviViewModel<
    SelectNetworkIntent,
    SelectNetworkViewState,
    SelectNetworkModelState,
    EmptyNavEvent,
    ModelConfigArgs.NoArgs
    >(
    initialState = SelectNetworkModelState(
        selectedChainId = dexNetworkService.selectedChainId()
    )
) {

    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    init {
        viewModelScope.launch {
            val networks = dexNetworkService.supportedNetworks()
                .filter {
                    it.chainId != null &&
                        assetCatalogue.assetInfoFromNetworkTicker(it.nativeAssetTicker) != null
                }
            updateState {
                copy(
                    networks = DataResource.Data(networks)
                )
            }
        }
    }

    override fun SelectNetworkModelState.reduce() = SelectNetworkViewState(
        networks = networks.mapList {
            it.toDexNetwork(selectedChainId)
        }
    )

    override suspend fun handleIntent(
        modelState: SelectNetworkModelState,
        intent: SelectNetworkIntent
    ) {
        when (intent) {
            is SelectNetworkIntent.UpdateSelectedNetwork -> {
                dexNetworkService.updateSelectedNetwork(intent.chainId)
            }
        }
    }

    private fun CoinNetwork.toDexNetwork(selectedChainId: Int): DexNetworkViewState {
        val chainId = chainId
        val assetInfo = assetCatalogue.assetInfoFromNetworkTicker(nativeAssetTicker)
        check(chainId != null)
        check(assetInfo != null)
        return DexNetworkViewState(
            chainId = chainId,
            logo = assetInfo.logo,
            name = shortName,
            selected = chainId == selectedChainId
        )
    }
}
