package com.blockchain.presentation.backup.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.MinimalButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.control.NoPaddingRadio
import com.blockchain.componentlib.control.RadioButtonState
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey100
import com.blockchain.componentlib.theme.Grey900
import com.blockchain.componentlib.utils.clickableNoEffect
import com.blockchain.presentation.R
import com.blockchain.presentation.backup.BackUpStatus
import com.blockchain.presentation.backup.BackupPhraseIntent
import com.blockchain.presentation.backup.BackupPhraseViewState
import com.blockchain.presentation.backup.viewmodel.BackupPhraseViewModel
import com.blockchain.utils.isNotLastIn

/**
 * figma: https://www.figma.com/file/VTMHbEoX0QDNOLKKdrgwdE/AND---Super-App?node-id=260%3A17284
 */
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
            showSkipBackup = state.showSkipBackup,
            backOnClick = { viewModel.onIntent(BackupPhraseIntent.EndFlow(isSuccessful = false)) },
            backUpNowOnClick = { viewModel.onIntent(BackupPhraseIntent.StartBackupProcess) },
            skipOnClick = { viewModel.onIntent(BackupPhraseIntent.GoToSkipBackup) }
        )
    }
}

@Composable
fun BackupPhraseIntroScreen(
    backupStatus: BackUpStatus,
    showSkipBackup: Boolean,
    backOnClick: () -> Unit,
    backUpNowOnClick: () -> Unit,
    skipOnClick: () -> Unit,
) {
    var allAcknowledgementsChecked by remember { mutableStateOf(false) }

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

            Spacer(modifier = Modifier.weight(1F))

            BackupPhraseIntroAcknowledgments(
                acknowledgments = listOf(
                    R.string.backup_phrase_intro_ack_1,
                    R.string.backup_phrase_intro_ack_2,
                    R.string.backup_phrase_intro_ack_3,
                )
            ) {
                allAcknowledgementsChecked = true
            }

            Spacer(modifier = Modifier.weight(3F))

            PrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.back_up_now),
                state = if (allAcknowledgementsChecked) ButtonState.Enabled else ButtonState.Disabled,
                onClick = backUpNowOnClick
            )

            if (showSkipBackup) {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.paddingMedium))

                MinimalButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.common_skip),
                    onClick = skipOnClick
                )
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
fun BackupPhraseIntroAcknowledgmentItem(
    text: String,
    onAccepted: () -> Unit,
) {
    var isChecked by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .clickableNoEffect {
                isChecked = true
                onAccepted()
            }
            .border(
                width = 1.dp,
                color = Grey100,
                shape = RoundedCornerShape(dimensionResource(R.dimen.borderRadiiMedium))
            )
            .padding(
                horizontal = dimensionResource(R.dimen.very_small_margin),
                vertical = dimensionResource(R.dimen.small_margin)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1F),
            text = text,
            style = AppTheme.typography.caption1
        )

        Spacer(modifier = Modifier.size(dimensionResource(R.dimen.small_margin)))

        NoPaddingRadio(
            state = if (isChecked) RadioButtonState.Selected else RadioButtonState.Unselected,
            onSelectedChanged = {
                isChecked = true
                onAccepted()
            }
        )
    }
}

@Composable
fun BackupPhraseIntroAcknowledgments(@StringRes acknowledgments: List<Int>, allChecked: () -> Unit) {
    val acknowledgementChecks = mutableMapOf<Int, Boolean>()

    acknowledgments.associateWithTo(acknowledgementChecks) { false }

    Column {
        acknowledgementChecks.keys.forEachIndexed { index, acknowledgement ->
            BackupPhraseIntroAcknowledgmentItem(stringResource(acknowledgement)) {
                acknowledgementChecks[acknowledgement] = true

                if (acknowledgementChecks.values.none { checked -> checked.not() }) {
                    allChecked()
                }
            }

            if (index isNotLastIn acknowledgments) {
                Spacer(modifier = Modifier.size(dimensionResource(R.dimen.tiny_margin)))
            }
        }
    }
}

// ///////////////
// PREVIEWS
// ///////////////

@Preview(name = "no backup", showBackground = true)
@Composable
fun PreviewBackupPhraseIntroScreen_NoBackup_Skip() {
    BackupPhraseIntroScreen(
        BackUpStatus.NO_BACKUP, showSkipBackup = true,
        backOnClick = {}, backUpNowOnClick = {}, skipOnClick = {}
    )
}

@Preview(name = "no backup no skip", showBackground = true)
@Composable
fun PreviewBackupPhraseIntroScreen_NoBackup_NoSkip() {
    BackupPhraseIntroScreen(
        BackUpStatus.NO_BACKUP, showSkipBackup = false,
        backOnClick = {}, backUpNowOnClick = {}, skipOnClick = {}
    )
}

@Preview(name = "backed up", showBackground = true)
@Composable
fun PreviewBackupPhraseIntroScreenBackedUp() {
    BackupPhraseIntroScreen(
        BackUpStatus.BACKED_UP, showSkipBackup = true,
        backOnClick = {}, backUpNowOnClick = {}, skipOnClick = {}
    )
}

@Preview(name = "Backup Intro Description", showBackground = true)
@Composable
fun PreviewBackupPhraseIntroScreenDescription() {
    BackupPhraseIntroScreenDescription()
}

@Preview(name = "Acknowledgment Item", showBackground = true)
@Composable
fun PreviewBackupPhraseIntroAcknowledgmentItem() {
    BackupPhraseIntroAcknowledgmentItem(text = stringResource(id = R.string.backup_phrase_intro_ack_1)) {}
}

@Preview(name = "Acknowledgments", showBackground = true)
@Composable
fun PreviewBackupPhraseIntroAcknowledgments() {
    BackupPhraseIntroAcknowledgments(
        acknowledgments = listOf(
            R.string.backup_phrase_intro_ack_1,
            R.string.backup_phrase_intro_ack_2,
            R.string.backup_phrase_intro_ack_3,
        )
    ) {}
}
