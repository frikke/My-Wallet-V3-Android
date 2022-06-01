package com.blockchain.presentation.navigation

import androidx.navigation.NavHostController
import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationRouter
import com.blockchain.extensions.exhaustive
import com.blockchain.presentation.BackPhraseDestination

class BackupPhraseNavigationRouter(
    override val navController: NavHostController
) : ComposeNavigationRouter<BackupPhraseNavigationEvent> {

    override fun route(navigationEvent: BackupPhraseNavigationEvent) {
        val route = when (navigationEvent) {
            BackupPhraseNavigationEvent.RecoveryPhrase -> {
                BackPhraseDestination.RecoveryPhrase.route
            }

            BackupPhraseNavigationEvent.ManualBackup -> {
                BackPhraseDestination.ManualBackup.route
            }

            BackupPhraseNavigationEvent.VerifyPhrase -> {
                BackPhraseDestination.VerifyPhrase.route
            }

            BackupPhraseNavigationEvent.BackupConfirmation -> {
                BackPhraseDestination.BackupConfirmation.route
            }
        }.exhaustive

        navController.navigate(route)
    }
}
