package info.blockchain.wallet.test_data

import java.util.ArrayList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class TestVectorCoin {
    @SerialName("coinPath")
    val coinPath: String? = null

    @SerialName("coinUriScheme")
    val coinUriScheme: String? = null

    @SerialName("accounts")
    val accountList: ArrayList<TestVectorAccount>? = null

    fun toJson(): String {
        return Json.encodeToString(this)
    }

    companion object {
        @JvmStatic
        fun fromJson(json: String): TestVectorCoin {
            return Json.decodeFromString(json)
        }
    }
}
