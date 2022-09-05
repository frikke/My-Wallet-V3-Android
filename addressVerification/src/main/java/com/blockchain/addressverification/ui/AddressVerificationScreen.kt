package com.blockchain.addressverification.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.addressverification.R
import com.blockchain.addressverification.domain.model.AutocompleteAddress
import com.blockchain.addressverification.domain.model.AutocompleteAddressType
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
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.system.CircularProgressBar
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun AddressVerificationScreen(
    viewState: StateFlow<AddressVerificationState>,
    onIntent: (AddressVerificationIntent) -> Unit,
) {
    val state by viewState.collectAsStateLifecycleAware()

    Column(
        modifier = Modifier
            .padding(AppTheme.dimensions.paddingLarge)
            .fillMaxWidth()
    ) {
        OutlinedTextInput(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = AppTheme.dimensions.paddingMedium),
            value = state.mainLineInput,
            label = stringResource(R.string.address_verification_home_address),
            onValueChange = {
                onIntent(AddressVerificationIntent.MainLineInputChanged(it))
            },
            singleLine = true,
            placeholder = stringResource(R.string.address_verification_home_address),
            focusedTrailingIcon = ImageResource.Local(R.drawable.ic_close_circle),
            unfocusedTrailingIcon = ImageResource.None,
            onTrailingIconClicked = {
                onIntent(AddressVerificationIntent.MainLineInputChanged(TextFieldValue("")))
            },
        )

        when (state.step) {
            AddressVerificationStep.SEARCH -> SearchStep(state, onIntent)
            AddressVerificationStep.DETAILS -> DetailsStep(state, onIntent)
        }
    }
}

@Composable
private fun ColumnScope.SearchStep(
    state: AddressVerificationState,
    onIntent: (AddressVerificationIntent) -> Unit,
) {
    Box {
        if (state.isSearchLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressBar(modifier = Modifier.size(32.dp))
            }
        }

        val suggestions = state.results
        if (!state.areResultsHidden) {
            Column {
                SimpleText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onIntent(AddressVerificationIntent.ManualOverrideClicked)
                        }
                        .padding(vertical = AppTheme.dimensions.paddingSmall),
                    text = stringResource(R.string.address_verification_my_address_is_not_here),
                    style = ComposeTypographies.Body2,
                    color = ComposeColors.Primary,
                    gravity = ComposeGravities.Start
                )

                val listState = rememberLazyListState()

                LaunchedEffect(suggestions) {
                    listState.scrollToItem(0)
                }

                LazyColumn(
                    modifier = Modifier.padding(vertical = AppTheme.dimensions.paddingSmall),
                    state = listState,
                ) {
                    itemsIndexed(
                        items = suggestions,
                        itemContent = { index, suggestion ->
                            Box(
                                modifier = Modifier.clickable {
                                    onIntent(AddressVerificationIntent.ResultClicked(suggestion))
                                }
                            ) {
                                AutoCompleteItem(
                                    isContainer = suggestion.type != AutocompleteAddressType.ADDRESS,
                                    containedAddressesCount = suggestion.containedAddressesCount,
                                    isLoading = state.loadingAddressDetails == suggestion,
                                    title = suggestion.title,
                                    titleHighlightRange = suggestion.titleHighlightRanges,
                                    description = suggestion.description,
                                    descriptionHighlightRange = suggestion.descriptionHighlightRanges
                                )
                            }
                            if (index < suggestions.lastIndex)
                                HorizontalDivider(modifier = Modifier.fillMaxWidth())
                        }
                    )
                }

                if (suggestions.isEmpty()) {
                    SimpleText(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.common_no_results_found),
                        style = ComposeTypographies.Paragraph1,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Centre,
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.DetailsStep(
    state: AddressVerificationState,
    onIntent: (AddressVerificationIntent) -> Unit,
) {
    OutlinedTextInput(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = AppTheme.dimensions.paddingMedium),
        value = state.secondLineInput,
        label = stringResource(R.string.address_verification_apt_suite),
        onValueChange = {
            onIntent(AddressVerificationIntent.SecondLineInputChanged(it))
        },
        singleLine = true,
        placeholder = stringResource(R.string.address_verification_apt_suite)
    )

    OutlinedTextInput(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = AppTheme.dimensions.paddingMedium),
        value = state.cityInput,
        label = stringResource(R.string.address_verification_city),
        onValueChange = {
            onIntent(AddressVerificationIntent.CityInputChanged(it))
        },
        singleLine = true,
        placeholder = stringResource(R.string.address_verification_city)
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.paddingMedium)
    ) {
        if (state.isShowingStateInput) {
            OutlinedTextInput(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = AppTheme.dimensions.paddingMedium),
                value = state.stateInput,
                label = stringResource(R.string.address_verification_state),
                onValueChange = {},
                singleLine = true,
                placeholder = stringResource(R.string.address_verification_state),
                state = TextInputState.Disabled(),
            )
        }

        val postcodeLabel = if (state.isShowingStateInput) {
            stringResource(R.string.address_verification_zip)
        } else {
            stringResource(R.string.address_verification_postcode)
        }
        OutlinedTextInput(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = AppTheme.dimensions.paddingMedium),
            value = state.postCodeInput,
            label = postcodeLabel,
            onValueChange = {
                onIntent(AddressVerificationIntent.PostCodeInputChanged(it))
            },
            singleLine = true,
            placeholder = postcodeLabel,
            state = if (state.showPostcodeError) {
                TextInputState.Error(stringResource(R.string.address_verification_postcode_error))
            } else {
                TextInputState.Default()
            }
        )
    }

    OutlinedTextInput(
        modifier = Modifier.fillMaxWidth(),
        value = state.countryInput,
        label = stringResource(R.string.address_verification_country),
        onValueChange = {},
        singleLine = true,
        readOnly = true,
        placeholder = stringResource(R.string.address_verification_country),
        state = TextInputState.Disabled()
    )

    Spacer(Modifier.weight(1f))

    PrimaryButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(R.string.common_save),
        state = state.saveButtonState,
        onClick = { onIntent(AddressVerificationIntent.SaveClicked) },
    )
}

