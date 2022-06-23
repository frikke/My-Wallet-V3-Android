package com.blockchain.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.rememberNavController
import com.blockchain.commonarch.presentation.mvi_v2.compose.MviFragmentNavHost
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import com.blockchain.presentation.BackPhraseDestination
import com.blockchain.presentation.BackupPhraseArgs
import com.blockchain.presentation.screens.BackupPhraseIntro
import com.blockchain.presentation.screens.BackupSuccess
import com.blockchain.presentation.screens.ManualBackup
import com.blockchain.presentation.screens.RecoveryPhrase
import com.blockchain.presentation.screens.VerifyPhrase
import com.blockchain.presentation.viewmodel.BackupPhraseViewModel

@Composable
fun BackupPhraseNavHost(
    viewModel: BackupPhraseViewModel,
    backupPhraseArgs: BackupPhraseArgs
) {
    viewModel.viewCreated(backupPhraseArgs)

    val lifecycleOwner = LocalLifecycleOwner.current

    val navEventsFlowLifecycleAware = remember(viewModel.navigationEventFlow, lifecycleOwner) {
        viewModel.navigationEventFlow.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }

    MviFragmentNavHost(
        navEvents = navEventsFlowLifecycleAware,
        navigationRouter = BackupPhraseNavigationRouter(
            navController = rememberNavController()
        ),
        startDestination = BackPhraseDestination.BackupPhraseIntro,
    ) {
        // Intro
        backupPhraseIntroDestination(viewModel)

        // Recovery Phrase
        recoveryPhraseDestination(viewModel)

        manualBackupDestination(viewModel)

        verifyPhraseDestination(viewModel)

        backupSuccessDestination(viewModel)
    }
}

private fun NavGraphBuilder.backupPhraseIntroDestination(viewModel: BackupPhraseViewModel) {
    composable(navigationEvent = BackPhraseDestination.BackupPhraseIntro) {
        BackupPhraseIntro(viewModel)
    }
}

private fun NavGraphBuilder.recoveryPhraseDestination(viewModel: BackupPhraseViewModel) {
    composable(navigationEvent = BackPhraseDestination.RecoveryPhrase) {
        RecoveryPhrase(viewModel)
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
