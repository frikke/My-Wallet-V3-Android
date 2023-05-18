package com.blockchain.walletconnect.ui.networks

import androidx.lifecycle.viewModelScope
import com.blockchain.coincore.Coincore
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.core.chains.ethereum.EthDataManager
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.doOnSuccess
import com.blockchain.utils.awaitOutcome
import info.blockchain.balance.CoinNetwork
import kotlinx.coroutines.launch
import timber.log.Timber

class SelectNetworkViewModel(
    private val coincore: Coincore,
    private val ethDataManager: EthDataManager
) : MviViewModel<
    SelectNetworkIntents,
    SelectNetworkViewState,
    SelectNetworkModelState,
    SelectNetworkNavigationEvent,
    ModelConfigArgs.NoArgs
    >(
    SelectNetworkModelState()
) {
    override fun viewCreated(args: ModelConfigArgs.NoArgs) {}

    override fun SelectNetworkModelState.reduce() = SelectNetworkViewState(
        networks = networks,
        selectedNetwork = selectedNetwork
    )

    override suspend fun handleIntent(modelState: SelectNetworkModelState, intent: SelectNetworkIntents) {
        when (intent) {
            is SelectNetworkIntents.LoadSupportedNetworks -> loadSupportedNetworks(intent.preSelectedChainId)
            is SelectNetworkIntents.LoadIconForNetworks -> loadIconsForNetworks(intent.networks, intent.selectedNetwork)
            is SelectNetworkIntents.SelectNetwork -> updateState {
                copy(
                    selectedNetwork = networks.findByChainId(intent.chainId)
                )
            }
        }
    }

    private suspend fun loadSupportedNetworks(chainIdToSelect: Int) = viewModelScope.launch {
        ethDataManager.supportedNetworks
            .awaitOutcome()
            .doOnSuccess { supportedNetworks ->
                val networks = supportedNetworks.map { evmNetwork -> evmNetwork.toNetworkInfo() }
                val selectedNetwork = networks.findByChainId(chainIdToSelect)
                updateState {
                    copy(
                        networks = networks,
                        selectedNetwork = selectedNetwork
                    )
                }
                onIntent(
                    SelectNetworkIntents.LoadIconForNetworks(
                        networks,
                        selectedNetwork
                    )
                )
            }
            .doOnFailure {
                Timber.e(it)
                updateState { copy(networks = emptyList()) }
            }
    }

    private fun loadIconsForNetworks(networks: List<NetworkInfo>, selectedNetwork: NetworkInfo?) = updateState {
        copy(
            networks = networks.map { network ->
                network.copy(
                    logo = coincore[network.networkTicker]?.currency?.logo
                )
            },
            selectedNetwork = selectedNetwork?.copy(
                logo = coincore[selectedNetwork.networkTicker]?.currency?.logo
            )
        )
    }

    private fun CoinNetwork.toNetworkInfo() =
        NetworkInfo(
            networkTicker = nativeAssetTicker,
            name = name,
            chainId = chainId!!
        )

    private fun List<NetworkInfo>.findByChainId(chainId: Int) =
        firstOrNull { network ->
            network.chainId == chainId
        }
}
