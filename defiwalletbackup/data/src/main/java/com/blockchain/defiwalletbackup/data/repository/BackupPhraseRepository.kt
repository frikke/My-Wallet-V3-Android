package com.blockchain.defiwalletbackup.data.repository

import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.defiwalletbackup.domain.errors.BackupPhraseError
import com.blockchain.defiwalletbackup.domain.service.BackupPhraseService
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.doOnSuccess
import com.blockchain.outcome.mapError
import com.blockchain.preferences.WalletStatusPrefs
import com.blockchain.utils.awaitOutcome
import com.blockchain.wallet.BackupWallet
import com.blockchain.walletmode.WalletMode
import info.blockchain.wallet.payload.WalletPayloadService
import timber.log.Timber

class BackupPhraseRepository(
    private val walletPayloadService: WalletPayloadService,
    private val backupWallet: BackupWallet,
    private val walletStatusPrefs: WalletStatusPrefs,
    private val payloadManager: PayloadDataManager
) : BackupPhraseService {

    override fun isBackedUp() = walletPayloadService.isBackedUp

    override fun getMnemonic(secondPassword: String?): Outcome<BackupPhraseError, List<String>> {
        return backupWallet.getMnemonic(secondPassword)?.let {
            Outcome.Success(it)
        } ?: kotlin.run {
            Outcome.Failure(BackupPhraseError.NoMnemonicFound)
        }
    }

    override suspend fun confirmRecoveryPhraseBackedUp(): Outcome<BackupPhraseError, Unit> {
        return walletPayloadService.updateMnemonicVerified(true).awaitOutcome()
            .doOnSuccess {
                walletStatusPrefs.lastBackupTime = System.currentTimeMillis() / 1000
            }
            .mapError {
                Timber.e(it)
                BackupPhraseError.BackupConfirmationError
            }
    }

    override fun shouldBackupPhraseForMode(walletMode: WalletMode): Boolean {
        return when (walletMode) {
            WalletMode.NON_CUSTODIAL -> {
                payloadManager.isBackedUp.not() && walletStatusPrefs.isWalletBackUpSkipped.not()
            }
            else -> {
                false
            }
        }
    }
}
