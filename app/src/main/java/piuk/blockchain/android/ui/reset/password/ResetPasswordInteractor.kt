package piuk.blockchain.android.ui.reset.password

import com.blockchain.metadata.MetadataRepository
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.nabu.metadata.NabuCredentialsMetadata
import io.reactivex.rxjava3.core.Completable
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.extensions.then

@OptIn(InternalSerializationApi::class)
class ResetPasswordInteractor(
    private val authDataManager: AuthDataManager,
    private val payloadDataManager: PayloadDataManager,
    private val prefs: PersistentPrefs,
    private val nabuDataManager: NabuDataManager,
    private val metadataManager: MetadataManager,
    private val metadataRepository: MetadataRepository
) {

    fun createWalletForAccount(email: String, password: String, walletName: String): Completable {
        return payloadDataManager.createHdWallet(password, walletName, email)
            .doOnSuccess { wallet ->
                prefs.apply {
                    isNewlyCreated = true
                    walletGuid = wallet.guid
                    sharedKey = wallet.sharedKey
                    this.email = email
                }
            }.ignoreElement()
    }

    fun recoverAccount(userId: String, recoveryToken: String): Completable =
        nabuDataManager.recoverAccount(userId, recoveryToken).flatMapCompletable { nabuMetadata ->
            metadataManager.attemptMetadataSetup()
                .then {
                    metadataRepository.saveMetadata(
                        nabuMetadata,
                        NabuCredentialsMetadata::class.java,
                        NabuCredentialsMetadata::class.serializer(),
                        NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
                    )
                }
        }

    fun setNewPassword(password: String): Completable {
        val fallbackPassword = payloadDataManager.tempPassword
        payloadDataManager.tempPassword = password
        prefs.isRestored = true
        return authDataManager.verifyCloudBackup()
            .then { payloadDataManager.syncPayloadWithServer() }
            .doOnError {
                payloadDataManager.tempPassword = fallbackPassword
            }
    }

    fun resetUserKyc(): Completable = nabuDataManager.resetUserKyc()
}
