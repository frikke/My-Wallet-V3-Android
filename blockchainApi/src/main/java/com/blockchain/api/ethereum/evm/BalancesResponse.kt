package com.blockchain.api.ethereum.evm

import java.math.BigInteger
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BalancesResponse(
    @SerialName("results")
    val addresses: List<EvmAddressResponse>
)

@Serializable
data class EvmAddressResponse(
    @SerialName("address")
    val address: String,
    @SerialName("balances")
    val balances: List<EvmBalanceResponse>
)

@Serializable
data class EvmBalanceResponse(
    @SerialName("identifier") // contract address or "native"
    val contractAddress: String,
    @SerialName("currency")
    val name: String,
    @SerialName("balance")
    val amount: @Contextual BigInteger
) {
    companion object {
        const val NATIVE_IDENTIFIER = "native"
    }
}
