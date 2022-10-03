package com.blockchain.presentation.backup

import com.blockchain.commonarch.presentation.mvi_v2.Intent

sealed interface BackupPhraseIntent : Intent<BackupPhraseModelState> {
    // Trigger email
    object TriggerEmailAlert : BackupPhraseIntent

    // Backed up
    object StartBackup : BackupPhraseIntent

    // intro
    object LoadData : BackupPhraseIntent
    object GoToSkipBackup : BackupPhraseIntent
    object StartBackupProcess : BackupPhraseIntent

    // skip backup
    object SkipBackup : BackupPhraseIntent

    // recover phrase
    object EnableCloudBackup : BackupPhraseIntent
    object StartManualBackup : BackupPhraseIntent

    // manual backup
    object MnemonicCopied : BackupPhraseIntent
    object ResetCopy : BackupPhraseIntent
    object ClipboardReset : BackupPhraseIntent
    object StartUserPhraseVerification : BackupPhraseIntent

    // verify phrase
    data class VerifyPhrase(val userMnemonic: List<String>) : BackupPhraseIntent
    object ResetVerificationStatus : BackupPhraseIntent {
        override fun isValidFor(modelState: BackupPhraseModelState) =
            modelState.mnemonicVerificationStatus == UserMnemonicVerificationStatus.INCORRECT
    }

    // flow
    object GoToPreviousScreen : BackupPhraseIntent
    data class EndFlow(val isSuccessful: Boolean) : BackupPhraseIntent
}
