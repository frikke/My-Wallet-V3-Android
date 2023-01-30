package com.blockchain.presentation.backup.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.navigation.ModeBackgroundColor
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.theme.TinyVerticalSpacer
import com.blockchain.presentation.R
import com.blockchain.presentation.backup.BackupPhraseIntent
import com.blockchain.presentation.backup.BackupPhraseViewState
import com.blockchain.presentation.backup.CopyState
import com.blockchain.presentation.backup.TOTAL_STEP_COUNT
import com.blockchain.presentation.backup.viewmodel.BackupPhraseViewModel
import com.blockchain.walletmode.WalletMode
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
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NavigationBar(
            modeColor = ModeBackgroundColor.Override(WalletMode.NON_CUSTODIAL),
            title = stringResource(R.string.backup_phrase_title_steps, STEP_INDEX, TOTAL_STEP_COUNT),
            mutedBackground = false,
            onBackButtonClick = backOnClick
        )

        TinyVerticalSpacer()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(dimensionResource(id = R.dimen.standard_spacing)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            SimpleText(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.manual_backup_title),
                style = ComposeTypographies.Title2,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            SmallVerticalSpacer()

            SimpleText(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.manual_backup_description),
                style = ComposeTypographies.Body1,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            SmallVerticalSpacer()

            Mnemonic(mnemonic = mnemonic)

            SmallVerticalSpacer()

            CopyMnemonicCta(
                copyState = copyState,
                mnemonic = mnemonic,
                mnemonicCopied = mnemonicCopied
            )

            SmallVerticalSpacer()

            SimpleText(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.manual_backup_copy_description),
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            Spacer(modifier = Modifier.weight(1F))

            PrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.common_next),
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
