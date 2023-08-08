@file:UseSerializers(BigIntSerializer::class)

package info.blockchain.wallet.ethereum.data

import com.blockchain.serializers.BigIntSerializer
import java.math.BigInteger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
class EthLatestBlockNumber {
    @SerialName("number")
    var number: BigInteger = 0.toBigInteger()
}
