package piuk.blockchain.androidcore.data.auth.metadata

import com.blockchain.metadata.MetadataEntry
import com.blockchain.metadata.MetadataRepository
import com.blockchain.metadata.load
import com.blockchain.metadata.save
import io.reactivex.rxjava3.core.Completable
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

class WalletCredentialsMetadataUpdater(
    private val metadataRepository: MetadataRepository,
    private val payloadDataManager: PayloadDataManager
) {
    private fun updateMetadata(guid: String, password: String, sharedKey: String) =
        metadataRepository.save(
            WalletCredentialsMetadata(guid, password, sharedKey),
            MetadataEntry.WALLET_CREDENTIALS
        )

    fun checkAndUpdate(): Completable {
        val guid = payloadDataManager.guid
        val password = payloadDataManager.tempPassword ?: ""
        val sharedKey = payloadDataManager.sharedKey

        return metadataRepository.load<WalletCredentialsMetadata>(MetadataEntry.WALLET_CREDENTIALS).filter {
            it.guid == guid && it.password == password && it.sharedKey == sharedKey
        }.isEmpty
            .flatMapCompletable {
                if (it) updateMetadata(guid, password, sharedKey) else Completable.complete()
            }
    }
}
