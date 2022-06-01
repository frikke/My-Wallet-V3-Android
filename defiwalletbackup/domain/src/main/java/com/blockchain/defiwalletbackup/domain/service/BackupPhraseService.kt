package com.blockchain.defiwalletbackup.domain.service

import com.blockchain.defiwalletbackup.domain.errors.BackupPhraseError
import com.blockchain.outcome.Outcome

interface BackupPhraseService {
    fun isBackedUp(): Boolean
    fun getMnemonic(secondPassword: String?): Outcome<BackupPhraseError, List<String>>
    suspend fun confirmRecoveryPhraseBackedUp(): Outcome<BackupPhraseError, Unit>
}
