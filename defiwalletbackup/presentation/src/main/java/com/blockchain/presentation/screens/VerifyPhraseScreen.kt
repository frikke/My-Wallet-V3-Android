package com.blockchain.presentation.screens

import androidx.annotation.StringRes
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
import androidx.compose.runtime.toMutableStateList
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
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.extensions.exhaustive
import com.blockchain.presentation.BackupPhraseIntent
import com.blockchain.presentation.BackupPhraseViewState
import com.blockchain.presentation.R
import com.blockchain.presentation.UserMnemonicVerificationStatus
import com.blockchain.presentation.viewmodel.BackupPhraseViewModel
import java.util.Locale

@Composable
fun VerifyPhrase(viewModel: BackupPhraseViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val viewState: BackupPhraseViewState? by stateFlowLifecycleAware.collectAsState(null)

    viewState?.let { state ->
        VerifyPhraseScreen(
            mnemonic = state.mnemonic,
            isLoading = state.showLoading,
            mnemonicVerificationStatus = state.mnemonicVerificationStatus,
            /*todo(othmna) onclick verify validity*/
            nextOnClick = { userMnemonic ->
                /*navController.navigate(BackPhraseDestination.BackupConfirmation.route)*/
                viewModel.onIntent(BackupPhraseIntent.VerifyPhrase(userMnemonic))
            }
        )
    }
}

@Composable
fun VerifyPhraseScreen(
    mnemonic: List<String>,
    isLoading: Boolean,
    mnemonicVerificationStatus: UserMnemonicVerificationStatus,
    nextOnClick: (userMnemonic: List<String>) -> Unit,
) {
    val userMnemonic = remember { mutableStateListOf<String>() }
    val randomizedMnemonic = remember { mnemonic.toMutableStateList() }

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
                text = stringResource(id = R.string.verify_phrase_title),
                style = ComposeTypographies.Title2,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            Spacer(modifier = Modifier.size(dimensionResource(R.dimen.large_margin)))

            SimpleText(
                text = stringResource(id = R.string.verify_phrase_description),
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            Spacer(modifier = Modifier.size(dimensionResource(R.dimen.small_margin)))

            MnemonicVerification(userMnemonic) { word ->
                if (isLoading.not() && mnemonicVerificationStatus != UserMnemonicVerificationStatus.VERIFIED) {
                    userMnemonic.remove(word)
                    randomizedMnemonic.add(word)
                }
            }

            Spacer(modifier = Modifier.size(dimensionResource(R.dimen.standard_margin)))

            MnemonicSelection(randomizedMnemonic) { word ->
                if (isLoading.not() && mnemonicVerificationStatus != UserMnemonicVerificationStatus.VERIFIED) {
                    userMnemonic.add(word)
                    randomizedMnemonic.remove(word)
                }
            }

            Spacer(modifier = Modifier.weight(1F))

            VerifyPhraseCta(
                allWordsSelected = randomizedMnemonic.isEmpty(),
                isLoading = isLoading,
                mnemonicVerificationStatus = mnemonicVerificationStatus
            ) {
                nextOnClick(userMnemonic)
            }
        }
    }
}

@Composable
fun VerifyPhraseCta(
    allWordsSelected: Boolean,
    isLoading: Boolean,
    mnemonicVerificationStatus: UserMnemonicVerificationStatus,
    onClick: () -> Unit
) {
    val state: ButtonState = when {
        allWordsSelected.not() -> ButtonState.Disabled
        isLoading -> ButtonState.Loading
        else -> ButtonState.Enabled
    }.exhaustive

    @StringRes
    val text: Int = when (mnemonicVerificationStatus) {
        UserMnemonicVerificationStatus.NO_STATUS,
        UserMnemonicVerificationStatus.INCORRECT -> R.string.verify

        UserMnemonicVerificationStatus.VERIFIED -> R.string.next
    }.exhaustive

    PrimaryButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(text),
        state = state,
        onClick = onClick
    )
}

// ///////////////
// PREVIEWS
// ///////////////

private val mnemonic = Locale.getISOCountries().toList().map {
    Locale("", it).isO3Country
}.shuffled().subList(0, 12)

@Preview(name = "Verify Phrase", backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun PreviewVerifyPhrase() {
    VerifyPhraseScreen(
        mnemonic = mnemonic,
        isLoading = false,
        mnemonicVerificationStatus = UserMnemonicVerificationStatus.NO_STATUS,
        nextOnClick = {}
    )
}

@Preview(name = "Verify Phrase Loading", backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun PreviewVerifyPhraseScreenLoading() {
    VerifyPhraseScreen(
        mnemonic = mnemonic,
        isLoading = true,
        mnemonicVerificationStatus = UserMnemonicVerificationStatus.NO_STATUS,
        nextOnClick = {}
    )
}

@Preview(name = "Verify Phrase Cta - selected:false, loading:false, status:nostatus")
@Composable
fun PreviewVerifyPhraseCta_SelectedFalse_LoadingFalse_StatusNoStatus() {
    VerifyPhraseCta(
        allWordsSelected = false,
        isLoading = false,
        mnemonicVerificationStatus = UserMnemonicVerificationStatus.NO_STATUS,
        onClick = {}
    )
}

@Preview(name = "Verify Phrase Cta - selected:true, loading:false, status:nostatus")
@Composable
fun PreviewVerifyPhraseCta_SelectedTrue_LoadingFalse_StatusIncorrect() {
    VerifyPhraseCta(
        allWordsSelected = true,
        isLoading = false,
        mnemonicVerificationStatus = UserMnemonicVerificationStatus.NO_STATUS,
        onClick = {}
    )
}

@Preview(name = "Verify Phrase Cta - selected:true, loading:true, status:nostatus")
@Composable
fun PreviewVerifyPhraseCta_SelectedTrue_LoadingTrue_StatusIncorrect() {
    VerifyPhraseCta(
        allWordsSelected = true,
        isLoading = true,
        mnemonicVerificationStatus = UserMnemonicVerificationStatus.NO_STATUS,
        onClick = {}
    )
}

@Preview(name = "Verify Phrase Cta - selected:true, loading:false, status:verified")
@Composable
fun PreviewVerifyPhraseCta_SelectedTrue_LoadingFalse_StatusVerified() {
    VerifyPhraseCta(
        allWordsSelected = true,
        isLoading = false,
        mnemonicVerificationStatus = UserMnemonicVerificationStatus.VERIFIED,
        onClick = {}
    )
}
