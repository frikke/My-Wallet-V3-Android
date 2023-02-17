package info.blockchain.wallet.payload.data

import info.blockchain.wallet.crypto.AESUtil
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.exceptions.UnsupportedVersionException
import java.io.IOException
import java.lang.Exception
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import org.json.JSONException

@Serializable
data class WalletWrapper(
    @SerialName("version")
    val version: Int,
    /**
     * for some reason it needs validation and fallback to DEFAULT_PBKDF2_ITERATIONS_V2.
     */
    @Serializable(with = Pbkdf2IterationsSerializer::class)
    @SerialName("pbkdf2_iterations")
    val pbkdf2Iterations: Int,

    @SerialName("payload")
    val payload: String? = null
) {

    fun toJson(): String {
        val jsonBuilder = Json {
            ignoreUnknownKeys = true
        }
        return jsonBuilder.encodeToString(this)
    }

    private fun validateVersion() {
        if (version > SUPPORTED_VERSION) {
            throw UnsupportedVersionException(version.toString() + "")
        }
    }

    fun decryptPayload(password: String?): Wallet {
        validateVersion()
        val decryptedPayload: String = try {
            AESUtil.decrypt(payload, password, pbkdf2Iterations)
        } catch (e: Exception) {
            throw DecryptionException(e)
        } ?: throw DecryptionException("Decryption failed.")

        try {
            return Wallet.fromJson(decryptedPayload, version)
        } catch (e: JSONException) {
            throw DecryptionException(e)
        }
    }

    companion object {
        const val V4 = 4
        const val V3 = 3
        const val SUPPORTED_VERSION = V4
        const val DEFAULT_PBKDF2_ITERATIONS_V2 = 5000

        @JvmStatic
        @Throws(IOException::class)
        fun fromJson(json: String): WalletWrapper {
            val jsonBuilder = Json {
                ignoreUnknownKeys = true
            }
            return jsonBuilder.decodeFromString(json)
        }

        @JvmStatic
        fun wrap(encryptedPayload: String, version: Int, iterations: Int): WalletWrapper {
            return WalletWrapper(version = version, pbkdf2Iterations = iterations, payload = encryptedPayload)
        }
    }
}

private object Pbkdf2IterationsSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor = Int.serializer().descriptor
    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeInt(value)
    }

    override fun deserialize(decoder: Decoder): Int {
        val original = decoder.decodeInt()
        return if (original <= 0) return WalletWrapper.DEFAULT_PBKDF2_ITERATIONS_V2 else original
    }
}
