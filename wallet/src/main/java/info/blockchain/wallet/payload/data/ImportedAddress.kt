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
    @SerialName("addr")
    var address: String = "",

    @SerialName("priv")
    var privateKey: String? = null,

    @SerialName("label")
    private var labelField: String? = null,

    @SerialName("created_time")
    val createdTime: Long? = null,

    @SerialName("tag")
    var tag: Int = 0,

    @SerialName("created_device_name")
    val createdDeviceName: String? = null,

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
