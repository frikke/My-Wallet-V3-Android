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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import com.blockchain.presentation.BackUpStatus
import com.blockchain.presentation.DefaultPhraseViewState
import com.blockchain.presentation.R
import com.blockchain.presentation.viewmodel.DefaultPhraseViewModel
import java.util.Locale

@Composable
fun BackupConfirmation(
    viewModel: DefaultPhraseViewModel,
    navController: NavController
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val viewState: DefaultPhraseViewState? by stateFlowLifecycleAware.collectAsState(null)

    viewState?.let { state ->
        PhraseConfirmationScreen(
            mnemonic = state.mnemonic,
            nextOnClick = { /*todo*/ }
        )
    }
}

@Composable
fun PhraseConfirmationScreen(
    mnemonic: List<String>,
    nextOnClick: () -> Unit
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
                text = stringResource(id = R.string.phrase_confirmation_title),
                style = ComposeTypographies.Title2,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            Spacer(modifier = Modifier.size(dimensionResource(R.dimen.standard_margin)))

            BackupStatus(BackUpStatus.BACKED_UP)

            Spacer(modifier = Modifier.size(dimensionResource(R.dimen.large_margin)))

            HidableMnemonic(mnemonic = mnemonic)

            Spacer(modifier = Modifier.size(dimensionResource(R.dimen.standard_margin)))

            SimpleText(
                text = stringResource(id = R.string.phrase_confirmation_description),
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

@Preview(name = "Phrase Confirmation", backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun PreviewPhraseConfirmationScreen() {
    PhraseConfirmationScreen(
        mnemonic = mnemonic,
        nextOnClick = {}
    )
}
