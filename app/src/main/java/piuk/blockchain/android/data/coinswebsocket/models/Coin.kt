package piuk.blockchain.android.data.coinswebsocket.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Coin {
    @SerialName("eth")
    ETH,
    @SerialName("btc")
    BTC,
    @SerialName("bch")
    BCH,
    @SerialName("none")
    None
}
