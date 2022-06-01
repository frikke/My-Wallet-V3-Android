package com.blockchain.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.control.PrimarySwitch
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.sheets.BottomSheetButton
import com.blockchain.componentlib.sheets.ButtonType
import com.blockchain.componentlib.sheets.MinimalSheet
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey900
import com.blockchain.presentation.BackUpStatus
import com.blockchain.presentation.BackupPhraseIntent
import com.blockchain.presentation.BackupPhraseViewState
import com.blockchain.presentation.R
import com.blockchain.presentation.viewmodel.BackupPhraseViewModel
import kotlinx.coroutines.launch

@Composable
fun BackupPhraseIntro(viewModel: BackupPhraseViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val stateFlowLifecycleAware = remember(viewModel.viewState, lifecycleOwner) {
        viewModel.viewState.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
    val viewState: BackupPhraseViewState? by stateFlowLifecycleAware.collectAsState(null)

    viewState?.let { state ->
        BackupPhraseIntroScreen(
            backupStatus = state.backUpStatus,
            showError = state.isError,
            errorButtonOnClick = { viewModel.onIntent(BackupPhraseIntent.EndFlow(isSuccessful = false)) },
            backOnClick = { viewModel.onIntent(BackupPhraseIntent.EndFlow(isSuccessful = false)) },
            backUpNowOnClick = { viewModel.onIntent(BackupPhraseIntent.StartBackupProcess) }
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BackupPhraseIntroScreen(
    backupStatus: BackUpStatus,
    showError: Boolean,
    errorButtonOnClick: () -> Unit,
    backOnClick: () -> Unit,
    backUpNowOnClick: () -> Unit
) {
    val errorSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmStateChange = { false }
    )

    val scope = rememberCoroutineScope()

    LaunchedEffect(showError) {
        if (showError) scope.launch { errorSheetState.show() }
    }

    ModalBottomSheetLayout(
        sheetContent = {
            MinimalSheet(
                title = stringResource(R.string.ops),
                subtitle = stringResource(R.string.something_went_wrong_try_again),
                button = BottomSheetButton(
                    type = ButtonType.PRIMARY,
                    onClick = errorButtonOnClick,
                    text = stringResource(R.string.common_ok)
                )
            )
        },
        sheetState = errorSheetState,
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            NavigationBar(
                title = stringResource(R.string.backup_phrase_title_secure_wallet), onBackButtonClick = backOnClick
            )

            Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.standard_margin)))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = dimensionResource(id = R.dimen.standard_margin),
                        end = dimensionResource(id = R.dimen.standard_margin),
                        bottom = dimensionResource(id = R.dimen.standard_margin)
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                if (backupStatus == BackUpStatus.NO_BACKUP) {
                    BackupStatus(backupStatus)
                }

                Spacer(modifier = Modifier.weight(1F))

                BackupPhraseIntroScreenDescription()

                Spacer(modifier = Modifier.weight(3F))

                BackupPhraseIntroScreenCta(backUpNowOnClick = backUpNowOnClick)
            }
        }
    }
}

@Composable
fun BackupPhraseIntroScreenDescription() {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(imageResource = ImageResource.Local(R.drawable.ic_padlock))

        Spacer(Modifier.size(dimensionResource(R.dimen.standard_margin)))

        Text(
            text = stringResource(R.string.backup_phrase_intro_title),
            textAlign = TextAlign.Center,
            style = AppTheme.typography.title2,
            color = Grey900
        )

        Spacer(Modifier.size(dimensionResource(R.dimen.tiny_margin)))

        Text(
            text = stringResource(R.string.backup_phrase_intro_description),
            textAlign = TextAlign.Center,
            style = AppTheme.typography.paragraph1,
            color = Grey900
        )
    }
}

@Composable
fun BackupPhraseIntroScreenCta(backUpNowOnClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var isChecked by remember { mutableStateOf(false) }

        Row {
            PrimarySwitch(
                isChecked = isChecked, onCheckChanged = { isChecked = it }
            )

            Spacer(Modifier.size(dimensionResource(R.dimen.very_small_margin)))

            Text(
                text = stringResource(id = R.string.backup_phrase_intro_warning),
                style = AppTheme.typography.micro2
            )
        }

        Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.standard_margin)))

        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.back_up_now),
            state = if (isChecked) ButtonState.Enabled else ButtonState.Disabled,
            onClick = backUpNowOnClick
        )
    }
}

// ///////////////
// PREVIEWS
// ///////////////

@Preview(name = "Backup Intro no backup", showBackground = true)
@Composable
fun PreviewBackupPhraseIntroScreenNoBackup() {
    BackupPhraseIntroScreen(
        BackUpStatus.NO_BACKUP,
        showError = false, errorButtonOnClick = {}, backOnClick = {}, backUpNowOnClick = { }
    )
}

@Preview(name = "Backup Intro backed up", showBackground = true)
@Composable
fun PreviewBackupPhraseIntroScreenBackedUp() {
    BackupPhraseIntroScreen(
        BackUpStatus.BACKED_UP,
        errorButtonOnClick = {}, showError = false, backOnClick = {}, backUpNowOnClick = { }
    )
}

@Preview(name = "Backup Intro Description", showBackground = true)
@Composable
fun PreviewBackupPhraseIntroScreenDescription() {
    BackupPhraseIntroScreenDescription()
}

@Preview(name = "Backup Intro CTA", showBackground = true)
@Composable
fun PreviewBackupPhraseIntroScreenCta() {
    BackupPhraseIntroScreenCta {}
}
