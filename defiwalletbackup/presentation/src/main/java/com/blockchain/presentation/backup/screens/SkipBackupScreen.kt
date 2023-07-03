package com.blockchain.presentation.backup.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.MinimalPrimaryButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.icon.SmallTagIcon
import com.blockchain.componentlib.icons.Alert
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Lock
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.navigation.ModeBackgroundColor
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.StandardVerticalSpacer
import com.blockchain.componentlib.theme.TinyVerticalSpacer
import com.blockchain.presentation.R
import com.blockchain.presentation.backup.BackupAnalyticsEvents
import com.blockchain.presentation.backup.BackupPhraseIntent
import com.blockchain.presentation.backup.viewmodel.BackupPhraseViewModel
import com.blockchain.walletmode.WalletMode
import org.koin.androidx.compose.get

@Composable
fun SkipBackup(
    viewModel: BackupPhraseViewModel,
    analytics: Analytics = get()
) {
    SkipBackupScreen(
        backOnClick = { viewModel.onIntent(BackupPhraseIntent.EndFlow(isSuccessful = false)) },
        skipOnClick = {
            viewModel.onIntent(BackupPhraseIntent.SkipBackup)
            analytics.logEvent(BackupAnalyticsEvents.BackupSkipClicked)
        },
        backUpNowOnClick = { viewModel.onIntent(BackupPhraseIntent.GoToPreviousScreen) }
    )
}

@Composable
fun SkipBackupScreen(
    backOnClick: () -> Unit,
    skipOnClick: () -> Unit,
    backUpNowOnClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NavigationBar(
            modeColor = ModeBackgroundColor.Override(WalletMode.NON_CUSTODIAL),
            title = stringResource(com.blockchain.stringResources.R.string.backup_phrase_title_secure_wallet),
            onBackButtonClick = backOnClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppTheme.dimensions.standardSpacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1F))

            SmallTagIcon(
                icon = StackedIcon.SmallTag(
                    main = Icons.Filled.Lock
                        .withTint(AppColors.backgroundSecondary)
                        .withBackground(backgroundColor = AppColors.explorer, iconSize = 58.dp, backgroundSize = 88.dp),
                    tag = Icons.Filled.Alert
                        .withTint(AppColors.warning),
                ),
                iconBackground = AppColors.backgroundSecondary,
                mainIconSize = 88.dp,
                tagIconSize = 44.dp,
            )

            StandardVerticalSpacer()

            SimpleText(
                text = stringResource(com.blockchain.stringResources.R.string.skip_backup_title),
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            TinyVerticalSpacer()

            SimpleText(
                text = stringResource(com.blockchain.stringResources.R.string.skip_backup_description),
                style = ComposeTypographies.Body1,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            Spacer(modifier = Modifier.weight(2F))

            PrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = com.blockchain.stringResources.R.string.skip_backup_cta_skip),
                onClick = skipOnClick
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

            MinimalPrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = com.blockchain.stringResources.R.string.skip_backup_cta_backup),
                onClick = backUpNowOnClick
            )
        }
    }
}

// ///////////////
// PREVIEWS
// ///////////////

@Preview(showBackground = true)
@Composable
private fun PreviewSkipBackupScreen() {
    SkipBackupScreen(backOnClick = {}, skipOnClick = {}, backUpNowOnClick = {})
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSkipBackupScreenDark() {
    PreviewSkipBackupScreen()
}
