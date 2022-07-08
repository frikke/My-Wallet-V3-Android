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
import androidx.compose.runtime.setValue
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
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.presentation.R
import com.blockchain.presentation.backup.BackupPhraseIntent
import com.blockchain.presentation.backup.BackupPhraseViewState
import com.blockchain.presentation.backup.CopyState
import com.blockchain.presentation.backup.TOTAL_STEP_COUNT
import com.blockchain.presentation.backup.viewmodel.BackupPhraseViewModel
import java.util.Locale

private const val STEP_INDEX = 1

@Composable
fun ManualBackup(viewModel: BackupPhraseViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val viewState: BackupPhraseViewState? by stateFlowLifecycleAware.collectAsState(null)

    viewState?.let { state ->
        ManualBackupScreen(
            mnemonic = state.mnemonic,
            copyState = state.copyState,

            backOnClick = { viewModel.onIntent(BackupPhraseIntent.GoToPreviousScreen) },
            mnemonicCopied = { viewModel.onIntent(BackupPhraseIntent.MnemonicCopied) },
            nextOnClick = { viewModel.onIntent(BackupPhraseIntent.StartUserPhraseVerification) }
        )
    }
}

@Composable
fun ManualBackupScreen(
    mnemonic: List<String>,
    copyState: CopyState,

    backOnClick: () -> Unit,
    mnemonicCopied: () -> Unit,
    nextOnClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NavigationBar(
            title = stringResource(R.string.backup_phrase_title_steps, STEP_INDEX, TOTAL_STEP_COUNT),
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
                text = stringResource(id = R.string.manual_backup_title),
                style = ComposeTypographies.Title2,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            Spacer(modifier = Modifier.size(dimensionResource(R.dimen.large_margin)))

            SimpleText(
                text = stringResource(id = R.string.manual_backup_description),
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            Spacer(modifier = Modifier.size(dimensionResource(R.dimen.small_margin)))

            Mnemonic(mnemonic = mnemonic)

            Spacer(modifier = Modifier.size(dimensionResource(R.dimen.small_margin)))

            CopyMnemonicCta(
                copyState = copyState,
                mnemonic = mnemonic,
                mnemonicCopied = mnemonicCopied
            )

            Spacer(modifier = Modifier.size(dimensionResource(R.dimen.small_margin)))

            SimpleText(
                text = stringResource(id = R.string.manual_backup_copy_description),
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            Spacer(modifier = Modifier.weight(1F))

            PrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.next),
                onClick = nextOnClick
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

@Preview(name = "Manual Backup Copy", backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun PreviewManualBackupScreenCopy() {
    ManualBackupScreen(
        mnemonic = mnemonic,
        copyState = CopyState.Idle(false),

        backOnClick = {},
        mnemonicCopied = {},
        nextOnClick = {}
    )
}

@Preview(name = "Manual Backup Copied", backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun PreviewManualBackupScreenCopied() {
    ManualBackupScreen(
        mnemonic = mnemonic,
        copyState = CopyState.Copied,

        backOnClick = {},
        mnemonicCopied = {},
        nextOnClick = {}
    )
}
