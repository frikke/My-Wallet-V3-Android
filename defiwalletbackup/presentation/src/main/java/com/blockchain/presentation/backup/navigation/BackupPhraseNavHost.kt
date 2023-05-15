package com.blockchain.presentation.backup.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.rememberNavController
import com.blockchain.commonarch.presentation.mvi_v2.compose.MviFragmentNavHost
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import com.blockchain.presentation.backup.BackupPhraseArgs
import com.blockchain.presentation.backup.screens.BackedUpPhrase
import com.blockchain.presentation.backup.screens.BackupPhraseIntro
import com.blockchain.presentation.backup.screens.BackupSuccess
import com.blockchain.presentation.backup.screens.CloudBackupConfirmation
import com.blockchain.presentation.backup.screens.ManualBackup
import com.blockchain.presentation.backup.screens.RecoveryPhrase
import com.blockchain.presentation.backup.screens.SkipBackup
import com.blockchain.presentation.backup.screens.VerifyPhrase
import com.blockchain.presentation.backup.viewmodel.BackupPhraseViewModel

@Composable
fun BackupPhraseNavHost(
    viewModel: BackupPhraseViewModel,
    backupPhraseArgs: BackupPhraseArgs
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val navEventsFlowLifecycleAware = remember(viewModel.navigationEventFlow, lifecycleOwner) {
        viewModel.navigationEventFlow.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }

    viewModel.viewCreated(backupPhraseArgs)

    MviFragmentNavHost(
        navEvents = navEventsFlowLifecycleAware,
        navigationRouter = BackupPhraseNavigationRouter(
            navController = rememberNavController()
        ),
        startDestination = if (viewModel.isBackedUp()) {
            BackPhraseDestination.BackedUpPhrase
        } else {
            BackPhraseDestination.BackupPhraseIntro
        }
    ) {
        // Backed Up Phrase (initial screen when phrase is already backed up)
        backedUpPhraseDestination(viewModel)

        // Intro
        backupPhraseIntroDestination(viewModel)

        // Skip Backup
        skipBackupDestination(viewModel)

        // Recovery Phrase
        recoveryPhraseDestination(viewModel)

        // Cloud Backup
        cloudBackupConfirmationDestination(viewModel)

        // Manual Backup
        manualBackupDestination(viewModel)

        // Verify Phrase
        verifyPhraseDestination(viewModel)

        // Backup Successful
        backupSuccessDestination(viewModel)
    }
}

private fun NavGraphBuilder.backedUpPhraseDestination(viewModel: BackupPhraseViewModel) {
    composable(navigationEvent = BackPhraseDestination.BackedUpPhrase) {
        BackedUpPhrase(viewModel)
    }
}

private fun NavGraphBuilder.backupPhraseIntroDestination(viewModel: BackupPhraseViewModel) {
    composable(navigationEvent = BackPhraseDestination.BackupPhraseIntro) {
        BackupPhraseIntro(viewModel)
    }
}

private fun NavGraphBuilder.skipBackupDestination(viewModel: BackupPhraseViewModel) {
    composable(navigationEvent = BackPhraseDestination.SkipBackup) {
        SkipBackup(viewModel)
    }
}

private fun NavGraphBuilder.recoveryPhraseDestination(viewModel: BackupPhraseViewModel) {
    composable(navigationEvent = BackPhraseDestination.RecoveryPhrase) {
        RecoveryPhrase(viewModel)
    }
}

private fun NavGraphBuilder.cloudBackupConfirmationDestination(viewModel: BackupPhraseViewModel) {
    composable(navigationEvent = BackPhraseDestination.CloudBackupConfirmation) {
        CloudBackupConfirmation(viewModel)
    }
}

private fun NavGraphBuilder.manualBackupDestination(viewModel: BackupPhraseViewModel) {
    composable(navigationEvent = BackPhraseDestination.ManualBackup) {
        ManualBackup(viewModel)
    }
}

private fun NavGraphBuilder.verifyPhraseDestination(viewModel: BackupPhraseViewModel) {
    composable(navigationEvent = BackPhraseDestination.VerifyPhrase) {
        VerifyPhrase(viewModel)
    }
}

private fun NavGraphBuilder.backupSuccessDestination(viewModel: BackupPhraseViewModel) {
    composable(navigationEvent = BackPhraseDestination.BackupSuccess) {
        BackupSuccess(viewModel)
    }
}
