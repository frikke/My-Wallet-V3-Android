package com.blockchain.presentation.backup.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.button.SecondaryButton
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.presentation.R
import com.blockchain.presentation.backup.BackUpStatus
import com.blockchain.presentation.backup.BackupPhraseIntent
import com.blockchain.presentation.backup.BackupPhraseViewState
import com.blockchain.presentation.backup.viewmodel.BackupPhraseViewModel
import java.util.Locale

@Composable
fun RecoveryPhrase(viewModel: BackupPhraseViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val viewState: BackupPhraseViewState? by stateFlowLifecycleAware.collectAsState(null)

    viewState?.let { state ->
        RecoveryPhraseScreen(
            backupStatus = state.backUpStatus,
            mnemonic = state.mnemonic,
            showLoading = state.showLoading,
            backOnClick = { viewModel.onIntent(BackupPhraseIntent.GoToPreviousScreen) },
            backUpCloudOnClick = { viewModel.onIntent(BackupPhraseIntent.EnableCloudBackup) },
            backUpManualOnClick = { viewModel.onIntent(BackupPhraseIntent.StartManualBackup) }
        )
    }
}

@Composable
fun RecoveryPhraseScreen(
    backupStatus: BackUpStatus,
    mnemonic: List<String>,
    showLoading: Boolean,
    backOnClick: () -> Unit,
    backUpCloudOnClick: () -> Unit,
    backUpManualOnClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NavigationBar(
            title = stringResource(R.string.backup_phrase_title_secure_wallet),
            onBackButtonClick = backOnClick
        )

        Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.tiny_margin)))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(dimensionResource(id = R.dimen.standard_margin)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            SimpleText(
                text = stringResource(id = R.string.recovery_phrase_title),
                style = ComposeTypographies.Title2,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            if (backupStatus == BackUpStatus.NO_BACKUP) {
                Spacer(modifier = Modifier.size(dimensionResource(R.dimen.standard_margin)))

                BackupStatus(backupStatus)
            }

            Spacer(modifier = Modifier.size(dimensionResource(R.dimen.large_margin)))

            Mnemonic(mnemonic = mnemonic)

            Spacer(modifier = Modifier.size(dimensionResource(R.dimen.large_margin)))

            SimpleText(
                text = stringResource(id = R.string.recovery_phrase_description),
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            Spacer(modifier = Modifier.weight(1F))

            PrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.recovery_phrase_backup_cloud),
                state = if (showLoading) ButtonState.Loading else ButtonState.Enabled,
                onClick = backUpCloudOnClick
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.paddingSmall))

            SecondaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.recovery_phrase_backup_manual),
                state = if (showLoading) ButtonState.Disabled else ButtonState.Enabled,
                onClick = backUpManualOnClick
            )
        }
    }
}

// ///////////////
// PREVIEWS
// ///////////////

private val mnemonic = Locale.getISOCountries().toList().map {
    Locale("", it).isO3Country
}.shuffled().subList(0, 12)

@Preview(name = "Recovery Phrase - no backup", showBackground = true)
@Composable
fun PreviewRecoveryPhraseScreenNoBackup() {
    RecoveryPhraseScreen(
        backupStatus = BackUpStatus.NO_BACKUP,
        mnemonic = mnemonic,
        showLoading = false,
        backOnClick = {},
        backUpCloudOnClick = {},
        backUpManualOnClick = {}
    )
}

@Preview(name = "Recovery Phrase - no backup - loading", showBackground = true)
@Composable
fun PreviewRecoveryPhraseScreenNoBackupLoading() {
    RecoveryPhraseScreen(
        backupStatus = BackUpStatus.NO_BACKUP,
        mnemonic = mnemonic,
        showLoading = true,
        backOnClick = {},
        backUpCloudOnClick = {},
        backUpManualOnClick = {}
    )
}

@Preview(name = "Recovery Phrase - backup", showBackground = true)
@Composable
fun PreviewRecoveryPhraseScreenBackup() {
    RecoveryPhraseScreen(
        backupStatus = BackUpStatus.BACKED_UP,
        mnemonic = mnemonic,
        showLoading = false,
        backOnClick = {},
        backUpCloudOnClick = {},
        backUpManualOnClick = {}
    )
}
