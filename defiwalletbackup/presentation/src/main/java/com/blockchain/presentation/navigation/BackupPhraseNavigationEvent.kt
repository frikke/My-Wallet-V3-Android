package com.blockchain.presentation.navigation

import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent

sealed interface BackupPhraseNavigationEvent : NavigationEvent {
    object GoToPreviousScreen : BackupPhraseNavigationEvent
    object RecoveryPhrase : BackupPhraseNavigationEvent
    object ManualBackup : BackupPhraseNavigationEvent
    object VerifyPhrase : BackupPhraseNavigationEvent
    object BackupConfirmation : BackupPhraseNavigationEvent
}
