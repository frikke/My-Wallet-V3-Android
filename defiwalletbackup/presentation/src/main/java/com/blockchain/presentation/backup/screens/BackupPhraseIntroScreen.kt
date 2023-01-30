package com.blockchain.presentation.backup.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.button.TertiaryButton
import com.blockchain.componentlib.control.NoPaddingRadio
import com.blockchain.componentlib.control.RadioButtonState
import com.blockchain.componentlib.navigation.ModeBackgroundColor
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey900
import com.blockchain.componentlib.utils.clickableNoEffect
import com.blockchain.presentation.R
import com.blockchain.presentation.backup.BackUpStatus
import com.blockchain.presentation.backup.BackupAnalyticsEvents
import com.blockchain.presentation.backup.BackupPhraseIntent
import com.blockchain.presentation.backup.BackupPhraseViewState
import com.blockchain.presentation.backup.viewmodel.BackupPhraseViewModel
import com.blockchain.walletmode.WalletMode
import org.koin.androidx.compose.get

/**
 * figma: https://www.figma.com/file/VTMHbEoX0QDNOLKKdrgwdE/AND---Super-App?node-id=260%3A17284
 */
@Composable
fun BackupPhraseIntro(
    viewModel: BackupPhraseViewModel,
    analytics: Analytics = get()
) {
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
            backUpNowOnClick = {
                viewModel.onIntent(BackupPhraseIntent.StartBackupProcess)
                analytics.logEvent(BackupAnalyticsEvents.BackupNowClicked)
            },
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
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.backgroundMuted)
    ) {
        NavigationBar(
            modeColor = ModeBackgroundColor.Override(WalletMode.NON_CUSTODIAL),
            title = stringResource(R.string.backup_phrase_title_secure_wallet),
            onBackButtonClick = backOnClick
        )

        Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.standard_spacing)))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = dimensionResource(id = R.dimen.standard_spacing),
                    end = dimensionResource(id = R.dimen.standard_spacing),
                    bottom = dimensionResource(id = R.dimen.standard_spacing)
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            BackupStatus(backupStatus)

            Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))

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
                Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

                TertiaryButton(
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
        Text(
            text = stringResource(R.string.backup_phrase_intro_title),
            textAlign = TextAlign.Center,
            style = AppTheme.typography.title3,
            color = Grey900
        )

        Spacer(Modifier.size(dimensionResource(R.dimen.tiny_spacing)))

        Text(
            text = stringResource(R.string.backup_phrase_intro_description),
            textAlign = TextAlign.Center,
            style = AppTheme.typography.body1,
            color = AppTheme.colors.body
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
            .background(
                color = AppTheme.colors.background,
                shape = RoundedCornerShape(dimensionResource(R.dimen.borderRadiiMedium))
            )
            .padding(
                horizontal = dimensionResource(R.dimen.very_small_spacing),
                vertical = dimensionResource(R.dimen.small_spacing)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1F),
            text = text,
            color = AppTheme.colors.title,
            style = AppTheme.typography.paragraph2
        )

        Spacer(modifier = Modifier.size(dimensionResource(R.dimen.small_spacing)))

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

            if (index != acknowledgments.lastIndex) {
                Spacer(modifier = Modifier.size(dimensionResource(R.dimen.tiny_spacing)))
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
