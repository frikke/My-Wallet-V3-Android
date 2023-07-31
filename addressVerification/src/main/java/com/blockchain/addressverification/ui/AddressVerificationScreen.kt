package com.blockchain.addressverification.ui

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.blockchain.addressverification.domain.model.AutocompleteAddress
import com.blockchain.addressverification.domain.model.AutocompleteAddressType
import com.blockchain.commonarch.presentation.mvi_v2.compose.bindViewModel
import com.blockchain.componentlib.basic.AppDivider
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.basic.closeImageResource
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.controls.OutlinedTextInput
import com.blockchain.componentlib.controls.TextInputState
import com.blockchain.componentlib.icons.ChevronRight
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.loader.LoadingIndicator
import com.blockchain.componentlib.system.CircularProgressBar
import com.blockchain.componentlib.system.DialogueButton
import com.blockchain.componentlib.system.DialogueCard
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BackHandlerCallback
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.componentlib.utils.rememberFlow
import com.blockchain.koin.payloadScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.koin.androidx.compose.getViewModel

abstract class AddressVerificationHost {

    // Used by the host to communicate back into AddressVerification that the address was not valid for some reason
    // This will reenable the button that was previously in a loading state and show the error
    open val errorWhileSaving: Flow<AddressVerificationSavingError> = MutableSharedFlow()

    abstract fun launchContactSupport()
    abstract fun addressVerifiedSuccessfully(address: AddressDetails)
}

