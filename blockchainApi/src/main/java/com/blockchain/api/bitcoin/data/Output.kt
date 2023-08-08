@file:UseSerializers(BigIntSerializer::class)

package com.blockchain.api.bitcoin.data

import com.blockchain.serializers.BigIntSerializer
import java.math.BigInteger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class Output(
    @SerialName("spent")
    val isSpent: Boolean = false,
    @SerialName("tx_index")
    val txIndex: Long = 0,
    @SerialName("type")
    val type: Int = 0,
    @SerialName("addr")
    val addr: String? = null,
    @SerialName("value")
    val value: BigInteger,
    @SerialName("n")
    val count: Long = 0,
    @SerialName("script")
    val script: String? = null,
    @SerialName("xpub")
    val xpub: XpubDto? = null
)
