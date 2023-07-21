package com.blockchain.kycproviders.prove.presentation.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.toSize
import com.blockchain.addressverification.ui.AddressDetails
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.controls.OutlinedTextInput
import com.blockchain.componentlib.controls.TextInputState
import com.blockchain.componentlib.icons.ChevronDown
import com.blockchain.componentlib.icons.ChevronUp
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ViewPrefillData(
    state: ProvePrefillViewState,
    onIntent: (ProvePrefillIntent) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val localFocusManager = LocalFocusManager.current

    Column(
        Modifier.background(AppColors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(
                    top = AppTheme.dimensions.xHugeSpacing,
                    bottom = AppTheme.dimensions.standardSpacing
                ),
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
                text = stringResource(com.blockchain.stringResources.R.string.prove_view_prefill_data_title),
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
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
                value = state.prefillFirstNameInput,
                label = stringResource(com.blockchain.stringResources.R.string.first_name),
                placeholder = stringResource(
                    com.blockchain.stringResources.R.string.prove_view_prefill_data_first_name_placeholder
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    capitalization = KeyboardCapitalization.Words
                ),
                keyboardActions = KeyboardActions(onNext = { localFocusManager.moveFocus(FocusDirection.Next) }),
                onValueChange = { value -> onIntent(ProvePrefillIntent.PrefillFirstNameInputChanged(value)) }
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
                value = state.prefillLastNameInput,
                label = stringResource(com.blockchain.stringResources.R.string.last_name),
                placeholder = stringResource(
                    com.blockchain.stringResources.R.string.prove_view_prefill_data_last_name_placeholder
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    capitalization = KeyboardCapitalization.Words
                ),
                keyboardActions = KeyboardActions(onNext = {
                    keyboardController?.hide()
                    localFocusManager.clearFocus(force = true)
                    onIntent(ProvePrefillIntent.PrefillAddressClicked)
                }),
                onValueChange = { value -> onIntent(ProvePrefillIntent.PrefillLastNameInputChanged(value)) }
            )

            val addresses = listOfNotNull(state.manualEntryAddress) + state.prefillAddresses
            // This is so we're able to fix the DropdownMenu width correctly as it does not respect .fillMaxWidth()
            var boxSize by remember { mutableStateOf(Size.Zero) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = AppTheme.dimensions.standardSpacing,
                        start = AppTheme.dimensions.smallSpacing,
                        end = AppTheme.dimensions.smallSpacing
                    )
                    .onGloballyPositioned {
                        boxSize = it.size.toSize()
                    }
            ) {
                val icon = when {
                    addresses.size <= 1 -> ImageResource.None
                    state.isAddressDropdownOpen -> Icons.ChevronUp.withTint(AppColors.title)
                    else -> Icons.ChevronDown.withTint(AppColors.title)
                }
                // We're hiding the label because otherwise the placeholder would not show up and we want it to show up when
                // the field is empty, placeholder only shows when focused, and this view because it's viewonly cannot be focused
                val label = if (state.prefillSelectedAddress != null) {
                    stringResource(com.blockchain.stringResources.R.string.prove_view_prefill_data_address_label)
                } else {
                    null
                }
                val placeholder = if (addresses.isEmpty()) {
                    stringResource(
                        com.blockchain.stringResources.R.string.prove_view_prefill_data_address_placeholder_enter
                    )
                } else {
                    stringResource(
                        com.blockchain.stringResources.R.string.prove_view_prefill_data_address_placeholder_select
                    )
                }
                OutlinedTextInput(
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    value = state.prefillSelectedAddress?.asString().orEmpty(),
                    label = label,
                    placeholder = placeholder,
                    readOnly = true,
                    unfocusedTrailingIcon = icon,
                    focusedTrailingIcon = icon,
                    onValueChange = {
                        // no op readonly
                    }
                )

                if (!state.isAddressDropdownOpen) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .clickable { onIntent(ProvePrefillIntent.PrefillAddressClicked) }
                    )
                }
                DropdownMenu(
                    modifier = Modifier.width(with(LocalDensity.current) { boxSize.width.toDp() }),
                    expanded = state.isAddressDropdownOpen,
                    onDismissRequest = { onIntent(ProvePrefillIntent.PrefillAddressDropdownClosed) }
                ) {
                    addresses.forEach { address ->
                        DropdownMenuItem(
                            onClick = { onIntent(ProvePrefillIntent.PrefillAddressSelected(address)) }
                        ) {
                            Text(address.asString(), color = AppTheme.colors.title)
                        }
                    }
                }
            }

            val isEnterManuallyVisible = state.prefillAddresses.isNotEmpty()
            if (isEnterManuallyVisible) {
                Row(
                    modifier = Modifier.padding(
                        vertical = AppTheme.dimensions.tinySpacing,
                        horizontal = AppTheme.dimensions.smallSpacing
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SimpleText(
                        text = stringResource(
                            com.blockchain.stringResources.R.string.prove_view_prefill_data_address_manual_or
                        ),
                        style = ComposeTypographies.Caption1,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Start
                    )

                    Box(
                        Modifier
                            .padding(vertical = AppTheme.dimensions.tinySpacing)
                            .clickable { onIntent(ProvePrefillIntent.PrefillAddressEnterManuallyClicked) }
                    ) {
                        SimpleText(
                            text = stringResource(
                                com.blockchain.stringResources.R
                                    .string.prove_view_prefill_data_address_manual_enter_manually
                            ),
                            style = ComposeTypographies.Caption1,
                            color = ComposeColors.Primary,
                            gravity = ComposeGravities.Start
                        )
                    }
                }
            }

            val dobDisplayFormat = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.US) }
            val dob = state.prefillDob?.let { dobDisplayFormat.format(it.time) }.orEmpty()
            OutlinedTextInput(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = if (isEnterManuallyVisible) {
                            AppTheme.dimensions.tinySpacing
                        } else {
                            AppTheme.dimensions.standardSpacing
                        },
                        start = AppTheme.dimensions.smallSpacing,
                        end = AppTheme.dimensions.smallSpacing
                    ),
                singleLine = true,
                value = dob,
                label = stringResource(com.blockchain.stringResources.R.string.kyc_profile_dob_hint),
                readOnly = true,
                state = TextInputState.Disabled(),
                onValueChange = {
                    // no op readonly
                }
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
                    com.blockchain.stringResources.R.string.prove_view_prefill_data_cannot_be_modifier
                ),
                style = ComposeTypographies.Caption1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Start
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
                value = state.prefillMobileNumber,
                label = stringResource(com.blockchain.stringResources.R.string.prove_phone_number),
                readOnly = true,
                state = TextInputState.Disabled(),
                onValueChange = {
                    // no op readonly
                }
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
                    com.blockchain.stringResources.R.string.prove_view_prefill_data_cannot_be_modifier
                ),
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
            text = stringResource(com.blockchain.stringResources.R.string.common_continue),
            state = state.prefillContinueButtonState,
            onClick = { onIntent(ProvePrefillIntent.PrefillContinueClicked) }
        )
    }
}

