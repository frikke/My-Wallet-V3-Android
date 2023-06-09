package com.blockchain.presentation.backup.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.MinimalPrimaryButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.control.RadioButtonState
import com.blockchain.componentlib.control.RadioCheckMark
import com.blockchain.componentlib.navigation.ModeBackgroundColor
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey100
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.utils.clickableNoEffect
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
    skipOnClick: () -> Unit
) {
    var allAcknowledgementsChecked by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.backgroundSecondary)
    ) {
        NavigationBar(
            modeColor = ModeBackgroundColor.Override(WalletMode.NON_CUSTODIAL),
            title = stringResource(com.blockchain.stringResources.R.string.backup_phrase_title_secure_wallet),
            mutedBackground = false,
            onBackButtonClick = backOnClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppTheme.dimensions.standardSpacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BackupStatus(backupStatus)

            Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))

            BackupPhraseIntroScreenDescription()

            Spacer(modifier = Modifier.weight(1F))

            BackupPhraseIntroAcknowledgments(
                acknowledgments = listOf(
                    com.blockchain.stringResources.R.string.backup_phrase_intro_ack_1,
                    com.blockchain.stringResources.R.string.backup_phrase_intro_ack_2,
                    com.blockchain.stringResources.R.string.backup_phrase_intro_ack_3
                )
            ) {
                allAcknowledgementsChecked = true
            }

            Spacer(modifier = Modifier.weight(3F))

            PrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = com.blockchain.stringResources.R.string.back_up_now),
                state = if (allAcknowledgementsChecked) ButtonState.Enabled else ButtonState.Disabled,
                onClick = backUpNowOnClick
            )

            if (showSkipBackup) {
                Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

                MinimalPrimaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = com.blockchain.stringResources.R.string.common_skip),
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
        Image(
            imageResource = ImageResource.Local(
                id = com.blockchain.componentlib.icons.R.drawable.lock_on,
                contentDescription = stringResource(com.blockchain.stringResources.R.string.backup_phrase_intro_title),
                colorFilter = ColorFilter.tint(AppTheme.colors.primary)
            ),
            modifier = Modifier.size(32.dp)
        )

        SmallVerticalSpacer()

        SimpleText(
            text = stringResource(com.blockchain.stringResources.R.string.backup_phrase_intro_title),
            style = ComposeTypographies.Title2,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )

        SmallVerticalSpacer()

        SimpleText(
            text = stringResource(com.blockchain.stringResources.R.string.backup_phrase_intro_description),
            style = ComposeTypographies.Paragraph1,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )
    }
}

@Composable
fun BackupPhraseIntroAcknowledgmentItem(
    text: String,
    onAccepted: () -> Unit
) {
    var isChecked by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .border(
                width = AppTheme.dimensions.borderSmall,
                color = Grey100,
                shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium)
            )
            .clickableNoEffect {
                isChecked = true
                onAccepted()
            }
            .background(color = Color.White, shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium))
            .padding(
                horizontal = AppTheme.dimensions.smallSpacing,
                vertical = dimensionResource(com.blockchain.componentlib.R.dimen.very_small_spacing)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SimpleText(
            text = text,
            style = ComposeTypographies.Caption1,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Start,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing)))

        RadioCheckMark(
            state = if (isChecked) RadioButtonState.Selected else RadioButtonState.Unselected,
            onSelectedChanged = {
                isChecked = !isChecked
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
                Spacer(modifier = Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.tiny_spacing)))
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
        BackUpStatus.NO_BACKUP,
        showSkipBackup = true,
        backOnClick = {},
        backUpNowOnClick = {},
        skipOnClick = {}
    )
}

@Preview(name = "no backup no skip", showBackground = true)
@Composable
fun PreviewBackupPhraseIntroScreen_NoBackup_NoSkip() {
    BackupPhraseIntroScreen(
        BackUpStatus.NO_BACKUP,
        showSkipBackup = false,
        backOnClick = {},
        backUpNowOnClick = {},
        skipOnClick = {}
    )
}

@Preview(name = "backed up", showBackground = true)
@Composable
fun PreviewBackupPhraseIntroScreenBackedUp() {
    BackupPhraseIntroScreen(
        BackUpStatus.BACKED_UP,
        showSkipBackup = true,
        backOnClick = {},
        backUpNowOnClick = {},
        skipOnClick = {}
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
    BackupPhraseIntroAcknowledgmentItem(
        text = stringResource(id = com.blockchain.stringResources.R.string.backup_phrase_intro_ack_1)
    ) {
    }
}

@Preview(name = "Acknowledgments", showBackground = true)
@Composable
fun PreviewBackupPhraseIntroAcknowledgments() {
    BackupPhraseIntroAcknowledgments(
        acknowledgments = listOf(
            com.blockchain.stringResources.R.string.backup_phrase_intro_ack_1,
            com.blockchain.stringResources.R.string.backup_phrase_intro_ack_2,
            com.blockchain.stringResources.R.string.backup_phrase_intro_ack_3
        )
    ) {}
}
