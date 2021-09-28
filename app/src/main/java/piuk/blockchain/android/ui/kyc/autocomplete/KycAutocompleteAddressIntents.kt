package piuk.blockchain.android.ui.kyc.autocomplete

import piuk.blockchain.android.ui.base.mvi.MviIntent
import piuk.blockchain.android.ui.kyc.profile.models.AddressDetailsModel

sealed class KycAutocompleteAddressIntents : MviIntent<KycAutocompleteAddressState> {

    data class SelectAddress(val selectedAddress: KycAddressResult) : KycAutocompleteAddressIntents() {
        override fun reduce(oldState: KycAutocompleteAddressState) = oldState
    }

    data class UpdateSearchText(val addressSearchText: String, val countryCode: String) :
        KycAutocompleteAddressIntents() {
        override fun reduce(oldState: KycAutocompleteAddressState) = oldState
    }

    data class UpdateAddresses(val addresses: List<KycAddressResult>) :
        KycAutocompleteAddressIntents() {
        override fun reduce(oldState: KycAutocompleteAddressState) =
            oldState.copy(addresses = addresses, shouldShowManualButton = addresses.isEmpty())
    }

    data class NavigateToAddress(val addressDetailsModel: AddressDetailsModel) :
        KycAutocompleteAddressIntents() {
        override fun reduce(oldState: KycAutocompleteAddressState) =
            oldState.copy(autocompleteAddressStep = AutocompleteAddressStep.Address(addressDetailsModel))
    }

    object ClearNavigation : KycAutocompleteAddressIntents() {
        override fun reduce(oldState: KycAutocompleteAddressState) =
            oldState.copy(autocompleteAddressStep = null)
    }
}