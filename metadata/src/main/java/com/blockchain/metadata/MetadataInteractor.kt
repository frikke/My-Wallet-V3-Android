package com.blockchain.metadata

import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.asSingle
import com.blockchain.internalnotifications.NotificationEvent
import com.blockchain.internalnotifications.NotificationTransmitter
import com.blockchain.metadata.data.MetadataStore
import com.blockchain.metadata.data.MetadataStoreKey
import info.blockchain.wallet.crypto.AESUtil
import info.blockchain.wallet.metadata.Metadata
import info.blockchain.wallet.metadata.MetadataApiService
import info.blockchain.wallet.metadata.data.MetadataBody
import info.blockchain.wallet.util.FormatsUtil
import info.blockchain.wallet.util.MetadataUtil
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import org.json.JSONException
import org.spongycastle.crypto.CryptoException
import org.spongycastle.util.encoders.Base64
import org.spongycastle.util.encoders.Hex
import retrofit2.HttpException

class MetadataInteractor(
    private val metadataApiService: MetadataApiService,
    private val metadataStore: MetadataStore,
    private val notificationTransmitter: NotificationTransmitter
) {
    fun fetchMagic(address: String): Single<ByteArray> =
        metadataApiService.getMetadata(address).map {
            val encryptedPayloadBytes = Base64.decode(it.payload.toByteArray(StandardCharsets.UTF_8))
            if (it.prevMagicHash != null) {
                val prevMagicBytes = Hex.decode(it.prevMagicHash)
                MetadataUtil.magic(encryptedPayloadBytes, prevMagicBytes)
            } else {
                MetadataUtil.magic(encryptedPayloadBytes, null)
            }
        }

    fun putMetadata(payloadJson: String, metadata: Metadata): Completable {
        if (!FormatsUtil.isValidJson(payloadJson)) {
            return Completable.error(JSONException("Payload is not a valid json object."))
        }

        val encryptedPayloadBytes: ByteArray =
            Base64.decode(AESUtil.encryptWithKey(metadata.encryptionKey, payloadJson))
        return fetchMagic(metadata.address)
            .onErrorReturn { ByteArray(0) }
            .flatMapCompletable { m ->
                val magic = if (m.isEmpty()) null else m
                val message = MetadataUtil.message(encryptedPayloadBytes, magic)
                val sig = metadata.node.signMessage(String(Base64.encode(message)))
                val body = MetadataBody(
                    version = METADATA_VERSION,
                    payload = String(Base64.encode(encryptedPayloadBytes)),
                    signature = sig,
                    prevMagicHash = magic?.let {
                        Hex.toHexString(it)
                    },
                    typeId = metadata.type
                )
                metadataApiService.putMetadata(metadata.address, body).doOnComplete {
                    notificationTransmitter.postEvent(NotificationEvent.MetadataUpdated)
                }
            }.retryWhen { errors ->
                errors.zipWith(
                    Flowable.range(0, FETCH_MAGIC_HASH_ATTEMPT_LIMIT)
                )
                    .flatMap { (error, attempt) ->
                        if (error is HttpException && error.code() == 404 && attempt < FETCH_MAGIC_HASH_ATTEMPT_LIMIT) {
                            Flowable.timer(1, TimeUnit.SECONDS)
                        } else {
                            Flowable.error(error)
                        }
                    }
            }
    }

    fun loadRemoteMetadata(metadata: Metadata): Maybe<String> {
        return metadataStore.stream(
            FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
                .withKey(MetadataStoreKey(metadata.address, metadata.type))
        )
            .asSingle()
            .toMaybe()
            .map {
                decryptMetadata(metadata, it.payload)
            }.onErrorResumeNext {
                if (it is HttpException && it.code() == 404) { // haven't been created{
                    Maybe.empty()
                } else Maybe.error(it)
            }
    }

    private fun decryptMetadata(metadata: Metadata, payload: String): String =
        try {
            AESUtil.decryptWithKey(metadata.encryptionKey, payload).apply {
                if (!FormatsUtil.isValidJson(this)) {
                    throw CryptoException("Malformed plaintext")
                }
            }
        } catch (e: CryptoException) {
            metadata.unpaddedEncryptionKey?.let {
                AESUtil.decryptWithKey(it, payload)
            } ?: throw e
        }

    companion object {
        const val METADATA_VERSION = 1
        const val FETCH_MAGIC_HASH_ATTEMPT_LIMIT = 1
    }
}
