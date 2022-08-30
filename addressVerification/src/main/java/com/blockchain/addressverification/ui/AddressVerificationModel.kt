package com.blockchain.addressverification.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewModelScope
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
import piuk.blockchain.androidcore.utils.extensions.awaitOutcome

sealed class AddressVerificationError {
    object InvalidState : AddressVerificationError()
    data class Unknown(val message: String?) : AddressVerificationError()
}

// Used by the host to communicate back into AddressVerification that the address was not valid for some reason
sealed class AddressVerificationSavingError {
    object InvalidPostCode : AddressVerificationSavingError()
    data class Unknown(val message: String?) : AddressVerificationSavingError()
}

sealed class Navigation : NavigationEvent {
    data class FinishSuccessfully(val address: AddressDetails) : Navigation()
}

@Parcelize
data class Args(
    val countryIso: CountryIso,
    val stateIso: StateIso?,
    val prefilledAddress: AddressDetails?,
) : ModelConfigArgs.ParcelableArgs

class AddressVerificationModel(
    private val interactor: AddressVerificationInteractor
) : MviViewModel<
    AddressVerificationIntent,
    AddressVerificationState,
    AddressVerificationModelState,
    Navigation,
    Args
    >(AddressVerificationModelState()) {

    private lateinit var countryIso: CountryIso
    private var state: USState? = null

    override fun viewCreated(args: Args) {
        countryIso = args.countryIso
        state = args.stateIso?.let { USState.findStateByIso(it) }

        val prefilledAddress = args.prefilledAddress

        if (prefilledAddress != null) {
            updateState {
                it.copy(
                    isShowingStateInput = countryIso == "US",
                    countryInput = Locale("", countryIso).displayCountry,
                    stateInput = state?.displayName.orEmpty(),
                    step = AddressVerificationStep.DETAILS,
                    mainLineInput = TextFieldValue(
                        prefilledAddress.firstLine,
                        TextRange(prefilledAddress.firstLine.length)
                    ),
                    secondLineInput = prefilledAddress.secondLine.orEmpty(),
                    cityInput = prefilledAddress.city,
                    postCodeInput = prefilledAddress.postCode,
                )
            }
        } else {
            updateState {
                it.copy(
                    isShowingStateInput = countryIso == "US",
                    countryInput = Locale("", countryIso).displayCountry,
                    stateInput = state?.displayName.orEmpty(),
                )
            }
        }
    }

    override fun reduce(state: AddressVerificationModelState) = AddressVerificationState(
        step = state.step,
        mainLineInput = state.mainLineInput,
        isSearchLoading = state.isSearchLoading,
        error = state.error,
        areResultsHidden = state.areResultsHidden,
        results = state.results,
        loadingAddressDetails = state.loadingAddressDetails,
        secondLineInput = state.secondLineInput,
        cityInput = state.cityInput,
        isShowingStateInput = state.isShowingStateInput,
        stateInput = state.stateInput,
        showPostcodeError = state.showPostcodeError,
        postCodeInput = state.postCodeInput,
        countryInput = state.countryInput,
        saveButtonState = if (state.isSaveLoading) {
            ButtonState.Loading
        } else if (
            state.mainLineInput.text.isNotBlank() &&
            state.cityInput.isNotBlank() &&
            !(state.isShowingStateInput && state.stateInput.isBlank()) &&
            state.postCodeInput.isNotBlank()
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
            AddressVerificationIntent.ErrorHandled -> updateState { it.copy(error = null) }
            is AddressVerificationIntent.MainLineInputChanged -> {
                // If just the selection changes we just want to update the state and do nothing else
                if (modelState.mainLineInput.text == intent.newInput.text) {
                    updateState { it.copy(mainLineInput = intent.newInput) }
                    return
                }
                cancelSearchForAddresses()
                cancelFetchAddressDetails()

                if (
                    modelState.step == AddressVerificationStep.DETAILS &&
                    modelState.mainLineInput.text != intent.newInput.text
                ) {
                    updateState {
                        it.copy(
                            step = AddressVerificationStep.SEARCH,
                            areResultsHidden = true
                        )
                    }
                }
                // If the user starts erasing characters from the query we remove the container
                val newContainer = modelState.container?.takeIf { container ->
                    intent.newInput.text.startsWith(container.title + " ")
                }
                updateState {
                    it.copy(
                        mainLineInput = intent.newInput,
                        container = newContainer
                    )
                }
                if (intent.newInput.text.length < MIN_QUERY_LENGTH) {
                    updateState { it.copy(areResultsHidden = true, isSearchLoading = false) }
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
                        it.copy(
                            mainLineInput = newInput,
                            container = intent.result
                        )
                    }
                    searchForAddresses(newInput.text, intent.result.id)
                }
            }
            is AddressVerificationIntent.ErrorWhileSaving -> {
                when (intent.error) {
                    AddressVerificationSavingError.InvalidPostCode -> updateState {
                        it.copy(
                            showPostcodeError = true,
                            isSaveLoading = false
                        )
                    }
                    is AddressVerificationSavingError.Unknown -> updateState {
                        it.copy(
                            isSaveLoading = false,
                            error = AddressVerificationError.Unknown(intent.error.message)
                        )
                    }
                }
            }
            AddressVerificationIntent.ManualOverrideClicked -> {
                updateState { it.copy(step = AddressVerificationStep.DETAILS) }
            }
            is AddressVerificationIntent.SecondLineInputChanged -> {
                updateState { it.copy(secondLineInput = intent.newInput) }
            }
            is AddressVerificationIntent.CityInputChanged -> {
                updateState { it.copy(cityInput = intent.newInput) }
            }
            is AddressVerificationIntent.PostCodeInputChanged -> {
                updateState { it.copy(postCodeInput = intent.newInput, showPostcodeError = false) }
            }
            AddressVerificationIntent.SaveClicked -> {
                updateState {
                    it.copy(
                        showPostcodeError = false,
                        // We show a loading as the host will handle any address validations and submissions
                        // We're canceling the loading if we get any errors back via Intent.ErrorWhileSaving
                        isSaveLoading = true,
                    )
                }

                val addressDetails = AddressDetails(
                    firstLine = modelState.mainLineInput.text,
                    secondLine = modelState.secondLineInput,
                    city = modelState.cityInput,
                    postCode = modelState.postCodeInput,
                    countryIso = countryIso,
                    stateIso = state?.iSOAbbreviation,
                )
                navigate(Navigation.FinishSuccessfully(addressDetails))
            }
        }
    }

    private fun fetchAddressDetails(result: AutocompleteAddress) {
        updateState {
            it.copy(
                loadingAddressDetails = result,
            )
        }
        fetchAddressDetailsJob = viewModelScope.launch {
            interactor.getAddressDetails(result.id).awaitOutcome()
                .doOnSuccess { result ->
                    if (
                        state != null &&
                        // We don't care if result.stateIso is null, it might just be some Loqate issue,
                        // in this case we assume the state is the one that the same as the user's current one.
                        result.stateIso != null &&
                        state != USState.findStateByCode(result.stateIso)
                    ) {
                        updateState {
                            it.copy(
                                loadingAddressDetails = null,
                                error = AddressVerificationError.InvalidState,
                            )
                        }
                        return@doOnSuccess
                    }

                    val newInputText = result.address.orEmpty()
                    val newInput = TextFieldValue(text = newInputText, selection = TextRange(newInputText.length))
                    updateState {
                        it.copy(
                            step = AddressVerificationStep.DETAILS,
                            loadingAddressDetails = null,
                            mainLineInput = newInput,
                            cityInput = result.locality.orEmpty(),
                            postCodeInput = result.postalCode.orEmpty(),
                        )
                    }
                }
                .doOnFailure { error ->
                    updateState {
                        it.copy(
                            loadingAddressDetails = null,
                            error = AddressVerificationError.Unknown(error.localizedMessage)
                        )
                    }
                }
        }
    }

    private fun cancelFetchAddressDetails() {
        fetchAddressDetailsJob?.cancel()
        updateState { it.copy(loadingAddressDetails = null) }
    }

    private fun searchForAddresses(input: String, containerId: String?) {
        updateState { it.copy(isSearchLoading = true) }
        searchQueryJob = viewModelScope.launch {
            delay(500) // debounce
            interactor.searchForAddresses(input, countryIso, state?.iSOAbbreviation, containerId).awaitOutcome()
                .doOnSuccess { results ->
                    updateState {
                        it.copy(isSearchLoading = false, results = results, areResultsHidden = false)
                    }
                }
                .doOnFailure { error ->
                    updateState {
                        it.copy(
                            isSearchLoading = false,
                            error = AddressVerificationError.Unknown(error.localizedMessage)
                        )
                    }
                }
        }
    }

    private fun cancelSearchForAddresses() {
        searchQueryJob?.cancel()
        updateState { it.copy(isSearchLoading = false) }
    }
}
