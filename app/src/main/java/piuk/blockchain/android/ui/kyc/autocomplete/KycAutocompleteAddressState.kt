package piuk.blockchain.android.ui.kyc.autocomplete

import piuk.blockchain.android.ui.base.mvi.MviState
import piuk.blockchain.android.ui.kyc.profile.models.AddressDetailsModel

sealed class AutocompleteAddressStep {
    data class Address(val addressDetailsModel: AddressDetailsModel) : AutocompleteAddressStep()
}

enum class AutocompleteAddressToastType {
    ADDRESSES_ERROR, SELECTED_ADDRESS_ERROR
}

data class KycAutocompleteAddressState(
    val autocompleteAddressStep: AutocompleteAddressStep? = null,
    val addresses: List<KycAddressResult> = emptyList(),
    val shouldShowManualButton: Boolean = addresses.isEmpty(),
    val toastType: AutocompleteAddressToastType? = null
) : MviState
