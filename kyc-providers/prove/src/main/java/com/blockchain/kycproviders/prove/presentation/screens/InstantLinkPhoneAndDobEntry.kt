package com.blockchain.kycproviders.prove.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.controls.OutlinedTextInput
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.User
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.White
import com.blockchain.kycproviders.prove.R
import com.blockchain.kycproviders.prove.presentation.ProvePrefillIntent
import com.blockchain.kycproviders.prove.presentation.ProvePrefillViewState
import com.blockchain.kycproviders.prove.presentation.defaultViewState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun InstantLinkPhoneAndDobEntry(
    state: ProvePrefillViewState,
    onIntent: (ProvePrefillIntent) -> Unit,
    showDatePicker: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val localFocusManager = LocalFocusManager.current

    Column(
        Modifier.background(White)
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
                text = stringResource(
                    com.blockchain.stringResources.R.string.prove_instant_link_phone_and_dob_entry_title
                ),
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
                text = stringResource(
                    com.blockchain.stringResources.R.string.prove_instant_link_phone_and_dob_entry_subtitle
                ),
                style = ComposeTypographies.Body1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Centre
            )

            OutlinedTextInput(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = AppTheme.dimensions.standardSpacing,
                        start = AppTheme.dimensions.smallSpacing,
                        end = AppTheme.dimensions.smallSpacing
                    ),
                singleLine = true,
                value = state.mobileNumberInput,
                label = stringResource(com.blockchain.stringResources.R.string.prove_phone_number),
                placeholder = stringResource(
                    com.blockchain.stringResources.R.string.prove_instant_link_phone_and_dob_entry_phone_placeholder
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Phone
                ),
                keyboardActions = KeyboardActions(onNext = {
                    keyboardController?.hide()
                    localFocusManager.clearFocus(force = true)
                    showDatePicker()
                }),
                visualTransformation = PhoneVisualTransformation.US,
                onValueChange = { value -> onIntent(ProvePrefillIntent.MobileNumberInputChanged(value)) }
            )
            SimpleText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = AppTheme.dimensions.smallestSpacing,
                        start = AppTheme.dimensions.smallSpacing,
                        end = AppTheme.dimensions.smallSpacing
                    ),
                text = stringResource(
                    com.blockchain.stringResources.R.string.prove_instant_link_phone_and_dob_entry_us_phone_only
                ),
                style = ComposeTypographies.Caption1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Start
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

        Column {
            HorizontalDivider(Modifier.fillMaxWidth())

            SimpleText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.dimensions.smallSpacing),
                text = stringResource(
                    com.blockchain.stringResources.R.string.prove_instant_link_phone_and_dob_entry_cta_info
                ),
                style = ComposeTypographies.Caption1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Centre
            )

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
                onClick = { onIntent(ProvePrefillIntent.InstantLinkDataEntryContinueClicked) }
            )
        }
    }
}

@Preview
@Composable
private fun PreviewEmpty() {
    InstantLinkPhoneAndDobEntry(
        state = defaultViewState,
        onIntent = {},
        showDatePicker = {}
    )
}

@Preview
@Composable
private fun PreviewFilled() {
    InstantLinkPhoneAndDobEntry(
        state = defaultViewState.copy(
            mobileNumberInput = "2025550100",
            dateOfBirthInput = Calendar.getInstance().apply {
                set(1990, 1, 1)
            }
        ),
        onIntent = {},
        showDatePicker = {}
    )
}

data class PhoneVisualTransformation(
    val prefix: String?,
    val mask: String,
    val maskChar: Char
) : VisualTransformation {

    private val maxLength = mask.count { it == maskChar }

    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.length > maxLength) text.take(maxLength) else text

        val annotatedString = buildAnnotatedString {
            if (prefix != null) append(prefix)
            if (trimmed.isEmpty()) return@buildAnnotatedString

            var maskIndex = 0
            var textIndex = 0
            while (textIndex < trimmed.length && maskIndex < mask.length) {
                if (mask[maskIndex] != maskChar) {
                    val nextDigitIndex = mask.indexOf(maskChar, maskIndex)
                    append(mask.substring(maskIndex, nextDigitIndex))
                    maskIndex = nextDigitIndex
                }
                append(trimmed[textIndex++])
                maskIndex++
            }
        }

        return TransformedText(annotatedString, PhoneOffsetMapper(prefix, mask, maskChar))
    }

    companion object {
        val US by lazy {
            PhoneVisualTransformation("+1 ", "(000) 000-0000", '0')
        }
    }
}

private class PhoneOffsetMapper(
    val prefix: String?,
    val mask: String,
    val maskChar: Char
) : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int {
        var noneDigitCount = 0
        var i = 0
        while (i < offset + noneDigitCount) {
            if (mask[i++] != maskChar) {
                noneDigitCount++
            }
        }
        val prefixCount = prefix?.length ?: 0
        return prefixCount + offset + noneDigitCount
    }

    override fun transformedToOriginal(offset: Int): Int {
        val prefixCount = prefix?.length ?: 0
        val offsetWithoutPrefix = (offset - prefixCount).coerceAtLeast(0)
        return offsetWithoutPrefix - mask.take(offsetWithoutPrefix).count { it != maskChar }
    }
}
