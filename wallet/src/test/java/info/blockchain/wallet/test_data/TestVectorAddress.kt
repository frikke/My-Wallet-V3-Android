package info.blockchain.wallet.test_data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class TestVectorAddress {
    @SerialName("receiveLegacy")
    val receiveLegacy: String? = null

    @SerialName("changeLegacy")
    val changeLegacy: String? = null

    @SerialName("receiveCashAddress")
    val receiveCashAddress: String? = null

    @SerialName("changeCashAddress")
    val changeCashAddress: String? = null

    fun toJson(): String {
        return Json.encodeToString(this)
    }

    companion object {
        @JvmStatic
        fun fromJson(json: String): TestVectorAddress {
            return Json.decodeFromString(json)
        }
    }
}