@Composable
fun AutoCompleteItem(
    isContainer: Boolean,
    containedAddressesCount: Int?,
    isLoading: Boolean,
    title: String,
    titleHighlightRange: List<IntRange>,
    description: String,
    descriptionHighlightRange: List<IntRange>
) {
    val titleHighlights = titleHighlightRange.map { range ->
        AnnotatedString.Range(SpanStyle(fontWeight = FontWeight.Bold), range.start, range.endInclusive + 1)
    }

    val descriptionHighlights = descriptionHighlightRange.map { range ->
        AnnotatedString.Range(SpanStyle(fontWeight = FontWeight.Bold), range.start, range.endInclusive + 1)
    }

    Row(
        modifier = Modifier.padding(vertical = AppTheme.dimensions.paddingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            SimpleText(
                modifier = Modifier.fillMaxWidth(),
                text = AnnotatedString(
                    text = title,
                    spanStyles = titleHighlights
                ),
                style = ComposeTypographies.Body1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Start
            )

            Spacer(modifier = Modifier.padding(vertical = 4.dp))

            SimpleText(
                modifier = Modifier.fillMaxWidth(),
                text = AnnotatedString(
                    text = description,
                    spanStyles = descriptionHighlights
                ),
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Start
            )
        }

        if (isContainer) {
            SimpleText(
                modifier = Modifier.padding(start = AppTheme.dimensions.xPaddingSmall),
                text = stringResource(R.string.address_verification_addresses_count, containedAddressesCount ?: 0),
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.End
            )
            Image(
                modifier = Modifier.padding(start = AppTheme.dimensions.paddingMedium),
                imageResource = ImageResource.Local(R.drawable.ic_arrow_right)
            )
        } else {
            // Always put the loading in the screen and change the alpha so the space is reserved
            // and the text doesn't jump around
            CircularProgressBar(
                Modifier
                    .padding(start = AppTheme.dimensions.paddingMedium)
                    .size(24.dp)
                    .alpha(if (isLoading) 1f else 0f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSearchScreen() {
    val suggestions = listOf(
        AutocompleteAddress(
            id = "1330 Rivera Court",
            type = AutocompleteAddressType.ADDRESS,
            title = "1330 Rivera Court",
            titleHighlightRanges = listOf(IntRange(0, 9)),
            description = "Santa Rosa, CA",
            descriptionHighlightRanges = listOf(),
            containedAddressesCount = null,
        ),
        AutocompleteAddress(
            id = "1330 River Park Boulevard",
            type = AutocompleteAddressType.STREET,
            title = "1330 River Park Boulevard",
            titleHighlightRanges = listOf(IntRange(0, 9)),
            description = "Napa, CA",
            descriptionHighlightRanges = listOf(),
            containedAddressesCount = 13,
        ),
        AutocompleteAddress(
            id = "1330 River Park Boulevard LOADING",
            type = AutocompleteAddressType.STREET,
            title = "1330 River Park Boulevard LOADING",
            titleHighlightRanges = listOf(IntRange(0, 9)),
            description = "Napa, CA",
            descriptionHighlightRanges = listOf(),
            containedAddressesCount = 67,
        ),
        AutocompleteAddress(
            id = "1330 River Road",
            type = AutocompleteAddressType.OTHER,
            title = "1330 River Road",
            titleHighlightRanges = listOf(IntRange(0, 9)),
            description = "Modesto, CA",
            descriptionHighlightRanges = listOf(),
            containedAddressesCount = 12,
        )
    )

    val state = AddressVerificationState(
        step = AddressVerificationStep.SEARCH,
        mainLineInput = TextFieldValue("1330 River"),
        areResultsHidden = false,
        results = suggestions,
        isSearchLoading = true,
        loadingAddressDetails = suggestions[2],
        error = null,
        secondLineInput = "",
        cityInput = "",
        isShowingStateInput = false,
        stateInput = "",
        showPostcodeError = false,
        postCodeInput = "",
        countryInput = "",
        saveButtonState = ButtonState.Disabled,
    )

    AddressVerificationScreen(
        viewState = MutableStateFlow(state),
        onIntent = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewSearchScreenEmptyState() {
    val state = AddressVerificationState(
        step = AddressVerificationStep.SEARCH,
        mainLineInput = TextFieldValue("1330 River"),
        areResultsHidden = false,
        results = emptyList(),
        isSearchLoading = false,
        loadingAddressDetails = null,
        error = null,
        secondLineInput = "",
        cityInput = "",
        isShowingStateInput = false,
        stateInput = "",
        showPostcodeError = false,
        postCodeInput = "",
        countryInput = "",
        saveButtonState = ButtonState.Disabled,
    )

    AddressVerificationScreen(
        viewState = MutableStateFlow(state),
        onIntent = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewDetailsScreen() {
    val state = AddressVerificationState(
        step = AddressVerificationStep.DETAILS,
        mainLineInput = TextFieldValue("1330 River"),
        areResultsHidden = false,
        results = emptyList(),
        isSearchLoading = false,
        loadingAddressDetails = null,
        error = null,
        secondLineInput = "10 West",
        cityInput = "Brooklyn",
        isShowingStateInput = true,
        stateInput = "New York",
        showPostcodeError = false,
        postCodeInput = "13200",
        countryInput = "United States",
        saveButtonState = ButtonState.Enabled,
    )

    AddressVerificationScreen(
        viewState = MutableStateFlow(state),
        onIntent = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewAutoCompleteItemWithHighlight() {
    AutoCompleteItem(
        isContainer = false,
        containedAddressesCount = null,
        isLoading = false,
        title = "Lisbone",
        titleHighlightRange = listOf(IntRange(0, 1), IntRange(2, 4), IntRange(5, 6)),
        description = "Lisbon is the capital of Portugal",
        descriptionHighlightRange = listOf(IntRange(0, 1), IntRange(2, 4), IntRange(5, 6))
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewAutoCompleteContainerItemWithHighlight() {
    AutoCompleteItem(
        isContainer = true,
        containedAddressesCount = 32,
        isLoading = true,
        title = "Lisbone",
        titleHighlightRange = listOf(IntRange(0, 1), IntRange(2, 4), IntRange(5, 6)),
        description = "Lisbon is the capital of Portugal",
        descriptionHighlightRange = listOf(IntRange(0, 1), IntRange(2, 4), IntRange(5, 6))
    )
}
