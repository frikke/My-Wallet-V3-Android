package com.blockchain.kycproviders.prove.presentation.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.MinimalPrimarySmallButton
import com.blockchain.componentlib.loader.LoadingIndicator
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.kycproviders.prove.presentation.ProvePrefillIntent
import com.blockchain.kycproviders.prove.presentation.ProvePrefillViewState
import com.blockchain.kycproviders.prove.presentation.defaultViewState

@Composable
internal fun WaitingInstantLinkValidation(
    state: ProvePrefillViewState,
    onIntent: (ProvePrefillIntent) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(AppColors.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LoadingIndicator(
            color = AppColors.primary
        )

        SimpleText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = AppTheme.dimensions.xLargeSpacing,
                    start = AppTheme.dimensions.standardSpacing,
                    end = AppTheme.dimensions.standardSpacing
                ),
            text = stringResource(com.blockchain.stringResources.R.string.prove_waiting_instant_link_validation_title),
            style = ComposeTypographies.Title3,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )

        SimpleText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = AppTheme.dimensions.tinySpacing,
                    start = AppTheme.dimensions.standardSpacing,
                    end = AppTheme.dimensions.standardSpacing
                ),
            text = stringResource(
                com.blockchain.stringResources.R.string.prove_waiting_instant_link_validation_subtitle
            ),
            style = ComposeTypographies.Body1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Centre
        )

        val buttonText = if (state.resendSmsWaitTime == 0L) {
            stringResource(com.blockchain.stringResources.R.string.prove_resend_sms)
        } else {
            val minutes = state.resendSmsWaitTime / 60
            val seconds = state.resendSmsWaitTime % 60

            stringResource(com.blockchain.stringResources.R.string.prove_resend_sms_in_seconds, minutes, seconds)
        }

        MinimalPrimarySmallButton(
            modifier = Modifier.padding(top = AppTheme.dimensions.standardSpacing),
            text = buttonText,
            state = state.resendSmsButtonState,
            onClick = { onIntent(ProvePrefillIntent.ResendSmsClicked) }
        )
    }
}

@Preview
@Composable
private fun PreviewWaitingInstantLinkValidation() {
    WaitingInstantLinkValidation(
        state = defaultViewState.copy(
            resendSmsButtonState = ButtonState.Enabled
        ),
        onIntent = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewWaitingInstantLinkValidationDark() {
    PreviewWaitingInstantLinkValidation()
}

@Preview
@Composable
private fun PreviewWaitingResend() {
    WaitingInstantLinkValidation(
        state = defaultViewState.copy(
            resendSmsWaitTime = 58,
            resendSmsButtonState = ButtonState.Disabled
        ),
        onIntent = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewWaitingResendDark() {
    PreviewWaitingResend()
}
