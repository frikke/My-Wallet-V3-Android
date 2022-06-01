package com.blockchain.presentation.navigation

import androidx.navigation.NavHostController
import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationRouter
import com.blockchain.extensions.exhaustive
import com.blockchain.presentation.BackPhraseDestination

class BackupPhraseNavigationRouter(
    override val navController: NavHostController
) : ComposeNavigationRouter<BackupPhraseNavigationEvent> {

    @Suppress("IMPLICIT_CAST_TO_ANY")
    override fun route(navigationEvent: BackupPhraseNavigationEvent) {
        when (navigationEvent) {
            BackupPhraseNavigationEvent.RecoveryPhrase -> {
                navController.navigate(BackPhraseDestination.RecoveryPhrase.route)
            }

            BackupPhraseNavigationEvent.ManualBackup -> {
                navController.navigate(BackPhraseDestination.ManualBackup.route)
            }

            BackupPhraseNavigationEvent.VerifyPhrase -> {
                navController.navigate(BackPhraseDestination.VerifyPhrase.route)
            }

            BackupPhraseNavigationEvent.BackupConfirmation -> {
                navController.navigate(BackPhraseDestination.BackupConfirmation.route){
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
