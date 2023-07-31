package com.blockchain.addressverification.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewModelScope
import com.blockchain.addressverification.domain.AddressVerificationService
import com.blockchain.addressverification.domain.model.AutocompleteAddress
import com.blockchain.addressverification.domain.model.AutocompleteAddressType
import com.blockchain.commonarch.presentation.mvi_v2.ModelConfigArgs
import com.blockchain.commonarch.presentation.mvi_v2.MviViewModel
import com.blockchain.commonarch.presentation.mvi_v2.NavigationEvent
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.domain.common.model.CountryIso
import com.blockchain.domain.common.model.StateIso
import com.blockchain.outcome.doOnFailure
import com.blockchain.outcome.doOnSuccess
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

sealed class AddressVerificationError {
    data class Unknown(val message: String?) : AddressVerificationError()
}

// Used by the host to communicate back into AddressVerification that the address was not valid for some reason
sealed class AddressVerificationSavingError {
    object InvalidPostCode : AddressVerificationSavingError()
    data class Unknown(val message: String?) : AddressVerificationSavingError()
}

sealed class Navigation : NavigationEvent {
    data class FinishSuccessfully(val address: AddressDetails) : Navigation()
    object LaunchSupport : Navigation()
    object Back : Navigation()
}

@Parcelize
data class Args(
    val countryIso: CountryIso,
    val stateIso: StateIso?,
    val prefilledAddress: AddressDetails?,
    val allowManualOverride: Boolean
) : ModelConfigArgs.ParcelableArgs

