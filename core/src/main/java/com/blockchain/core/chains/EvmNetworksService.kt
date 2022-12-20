package com.blockchain.core.chains

import com.blockchain.core.chains.ethereum.EthDataManager
import com.blockchain.domain.experiments.RemoteConfigService
import com.blockchain.outcome.map
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class EvmNetworkList(
    @SerialName("networks")
    val networks: List<EvmNetwork> = listOf()
)

@Serializable
data class EvmNetwork(
    @SerialName("networkTicker")
    val networkTicker: String,
    @SerialName("networkName")
    val networkName: String,
    @SerialName("chainId")
    val chainId: Int,
    @SerialName("nodeUrl")
    val nodeUrl: String,
    @SerialName("explorerUrl")
    val explorerUrl: String
)

class EvmNetworksService(
    private val remoteConfig: RemoteConfigService
) {
    fun getSupportedNetworks(): Single<List<EvmNetwork>> {
        return remoteConfig.getRawJson(LAYER_TWO_NETWORKS).map { json ->
            jsonBuilder.decodeFromString<EvmNetworkList>(json).networks
        }
    }

    fun getSupportedNetworkForCurrency(currency: String): Maybe<EvmNetwork> {
        return remoteConfig.getRawJson(LAYER_TWO_NETWORKS).map {
            it.plus(EthDataManager.ethChain)
        }
            .map { json ->
                jsonBuilder.decodeFromString<EvmNetworkList>(json).networks
                    .first { it.networkTicker == currency }
            }.toMaybe()
    }

    companion object {
        private val jsonBuilder = Json {
            ignoreUnknownKeys = true
        }
        const val LAYER_TWO_NETWORKS = "android_ff_layer_two_networks"
    }
}
