package com.blockchain.presentation.backup.navigation

import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent

sealed interface BackupPhraseNavigationEvent : NavigationEvent {
    object GoToPreviousScreen : BackupPhraseNavigationEvent
    object BackedUp : BackupPhraseNavigationEvent
    object BackupPhraseIntro : BackupPhraseNavigationEvent
    object RecoveryPhrase : BackupPhraseNavigationEvent
    object CloudBackupConfirmation : BackupPhraseNavigationEvent
    object ManualBackup : BackupPhraseNavigationEvent
    object VerifyPhrase : BackupPhraseNavigationEvent
    object BackupConfirmation : BackupPhraseNavigationEvent
}
