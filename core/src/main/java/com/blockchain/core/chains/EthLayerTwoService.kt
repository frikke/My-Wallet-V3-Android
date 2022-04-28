package com.blockchain.core.chains

import com.blockchain.remoteconfig.RemoteConfig
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class EthL2NetworkList(
    @SerialName("networks")
    val networks: List<EthL2Chain> = listOf()
)

@Serializable
data class EthL2Chain(
    @SerialName("networkTicker")
    val networkTicker: String,
    @SerialName("networkName")
    val networkName: String,
    @SerialName("chainId")
    val chainId: Int,
    @SerialName("nodeUrl")
    val nodeUrl: String
)

class EthLayerTwoService(
    private val remoteConfig: RemoteConfig
) {
    fun getSupportedNetworks(): Single<List<EthL2Chain>> {
        return remoteConfig.getRawJson(LAYER_TWO_NETWORKS).map { json ->
            jsonBuilder.decodeFromString<EthL2NetworkList>(json).networks
        }
    }

    companion object {
        private val jsonBuilder = Json {
            ignoreUnknownKeys = true
        }
        const val LAYER_TWO_NETWORKS = "android_ff_layer_two_networks"
    }
}
