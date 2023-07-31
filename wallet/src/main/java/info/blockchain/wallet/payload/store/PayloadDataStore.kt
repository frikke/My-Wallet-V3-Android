package info.blockchain.wallet.payload.store

import com.blockchain.internalnotifications.NotificationEvent
import com.blockchain.store.CacheConfiguration
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import info.blockchain.wallet.api.WalletApi
import info.blockchain.wallet.payload.data.walletdto.WalletBaseDto
import java.security.MessageDigest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

class PayloadDataStore internal constructor(private val walletApi: WalletApi) :
    KeyedStore<WalletPayloadCredentials, WalletBaseDto> by PersistedJsonSqlDelightStoreBuilder().buildKeyed(
        reset = CacheConfiguration.on(listOf(NotificationEvent.PayloadUpdated, NotificationEvent.Logout)),
        storeId = "PayloadStorage",
        fetcher = Fetcher.Keyed.ofSingle { key ->
            walletApi.fetchWalletData(
                key.guid,
                key.sharedKey,
                key.sessionId
            )
        },
        dataSerializer = WalletBaseDto.serializer(),
        mediator = FreshnessMediator(Freshness.ofHours(48)),
        keySerializer = WalletPayloadCredentialsSerializer
    )

@Serializable
data class WalletPayloadCredentials(
    val guid: String,
    val sharedKey: String,
    val sessionId: String,
)

object WalletPayloadCredentialsSerializer : KSerializer<WalletPayloadCredentials> {
    override val descriptor: SerialDescriptor =
        WalletPayloadCredentials.serializer().descriptor

    override fun deserialize(decoder: Decoder): WalletPayloadCredentials {
        return decoder.decodeStructure(descriptor) {
            WalletPayloadCredentials(
                guid = decodeStringElement(descriptor, 0).decrypt(),
                sharedKey = decodeStringElement(descriptor, 1).decrypt(),
                sessionId = decodeStringElement(descriptor, 2).decrypt()
            )
        }
    }

    override fun serialize(encoder: Encoder, value: WalletPayloadCredentials) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.guid.encrypt())
            encodeStringElement(descriptor, 1, value.sharedKey.encrypt())
            encodeStringElement(descriptor, 2, value.sessionId.encrypt())
        }
    }

    private fun String.encrypt(): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun String.decrypt(): String {
        throw UnsupportedOperationException("Decryption is not supported.")
    }
}
