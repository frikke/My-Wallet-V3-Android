package com.blockchain.api.ethereum.layertwo

import java.math.BigInteger
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BalancesResponse(
    @SerialName("results")
    val addresses: List<L2AddressResponse>
)

@Serializable
data class L2AddressResponse(
    @SerialName("address")
    val address: String,
    @SerialName("balances")
    val balances: List<L2BalanceResponse>
)

@Serializable
data class L2BalanceResponse(
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
