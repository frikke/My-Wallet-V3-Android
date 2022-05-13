package piuk.blockchain.android.ui.reset.password

import com.blockchain.featureflag.FeatureFlag
import com.blockchain.metadata.MetadataEntry
import com.blockchain.metadata.MetadataRepository
import com.blockchain.metadata.MetadataService
import com.blockchain.metadata.save
import com.blockchain.nabu.datamanagers.NabuDataManager
import io.reactivex.rxjava3.core.Completable
import kotlinx.serialization.InternalSerializationApi
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.extensions.then

@OptIn(InternalSerializationApi::class)
class ResetPasswordInteractor(
    private val authDataManager: AuthDataManager,
    private val payloadDataManager: PayloadDataManager,
    private val prefs: PersistentPrefs,
    private val nabuDataManager: NabuDataManager,
    private val metadataService: MetadataService,
    private val metadataRepository: MetadataRepository,
    private val accountMetadataFF: FeatureFlag
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
        accountMetadataFF.enabled.flatMapCompletable {
            nabuDataManager.recoverLegacyAccount(userId, recoveryToken).flatMapCompletable { nabuMetadata ->
                metadataService.attemptMetadataSetup()
                    .then {
                        metadataRepository.save(
                            nabuMetadata,
                            MetadataEntry.NABU_LEGACY_CREDENTIALS
                        )
                    }
            }.then {
                if (it) {
                    nabuDataManager.recoverBlockchainAccount(userId, recoveryToken).flatMapCompletable { nabuMetadata ->
                        metadataService.attemptMetadataSetup()
                            .then {
                                metadataRepository.save(
                                    nabuMetadata,
                                    MetadataEntry.BLOCKCHAIN_UNIFIED_CREDENTIALS
                                )
                            }
                    }
                } else {
                    Completable.complete()
                }
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
