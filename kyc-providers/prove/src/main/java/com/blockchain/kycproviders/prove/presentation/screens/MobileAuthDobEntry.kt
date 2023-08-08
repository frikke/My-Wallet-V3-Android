package com.blockchain.kycproviders.prove.presentation.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.controls.OutlinedTextInput
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.User
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.kycproviders.prove.presentation.ProvePrefillIntent
import com.blockchain.kycproviders.prove.presentation.ProvePrefillViewState
import com.blockchain.kycproviders.prove.presentation.defaultViewState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
internal fun MobileAuthDobEntry(
    state: ProvePrefillViewState,
    onIntent: (ProvePrefillIntent) -> Unit,
    showDatePicker: () -> Unit
) {
    Column(
        Modifier.background(AppColors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                Icons.User
                    .withTint(AppTheme.colors.primary)
                    .withSize(AppTheme.dimensions.largeSpacing)
            )

            SimpleText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = AppTheme.dimensions.standardSpacing,
                        start = AppTheme.dimensions.smallSpacing,
                        end = AppTheme.dimensions.smallSpacing
                    ),
                text = stringResource(com.blockchain.stringResources.R.string.prove_mobile_auth_dob_entry_title),
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            SimpleText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = AppTheme.dimensions.tinySpacing,
                        start = AppTheme.dimensions.smallSpacing,
                        end = AppTheme.dimensions.smallSpacing
                    ),
                text = stringResource(com.blockchain.stringResources.R.string.prove_mobile_auth_dob_entry_subtitle),
                style = ComposeTypographies.Body1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Centre
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = AppTheme.dimensions.standardSpacing,
                        start = AppTheme.dimensions.smallSpacing,
                        end = AppTheme.dimensions.smallSpacing
                    )
            ) {
                val dobDisplayFormat = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.US) }
                val dob = state.dateOfBirthInput?.let { dobDisplayFormat.format(it.time) }.orEmpty()
                OutlinedTextInput(
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    value = dob,
                    label = stringResource(com.blockchain.stringResources.R.string.kyc_profile_dob_hint),
                    placeholder = stringResource(
                        com.blockchain.stringResources.R.string.prove_mobile_auth_dob_entry_placeholder
                    ),
                    readOnly = true,
                    onValueChange = {
                        // no op readonly
                    }
                )

                Box(
                    Modifier
                        .matchParentSize()
                        .clickable(onClick = showDatePicker)
                )
            }

            SimpleText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = AppTheme.dimensions.smallestSpacing,
                        start = AppTheme.dimensions.smallSpacing,
                        end = AppTheme.dimensions.smallSpacing
                    ),
                text = stringResource(com.blockchain.stringResources.R.string.prove_mobile_auth_dob_entry_min_age),
                style = ComposeTypographies.Caption1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Start
            )
        }

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = AppTheme.dimensions.smallSpacing,
                    end = AppTheme.dimensions.smallSpacing,
                    bottom = AppTheme.dimensions.smallSpacing
                ),
            state = state.possessionDataEntryContinueButtonState,
            text = stringResource(com.blockchain.stringResources.R.string.common_continue),
            onClick = { onIntent(ProvePrefillIntent.MobileAuthDobEntryContinueClicked) }
        )
    }
}

@Preview
@Composable
private fun PreviewEmpty() {
    MobileAuthDobEntry(
        state = defaultViewState,
        onIntent = {},
        showDatePicker = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewEmptyDark() {
    PreviewEmpty()
}

@Preview
@Composable
private fun PreviewFilled() {
    MobileAuthDobEntry(
        state = defaultViewState.copy(
            dateOfBirthInput = Calendar.getInstance().apply {
                set(1990, 1, 1)
            }
        ),
        onIntent = {},
        showDatePicker = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewFilledDark() {
    PreviewFilled()
}
