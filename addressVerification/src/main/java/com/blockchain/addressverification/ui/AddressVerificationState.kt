package com.blockchain.addressverification.ui

import androidx.compose.ui.text.input.TextFieldValue
import com.blockchain.addressverification.domain.model.AutocompleteAddress
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.domain.common.model.StateIso

data class AddressVerificationModelState(
    // Common
    val step: AddressVerificationStep = AddressVerificationStep.SEARCH,
    val searchInput: TextFieldValue = TextFieldValue(""),
    val container: AutocompleteAddress? = null,
    val isSearchLoading: Boolean = false,
    val error: AddressVerificationError? = null,

    // Search
    val areResultsHidden: Boolean = true,
    val showManualOverride: Boolean = false,
    val results: List<AutocompleteAddress> = emptyList(),
    val loadingAddressDetails: AutocompleteAddress? = null,

    // Details
    val mainLineInput: String = "",
    val secondLineInput: String = "",

    val cityInput: String = "",

    val isShowingStateInput: Boolean = false,
    val stateInput: StateIso = "",

    val showPostcodeError: Boolean = false,
    val postCodeInput: String = "",

    val countryInput: String = "",

    val isSaveLoading: Boolean = false,
) : ViewState, ModelState

data class AddressVerificationState(
    // Common
    val step: AddressVerificationStep,
    val searchInput: TextFieldValue,
    val isSearchLoading: Boolean,
    val error: AddressVerificationError?,

    // Search
    val areResultsHidden: Boolean,
    val showManualOverride: Boolean,
    val results: List<AutocompleteAddress>,
    val loadingAddressDetails: AutocompleteAddress?,

    // Details
    val mainLineInput: String,
    val isMainLineInputEnabled: Boolean,
    val secondLineInput: String,

    val cityInput: String,

    val isShowingStateInput: Boolean,
    val stateInput: StateIso,

    val showPostcodeError: Boolean,
    val postCodeInput: String,

    val countryInput: String,

    val saveButtonState: ButtonState,
) : ViewState, ModelState

enum class AddressVerificationStep {
    SEARCH,
    DETAILS,
}
