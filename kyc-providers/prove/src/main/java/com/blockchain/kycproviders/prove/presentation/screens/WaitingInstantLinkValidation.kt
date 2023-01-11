package com.blockchain.kycproviders.prove.presentation.screens

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
import com.blockchain.componentlib.button.SmallMinimalButton
import com.blockchain.componentlib.system.CircularProgressBar
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.White
import com.blockchain.kycproviders.prove.R
import com.blockchain.kycproviders.prove.presentation.ProvePrefillIntent
import com.blockchain.kycproviders.prove.presentation.ProvePrefillViewState
import com.blockchain.kycproviders.prove.presentation.defaultViewState

@Composable
internal fun WaitingInstantLinkValidation(
    state: ProvePrefillViewState,
    onIntent: (ProvePrefillIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressBar()

        SimpleText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = AppTheme.dimensions.xLargeSpacing,
                    start = AppTheme.dimensions.standardSpacing,
                    end = AppTheme.dimensions.standardSpacing,
                ),
            text = stringResource(R.string.prove_waiting_instant_link_validation_title),
            style = ComposeTypographies.Title3,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre,
        )

        SimpleText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = AppTheme.dimensions.tinySpacing,
                    start = AppTheme.dimensions.standardSpacing,
                    end = AppTheme.dimensions.standardSpacing,
                ),
            text = stringResource(R.string.prove_waiting_instant_link_validation_subtitle),
            style = ComposeTypographies.Body1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Centre,
        )

        val buttonText = if (state.resendSmsWaitTime == 0L) {
            stringResource(R.string.prove_resend_sms)
        } else {
            val minutes = state.resendSmsWaitTime / 60
            val seconds = state.resendSmsWaitTime % 60

            stringResource(R.string.prove_resend_sms_in_seconds, minutes, seconds)
        }

        SmallMinimalButton(
            modifier = Modifier.padding(top = AppTheme.dimensions.standardSpacing),
            text = buttonText,
            state = state.resendSmsButtonState,
            onClick = { onIntent(ProvePrefillIntent.ResendSmsClicked) }
        )
    }
}

@Preview
@Composable
private fun Preview() {
    WaitingInstantLinkValidation(
        state = defaultViewState.copy(
            resendSmsButtonState = ButtonState.Enabled
        ),
        onIntent = {},
    )
}

@Preview
@Composable
private fun PreviewWaitingResend() {
    WaitingInstantLinkValidation(
        state = defaultViewState.copy(
            resendSmsWaitTime = 58,
            resendSmsButtonState = ButtonState.Disabled
        ),
        onIntent = {},
    )
}
