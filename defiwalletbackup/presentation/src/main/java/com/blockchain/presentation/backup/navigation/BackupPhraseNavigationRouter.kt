package com.blockchain.presentation.backup.navigation

import androidx.navigation.NavHostController
import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationRouter
import com.blockchain.extensions.exhaustive

class BackupPhraseNavigationRouter(
    override val navController: NavHostController
) : ComposeNavigationRouter<BackupPhraseNavigationEvent> {

    @Suppress("IMPLICIT_CAST_TO_ANY")
    override fun route(navigationEvent: BackupPhraseNavigationEvent) {
        when (navigationEvent) {
            BackupPhraseNavigationEvent.BackedUp -> {
                navController.navigate(BackPhraseDestination.BackedUpPhrase.route)
            }

            BackupPhraseNavigationEvent.BackupPhraseIntro -> {
                navController.navigate(BackPhraseDestination.BackupPhraseIntro.route) {
                    popUpTo(BackPhraseDestination.BackedUpPhrase.route) {
                        inclusive = true
                    }
                }
            }

            BackupPhraseNavigationEvent.SkipBackup -> {
                navController.navigate(BackPhraseDestination.SkipBackup.route)
            }

            BackupPhraseNavigationEvent.RecoveryPhrase -> {
                navController.navigate(BackPhraseDestination.RecoveryPhrase.route)
            }

            BackupPhraseNavigationEvent.CloudBackupConfirmation -> {
                navController.navigate(BackPhraseDestination.CloudBackupConfirmation.route) {
                    popUpTo(BackPhraseDestination.BackupPhraseIntro.route) {
                        inclusive = true
                    }
                }
            }

            BackupPhraseNavigationEvent.ManualBackup -> {
                navController.navigate(BackPhraseDestination.ManualBackup.route)
            }

            BackupPhraseNavigationEvent.VerifyPhrase -> {
                navController.navigate(BackPhraseDestination.VerifyPhrase.route)
            }

            BackupPhraseNavigationEvent.BackupConfirmation -> {
                navController.navigate(BackPhraseDestination.BackupSuccess.route) {
                    popUpTo(BackPhraseDestination.BackupPhraseIntro.route) {
                        inclusive = true
                    }
                }
            }

            BackupPhraseNavigationEvent.GoToPreviousScreen -> {
                navController.popBackStack()
            }
        }.exhaustive
    }
}
