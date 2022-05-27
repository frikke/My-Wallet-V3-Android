package com.blockchain.presentation.screens

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
import androidx.navigation.NavController
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.presentation.BackPhraseDestination
import com.blockchain.presentation.BackUpPhraseWarning
import com.blockchain.presentation.DefaultPhraseViewState
import com.blockchain.presentation.R
import com.blockchain.presentation.viewmodel.DefaultPhraseViewModel
import java.util.Locale

@Composable
fun DefaultPhrase(
    viewModel: DefaultPhraseViewModel,
    navController: NavController
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val state: DefaultPhraseViewState? by stateFlowLifecycleAware.collectAsState(null)

    DefaultPhraseScreen(
        warning = state?.warning ?: BackUpPhraseWarning.NONE,
        mnemonic = state?.mnemonic ?: listOf(),
        backUpNowOnClick = { navController.navigate(BackPhraseDestination.ManualBackup.route) }
    )
}

@Composable
fun DefaultPhraseScreen(
    warning: BackUpPhraseWarning,
    mnemonic: List<String>,
    backUpNowOnClick: () -> Unit
) {
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
                text = stringResource(id = R.string.recovery_phrase_title),
                style = ComposeTypographies.Title2,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            if (warning == BackUpPhraseWarning.NO_BACKUP) {
                Spacer(modifier = Modifier.size(dimensionResource(R.dimen.standard_margin)))

                BackupStatus()
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
                text = stringResource(id = R.string.back_up_now),
                onClick = backUpNowOnClick
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

@Preview(name = "Default Phrase - no backup", showBackground = true)
@Composable
fun PreviewDefaultPhraseScreenNoBackup() {
    DefaultPhraseScreen(
        warning = BackUpPhraseWarning.NO_BACKUP,
        mnemonic = mnemonic,
        backUpNowOnClick = {}
    )
}

@Preview(name = "Default Phrase - backup", showBackground = true)
@Composable
fun PreviewDefaultPhraseScreenBackup() {
    DefaultPhraseScreen(
        warning = BackUpPhraseWarning.NONE,
        mnemonic = mnemonic,
        backUpNowOnClick = {})
}