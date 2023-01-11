package com.blockchain.kycproviders.prove.presentation

import com.blockchain.addressverification.ui.AddressDetails
import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.commonarch.presentation.mvi_v2.ViewState
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.domain.common.model.Seconds
import java.util.Calendar

enum class Screen {
    INTRO,

    WAITING_MOBILE_AUTH_VALIDATION,
    MOBILE_AUTH_DOB_ENTRY,

    INSTANT_LINK_PHONE_AND_DOB_ENTRY,
    WAITING_INSTANT_LINK_VALIDATION,

    WAITING_PREFILL_DATA,

    VIEW_PREFILL_DATA,
    MANUAL_ADDRESS_ENTRY,

    WAITING_PREFILL_DATA_SUBMISSION,
}

sealed class ProveError {
    data class Generic(val message: String?) : ProveError()
    object PossessionVerificationTimeout : ProveError()
}

internal fun Exception.toProveError(): ProveError = ProveError.Generic(message)

data class ProvePrefillModelState(
    val currentScreen: Screen = Screen.INTRO,
    val error: ProveError? = null,

    // Phone and Dob Entry
    val mobileNumberInput: String = "",
    val dateOfBirthInput: Calendar? = null,
    val isStartingInstantLinkAuthLoading: Boolean = false,

    // Waiting Instant Link Validation
    val resendSmsWaitTime: Seconds = 0,

    // Prefill Data
    val prefillFirstNameInput: String = "",
    val prefillLastNameInput: String = "",
    val prefillSelectedAddress: AddressDetails? = null,
    val prefillAddresses: List<AddressDetails> = emptyList(),
    val manualEntryAddress: AddressDetails? = null,
    val isAddressDropdownOpen: Boolean = false,
    val prefillDob: Calendar? = null,
    val prefillMobileNumber: String = "",
) : ModelState

data class ProvePrefillViewState(
    val currentScreen: Screen,
    val error: ProveError?,

    // Phone and Dob Entry
    val mobileNumberInput: String,
    val dateOfBirthInput: Calendar?,
    val possessionDataEntryContinueButtonState: ButtonState,

    // Waiting Instant Link Validation
    val resendSmsButtonState: ButtonState,
    val resendSmsWaitTime: Seconds,

    // Prefill Data
    val prefillFirstNameInput: String,
    val prefillLastNameInput: String,
    val prefillSelectedAddress: AddressDetails?,
    val prefillAddresses: List<AddressDetails>,
    val manualEntryAddress: AddressDetails?,
    val isAddressDropdownOpen: Boolean,
    val prefillDob: Calendar?,
    val prefillMobileNumber: String,
    val prefillContinueButtonState: ButtonState,
) : ViewState
