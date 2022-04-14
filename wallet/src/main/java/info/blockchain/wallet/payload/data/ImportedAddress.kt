package info.blockchain.wallet.payload.data

import com.blockchain.serialization.JsonSerializableAccount
import java.lang.Exception
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bitcoinj.core.Base58
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.LegacyAddress
import org.bitcoinj.params.MainNetParams

@Serializable
data class ImportedAddress(
    @field:com.squareup.moshi.Json(name = "addr")
    @SerialName("addr")
    var address: String = "",

    @field:com.squareup.moshi.Json(name = "priv")
    @SerialName("priv")
    var privateKey: String? = null,

    @field:com.squareup.moshi.Json(name = "label")
    @SerialName("label")
    private var labelField: String? = null,

    @field:com.squareup.moshi.Json(name = "created_time")
    @SerialName("created_time")
    val createdTime: Long? = null,

    @field:com.squareup.moshi.Json(name = "tag")
    @SerialName("tag")
    var tag: Int = 0,

    @field:com.squareup.moshi.Json(name = "created_device_name")
    @SerialName("created_device_name")
    val createdDeviceName: String? = null,

    @field:com.squareup.moshi.Json(name = "created_device_version")
    @SerialName("created_device_version")
    val createdDeviceVersion: String? = null,
) : JsonSerializableAccount {

    @kotlinx.serialization.Transient
    @Transient
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

    fun xpubs(): XPubs {
        return XPubs(XPub(address, XPub.Format.LEGACY))
    }

    companion object {
        const val NORMAL_ADDRESS = 0
        const val ARCHIVED_ADDRESS = 2

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
