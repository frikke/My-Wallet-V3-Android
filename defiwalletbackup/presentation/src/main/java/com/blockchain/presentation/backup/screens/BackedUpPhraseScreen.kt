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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.MinimalPrimaryButton
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
fun BackedUpPhrase(viewModel: BackupPhraseViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val viewState: BackupPhraseViewState? by stateFlowLifecycleAware.collectAsState(null)

    viewState?.let { state ->
        BackedUpPhraseScreen(
            mnemonic = state.mnemonic,
            copyState = state.copyState,

            mnemonicCopied = { viewModel.onIntent(BackupPhraseIntent.MnemonicCopied) },
            nextOnClick = { viewModel.onIntent(BackupPhraseIntent.StartBackup) }
        )
    }
}

@Composable
fun BackedUpPhraseScreen(
    mnemonic: List<String>,
    copyState: CopyState,

    mnemonicCopied: () -> Unit,
    nextOnClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.backgroundSecondary),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NavigationBar(
            modeColor = ModeBackgroundColor.Override(WalletMode.NON_CUSTODIAL),
            title = stringResource(com.blockchain.stringResources.R.string.backup_phrase_title_secure_wallet),
            mutedBackground = false,
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
                style = ComposeTypographies.Title2,
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

            SmallVerticalSpacer()

            SimpleText(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = com.blockchain.stringResources.R.string.recovery_phrase_description),
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            Spacer(modifier = Modifier.weight(1F))

            MinimalPrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = com.blockchain.stringResources.R.string.recovery_phrase_backup_again),
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

@Preview(name = "Backed Up Phrase", backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun PreviewBackedUpPhraseScreen() {
    BackedUpPhraseScreen(
        mnemonic = mnemonic,
        copyState = CopyState.Idle(resetClipboard = false),
        mnemonicCopied = {},
        nextOnClick = {}
    )
}
