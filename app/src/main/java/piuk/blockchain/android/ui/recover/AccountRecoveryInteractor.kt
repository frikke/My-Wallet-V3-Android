package piuk.blockchain.android.ui.recover

import com.blockchain.core.auth.metadata.WalletRecoveryMetadata
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.metadata.MetadataInteractor
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.preferences.AuthPrefs
import com.blockchain.utils.then
import info.blockchain.wallet.metadata.Metadata
import info.blockchain.wallet.metadata.MetadataDerivation
import io.reactivex.rxjava3.core.Completable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class AccountRecoveryInteractor(
    private val payloadDataManager: PayloadDataManager,
    private val authPrefs: AuthPrefs,
    private val metadataInteractor: MetadataInteractor,
    private val metadataDerivation: MetadataDerivation,
    private val nabuDataManager: NabuDataManager
) {

    fun recoverCredentials(seedPhrase: String): Completable {
        val masterKey = payloadDataManager.generateMasterKeyFromSeed(seedPhrase)
        val metadataNode = metadataDerivation.deriveMetadataNode(masterKey)

        return metadataInteractor.loadRemoteMetadata(
            Metadata.newInstance(
                metaDataHDNode = metadataDerivation.deserializeMetadataNode(metadataNode),
                type = WalletRecoveryMetadata.WALLET_CREDENTIALS_METADATA_NODE,
                metadataDerivation = metadataDerivation
            )
        )
            .flatMapCompletable { json ->
                val credentials = Json.decodeFromString<WalletRecoveryMetadata>(json)
                payloadDataManager.initializeAndDecrypt(
                    credentials.sharedKey,
                    credentials.guid,
                    credentials.password
                )
            }
    }

    private fun restoreWallet() = Completable.fromCallable {
        payloadDataManager.wallet.let { wallet ->
            authPrefs.sharedKey = wallet.sharedKey
            authPrefs.walletGuid = wallet.guid
        }
    }

    fun recoverWallet(): Completable {
        return restoreWallet().then {
            nabuDataManager.resetUserKyc()
        }
    }
}
