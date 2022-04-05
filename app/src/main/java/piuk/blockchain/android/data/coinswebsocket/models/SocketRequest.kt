package piuk.blockchain.android.data.coinswebsocket.models

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class SocketRequest(private val command: Command) {

    @Serializable
    object PingRequest : SocketRequest(Command.PING)

    @Serializable
    data class SubscribeRequest(val entity: Entity, val coin: Coin, val param: Parameters?) :
        SocketRequest(Command.SUBSCRIBE)

    @Serializable
    data class UnSubscribeRequest(val entity: Entity, val coin: Coin, val param: Parameters?) :
        SocketRequest(Command.UNSUBSCRIBE)
}

@Serializable
enum class Command {
    @SerializedName("ping")
    @SerialName("ping")
    PING,

    @SerializedName("subscribe")
    @SerialName("subscribe")
    SUBSCRIBE,

    @SerializedName("unsubscribe")
    @SerialName("unsubscribe")
    UNSUBSCRIBE
}

@Serializable
sealed class Parameters {
    @Serializable
    data class SimpleAddress(val address: String) : Parameters()

    @Serializable
    data class Guid(val guid: String) : Parameters()

    @Serializable
    data class TokenedAddress(
        val address: String,
        @SerializedName("token_address")
        @SerialName("token_address")
        val tokenAddress: String
    ) : Parameters()
}
