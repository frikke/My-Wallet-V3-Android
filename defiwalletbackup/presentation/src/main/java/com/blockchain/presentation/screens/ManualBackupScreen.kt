package com.blockchain.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.NavController
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.button.TertiaryButton
import com.blockchain.componentlib.extensions.copyToClipboard
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Green600
import com.blockchain.presentation.BackPhraseDestination
import com.blockchain.presentation.CopyState
import com.blockchain.presentation.DefaultPhraseViewState
import com.blockchain.presentation.R
import com.blockchain.presentation.viewmodel.DefaultPhraseIntent
import com.blockchain.presentation.viewmodel.DefaultPhraseViewModel
import java.util.Locale

@Composable
fun ManualBackup(
    viewModel: DefaultPhraseViewModel,
    navController: NavController
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val state: DefaultPhraseViewState? by stateFlowLifecycleAware.collectAsState(null)

    ManualBackupScreen(
        mnemonic = state?.mnemonic ?: listOf(),
        mnemonicString = state?.mnemonicString ?: "",
        copyState = state?.copyState ?: CopyState.Idle,

        mnemonicCopied = { viewModel.onIntent(DefaultPhraseIntent.MnemonicCopied) },
        nextOnClick = { navController.navigate(BackPhraseDestination.VerifyPhrase.route) }
    )
}

@Composable
fun ManualBackupScreen(
    mnemonic: List<String>,
    mnemonicString: String,
    copyState: CopyState,

    mnemonicCopied: () -> Unit,
    nextOnClick: () -> Unit
) {
    var copyMnemonic by remember { mutableStateOf(false) }

    if (copyMnemonic) {
        CopyMnemonic(mnemonicString)
        mnemonicCopied()
        copyMnemonic = false
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NavigationBar(title = stringResource(id = R.string.secure_defi_wallets), onBackButtonClick = { })

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

            when (copyState) {
                CopyState.Idle -> {
                    TertiaryButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = R.string.common_copy),
                        onClick = { copyMnemonic = true }
                    )
                }

                CopyState.Copied -> {
                    MnemonicCopied()
                }
            }


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

@Composable
fun MnemonicCopied() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(id = R.dimen.very_small_margin)),
        horizontalArrangement = Arrangement.Center
    ) {
        Image(imageResource = ImageResource.Local(R.drawable.ic_check))

        Spacer(modifier = Modifier.size(dimensionResource(R.dimen.tiny_margin)))

        Text(
            text = stringResource(R.string.manual_backup_copied),
            textAlign = TextAlign.Center,
            style = AppTheme.typography.body2,
            color = Green600
        )
    }
}

@Composable
fun CopyMnemonic(mnemonic: String) {
    LocalContext.current.copyToClipboard(
        label = stringResource(id = R.string.manual_backup_title),
        text = mnemonic
    )
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
        mnemonicString = "",
        copyState = CopyState.Idle,

        mnemonicCopied = {},
        nextOnClick = {}
    )
}

@Preview(name = "Manual Backup Copied", backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun PreviewManualBackupScreenCopied() {
    ManualBackupScreen(
        mnemonic = mnemonic,
        mnemonicString = "",
        copyState = CopyState.Copied,

        mnemonicCopied = {},
        nextOnClick = {}
    )
}
