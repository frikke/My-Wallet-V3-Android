package com.blockchain.presentation.backup.screens

import androidx.compose.foundation.background
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
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.button.SecondaryButton
import com.blockchain.componentlib.navigation.ModeBackgroundColor
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.LargeVerticalSpacer
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.theme.StandardVerticalSpacer
import com.blockchain.componentlib.theme.TinyVerticalSpacer
import com.blockchain.presentation.backup.BackUpStatus
import com.blockchain.presentation.backup.BackupPhraseIntent
import com.blockchain.presentation.backup.BackupPhraseViewState
import com.blockchain.presentation.backup.CopyState
import com.blockchain.presentation.backup.viewmodel.BackupPhraseViewModel
import com.blockchain.walletmode.WalletMode
import java.util.Locale

@Composable
fun CloudBackupConfirmation(viewModel: BackupPhraseViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val viewState: BackupPhraseViewState? by stateFlowLifecycleAware.collectAsState(null)

    viewState?.let { state ->
        CloudBackupConfirmationScreen(
            mnemonic = state.mnemonic,
            copyState = state.copyState,

            mnemonicCopied = { viewModel.onIntent(BackupPhraseIntent.MnemonicCopied) },
            doneOnClick = { viewModel.onIntent(BackupPhraseIntent.EndFlow(isSuccessful = true)) },
            backUpManualOnClick = { viewModel.onIntent(BackupPhraseIntent.StartManualBackup) }
        )
    }
}

@Composable
fun CloudBackupConfirmationScreen(
    mnemonic: List<String>,
    copyState: CopyState,

    mnemonicCopied: () -> Unit,
    doneOnClick: () -> Unit,
    backUpManualOnClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.backgroundSecondary),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NavigationBar(
            modeColor = ModeBackgroundColor.Override(WalletMode.NON_CUSTODIAL),
            mutedBackground = false,
            title = stringResource(com.blockchain.stringResources.R.string.backup_phrase_title_secure_wallet),
            onBackButtonClick = null
        )

        TinyVerticalSpacer()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppTheme.dimensions.standardSpacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SimpleText(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = com.blockchain.stringResources.R.string.recovery_phrase_title),
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            StandardVerticalSpacer()

            BackupStatus(BackUpStatus.BACKED_UP)

            LargeVerticalSpacer()

            HidableMnemonic(mnemonic = mnemonic)

            SmallVerticalSpacer()

            CopyMnemonicCta(
                copyState = copyState,
                mnemonic = mnemonic,
                mnemonicCopied = mnemonicCopied
            )

            Spacer(modifier = Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing)))

            SimpleText(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = com.blockchain.stringResources.R.string.recovery_phrase_description),
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            Spacer(modifier = Modifier.weight(1F))

            PrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = com.blockchain.stringResources.R.string.done),
                onClick = doneOnClick
            )

            SmallVerticalSpacer()

            SecondaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = com.blockchain.stringResources.R.string.recovery_phrase_backup_manual),
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

@Preview(name = "Cloud Backup Confirmation", backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun PreviewCloudBackupConfirmationScreen() {
    CloudBackupConfirmationScreen(
        mnemonic = mnemonic,
        copyState = CopyState.Idle(false),
        mnemonicCopied = {},
        doneOnClick = {},
        backUpManualOnClick = {}
    )
}
