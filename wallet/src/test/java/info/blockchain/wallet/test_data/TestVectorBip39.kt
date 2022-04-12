package info.blockchain.wallet.test_data

import java.lang.Exception
import java.util.ArrayList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class TestVectorBip39 {
    @SerialName("entropy")
    val entropy: String? = null

    @SerialName("mnemonic")
    val mnemonic: String? = null

    @SerialName("passphrase")
    val passphrase: String? = null

    @SerialName("seed")
    val seed: String? = null

    @SerialName("coins")
    private val coinList: ArrayList<TestVectorCoin>? = null
    fun toJson(): String {
        return Json.encodeToString(this)
    }

    @Throws(Exception::class) fun getCoinTestVectors(uriScheme: String, coinPath: String): TestVectorCoin {
        for (coin: TestVectorCoin in coinList!!) {
            if (coin.coinUriScheme == uriScheme && coin.coinPath == coinPath) {
                return coin
            }
        }
        throw Exception(
            "Coin uri scheme " + uriScheme + " and path " + coinPath +
                " not found. Add test vectors for this coin to it to test_EN_BIP39.json"
        )
    }

    companion object {
        fun fromJson(json: String): TestVectorBip39 {
            return Json.decodeFromString(json)
        }
    }
}