private fun AddressDetails.asString(): String = "$firstLine $secondLine, $postCode, ${stateIso.orEmpty()}"

@Preview
@Composable
private fun PreviewMultipleAddressesUnselected() {
    ViewPrefillData(
        state = defaultViewState.copy(
            prefillDob = Calendar.getInstance().apply {
                set(1990, 1, 1)
            },
            prefillMobileNumber = "+1-202-555-0100",
            prefillAddresses = listOf(address1, address2),
            prefillSelectedAddress = null
        ),
        onIntent = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewMultipleAddressesUnselectedDark() {
    PreviewMultipleAddressesUnselected()
}

@Preview
@Composable
private fun PreviewMultipleAddressesUnselectedDropdownOpen() {
    ViewPrefillData(
        state = defaultViewState.copy(
            prefillDob = Calendar.getInstance().apply {
                set(1990, 1, 1)
            },
            prefillMobileNumber = "+1-202-555-0100",
            prefillAddresses = listOf(address1, address2),
            prefillSelectedAddress = null,
            isAddressDropdownOpen = true
        ),
        onIntent = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewMultipleAddressesUnselectedDropdownOpenDark() {
    PreviewMultipleAddressesUnselectedDropdownOpen()
}

@Preview
@Composable
private fun PreviewMultipleAddressesWithSelected() {
    ViewPrefillData(
        state = defaultViewState.copy(
            prefillDob = Calendar.getInstance().apply {
                set(1990, 1, 1)
            },
            prefillMobileNumber = "+1-202-555-0100",
            prefillAddresses = listOf(address1, address2),
            prefillSelectedAddress = address1
        ),
        onIntent = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewMultipleAddressesWithSelectedDark() {
    PreviewMultipleAddressesWithSelected()
}

@Preview
@Composable
private fun PreviewOnlyOneAddress() {
    ViewPrefillData(
        state = defaultViewState.copy(
            prefillFirstNameInput = "John",
            prefillLastNameInput = "Doe",
            prefillDob = Calendar.getInstance().apply {
                set(1990, 1, 1)
            },
            prefillMobileNumber = "+1-202-555-0100",
            prefillAddresses = listOf(address1),
            prefillSelectedAddress = address1
        ),
        onIntent = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewOnlyOneAddressDark() {
    PreviewOnlyOneAddress()
}

@Preview
@Composable
private fun PreviewNoAddresses() {
    ViewPrefillData(
        state = defaultViewState.copy(
            prefillDob = Calendar.getInstance().apply {
                set(1990, 1, 1)
            },
            prefillMobileNumber = "+1-202-555-0100",
            prefillAddresses = emptyList()
        ),
        onIntent = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewNoAddressesDark() {
    PreviewNoAddresses()
}

@Preview
@Composable
private fun PreviewComplete() {
    ViewPrefillData(
        state = defaultViewState.copy(
            prefillFirstNameInput = "John",
            prefillLastNameInput = "Doe",
            prefillDob = Calendar.getInstance().apply {
                set(1990, 1, 1)
            },
            prefillMobileNumber = "+1-202-555-0100",
            prefillAddresses = listOf(address1, address2),
            prefillSelectedAddress = address1,
            prefillContinueButtonState = ButtonState.Enabled
        ),
        onIntent = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCompleteDark() {
    PreviewComplete()
}

private val address1 = AddressDetails(
    firstLine = "622 Golden Ridge Road",
    secondLine = "19 E",
    city = "Albany",
    postCode = "12207",
    countryIso = "US",
    stateIso = null
)

private val address2 = AddressDetails(
    firstLine = "3288 Custer Street",
    secondLine = "Corner Sq",
    city = "Farmerville",
    postCode = "71241",
    countryIso = "US",
    stateIso = null
)
