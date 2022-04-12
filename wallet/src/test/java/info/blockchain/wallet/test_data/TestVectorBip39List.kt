package info.blockchain.wallet.test_data

import java.util.ArrayList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class TestVectorBip39List {
    @SerialName("vectors")
    val vectors: ArrayList<TestVectorBip39>? = null

    fun toJson(): String {
        return jsonBuilder.encodeToString(this)
    }

    companion object {
        val jsonBuilder = Json {
            ignoreUnknownKeys = true
        }
        @JvmStatic
        fun fromJson(json: String): TestVectorBip39List {
            return jsonBuilder.decodeFromString(json)
        }
    }
}
