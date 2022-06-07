package com.blockchain.defiwalletbackup.data.repository

import com.blockchain.defiwalletbackup.domain.errors.BackupPhraseError
import com.blockchain.defiwalletbackup.domain.service.BackupPhraseService
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.doOnSuccess
import com.blockchain.outcome.mapError
import com.blockchain.preferences.WalletStatus
import piuk.blockchain.androidcore.data.payload.BackupWalletUtil
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.extensions.awaitOutcome
import timber.log.Timber

class BackupPhraseRepository(
    private val payloadManager: PayloadDataManager,
    private val backupWalletUtil: BackupWalletUtil,
    private val walletStatus: WalletStatus
) : BackupPhraseService {

    override fun isBackedUp() = payloadManager.isBackedUp

    override fun getMnemonic(secondPassword: String?): Outcome<BackupPhraseError, List<String>> {
        return backupWalletUtil.getMnemonic(secondPassword)?.let {
            Outcome.Success(it)
        } ?: kotlin.run {
            Outcome.Failure(BackupPhraseError.NoMnemonicFound)
        }
    }

    override suspend fun confirmRecoveryPhraseBackedUp(): Outcome<BackupPhraseError, Unit> {
        return payloadManager.syncPayloadWithServer().awaitOutcome()
            .doOnSuccess {
                payloadManager.wallet?.walletBody?.mnemonicVerified = true
                walletStatus.lastBackupTime = System.currentTimeMillis() / 1000
            }
            .mapError {
                Timber.e(it)
                BackupPhraseError.BackupConfirmationError
            }
    }
}