class AddressVerificationModel(
    private val addressVerificationService: AddressVerificationService
) : MviViewModel<
    AddressVerificationIntent,
    AddressVerificationState,
    AddressVerificationModelState,
    Navigation,
    Args
    >(AddressVerificationModelState()) {

    private lateinit var countryIso: CountryIso
    private var state: USState? = null
    private var allowManualOverride: Boolean = false

    override fun viewCreated(args: Args) {
        countryIso = args.countryIso
        state = args.stateIso?.let { USState.findStateByIso(it) }
        allowManualOverride = args.allowManualOverride

        val prefilledAddress = args.prefilledAddress

        if (prefilledAddress != null) {
            updateState {
                copy(
                    isShowingStateInput = countryIso == "US",
                    countryInput = Locale("", countryIso).displayCountry,
                    stateInput = state?.displayName.orEmpty(),
                    step = AddressVerificationStep.DETAILS,
                    searchInput = TextFieldValue(
                        prefilledAddress.firstLine,
                        TextRange(prefilledAddress.firstLine.length)
                    ),
                    mainLineInput = prefilledAddress.firstLine,
                    secondLineInput = prefilledAddress.secondLine.orEmpty(),
                    cityInput = prefilledAddress.city,
                    postCodeInput = prefilledAddress.postCode
                )
            }
        } else {
            updateState {
                copy(
                    isShowingStateInput = countryIso == "US",
                    countryInput = Locale("", countryIso).displayCountry,
                    stateInput = state?.displayName.orEmpty()
                )
            }
        }
    }

    override fun AddressVerificationModelState.reduce() = AddressVerificationState(
        step = step,
        searchInput = searchInput,
        isSearchLoading = isSearchLoading,
        error = error,
        areResultsHidden = areResultsHidden,
        showManualOverride = allowManualOverride && showManualOverride,
        results = results,
        loadingAddressDetails = loadingAddressDetails,
        showInvalidStateErrorDialog = showInvalidStateErrorDialog,
        mainLineInput = mainLineInput,
        isMainLineInputEnabled = allowManualOverride,
        secondLineInput = secondLineInput,
        cityInput = cityInput,
        isShowingStateInput = isShowingStateInput,
        stateInput = stateInput,
        showPostcodeError = showPostcodeError,
        postCodeInput = postCodeInput,
        countryInput = countryInput,
        saveButtonState = if (
            mainLineInput.isNotBlank() &&
            cityInput.isNotBlank() &&
            !(isShowingStateInput && stateInput.isBlank()) &&
            postCodeInput.isNotBlank()
        ) {
            ButtonState.Enabled
        } else {
            ButtonState.Disabled
        }
    )

    private val MIN_QUERY_LENGTH = 4

    private var searchQueryJob: Job? = null
    private var fetchAddressDetailsJob: Job? = null

    override suspend fun handleIntent(
        modelState: AddressVerificationModelState,
        intent: AddressVerificationIntent
    ) {
        when (intent) {
            AddressVerificationIntent.ErrorHandled -> updateState { copy(error = null) }
            AddressVerificationIntent.InvalidStateErrorHandled ->
                updateState { copy(showInvalidStateErrorDialog = false) }
            AddressVerificationIntent.LaunchSupportClicked -> navigate(Navigation.LaunchSupport)
            is AddressVerificationIntent.SearchInputChanged -> {
                // If just the selection changes we just want to update the state and do nothing else
                if (modelState.searchInput.text == intent.newInput.text) {
                    updateState { copy(searchInput = intent.newInput) }
                    return
                }
                cancelSearchForAddresses()
                cancelFetchAddressDetails()

                // If the user starts erasing characters from the query we remove the container
                val newContainer = modelState.container?.takeIf { container ->
                    intent.newInput.text.startsWith(container.title + " ")
                }
                updateState {
                    copy(
                        searchInput = intent.newInput,
                        container = newContainer
                    )
                }

                if (intent.newInput.text.isEmpty()) {
                    updateState {
                        copy(results = emptyList())
                    }
                } else if (intent.newInput.text.length < MIN_QUERY_LENGTH) {
                    updateState {
                        copy(areResultsHidden = true, isSearchLoading = false, showManualOverride = false)
                    }
                } else {
                    searchForAddresses(intent.newInput.text, newContainer?.id)
                }
            }
            is AddressVerificationIntent.ResultClicked -> {
                cancelSearchForAddresses()
                cancelFetchAddressDetails()
                if (intent.result.type == AutocompleteAddressType.ADDRESS) {
                    fetchAddressDetails(intent.result)
                } else {
                    val newInputText = intent.result.title + " "
                    val newInput = TextFieldValue(text = newInputText, selection = TextRange(newInputText.length))
                    updateState {
                        copy(
                            searchInput = newInput,
                            container = intent.result
                        )
                    }
                    searchForAddresses(newInput.text, intent.result.id)
                }
            }
            is AddressVerificationIntent.ErrorWhileSaving -> {
                when (intent.error) {
                    AddressVerificationSavingError.InvalidPostCode -> updateState {
                        copy(showPostcodeError = true)
                    }
                    is AddressVerificationSavingError.Unknown -> updateState {
                        copy(error = AddressVerificationError.Unknown(intent.error.message))
                    }
                }
            }
            AddressVerificationIntent.ManualOverrideClicked -> {
                cancelSearchForAddresses()
                cancelFetchAddressDetails()
                updateState {
                    copy(step = AddressVerificationStep.DETAILS, mainLineInput = modelState.searchInput.text)
                }
            }
            is AddressVerificationIntent.MainLineInputChanged -> {
                updateState { copy(mainLineInput = intent.newInput) }
            }
            is AddressVerificationIntent.SecondLineInputChanged -> {
                updateState { copy(secondLineInput = intent.newInput) }
            }
            is AddressVerificationIntent.CityInputChanged -> {
                updateState { copy(cityInput = intent.newInput) }
            }
            is AddressVerificationIntent.PostCodeInputChanged -> {
                updateState { copy(postCodeInput = intent.newInput, showPostcodeError = false) }
            }
            AddressVerificationIntent.SaveClicked -> {
                updateState { copy(showPostcodeError = false) }

                val addressDetails = AddressDetails(
                    firstLine = modelState.mainLineInput,
                    secondLine = modelState.secondLineInput,
                    city = modelState.cityInput,
                    postCode = modelState.postCodeInput,
                    countryIso = countryIso,
                    stateIso = state?.iSOAbbreviation
                )
                navigate(Navigation.FinishSuccessfully(addressDetails))
            }
            AddressVerificationIntent.BackClicked -> {
                if (modelState.step == AddressVerificationStep.DETAILS) {
                    val searchInput = modelState.searchInput
                    updateState { copy(step = AddressVerificationStep.SEARCH, searchInput = TextFieldValue("")) }
                    onIntent(AddressVerificationIntent.SearchInputChanged(searchInput))
                } else {
                    navigate(Navigation.Back)
                }
            }
        }
    }

    private fun fetchAddressDetails(autocompleteAddress: AutocompleteAddress) {
        updateState {
            copy(
                loadingAddressDetails = autocompleteAddress
            )
        }
        fetchAddressDetailsJob = viewModelScope.launch {
            addressVerificationService.getCompleteAddress(autocompleteAddress.id)
                .doOnSuccess { result ->
                    val mainLine = listOf(
                        result.line1,
                        result.line2,
                        result.line3,
                        result.line4,
                        result.line5
                    ).filterNot { it.isNullOrEmpty() }
                        .joinToString(", ")

                    if (
                        state != null &&
                        // We don't care if result.stateIso is null, it might just be some Loqate issue,
                        // in this case we assume the state is the one that the same as the user's current one.
                        result.provinceCode != null &&
                        state != USState.findStateByCode(result.provinceCode)
                    ) {
                        updateState {
                            copy(
                                loadingAddressDetails = null,
                                showInvalidStateErrorDialog = true
                            )
                        }
                        return@doOnSuccess
                    }

                    updateState {
                        copy(
                            step = AddressVerificationStep.DETAILS,
                            loadingAddressDetails = null,
                            mainLineInput = mainLine.ifEmpty { autocompleteAddress.title },
                            cityInput = result.city.orEmpty(),
                            postCodeInput = result.postalCode.orEmpty(),
                            showPostcodeError = false
                        )
                    }
                }
                .doOnFailure { error ->
                    updateState {
                        copy(
                            loadingAddressDetails = null,
                            error = AddressVerificationError.Unknown(error.localizedMessage)
                        )
                    }
                }
        }
    }

    private fun cancelFetchAddressDetails() {
        fetchAddressDetailsJob?.cancel()
        updateState { copy(loadingAddressDetails = null) }
    }

    private fun searchForAddresses(input: String, containerId: String?) {
        updateState { copy(isSearchLoading = true) }
        searchQueryJob = viewModelScope.launch {
            delay(500) // debounce
            addressVerificationService.getAutocompleteAddresses(input, countryIso, state?.iSOAbbreviation, containerId)
                .doOnSuccess { results ->
                    updateState {
                        copy(
                            isSearchLoading = false,
                            results = results,
                            areResultsHidden = false,
                            showManualOverride = true
                        )
                    }
                }
                .doOnFailure { error ->
                    updateState {
                        copy(
                            isSearchLoading = false,
                            showManualOverride = true,
                            error = AddressVerificationError.Unknown(error.localizedMessage)
                        )
                    }
                }
        }
    }

    private fun cancelSearchForAddresses() {
        searchQueryJob?.cancel()
        updateState { copy(isSearchLoading = false) }
    }
}
