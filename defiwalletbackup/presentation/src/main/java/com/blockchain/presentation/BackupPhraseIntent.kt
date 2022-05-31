package com.blockchain.presentation

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface BackupPhraseIntent : Intent<BackupPhraseModelState> {
    // intro
    object GetBackupStatus : BackupPhraseIntent
    object LoadRecoveryPhrase : BackupPhraseIntent
    object StartBackupProcess : BackupPhraseIntent

    // recover phrase
    object StartManualBackup : BackupPhraseIntent

    // manual backup
    object MnemonicCopied : BackupPhraseIntent
    object ResetCopy : BackupPhraseIntent
    object StartUserPhraseVerification : BackupPhraseIntent

    // verify phrase
    data class VerifyPhrase(val userMnemonic: List<String>) : BackupPhraseIntent
}
