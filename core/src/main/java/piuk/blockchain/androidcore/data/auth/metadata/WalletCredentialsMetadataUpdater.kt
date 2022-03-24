package piuk.blockchain.androidcore.data.auth.metadata

import com.blockchain.metadata.MetadataRepository
import io.reactivex.rxjava3.core.Completable
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

@OptIn(InternalSerializationApi::class)
class WalletCredentialsMetadataUpdater(
    private val metadataRepository: MetadataRepository,
    private val payloadDataManager: PayloadDataManager
) {
    private fun updateMetadata(guid: String, password: String, sharedKey: String) =
        metadataRepository.saveMetadata(
            WalletCredentialsMetadata(guid, password, sharedKey),
            WalletCredentialsMetadata::class.java,
            WalletCredentialsMetadata::class.serializer(),
            WalletCredentialsMetadata.WALLET_CREDENTIALS_METADATA_NODE
        )

    fun checkAndUpdate(): Completable {
        val guid = payloadDataManager.guid
        val password = payloadDataManager.tempPassword ?: ""
        val sharedKey = payloadDataManager.sharedKey

        return metadataRepository.loadMetadata(
            WalletCredentialsMetadata.WALLET_CREDENTIALS_METADATA_NODE,
            WalletCredentialsMetadata::class.serializer(),
            WalletCredentialsMetadata::class.java
        ).filter {
            it.guid == guid && it.password == password && it.sharedKey == sharedKey
        }.isEmpty
            .flatMapCompletable {
                if (it) updateMetadata(guid, password, sharedKey) else Completable.complete()
            }
    }
}