@Composable
fun AddressVerificationScreen(
    args: Args,
    isVerifyAddressLoadingOverride: Boolean,
    host: AddressVerificationHost
) {
    val context = LocalContext.current

    val viewModel: AddressVerificationModel = getViewModel(scope = payloadScope)
    val state by viewModel.viewState.collectAsStateLifecycleAware()
    val onIntent = viewModel::onIntent

    val backCallback = BackHandlerCallback {
        onIntent(AddressVerificationIntent.BackClicked)
    }

    bindViewModel(viewModel, args) { navigation ->
        when (navigation) {
            Navigation.Back -> {
                backCallback.remove()
                (context as FragmentActivity).onBackPressedDispatcher.onBackPressed()
            }

            is Navigation.FinishSuccessfully -> host.addressVerifiedSuccessfully(navigation.address)
            Navigation.LaunchSupport -> host.launchContactSupport()
        }
    }

    val errorWhileSavingFlow = rememberFlow(host.errorWhileSaving)
    LaunchedEffect(Unit) {
        errorWhileSavingFlow.collect {
            onIntent(AddressVerificationIntent.ErrorWhileSaving(it))
        }
    }

    Content(state, isVerifyAddressLoadingOverride, onIntent)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Content(
    state: AddressVerificationState,
    isVerifyAddressLoadingOverride: Boolean,
    onIntent: (AddressVerificationIntent) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()
    Scaffold(scaffoldState = scaffoldState) { padding ->
        LaunchedEffect(state.error) {
            val error = state.error
            if (error != null) {
                keyboardController?.hide()
                scaffoldState.snackbarHostState.showSnackbar(
                    message = error.errorMessage(context),
                    duration = SnackbarDuration.Long
                )
                onIntent(AddressVerificationIntent.ErrorHandled)
            }
        }

        if (state.showInvalidStateErrorDialog) {
            DialogueCard(
                title = stringResource(
                    com.blockchain.stringResources.R.string.address_verification_invalid_state_title
                ),
                body = stringResource(
                    com.blockchain.stringResources.R.string.address_verification_invalid_state_message
                ),
                firstButton = DialogueButton(stringResource(com.blockchain.stringResources.R.string.contact_support)) {
                    onIntent(AddressVerificationIntent.InvalidStateErrorHandled)
                    onIntent(AddressVerificationIntent.LaunchSupportClicked)
                },
                secondButton = DialogueButton(stringResource(com.blockchain.stringResources.R.string.common_cancel)) {
                    onIntent(AddressVerificationIntent.InvalidStateErrorHandled)
                },
                onDismissRequest = { onIntent(AddressVerificationIntent.InvalidStateErrorHandled) }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.background)
                .padding(padding)
                .padding(AppTheme.dimensions.standardSpacing)
        ) {
            when (state.step) {
                AddressVerificationStep.SEARCH -> SearchStep(state, onIntent)
                AddressVerificationStep.DETAILS -> DetailsStep(state, isVerifyAddressLoadingOverride, onIntent)
            }
        }
    }
}

@Composable
private fun ColumnScope.SearchStep(
    state: AddressVerificationState,
    onIntent: (AddressVerificationIntent) -> Unit
) {
    val searchInputIcon =
        if (state.searchInput.text.isNotEmpty()) {
            closeImageResource()
        } else {
            ImageResource.None
        }
    OutlinedTextInput(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = AppTheme.dimensions.smallSpacing),
        value = state.searchInput,
        label = stringResource(com.blockchain.stringResources.R.string.address_verification_home_address),
        onValueChange = {
            onIntent(AddressVerificationIntent.SearchInputChanged(it))
        },
        singleLine = true,
        placeholder = stringResource(com.blockchain.stringResources.R.string.address_verification_home_address),
        focusedTrailingIcon = searchInputIcon,
        unfocusedTrailingIcon = searchInputIcon,
        onTrailingIconClicked = {
            onIntent(AddressVerificationIntent.SearchInputChanged(TextFieldValue("")))
        }
    )

    Box {
        if (state.isSearchLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressBar(modifier = Modifier.size(32.dp))
            }
        }

        Column {
            if (state.showManualOverride) {
                SimpleText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onIntent(AddressVerificationIntent.ManualOverrideClicked)
                        }
                        .padding(vertical = AppTheme.dimensions.tinySpacing),
                    text = stringResource(
                        com.blockchain.stringResources.R.string.address_verification_my_address_is_not_here
                    ),
                    style = ComposeTypographies.Body2,
                    color = ComposeColors.Primary,
                    gravity = ComposeGravities.Start
                )
            }

            val listState = rememberLazyListState()
            val suggestions = state.results

            LaunchedEffect(suggestions) {
                listState.scrollToItem(0)
            }

            LazyColumn(
                modifier = Modifier.padding(vertical = AppTheme.dimensions.tinySpacing),
                state = listState
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
                        if (index < suggestions.lastIndex) {
                            AppDivider(color = AppColors.medium)
                        }
                    }
                )
            }

            if (suggestions.isEmpty() && state.searchInput.text.isNotEmpty() && !state.isSearchLoading) {
                SimpleText(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(com.blockchain.stringResources.R.string.common_no_results_found),
                    style = ComposeTypographies.Paragraph1,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Centre
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.DetailsStep(
    state: AddressVerificationState,
    isVerifyAddressLoadingOverride: Boolean,
    onIntent: (AddressVerificationIntent) -> Unit
) {
    OutlinedTextInput(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = AppTheme.dimensions.smallSpacing),
        value = state.mainLineInput,
        label = stringResource(com.blockchain.stringResources.R.string.address_verification_home_address),
        onValueChange = {
            onIntent(AddressVerificationIntent.MainLineInputChanged(it))
        },
        singleLine = true,
        placeholder = stringResource(com.blockchain.stringResources.R.string.address_verification_home_address),
        state = if (state.isMainLineInputEnabled) {
            TextInputState.Default(null)
        } else {
            // TODO(labreu): Check if we want to show an explainer text as to why the user cannot change the address
            TextInputState.Disabled(null)
        }
    )

    OutlinedTextInput(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = AppTheme.dimensions.smallSpacing),
        value = state.secondLineInput,
        label = stringResource(com.blockchain.stringResources.R.string.address_verification_apt_suite),
        onValueChange = {
            onIntent(AddressVerificationIntent.SecondLineInputChanged(it))
        },
        singleLine = true,
        placeholder = stringResource(com.blockchain.stringResources.R.string.address_verification_apt_suite)
    )

    OutlinedTextInput(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = AppTheme.dimensions.smallSpacing),
        value = state.cityInput,
        label = stringResource(com.blockchain.stringResources.R.string.address_verification_city),
        onValueChange = {
            onIntent(AddressVerificationIntent.CityInputChanged(it))
        },
        singleLine = true,
        placeholder = stringResource(com.blockchain.stringResources.R.string.address_verification_city)
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.smallSpacing)
    ) {
        if (state.isShowingStateInput) {
            OutlinedTextInput(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = AppTheme.dimensions.smallSpacing),
                value = state.stateInput,
                label = stringResource(com.blockchain.stringResources.R.string.address_verification_state),
                onValueChange = {},
                singleLine = true,
                placeholder = stringResource(com.blockchain.stringResources.R.string.address_verification_state),
                state = TextInputState.Disabled()
            )
        }

        val postcodeLabel = if (state.isShowingStateInput) {
            stringResource(com.blockchain.stringResources.R.string.address_verification_zip)
        } else {
            stringResource(com.blockchain.stringResources.R.string.address_verification_postcode)
        }
        OutlinedTextInput(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = AppTheme.dimensions.smallSpacing),
            value = state.postCodeInput,
            label = postcodeLabel,
            onValueChange = {
                onIntent(AddressVerificationIntent.PostCodeInputChanged(it))
            },
            singleLine = true,
            placeholder = postcodeLabel,
            state = if (state.showPostcodeError) {
                TextInputState.Error(
                    stringResource(com.blockchain.stringResources.R.string.address_verification_postcode_error)
                )
            } else {
                TextInputState.Default()
            }
        )
    }

    OutlinedTextInput(
        modifier = Modifier.fillMaxWidth(),
        value = state.countryInput,
        label = stringResource(com.blockchain.stringResources.R.string.address_verification_country),
        onValueChange = {},
        singleLine = true,
        readOnly = true,
        placeholder = stringResource(com.blockchain.stringResources.R.string.address_verification_country),
        state = TextInputState.Disabled()
    )

    Spacer(Modifier.weight(1f))

    PrimaryButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(com.blockchain.stringResources.R.string.common_save),
        state = if (isVerifyAddressLoadingOverride) ButtonState.Loading else state.saveButtonState,
        onClick = { onIntent(AddressVerificationIntent.SaveClicked) }
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
        modifier = Modifier.padding(vertical = AppTheme.dimensions.smallSpacing),
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
                modifier = Modifier.padding(start = AppTheme.dimensions.smallestSpacing),
                text = stringResource(
                    com.blockchain.stringResources.R.string.address_verification_addresses_count,
                    containedAddressesCount ?: 0
                ),
                style = ComposeTypographies.Paragraph1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.End
            )
            Image(
                modifier = Modifier.padding(start = AppTheme.dimensions.smallSpacing),
                imageResource = Icons.ChevronRight.withTint(AppColors.muted)
            )
        } else {
            // Always put the loading in the screen and change the alpha so the space is reserved
            // and the text doesn't jump around
            LoadingIndicator(
                modifier = Modifier
                    .padding(start = AppTheme.dimensions.smallSpacing)
                    .alpha(if (isLoading) 1f else 0f),
                color = AppColors.primary
            )
        }
    }
}

