package piuk.blockchain.android.ui.reset.password

import com.blockchain.featureflag.FeatureFlag
import com.blockchain.metadata.MetadataEntry
import com.blockchain.metadata.MetadataRepository
import com.blockchain.metadata.MetadataService
import com.blockchain.metadata.save
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.nabu.metadata.BlockchainAccountCredentialsMetadata
import com.blockchain.nabu.metadata.NabuLegacyCredentialsMetadata
import io.reactivex.rxjava3.core.Completable
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.SessionPrefs
import piuk.blockchain.androidcore.utils.extensions.then

class ResetPasswordInteractor(
    private val authDataManager: AuthDataManager,
    private val payloadDataManager: PayloadDataManager,
    private val prefs: SessionPrefs,
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
        accountMetadataFF.enabled.flatMapCompletable { enabled ->
            if (enabled) {
                nabuDataManager.recoverBlockchainAccount(userId, recoveryToken)
            } else {
                nabuDataManager.recoverLegacyAccount(userId, recoveryToken)
            }.flatMapCompletable { metadata ->
                metadataService.attemptMetadataSetup().then {
                    when {
                        metadata is NabuLegacyCredentialsMetadata && metadata.isValid() -> {
                            metadataRepository.save(
                                metadata,
                                MetadataEntry.NABU_LEGACY_CREDENTIALS
                            )
                        }
                        metadata is BlockchainAccountCredentialsMetadata && metadata.isValid() -> {
                            metadataRepository.save(
                                metadata,
                                MetadataEntry.BLOCKCHAIN_UNIFIED_CREDENTIALS
                            ).then {
                                metadataRepository.save(
                                    // double-bang is safe here as isValid() checks for nulls
                                    NabuLegacyCredentialsMetadata(
                                        metadata.userId!!,
                                        metadata.lifetimeToken!!
                                    ),
                                    MetadataEntry.NABU_LEGACY_CREDENTIALS
                                )
                            }
                        }
                        else -> Completable.complete()
                    }
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
