package info.blockchain.wallet.payload.data

import info.blockchain.wallet.crypto.AESUtil
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.exceptions.UnsupportedVersionException
import java.io.IOException
import java.lang.Exception
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.json.JSONException

@Serializable
data class WalletWrapper(
    @SerialName("version")
    var version: Int = 0,

    @SerialName("pbkdf2_iterations")
    var pbkdf2Iterations: Int = 0,

    @SerialName("payload")
    var payload: String? = null
) {

    fun toJson(version: Int): String {
        val jsonBuilder = Json {
            ignoreUnknownKeys = true
            serializersModule = getSerializerForVersion(version)
        }
        return jsonBuilder.encodeToString(this)
    }

    @Throws(UnsupportedVersionException::class)
    private fun validateVersion() {
        if (version > SUPPORTED_VERSION) {
            throw UnsupportedVersionException(version.toString() + "")
        }
    }

    /**
     * Set iterations to default if need
     */
    private fun validatePbkdf2Iterations() {
        if (pbkdf2Iterations <= 0) {
            pbkdf2Iterations = DEFAULT_PBKDF2_ITERATIONS_V2
        }
    }

    @Throws(
        UnsupportedVersionException::class,
        IOException::class,
        DecryptionException::class,
        HDWalletException::class
    ) fun decryptPayload(password: String?): Wallet {
        validateVersion()
        validatePbkdf2Iterations()
        val decryptedPayload: String = try {
            AESUtil.decrypt(payload, password, pbkdf2Iterations)
        } catch (e: Exception) {
            throw DecryptionException(e)
        } ?: throw DecryptionException("Decryption failed.")

        return try {
            val wallet: Wallet = Wallet.fromJson(decryptedPayload, getSerializerForVersion(version))
            wallet.wrapperVersion = version
            wallet
        } catch (e: JSONException) {
            throw DecryptionException("Decryption failed.")
        }
    }

    companion object {
        const val V4 = 4
        const val V3 = 3
        const val SUPPORTED_VERSION = V4
        const val DEFAULT_PBKDF2_ITERATIONS_V2 = 5000

        @JvmStatic
        @Throws(IOException::class)
        fun fromJson(json: String?): WalletWrapper {
            val jsonBuilder = Json {
                ignoreUnknownKeys = true
            }
            return jsonBuilder.decodeFromString(json!!)
        }

        @JvmStatic
        fun wrap(encryptedPayload: String, version: Int, iterations: Int): WalletWrapper {
            val walletWrapperBody = WalletWrapper()
            walletWrapperBody.version = version
            walletWrapperBody.pbkdf2Iterations = iterations
            walletWrapperBody.payload = encryptedPayload
            return walletWrapperBody
        }

        @JvmStatic
        fun getSerializerForVersion(version: Int): SerializersModule {
            return if (version == V4) {
                SerializersModule {
                    polymorphic(Account::class) {
                        subclass(AccountV4::class)
                        defaultDeserializer { AccountV4.serializer() }
                    }
                }
            } else {
                SerializersModule {
                    polymorphic(Account::class) {
                        subclass(AccountV3::class)
                        defaultDeserializer { AccountV3.serializer() }
                    }
                }
            }
        }
    }
}
