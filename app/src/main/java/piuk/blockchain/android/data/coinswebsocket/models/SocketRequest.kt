package piuk.blockchain.android.data.coinswebsocket.models

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
    @SerialName("ping")
    PING,

    @SerialName("subscribe")
    SUBSCRIBE,

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
        @SerialName("token_address")
        val tokenAddress: String
    ) : Parameters()
}
