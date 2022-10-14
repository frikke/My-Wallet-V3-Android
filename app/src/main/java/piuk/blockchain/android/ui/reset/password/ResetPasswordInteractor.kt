package piuk.blockchain.android.ui.reset.password

import com.blockchain.core.auth.AuthDataManager
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.metadata.MetadataEntry
import com.blockchain.metadata.MetadataRepository
import com.blockchain.metadata.MetadataService
import com.blockchain.metadata.save
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.nabu.metadata.BlockchainAccountCredentialsMetadata
import com.blockchain.nabu.metadata.NabuLegacyCredentialsMetadata
import com.blockchain.preferences.AuthPrefs
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.utils.then
import io.reactivex.rxjava3.core.Completable

class ResetPasswordInteractor(
    private val authDataManager: AuthDataManager,
    private val payloadDataManager: PayloadDataManager,
    private val authPrefs: AuthPrefs,
    private val nabuDataManager: NabuDataManager,
    private val metadataService: MetadataService,
    private val metadataRepository: MetadataRepository,
    private val walletStatusPrefs: WalletStatusPrefs
) {

    fun createWalletForAccount(email: String, password: String, walletName: String): Completable {
        return payloadDataManager.createHdWallet(password, walletName, email, null)
            .doOnSuccess { wallet ->
                authPrefs.apply {
                    walletGuid = wallet.guid
                    sharedKey = wallet.sharedKey
                }

                walletStatusPrefs.apply {
                    isNewlyCreated = true
                    this.email = email
                }
            }.ignoreElement()
    }

    fun recoverAccount(userId: String, recoveryToken: String): Completable =
        nabuDataManager.recoverBlockchainAccount(userId, recoveryToken).flatMapCompletable { metadata ->
            metadataService.attemptMetadataSetup().then {
                when {
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

    fun setNewPassword(password: String): Completable {
        walletStatusPrefs.isRestored = true
        return authDataManager.verifyCloudBackup()
            .then {
                payloadDataManager.updatePassword(password)
            }
    }

    fun resetUserKyc(): Completable = nabuDataManager.resetUserKyc()
}
