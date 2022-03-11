package info.blockchain.wallet.payload.data

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
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

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.ANY,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
@Serializable
data class WalletWrapper(
    @field:JsonProperty("version")
    @SerialName("version")
    var version: Int = 0,

    @field:JsonProperty("pbkdf2_iterations")
    @SerialName("pbkdf2_iterations")
    var pbkdf2Iterations: Int = 0,

    @field:JsonProperty("payload")
    @SerialName("payload")
    var payload: String? = null
) {

    @Throws(JsonProcessingException::class)
    fun toJson(version: Int, withKotlinX: Boolean): String {
        return if (withKotlinX) {
            val jsonBuilder = Json {
                ignoreUnknownKeys = true
                serializersModule = getSerializerForVersion(version)
            }
            jsonBuilder.encodeToString(this)
        } else {
            val mapper: ObjectMapper = WalletWrapper.getMapperForVersion(version)
            mapper.writeValueAsString(this)
        }
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
    ) fun decryptPayload(password: String?, withKotlinX: Boolean): Wallet {
        validateVersion()
        validatePbkdf2Iterations()
        val decryptedPayload: String = try {
            AESUtil.decrypt(payload, password, pbkdf2Iterations)
        } catch (e: Exception) {
            throw DecryptionException(e)
        } ?: throw DecryptionException("Decryption failed.")

        return try {
            val wallet: Wallet = if (withKotlinX) {
                Wallet.fromJson(decryptedPayload, getSerializerForVersion(version))
            } else {
                val mapper: ObjectMapper = getMapperForVersion(version)
                Wallet.fromJson(decryptedPayload, mapper)
            }
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
        fun fromJson(json: String?, withKotlinX: Boolean): WalletWrapper {
            if (withKotlinX) {
                val jsonBuilder = Json {
                    ignoreUnknownKeys = true
                }
                return jsonBuilder.decodeFromString(json!!)
            } else {
                return ObjectMapper().readValue(json, WalletWrapper::class.java)
            }
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
        fun getMapperForVersion(version: Int): ObjectMapper {
            val mapper = ObjectMapper()
            mapper.setVisibility(
                mapper.serializationConfig
                    .defaultVisibilityChecker
                    .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                    .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withCreatorVisibility(JsonAutoDetect.Visibility.NONE)
            )
            val module = KotlinModule()
            if (version == V4) {
                module.addAbstractTypeMapping(
                    Account::class.java, AccountV4::class.java
                )
            } else {
                module.addAbstractTypeMapping(
                    Account::class.java, AccountV3::class.java
                )
            }
            mapper.registerModule(module)
            return mapper
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
