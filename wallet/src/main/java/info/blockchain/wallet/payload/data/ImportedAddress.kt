package info.blockchain.wallet.payload.data

import com.blockchain.serialization.JsonSerializableAccount
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException
import java.lang.Exception
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bitcoinj.core.Base58
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.LegacyAddress
import org.bitcoinj.params.MainNetParams

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
@Serializable
data class ImportedAddress(
    @field:com.squareup.moshi.Json(name = "addr")
    @field:JsonProperty("addr")
    @SerialName("addr")
    var address: String = "",

    @field:com.squareup.moshi.Json(name = "priv")
    @field:JsonProperty("priv")
    @SerialName("priv")
    var privateKey: String? = null,

    @field:com.squareup.moshi.Json(name = "label")
    @field:JsonProperty("label")
    @SerialName("label")
    private var labelField: String? = null,

    @field:com.squareup.moshi.Json(name = "created_time")
    @field:JsonProperty("created_time")
    @SerialName("created_time")
    val createdTime: Long? = null,

    @field:com.squareup.moshi.Json(name = "tag")
    @field:JsonProperty("tag")
    @SerialName("tag")
    var tag: Int = 0,

    @field:com.squareup.moshi.Json(name = "created_device_name")
    @field:JsonProperty("created_device_name")
    @SerialName("created_device_name")
    val createdDeviceName: String? = null,

    @field:com.squareup.moshi.Json(name = "created_device_version")
    @field:JsonProperty("created_device_version")
    @SerialName("created_device_version")
    val createdDeviceVersion: String? = null,
) : JsonSerializableAccount {

    @kotlinx.serialization.Transient
    @Transient
    @JsonIgnore
    override var label: String = labelField ?: ""

    fun isPrivateKeyEncrypted(): Boolean {
        return try {
            Base58.decode(privateKey)
            false
        } catch (e: Exception) {
            true
        }
    }

    fun setPrivateKeyFromBytes(privKeyBytes: ByteArray?) {
        privateKey = Base58.encode(privKeyBytes)
    }

    fun toJsonString(): String {
        return ObjectMapper().writeValueAsString(this)
    }

    fun xpubs(): XPubs {
        return XPubs(XPub(address!!, XPub.Format.LEGACY))
    }

    companion object {
        const val NORMAL_ADDRESS = 0
        const val ARCHIVED_ADDRESS = 2

        @JvmStatic
        @Throws(IOException::class)
        fun fromJson(json: String): ImportedAddress {
            return ObjectMapper().readValue(json, ImportedAddress::class.java)
        }

        fun fromECKey(ecKey: ECKey, device: String?, appVersion: String?): ImportedAddress {
            return ImportedAddress(
                labelField = "",
                address = LegacyAddress.fromKey(
                    MainNetParams.get(),
                    ecKey
                ).toBase58(),
                createdDeviceName = device,
                createdTime = System.currentTimeMillis(),
                createdDeviceVersion = appVersion
            ).apply {
                setPrivateKeyFromBytes(ecKey.privKeyBytes)
            }
        }
    }
}
