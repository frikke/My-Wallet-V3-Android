package com.blockchain.presentation.backup.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.button.TertiaryButton
import com.blockchain.componentlib.navigation.ModeBackgroundColor
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.LargeVerticalSpacer
import com.blockchain.componentlib.theme.Red600
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.theme.TinyVerticalSpacer
import com.blockchain.extensions.exhaustive
import com.blockchain.presentation.R
import com.blockchain.presentation.backup.BackupPhraseIntent
import com.blockchain.presentation.backup.BackupPhraseViewState
import com.blockchain.presentation.backup.TOTAL_STEP_COUNT
import com.blockchain.presentation.backup.UserMnemonicVerificationStatus
import com.blockchain.presentation.backup.viewmodel.BackupPhraseViewModel
import com.blockchain.utils.replaceInList
import com.blockchain.walletmode.WalletMode
import java.util.Locale

private const val STEP_INDEX = 2

/**
 * figma: https://www.figma.com/file/VTMHbEoX0QDNOLKKdrgwdE/AND---Super-App?node-id=260%3A17788
 */
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

            resetVerificationStatus = { viewModel.onIntent(BackupPhraseIntent.ResetVerificationStatus) },
            backOnClick = { viewModel.onIntent(BackupPhraseIntent.GoToPreviousScreen) },
            nextOnClick = { userMnemonic -> viewModel.onIntent(BackupPhraseIntent.VerifyPhrase(userMnemonic.toList())) }
        )
    }
}

@Composable
fun VerifyPhraseScreen(
    mnemonic: List<String>,
    isLoading: Boolean,
    mnemonicVerificationStatus: UserMnemonicVerificationStatus,

    resetVerificationStatus: () -> Unit,
    backOnClick: () -> Unit,
    nextOnClick: (userMnemonic: List<String>) -> Unit
) {
    // map to selectable word -> offers selection setting
    @Suppress("RememberReturnType")
    val selectableMnemonic = remember {
        mnemonic.mapIndexed { index, word ->
            SelectableMnemonicWord(
                id = index,
                word = word,
                selected = false
            )
        }
    }
    // mnemonic that user is selecting
    val userMnemonic = remember { mutableStateListOf<SelectableMnemonicWord>() }
    // create randomized mnemonic for user to verify with
    val randomizedMnemonic = remember { selectableMnemonic.shuffled().toMutableStateList() }

    // when the phrase is incorrect - reset the mnemonic lists
    var resetWords by remember { mutableStateOf(false) }
    if (resetWords) {
        userMnemonic.clear()
        randomizedMnemonic.apply {
            clear()
            addAll(selectableMnemonic.shuffled())
        }
        resetWords = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NavigationBar(
            modeColor = ModeBackgroundColor.Override(WalletMode.NON_CUSTODIAL),
            mutedBackground = false,
            title = stringResource(
                com.blockchain.stringResources.R.string.backup_phrase_title_steps,
                STEP_INDEX,
                TOTAL_STEP_COUNT
            ),
            onBackButtonClick = backOnClick
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
                text = stringResource(id = com.blockchain.stringResources.R.string.verify_phrase_title),
                style = ComposeTypographies.Title2,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            SmallVerticalSpacer()

            SimpleText(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = com.blockchain.stringResources.R.string.verify_phrase_description),
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            LargeVerticalSpacer()

            MnemonicVerification(userMnemonic, mnemonicVerificationStatus) { selectableWord ->
                if (isLoading.not()) {
                    // remove word from the final list
                    userMnemonic.remove(selectableWord)
                    // mark word as unselected to show it back in randomized list
                    randomizedMnemonic.replaceInList(
                        replacement = selectableWord.copy(selected = false),
                        where = { it.id == selectableWord.id }
                    )
                    // reset verification status in case of previous failure
                    resetVerificationStatus()
                }
            }

            if (mnemonicVerificationStatus == UserMnemonicVerificationStatus.IDLE) {
                Spacer(
                    modifier = Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing))
                )

                MnemonicSelection(randomizedMnemonic) { selectableWord ->
                    if (isLoading.not()) {
                        // add word to the final list
                        userMnemonic.add(selectableWord)
                        // mark word as selected to hide it from randomized list
                        randomizedMnemonic.replaceInList(
                            replacement = selectableWord.copy(selected = true),
                            where = { it.id == selectableWord.id }
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing)))

                VerifyPhraseIncorrect(resetOnClick = {
                    resetWords = true
                    resetVerificationStatus()
                })
            }

            Spacer(modifier = Modifier.weight(1F))

            VerifyPhraseCta(
                allWordsSelected = randomizedMnemonic.all { it.selected },
                isLoading = isLoading
            ) {
                nextOnClick(userMnemonic.map { it.word })
            }
        }
    }
}

@Composable
fun VerifyPhraseIncorrect(resetOnClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TertiaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(com.blockchain.stringResources.R.string.verify_phrase_incorrect_button),
            onClick = resetOnClick
        )

        Spacer(modifier = Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing)))

        Text(
            text = stringResource(com.blockchain.stringResources.R.string.verify_phrase_incorrect_message),
            style = AppTheme.typography.body1,
            color = Red600,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun VerifyPhraseCta(
    allWordsSelected: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val state: ButtonState = when {
        allWordsSelected.not() -> ButtonState.Disabled
        isLoading -> ButtonState.Loading
        else -> ButtonState.Enabled
    }.exhaustive

    PrimaryButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(com.blockchain.stringResources.R.string.verify),
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
        mnemonicVerificationStatus = UserMnemonicVerificationStatus.IDLE,

        resetVerificationStatus = {},
        backOnClick = {},
        nextOnClick = {}
    )
}

@Preview(name = "Verify Phrase Loading", backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun PreviewVerifyPhraseScreenLoading() {
    VerifyPhraseScreen(
        mnemonic = mnemonic,
        isLoading = true,
        mnemonicVerificationStatus = UserMnemonicVerificationStatus.IDLE,

        resetVerificationStatus = {},
        backOnClick = {},
        nextOnClick = {}
    )
}

@Preview(name = "Verify Phrase Incorrect", backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun PreviewVerifyPhraseScreenIncorrect() {
    VerifyPhraseScreen(
        mnemonic = mnemonic,
        isLoading = false,
        mnemonicVerificationStatus = UserMnemonicVerificationStatus.INCORRECT,

        resetVerificationStatus = {},
        backOnClick = {},
        nextOnClick = {}
    )
}

@Preview(name = "Incorrect Phrase Message")
@Composable
fun PreviewVerifyPhraseIncorrect() {
    VerifyPhraseIncorrect {}
}

@Preview(name = "Verify Phrase Cta - selected:false, loading:false, status:nostatus")
@Composable
fun PreviewVerifyPhraseCta_SelectedFalse_LoadingFalse_StatusNoStatus() {
    VerifyPhraseCta(
        allWordsSelected = false,
        isLoading = false,
        onClick = {}
    )
}

@Preview(name = "Verify Phrase Cta - selected:true, loading:false, status:nostatus")
@Composable
fun PreviewVerifyPhraseCta_SelectedTrue_LoadingFalse_StatusIncorrect() {
    VerifyPhraseCta(
        allWordsSelected = true,
        isLoading = false,
        onClick = {}
    )
}

@Preview(name = "Verify Phrase Cta - selected:true, loading:true, status:nostatus")
@Composable
fun PreviewVerifyPhraseCta_SelectedTrue_LoadingTrue_StatusIncorrect() {
    VerifyPhraseCta(
        allWordsSelected = true,
        isLoading = true,
        onClick = {}
    )
}
