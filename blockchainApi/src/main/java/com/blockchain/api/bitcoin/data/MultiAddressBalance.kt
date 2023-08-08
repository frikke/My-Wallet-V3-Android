@file:UseSerializers(BigIntSerializer::class)

package com.blockchain.api.bitcoin.data

import com.blockchain.serializers.BigIntSerializer
import java.math.BigInteger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class MultiAddressBalance(
    @SerialName("n_tx")
    val txCount: Long = 0,
    @SerialName("n_tx_filtered")
    val txCountFiltered: Long = 0,
    @SerialName("total_received")
    val totalReceived: BigInteger,
    @SerialName("total_sent")
    val totalSent: BigInteger,
    @SerialName("final_balance")
    val finalBalance: BigInteger
)
