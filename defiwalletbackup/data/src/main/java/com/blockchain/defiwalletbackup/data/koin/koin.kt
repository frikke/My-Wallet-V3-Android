package com.blockchain.defiwalletbackup.data.koin

import com.blockchain.defiwalletbackup.data.repository.BackupPhraseRepository
import com.blockchain.defiwalletbackup.domain.service.BackupPhraseService
import com.blockchain.koin.payloadScopeQualifier
import org.koin.dsl.module

val backupPhraseDataModule = module {
    scope(payloadScopeQualifier) {
        factory<BackupPhraseService> {
            BackupPhraseRepository(
                walletPayloadService = get(),
                backupWallet = get(),
                walletStatusPrefs = get(),
                payloadManager  = get()
            )
        }
    }
}
