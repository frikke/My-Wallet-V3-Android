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
    val address: String,

    @SerialName("priv")
    val privateKey: String? = null,

    @SerialName("label")
    private val labelField: String? = null,

    @SerialName("created_time")
    val createdTime: Long? = null,

    @SerialName("tag")
    private val _tag: Int? = null,

    @SerialName("created_device_name")
    val createdDeviceName: String? = null,

    @SerialName("created_device_version")
    val createdDeviceVersion: String? = null
) : JsonSerializableAccount {

    val tag: Int
        get() = _tag ?: NORMAL_ADDRESS

    override val label: String
        get() = labelField ?: ""

    fun isPrivateKeyEncrypted(): Boolean {
        return try {
            Base58.decode(privateKey)
            false
        } catch (e: Exception) {
            true
        }
    }

    fun withUpdatePrivateKey(privateKey: String): ImportedAddress =
        this.copy(
            privateKey = privateKey
        )

    fun xpubs(): XPubs {
        return XPubs(XPub(address, XPub.Format.LEGACY))
    }

    override fun updateArchivedState(isArchived: Boolean): ImportedAddress =
        this.copy(
            _tag = if (isArchived) {
                ARCHIVED_ADDRESS
            } else {
                NORMAL_ADDRESS
            }
        )

    override val isArchived: Boolean
        get() = tag == ARCHIVED_ADDRESS

    companion object {
        const val NORMAL_ADDRESS = 0
        private const val ARCHIVED_ADDRESS = 2

        fun fromECKey(ecKey: ECKey, device: String?, appVersion: String?): ImportedAddress {
            return ImportedAddress(
                labelField = "",
                address = LegacyAddress.fromKey(
                    MainNetParams.get(),
                    ecKey
                ).toBase58(),
                createdDeviceName = device,
                privateKey = Base58.encode(ecKey.privKeyBytes),
                createdTime = System.currentTimeMillis(),
                createdDeviceVersion = appVersion,
                _tag = NORMAL_ADDRESS
            )
        }
    }
}