private fun AddressVerificationError.errorMessage(context: Context): String = when (this) {
    is AddressVerificationError.Unknown -> message ?: context.getString(
        com.blockchain.stringResources.R.string.common_error
    )
}

@Preview
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
            containedAddressesCount = null
        ),
        AutocompleteAddress(
            id = "1330 River Park Boulevard",
            type = AutocompleteAddressType.STREET,
            title = "1330 River Park Boulevard",
            titleHighlightRanges = listOf(IntRange(0, 9)),
            description = "Napa, CA",
            descriptionHighlightRanges = listOf(),
            containedAddressesCount = 13
        ),
        AutocompleteAddress(
            id = "1330 River Park Boulevard LOADING",
            type = AutocompleteAddressType.STREET,
            title = "1330 River Park Boulevard LOADING",
            titleHighlightRanges = listOf(IntRange(0, 9)),
            description = "Napa, CA",
            descriptionHighlightRanges = listOf(),
            containedAddressesCount = 67
        ),
        AutocompleteAddress(
            id = "1330 River Road",
            type = AutocompleteAddressType.OTHER,
            title = "1330 River Road",
            titleHighlightRanges = listOf(IntRange(0, 9)),
            description = "Modesto, CA",
            descriptionHighlightRanges = listOf(),
            containedAddressesCount = 12
        )
    )

    val state = AddressVerificationState(
        step = AddressVerificationStep.SEARCH,
        searchInput = TextFieldValue("1330 River"),
        areResultsHidden = false,
        showManualOverride = false,
        results = suggestions,
        isSearchLoading = true,
        loadingAddressDetails = suggestions[2],
        showInvalidStateErrorDialog = false,
        error = null,
        mainLineInput = "",
        isMainLineInputEnabled = true,
        secondLineInput = "",
        cityInput = "",
        isShowingStateInput = false,
        stateInput = "",
        showPostcodeError = false,
        postCodeInput = "",
        countryInput = "",
        saveButtonState = ButtonState.Disabled
    )

    Content(
        state = state,
        isVerifyAddressLoadingOverride = false,
        onIntent = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSearchScreenDark() {
    PreviewSearchScreen()
}

@Preview(showBackground = true)
@Composable
fun PreviewSearchScreenEmptyState() {
    val state = AddressVerificationState(
        step = AddressVerificationStep.SEARCH,
        searchInput = TextFieldValue("1330 River"),
        areResultsHidden = false,
        showManualOverride = true,
        results = emptyList(),
        isSearchLoading = false,
        loadingAddressDetails = null,
        showInvalidStateErrorDialog = false,
        error = null,
        mainLineInput = "",
        isMainLineInputEnabled = true,
        secondLineInput = "",
        cityInput = "",
        isShowingStateInput = false,
        stateInput = "",
        showPostcodeError = false,
        postCodeInput = "",
        countryInput = "",
        saveButtonState = ButtonState.Disabled
    )

    Content(
        state = state,
        isVerifyAddressLoadingOverride = false,
        onIntent = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSearchScreenEmptyStateDark() {
    PreviewSearchScreenEmptyState()
}

@Preview
@Composable
fun PreviewDetailsScreen() {
    val state = AddressVerificationState(
        step = AddressVerificationStep.DETAILS,
        searchInput = TextFieldValue("1330 River"),
        areResultsHidden = false,
        showManualOverride = true,
        results = emptyList(),
        isSearchLoading = false,
        loadingAddressDetails = null,
        showInvalidStateErrorDialog = false,
        error = null,
        mainLineInput = "1330 River",
        isMainLineInputEnabled = true,
        secondLineInput = "10 West",
        cityInput = "Brooklyn",
        isShowingStateInput = true,
        stateInput = "New York",
        showPostcodeError = false,
        postCodeInput = "13200",
        countryInput = "United States",
        saveButtonState = ButtonState.Enabled
    )

    Content(
        state = state,
        isVerifyAddressLoadingOverride = false,
        onIntent = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewDetailsScreenDark() {
    PreviewDetailsScreen()
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
