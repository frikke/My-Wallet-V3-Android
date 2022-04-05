package piuk.blockchain.android.data.coinswebsocket.models

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Coin {
    @SerializedName("eth")
    @SerialName("eth")
    ETH,
    @SerializedName("btc")
    @SerialName("btc")
    BTC,
    @SerializedName("bch")
    @SerialName("bch")
    BCH,
    @SerializedName("none")
    @SerialName("none")
    None
}
